local math_random = math.random

local MIN_SPEED = 5
local MAX_SPEED = 15
local STEPS_BEFORE_SPEED_CHANGE = 40

local current_step = 0
local current_velocities = {0, 0}

function init()
  math.randomseed(os.time())
  current_step = 0
end

local function go_away_from_robot(robot_angle)
	if(robot_angle > 0) then
		robot.wheels.set_velocity(MAX_SPEED, 0)
	else
		robot.wheels.set_velocity(0, MAX_SPEED)
	end
end

local function move_randomly()
	robot.wheels.set_velocity(current_velocities[1], current_velocities[2])
end

function step() -- moves randomly but keeps away from other robots like this
  robot.range_and_bearing.set_data(1,1) -- notifying main robot
  robot.range_and_bearing.set_data(2,1) -- notifying other random robot to stay away
  if((current_step % STEPS_BEFORE_SPEED_CHANGE) == 0) then
    current_velocities = {math_random(MIN_SPEED, MAX_SPEED), math_random(MIN_SPEED, MAX_SPEED)}
  end
  current_step = current_step + 1
	for robot_index=1, #robot.range_and_bearing do
		if(robot.range_and_bearing[robot_index].data[2] == 1) then
		 go_away_from_robot(robot.range_and_bearing[robot_index].horizontal_bearing)
     	 return
		end
	end
  move_randomly()
end