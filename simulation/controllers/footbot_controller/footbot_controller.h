#ifndef FOOTBOT_CONTROLLER_H
#define FOOTBOT_CONTROLLER_H


#include <argos3/core/control_interface/ci_controller.h>
#include <argos3/plugins/robots/generic/control_interface/ci_differential_steering_actuator.h>
#include <argos3/plugins/robots/foot-bot/control_interface/ci_footbot_proximity_sensor.h>
#include <argos3/plugins/robots/generic/control_interface/ci_positioning_sensor.h>
#include <argos3/plugins/robots/generic/control_interface/ci_leds_actuator.h>
#include "json.hpp"
#include "bn_io.h"
#include "bn.h"

using namespace argos;

class CFootBotBn : public CCI_Controller {

public:
   CFootBotBn();
   ~CFootBotBn();
   void Init(TConfigurationNode& t_node);
   void ControlStep();
   void Reset();
   void Destroy();

private:

   void PrintAnalytics(bool printBnSchema);
   void RunAndEvaluateNetwork();

   Real bestNetworkFitness, testNetworkFitness;
   long printStep, currentStep;
   
   Bn* bestBn;
   Bn* testBn;
   BnIO* bestIO;
   BnIO* testIO;

   bool stayUpper;
   int myId;

   CCI_DifferentialSteeringActuator* m_pcWheels;
   CCI_FootBotProximitySensor* m_pcProximity;
   CCI_PositioningSensor* m_pcPositioning;
   CCI_LEDsActuator* m_pcLEDs;
};

#endif
