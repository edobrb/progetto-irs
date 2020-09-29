require("utilities")
require "libs.lua-collections"
require("boolean-networks")
local editing = require("boolean-network-editing")
local argos = require("argos-robot")
local pprint = require('libs.pprint')

local collect = collect
local my_if = my_if
local bool_to_int = bool_to_int
local math_sqrt = math.sqrt
local math_abs = math.abs

local best_network, test_network ---@type BooleanNetwork
local best_network_fitness = -1
local test_network_fitness = 0
local current_step = 0
local current_edit_attempt = 1
local fitness_results = {}
local saved_network_states = {{}, {}} --contains the history of network states of the first network and the last

local TICKS_PER_SECOND = 100
local SPEED = 1.5 * TICKS_PER_SECOND
local PROXIMITY_THRESHOLD = 0.1
local RAB_MINIMUM_DISTANCE = 5 -- in cm. Set to zero to ignore the min distance to the prey robot, 5 otherwise
local NETWORK_TEST_STEPS = 1200
local EDIT_ATTEMPTS_COUNT = 7200 * TICKS_PER_SECOND / NETWORK_TEST_STEPS -- the 1st factor has to be == to the experiment's length
local PRINT_MIDWAY_RESULTS = true
-- values to change for testing:
local MAX_INPUT_REWIRES = 1
local MAX_OUTPUT_REWIRES = 0 -- don't do more than 2
local MAX_BIT_FLIPS = 0
local MAX_NODE_INPUT_SWAPS = 0
local MAX_BRACCINI_EDITS = 0
local RANGE_AND_BEARING_COUNT = 0 -- disable range and bearing sensors by putting this to 0. Enable by setting 6.
local RANDOMIZE_NETWORK = false
local FOLLOW_OTHER_BOT = true
local INCREMENTAL_LEARNING = false
local USE_DUAL_ENCODING = false -- if true: obstacles are encoded as false and false values will turn on the wheels
local network_options = { ["node_count"] = 300, ["nodes_input_count"] = 6, ["bias"] =
 								0.79, --script edited
                          ["network_inputs_count"] = 12 + RANGE_AND_BEARING_COUNT, ["network_outputs_count"] = 2,
                          ["self_loops"] = false, ["override_output_nodes_bias"] = true}

function init()
    test_network = BooleanNetwork(network_options)
    seed = math.floor(os.clock() * 10000000)
    print(seed)
    math.randomseed(seed)
    best_network = test_network
end

local function get_fitness(follow_part, obstacle_avoidance_part)
    local compound_fitness = (1/3*obstacle_avoidance_part + 2/3*follow_part)
    if(not FOLLOW_OTHER_BOT) then return obstacle_avoidance_part end
    local incremental_fitness =
        my_if(current_edit_attempt > EDIT_ATTEMPTS_COUNT/2, compound_fitness, obstacle_avoidance_part/3)
    return my_if(INCREMENTAL_LEARNING, incremental_fitness, compound_fitness)
end

local function fitness_function(values, distance_to_prey, maximum_simulation_steps)
    local network_outputs, proximity_values, range_and_bearing_values = values[1], values[2], values[3]
    local left_wheel, right_wheel = bool_to_int(network_outputs[1]), bool_to_int(network_outputs[2])
    local obstacle_avoidance_part = (1 - collect(proximity_values):max()) *
            (1 - math_sqrt(math_abs(left_wheel - right_wheel))) * (left_wheel + right_wheel) / 2
    local follow_part = bool_to_int(collect(range_and_bearing_values):contains(true)) *
            bool_to_int(distance_to_prey >= RAB_MINIMUM_DISTANCE)
    return get_fitness(follow_part, obstacle_avoidance_part) * (100 / maximum_simulation_steps)
end

---@param network BooleanNetwork
---@param proximity_threshold number
---@param speed number
local function run_and_evaluate_network(network, maximum_simulation_steps, speed, proximity_threshold)
    local proximity_values = argos.get_proximity_values(#network.input_nodes - RANGE_AND_BEARING_COUNT)
    local inputs = argos.sensor_values_to_booleans(proximity_values, proximity_threshold, USE_DUAL_ENCODING)
    local range_and_bearing_values = {} ---@type boolean[]
    local distance_to_prey = 0
    if(RANGE_AND_BEARING_COUNT > 0) then
        range_and_bearing_values, distance_to_prey = argos.get_RAB_values_and_distance(RANGE_AND_BEARING_COUNT)
        inputs = collect(inputs):addAll(range_and_bearing_values):all()
    end
    network:force_input_values(inputs)
    local network_outputs = collect(network:update_and_get_outputs())
            :mapValues(function (value) return my_if(USE_DUAL_ENCODING, not value, value) end):all()
    argos.move_robot_by_booleans(network_outputs, speed)
    local values = {network_outputs, proximity_values, range_and_bearing_values}
    return fitness_function(values, distance_to_prey, maximum_simulation_steps)
end

---@param previous_network BooleanNetwork
local function build_new_network(previous_network)
    local new_network = previous_network
    new_network = editing.edit_network(new_network, MAX_INPUT_REWIRES, function (network, edit_count)
        return editing.rewire_inputs_or_outputs(network, edit_count, true)
    end)
    new_network = editing.edit_network(new_network, MAX_OUTPUT_REWIRES, function (network, edit_count)
        return editing.rewire_inputs_or_outputs(network, edit_count, false)
    end)
    new_network = editing.edit_network(new_network, MAX_BIT_FLIPS, editing.flip_boolean_functions)
    new_network = editing.edit_network(new_network, MAX_NODE_INPUT_SWAPS, function (network, edit_count)
        return editing.change_nodes_connections(network, edit_count, network_options["self_loops"])
    end)
    new_network = editing.edit_network(new_network, bool_to_int(RANDOMIZE_NETWORK),
            function (_, _) return BooleanNetwork(network_options) end)
    new_network = editing.edit_network(new_network, MAX_BRACCINI_EDITS, editing.edit_by_braccini_scheme)
    return new_network
end

---@param current_best_network BooleanNetwork
---@param tested_network BooleanNetwork
---@param best_fitness number
---@param test_fitness number
local function update_networks_and_best_fitness(current_best_network, tested_network, best_fitness, test_fitness)
    if(test_fitness > best_fitness) then
        current_best_network = tested_network
        best_fitness = test_fitness
        if(PRINT_MIDWAY_RESULTS) then print("found better network with fitness = " .. best_fitness) end
    end
    return current_best_network, build_new_network(current_best_network), best_fitness
end

local function update_saved_states()
    if((current_edit_attempt == 1) or (current_edit_attempt == EDIT_ATTEMPTS_COUNT)) then
        saved_network_states[math.min(current_edit_attempt, 2)][current_step] =
        collect(best_network.node_states):mapValues(bool_to_int):all()
    end
end

function step()
    if(current_step < NETWORK_TEST_STEPS-1) then
        current_step = current_step + 1
        update_saved_states()
        test_network_fitness = test_network_fitness +
                run_and_evaluate_network(test_network, NETWORK_TEST_STEPS, SPEED, PROXIMITY_THRESHOLD)
    else
        if(PRINT_MIDWAY_RESULTS) then print("tested network connection has fitness = " .. test_network_fitness) end
        current_edit_attempt = current_edit_attempt + 1
        table.insert(fitness_results, test_network_fitness)
        best_network, test_network, best_network_fitness =
            update_networks_and_best_fitness(best_network, test_network, best_network_fitness, test_network_fitness)
        current_step, test_network_fitness = 0, 0
        if(PRINT_MIDWAY_RESULTS) then print("testing new boolean network connection \n") end
    end
end

function destroy()
    if(best_network_fitness ~= -1) then -- sometimes destroy gets called too soon, so this solves the problem
        print("Best: " .. best_network_fitness .. " size: " .. best_network:get_node_count()  .. " network: " ..
                one_line_serialize(best_network) .. "results: " .. one_line_serialize(fitness_results) ..
                " startStates: " .. one_line_serialize(saved_network_states[1]) ..
                " finalStates: " .. one_line_serialize(saved_network_states[2]))
    end
end