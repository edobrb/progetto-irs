local math_random = math.random

local current_step = 0
local current_velocities = {0, 0}

local MIN_SPEED = 5
local MAX_SPEED = 15
local STEPS_BEFORE_SPEED_CHANGE = 40

function init()
  math.randomseed(os.time())
  current_step = 0
end

function step()
  if((current_step % STEPS_BEFORE_SPEED_CHANGE) == 0) then
    current_velocities = {math_random(MIN_SPEED, MAX_SPEED), math_random(MIN_SPEED, MAX_SPEED)}
  end
  current_step = current_step + 1
  robot.wheels.set_velocity(current_velocities[1], current_velocities[2])
  robot.range_and_bearing.set_data(1,1)
end