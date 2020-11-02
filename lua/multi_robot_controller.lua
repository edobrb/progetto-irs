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
local print_step = 0

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
local FEED_POSITION = config.robot.feed_position
local stay_upper = true

-- BN parameters
local MAX_INPUT_REWIRES = config.bn.max_input_rewires
local INPUT_REWIRES_PROBABILITY = config.bn.input_rewires_probability
local MAX_OUTPUT_REWIRES = config.bn.max_output_rewires
local OUTPUT_REWIRES_PROBABILITY = config.bn.output_rewires_probability
local USE_DUAL_ENCODING = config.bn.use_dual_encoding -- if true the obstacles are encoded as false and the wheels will turn on on false value
local NETWORK_OPTIONS = config.bn.options

function init()
    math.randomseed(math.floor(os.clock() * 10000000)) -- each robot will have a different seed

    if STAY_ON_HALF and FEED_POSITION then
        NETWORK_OPTIONS.network_inputs_count = NETWORK_OPTIONS.network_inputs_count + 1
    end
    test_network = BooleanNetwork(NETWORK_OPTIONS)

    if config.bn.initial ~= nil then
        test_network.boolean_functions = config.bn.initial.functions
        test_network.connection_matrix = config.bn.initial.connections
        test_network.input_nodes = config.bn.initial.inputs
        test_network.output_nodes = config.bn.initial.outputs
        test_network.overridden_output_functions = config.bn.initial.overridden_output_functions
    end

    best_network = test_network
    
    if STAY_ON_HALF then
        stay_upper = argos.is_upper()
        robot.leds.set_all_colors(my_if(stay_upper, "yellow", "green"))
    end
end

---@param network_outputs boolean[]
---@param proximity_values number[]
local function fitness_function(network_outputs, proximity_values)
    if STAY_ON_HALF and stay_upper ~= argos.is_upper() then
        return 0
    else
        local left_wheel, right_wheel = bool_to_int(network_outputs[1]), bool_to_int(network_outputs[2])
        local obstacle_avoidance_fitness = (1 - collect(proximity_values):max()) * (1 - math.sqrt(math.abs(left_wheel - right_wheel))) * (left_wheel + right_wheel) / 2
        return obstacle_avoidance_fitness * (100 / NETWORK_TEST_STEPS)
    end
end

---@param network BooleanNetwork
local function run_and_evaluate_test_network()
    local proximity_input_nodes_count = #test_network.input_nodes
    if STAY_ON_HALF and FEED_POSITION then
        proximity_input_nodes_count = proximity_input_nodes_count - 1
    end
    local proximity_values = argos.get_proximity_values(proximity_input_nodes_count)
    local network_inputs = argos.sensor_values_to_booleans(proximity_values, PROXIMITY_THRESHOLD, USE_DUAL_ENCODING)
    test_network:force_input_values(network_inputs)
    if STAY_ON_HALF and FEED_POSITION then
        test_network:force_input_value(stay_upper == argos.is_upper(), #test_network.input_nodes)
    end
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

function step()
    if(current_step >= NETWORK_TEST_STEPS) then
        if (PRINT_ANALYTICS) then print_network_state(test_network) end
        if(test_network_fitness >= best_network_fitness) then
            best_network = test_network
            best_network_fitness = test_network_fitness
        end
        test_network = build_new_network(best_network) -- TODO: check if not equals to previous
        current_step, test_network_fitness = 0, 0
    end


    if (current_step == 0 and PRINT_ANALYTICS) then print_network(test_network)
    elseif (PRINT_ANALYTICS) then print_network_state(test_network) end

    test_network_fitness = test_network_fitness + run_and_evaluate_test_network()
    current_step = current_step + 1
end

function destroy()
    if(best_network_fitness ~= -1) then -- sometimes destroy gets called too soon, so this solves the problem (called too soon when argos fail to place the robot, destroy and retry to replace)
        if(PRINT_ANALYTICS) then print_network_state(test_network) end
    end
end

function print_network(netowrk)
    local table = {
         id = robot.id, 
         step = print_step,
         fitness = test_network_fitness,
         boolean_network = {
            functions = netowrk.boolean_functions, 
            connections = netowrk.connection_matrix,
            inputs = netowrk.input_nodes,
            outputs = netowrk.output_nodes,
            overridden_output_functions = netowrk.overridden_output_functions
         },
         states = netowrk.node_states,
         --proximity = argos.get_proximity_values(24)
         --orientation = robot.positioning.orientation.z,
         position = {robot.positioning.position.x, robot.positioning.position.y}
        }
    print_step = print_step + 1
    local res = json.encode(table)
    print(res)
    io.flush()
end

function print_network_state(netowrk)
    local table = {
         id = robot.id, 
         step = print_step,
         fitness = test_network_fitness,
         states = netowrk.node_states,
         position = {robot.positioning.position.x, robot.positioning.position.y}
        }
    print_step = print_step + 1
    local res = json.encode(table)
    print(res)
    io.flush()
end