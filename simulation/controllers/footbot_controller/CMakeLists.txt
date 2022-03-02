add_library(footbot_controller MODULE footbot_controller.h footbot_controller.cpp bn.h bn.cpp bn_io.h bn_io.cpp) 

target_link_libraries(footbot_controller
  argos3core_simulator
  argos3plugin_simulator_footbot
  argos3plugin_simulator_genericrobot)