require("utilities")
require "libs.lua-collections"
local l = require("libs.lambda")
local pprint = require('libs.pprint')

local argos = {}

-- localizing global functions to increase performance
local bool_to_int = bool_to_int
local collect = collect
local math_ceil = math.ceil

local PROXIMITY_SENSORS_COUNT = 24
local PROXIMITY_SENSORS = collect(range(1, PROXIMITY_SENSORS_COUNT))

---@param output boolean
---@param speed number
---@return number
local function convert_output(output, speed)
    return bool_to_int(output)*speed
end

---@param boolean_outputs boolean[]
---@param speed number
function argos.move_robot_by_booleans(boolean_outputs, speed)
    robot.wheels.set_velocity(convert_output(boolean_outputs[1], speed), convert_output(boolean_outputs[2], speed))
end

---@param value_count_to_return number
---@return number[]
function argos.get_proximity_values(value_count_to_return)
    local sensor_values =
        PROXIMITY_SENSORS:mapValues(function (input_index) return robot.proximity[input_index].value end)
    if(value_count_to_return == PROXIMITY_SENSORS_COUNT) then return sensor_values:all() end

    return sensor_values:grouped(PROXIMITY_SENSORS_COUNT / value_count_to_return)
            :mapValues(function (group) return collect(group):max() end):all()
end

---@param value_count_to_return number
---@param maximum_range number
---@return boolean[], number
function argos.get_RAB_values_and_distance(value_count_to_return) -- !! only detects one robot
    local results = fill_array(value_count_to_return, false)
    local message_index = collect(range(1, #robot.range_and_bearing))
            :firstValue(function(index) return robot.range_and_bearing[index].data[1]==1 end) ---@type: number
    if(message_index == nil) then return results, -1 end
    local angle = robot.range_and_bearing[message_index].horizontal_bearing + 180
    local sensor_index = math_ceil(angle / (360/value_count_to_return))
    results[sensor_index] = true
    return results, robot.range_and_bearing[message_index].range
end

---@param values number[]
---@param threshold number
---@param invert_values boolean
---@return boolean[]
function argos.sensor_values_to_booleans(values, threshold, invert_values)
    return collect(values):mapValues(function (proximity)
        if(invert_values) then
            return proximity <= threshold
        else
            return proximity > threshold
        end
    end):all()
end

return argos