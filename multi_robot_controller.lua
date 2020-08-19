require("utilities")
require("libs.lua-collections")
require("boolean-networks")
local json = require("libs.json")

-- utilities
local editing = require("boolean-network-editing") -- BN utility editing module
local argos = require("argos-robot") -- robot utility wrapper
local pretty_print = require('libs.pprint') --for debug purpose
local collect = collect
local ternary = my_if
local bool_to_int = bool_to_int

-- local variables
local best_network, test_network ---@type BooleanNetwork
local best_network_fitness = -1
local test_network_fitness = 0
local current_step = 0
local global_step = 0
local current_edit_attempt = 1
local fitness_results = {} -- contains the fintess values for each edit (#EDIT_ATTEMPTS_COUNT)
local saved_network_states = {{}, {}} --contains the history of network states of the first network and the last

-- test parameters
local config = json.decode(argos.param("CONFIG"))
local TICKS_PER_SECOND = config.simulation.ticks_per_seconds
local EXPERIMENT_LENGTH = config.simulation.experiment_length
local NETWORK_TEST_STEPS = config.simulation.network_test_steps
local EDIT_ATTEMPTS_COUNT = EXPERIMENT_LENGTH * TICKS_PER_SECOND / NETWORK_TEST_STEPS -- the 1st factor has to be == to the experiment's length
local PRINT_ANALYTICS = config.simulation.print_analytics

-- robot parameters
local PROXIMITY_THRESHOLD = config.robot.proximity_threshold
local MAX_WHEELS_SPEED = config.robot.max_wheel_speed * TICKS_PER_SECOND
local STAY_ON_HALF = config.robot.stay_on_half
local stay_upper = true

-- BN parameters
local MAX_INPUT_REWIRES = config.bn.max_input_rewires
local INPUT_REWIRES_PROBABILITY = config.bn.input_rewires_probability
local MAX_OUTPUT_REWIRES = config.bn.max_output_rewires
local OUTPUT_REWIRES_PROBABILITY = config.bn.output_rewires_probability
local USE_DUAL_ENCODING = config.bn.use_dual_encoding -- if true the obstacles are encoded as false and the wheels will turn on on false value
local NETWORK_OPTIONS = config.bn.options

function init()
    if config.bn.initial ~= nil then 
        test_network = BooleanNetwork(NETWORK_OPTIONS)
        test_network.boolean_functions = config.bn.initial.functions
        test_network.connection_matrix = config.bn.initial.connections
        test_network.input_nodes = config.bn.initial.inputs
        test_network.output_nodes = config.bn.initial.outputs
        test_network.overridden_output_functions = config.bn.initial.overridden_output_functions
    else 
        test_network = BooleanNetwork(NETWORK_OPTIONS)
    end

    best_network = test_network
    math.randomseed(math.floor(os.clock() * 10000000)) -- each robot will have a different seed

    if STAY_ON_HALF then
        stay_upper = robot.positioning.position.x > 0
        robot.leds.set_all_colors(my_if(stay_upper, "green", "red")) 
    end
end

local function is_upper()
    return robot.positioning.position.x > 0
end
---@param network_outputs boolean[]
---@param proximity_values number[]
local function fitness_function(network_outputs, proximity_values)
    local left_wheel, right_wheel = bool_to_int(network_outputs[1]), bool_to_int(network_outputs[2])
    local obstacle_avoidance_fitness = (1 - collect(proximity_values):max()) * (1 - math.sqrt(math.abs(left_wheel - right_wheel))) * (left_wheel + right_wheel) / 2
    return obstacle_avoidance_fitness * (100 / NETWORK_TEST_STEPS) * my_if((STAY_ON_HALF == false) or (stay_upper == is_upper()), 1, 0)
end

---@param network BooleanNetwork
local function run_and_evaluate_test_network()
    local proximity_values = argos.get_proximity_values(#test_network.input_nodes)
    local network_inputs = argos.sensor_values_to_booleans(proximity_values, PROXIMITY_THRESHOLD, USE_DUAL_ENCODING)
    test_network:force_input_values(network_inputs)
    local network_outputs = collect(test_network:update_and_get_outputs()):mapValues(function (value) return ternary(USE_DUAL_ENCODING, not value, value) end):all()
    argos.move_robot_by_booleans(network_outputs, MAX_WHEELS_SPEED)
    return fitness_function(network_outputs, proximity_values)
end

---@param previous_network BooleanNetwork
local function build_new_network(previous_network)
    local new_network = previous_network

    local input_rewires = 0
    local output_rewires = 0
    while(input_rewires == 0 and output_rewires == 0) do --force to do at least one change
        input_rewires = count_winner_extractions(MAX_INPUT_REWIRES, INPUT_REWIRES_PROBABILITY)
        output_rewires = count_winner_extractions(MAX_OUTPUT_REWIRES, OUTPUT_REWIRES_PROBABILITY)
    end
    new_network = editing.edit_network(new_network, input_rewires, function (network, edit_count) return editing.rewire_inputs_or_outputs(network, edit_count, true) end)
    new_network = editing.edit_network(new_network, output_rewires, function (network, edit_count) return editing.rewire_inputs_or_outputs(network, edit_count, false) end)
    return new_network
end

-- if it's the first or last BN configuration: save it's state in saved_network_states
local function update_saved_states()
    if((current_edit_attempt == 1) or (current_edit_attempt == EDIT_ATTEMPTS_COUNT)) then
        saved_network_states[math.min(current_edit_attempt, 2)][current_step] =
        collect(best_network.node_states):mapValues(bool_to_int):all()
    end
end

function step()
    if (global_step == 0 and PRINT_ANALYTICS) then print_network(test_network) end

    global_step = global_step + 1
    if(current_step < NETWORK_TEST_STEPS) then
        current_step = current_step + 1
        update_saved_states()
        test_network_fitness = test_network_fitness + run_and_evaluate_test_network()
        if(PRINT_ANALYTICS and current_step < NETWORK_TEST_STEPS) then print_network_state(test_network) end
        if(PRINT_ANALYTICS and current_step >= NETWORK_TEST_STEPS) then print_network(test_network) end
    else
        current_edit_attempt = current_edit_attempt + 1
        table.insert(fitness_results, test_network_fitness)
        if(test_network_fitness >= best_network_fitness) then
            best_network = test_network
            best_network_fitness = test_network_fitness
        end
        test_network = build_new_network(best_network)
        current_step, test_network_fitness = 0, 0
        if(PRINT_ANALYTICS) then print_network(test_network) end
    end

end

function destroy()
    if(best_network_fitness ~= -1) then -- sometimes destroy gets called too soon, so this solves the problem (called too soon when argos fail to place the robot, destroy and retry to replace)
        if(PRINT_ANALYTICS) then print_network(test_network) end
    end
end

function print_network(netowrk)
    local table = {
         id = robot.id, 
         step = global_step,
         fitness = test_network_fitness,
         boolean_network = {
            functions = netowrk.boolean_functions, 
            connections = netowrk.connection_matrix,
            inputs = netowrk.input_nodes,
            outputs = netowrk.output_nodes,
            overridden_output_functions = netowrk.overridden_output_functions
         },
         states = netowrk.node_states
         --proximity = argos.get_proximity_values(24)
        }
    local res = json.encode(table)
    print(res)
end

function print_network_state(netowrk)
    local table = {
         id = robot.id, 
         step = global_step,
         fitness = test_network_fitness,
         states = netowrk.node_states
         --proximity = argos.get_proximity_values(24) --include robot position?
        }
    local res = json.encode(table)
    print(res)
end