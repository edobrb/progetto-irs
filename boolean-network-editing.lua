require("utilities")
require "libs.lua-collections"
require("boolean-networks")
local l = require("libs.lambda")
local pprint = require('libs.pprint')

local editing = {}

---@param network BooleanNetwork
---@param edits_count number
---@param network_producer fun(network: BooleanNetwork, edit_count: number): BooleanNetwork
---@return BooleanNetwork
function editing.edit_network(network, edits_count, network_producer)
    if(edits_count > 0) then
        return network_producer(network, math.random(1, edits_count))
    end
    return network
end

---@param network_to_edit BooleanNetwork
---@param nodes_amount_to_rewire number
---@param rewire_inputs boolean
---@return BooleanNetwork
function editing.rewire_inputs_or_outputs(network_to_edit, nodes_amount_to_rewire, rewire_inputs)
    local new_network = clone_object(network_to_edit) ---@type BooleanNetwork
    local nodes_to_rewire = my_if(rewire_inputs, new_network.input_nodes, new_network.output_nodes) ---@type number[]
    local indexes_of_nodes_to_change =
    random_extract(nodes_amount_to_rewire, 1, #nodes_to_rewire)
    local new_nodes = collect(range(1, #new_network.node_states))
            :diff(new_network.output_nodes):diff(new_network.input_nodes)
            :random(nodes_amount_to_rewire):all()
    local nodes = replace_in_array(nodes_to_rewire, indexes_of_nodes_to_change, new_nodes)
    if(rewire_inputs) then new_network.input_nodes = nodes else new_network.output_nodes = nodes end
    return new_network
end

---@param network BooleanNetwork
---@return boolean[][], number
local function get_merged_functions_and_cell_count(network) -- merging boolean functions with the output overridden ones
    local cells_count = get_fixed_matrix_cells_count(network.boolean_functions)
    local matrix_to_edit = network.boolean_functions
    if(network:has_overridden_output_bias()) then
        cells_count = cells_count + get_fixed_matrix_cells_count(network.overridden_output_functions)
        matrix_to_edit = collect(matrix_to_edit):merge(network.overridden_output_functions):all()
    end
    return matrix_to_edit, cells_count
end

local function get_flipped_booleans(matrix, indexes_2D_to_flip)
    return collect(indexes_2D_to_flip):mapValues(function (cell_position)
        return not matrix[cell_position[1]][cell_position[2]]
    end):all()
end

---@param network_to_edit BooleanNetwork
---@param flip_amount number
---@return BooleanNetwork
function editing.flip_boolean_functions(network_to_edit, flip_amount)
    local new_network = clone_object(network_to_edit) ---@type BooleanNetwork
    local matrix_to_edit, cells_count = get_merged_functions_and_cell_count(new_network)
    local indexes_1D_to_flip = random_extract(flip_amount, 1, cells_count)
    local indexes_2D_to_flip = collect(indexes_1D_to_flip)
            :mapValues(function (index) return from_1D_to_2D_index(index, matrix_to_edit) end):all()
    local flipped_values = get_flipped_booleans(matrix_to_edit, indexes_2D_to_flip)
    local edited_matrix = replace_in_matrix(matrix_to_edit, indexes_2D_to_flip, flipped_values)
    local split_matrix = collect(edited_matrix):splitAt(new_network:get_node_count()):all()
    new_network.boolean_functions = split_matrix[1]
    local override_functions = my_if(#split_matrix[2] > 0, split_matrix[2], nil) ---@type boolean[][]|nil
    new_network.overridden_output_functions = override_functions
    return new_network
end

---@param change_amount number
---@param network BooleanNetwork
local function get_new_input_nodes(network, nodes_to_edit, allow_self_loops, change_amount)
    local new_input_nodes = random_extract(change_amount, 1, network:get_node_count(), false)
    local nodes_with_new_inputs = collect(nodes_to_edit):zip(new_input_nodes):all()
    if(not allow_self_loops) then
        local network_nodes = range(1, network:get_node_count())
        for index, node_with_input in ipairs(nodes_with_new_inputs) do
            if(node_with_input[1] == node_with_input[2]) then --self loop
                new_input_nodes[index] =
                    random_extract_except(1, network_nodes, {node_with_input[1]})[1]
            end
        end
    end
    return new_input_nodes
end

---@param network_to_edit BooleanNetwork
---@param change_amount number
---@param allow_self_loops boolean
---@return BooleanNetwork
function editing.change_nodes_connections(network_to_edit, change_amount, allow_self_loops) -- keeps the same input count per node
    local new_network = clone_object(network_to_edit) ---@type BooleanNetwork
    local nodes_to_edit =
        random_extract(change_amount, 1, new_network:get_node_count(), false)
    local nodes_input_count = new_network:get_nodes_input_count()
    local input_indexes_to_edit = random_extract(change_amount, 1, nodes_input_count, true)
    local positions_2D = collect(nodes_to_edit):zip(input_indexes_to_edit):all() ---@type: number[][]
    local new_input_nodes = get_new_input_nodes(new_network, nodes_to_edit, allow_self_loops, change_amount)
    new_network.connection_matrix = replace_in_matrix(new_network.connection_matrix, positions_2D, new_input_nodes)
    return new_network
end

---@return number
---@param average number
local function extract_poisson_random(average) -- uses the Knuth algorithm.
    local l_value, k_value, p_value = math.exp(-average), 0, 1
    repeat
        k_value = k_value + 1
        p_value = p_value * math.random()
    until not (p_value > l_value)
    return k_value - 1
end

---@param network_to_edit BooleanNetwork
local function add_node_to_network(network_to_edit)
    local node_count = network_to_edit:get_node_count()
    local nodes_input_count = network_to_edit:get_nodes_input_count()
    local output_nodes = random_extract(extract_poisson_random(nodes_input_count) , 1, node_count)
    local inputs_to_change = random_extract(#output_nodes, 1, nodes_input_count, true)
    local positions_2D = collect(output_nodes):zip(inputs_to_change):all()  ---@type: number[][]
    local new_input_nodes = fill_array(#output_nodes, node_count+1)
    network_to_edit.node_states[node_count+1] = biased_random_boolean(0.5)
    network_to_edit.connection_matrix[node_count+1] = random_extract(nodes_input_count, 1, node_count)
    network_to_edit.boolean_functions[node_count+1] = network_to_edit.boolean_functions[math.random(1, node_count)]
    replace_in_matrix(network_to_edit.connection_matrix, positions_2D, new_input_nodes)
end

---@param network_to_edit BooleanNetwork
---@param change_amount number
---@return BooleanNetwork
function editing.add_nodes_to_network(network_to_edit, change_amount)
    local new_network = clone_object(network_to_edit) ---@type BooleanNetwork
    for _=1,change_amount do add_node_to_network(new_network) end
    return new_network
end

---@param network_to_edit BooleanNetwork
---@param change_amount number
function editing.edit_by_braccini_scheme(network_to_edit, change_amount)
    local probability = math.random() -- in the range [0,1)
    if(probability < 2/3) then
        return editing.rewire_inputs_or_outputs(network_to_edit, change_amount, probability < 1/3)
    else
        return editing.add_nodes_to_network(network_to_edit, change_amount)
    end
end

return editing