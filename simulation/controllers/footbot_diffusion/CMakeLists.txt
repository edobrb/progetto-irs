add_library(footbot_diffusion MODULE footbot_diffusion.h footbot_diffusion.cpp bn.h bn.cpp bn_io.h bn_io.cpp) 

target_link_libraries(footbot_diffusion
  argos3core_simulator
  argos3plugin_simulator_footbot
  argos3plugin_simulator_genericrobot)