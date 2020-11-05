/* Include the controller definition */
#include "footbot_diffusion.h"
/* Function definitions for XML parsing */
#include <argos3/core/utility/configuration/argos_configuration.h>
/* 2D vector definition */
#include <argos3/core/utility/math/vector2.h>
#include "json.hpp"
#include <iostream>
#include <cmath>
#include "bn.h"

/****************************************/
/****************************************/

CFootBotDiffusion::CFootBotDiffusion() :
   bestNetworkFitness(-1),
   testNetworkFitness(0),
   currentStep(0),
   printStep(0),
   m_pcWheels(NULL),
   m_pcProximity(NULL),
   m_pcPositioning(NULL),
   m_pcLEDs(NULL),
   m_cAlpha(10.0f),
   m_fDelta(0.5f),
   m_fWheelVelocity(2.5f),
   bestBn(NULL),
   currentBn(NULL),
   m_cGoStraightAngleRange(-ToRadians(m_cAlpha),
                           ToRadians(m_cAlpha)) {}

/****************************************/
/****************************************/

int TICKS_PER_SECOND;
int EXPERIMENT_LENGTH;
int NETWORK_TEST_STEPS;
bool PRINT_ANALYTICS;

Real PROXIMITY_THRESHOLD;
Real MAX_WHEELS_SPEED;
bool STAY_ON_HALF;
bool FEED_POSITION;
bool stay_upper = true;

int MAX_INPUT_REWIRES;
Real INPUT_REWIRES_PROBABILITY;
int MAX_OUTPUT_REWIRES;
Real OUTPUT_REWIRES_PROBABILITY;
//bool USE_DUAL_ENCODING
//local NETWORK_OPTIONS;

void CFootBotDiffusion::Init(TConfigurationNode& t_node) {
   /*
    * Get sensor/actuator handles
    *
    * The passed string (ex. "differential_steering") corresponds to the
    * XML tag of the device whose handle we want to have. For a list of
    * allowed values, type at the command prompt:
    *
    * $ argos3 -q actuators
    *
    * to have a list of all the possible actuators, or
    *
    * $ argos3 -q sensors
    *
    * to have a list of all the possible sensors.
    *
    * NOTE: ARGoS creates and initializes actuators and sensors
    * internally, on the basis of the lists provided the configuration
    * file at the <controllers><footbot_diffusion><actuators> and
    * <controllers><footbot_diffusion><sensors> sections. If you forgot to
    * list a device in the XML and then you request it here, an error
    * occurs.
    */
   m_pcWheels = GetActuator<CCI_DifferentialSteeringActuator>("differential_steering");
   m_pcProximity = GetSensor<CCI_FootBotProximitySensor>("footbot_proximity");
   m_pcPositioning = GetSensor<CCI_PositioningSensor>("positioning");
   m_pcLEDs = GetActuator<CCI_LEDsActuator>("leds");
   m_pcLEDs->SetAllColors(CColor::RED);

   /*
    * Parse the configuration json
    */
   std::string configStr = "null";
   GetNodeAttributeOrDefault(t_node, "CONFIG", configStr, configStr);
   config = nlohmann::json::parse(configStr);

   TICKS_PER_SECOND = config["simulation"]["ticks_per_seconds"].get<int>();
   EXPERIMENT_LENGTH = config["simulation"]["experiment_length"].get<int>();
   PRINT_ANALYTICS = config["simulation"]["print_analytics"].get<bool>();
   NETWORK_TEST_STEPS = config["simulation"]["network_test_steps"].get<int>(); //TODO: into task variation
   

   PROXIMITY_THRESHOLD = config["robot"]["proximity_threshold"].get<Real>();
   MAX_WHEELS_SPEED = config["robot"]["max_wheel_speed"].get<Real>();
   STAY_ON_HALF = config["robot"]["stay_on_half"].get<bool>();       //TODO: into task variation
   FEED_POSITION = config["robot"]["feed_position"].get<bool>();     //TODO: into task variation

   //TODO: into task variation
   MAX_INPUT_REWIRES = config["bn"]["max_input_rewires"].get<int>();
   INPUT_REWIRES_PROBABILITY = config["bn"]["input_rewires_probability"].get<Real>();
   MAX_OUTPUT_REWIRES = config["bn"]["max_output_rewires"].get<int>();
   OUTPUT_REWIRES_PROBABILITY = config["bn"]["output_rewires_probability"].get<Real>();

   if(GetId() == "fb0") {
      srand (time(NULL));
   }
   //BN Options
   bestBn = new Bn(10, 3, 0.79);
   currentBn = bestBn->Clone();

   printf("%d\n", rand());
   stay_upper = true; //TODO
}

/****************************************/
/****************************************/

void CFootBotDiffusion::ControlStep() {
   currentBn->Step();
   if(GetId() == "fb0") {
      printf("%ld\n", currentStep);
   }
   /* Get readings from proximity sensor */
   const CCI_FootBotProximitySensor::TReadings& tProxReads = m_pcProximity->GetReadings();
   /* Sum them together */
   CVector2 cAccumulator;
   for(size_t i = 0; i < tProxReads.size(); ++i) {
      cAccumulator += CVector2(tProxReads[i].Value, tProxReads[i].Angle);
   }
   cAccumulator /= tProxReads.size();
   /* If the angle of the vector is small enough and the closest obstacle
    * is far enough, continue going straight, otherwise curve a little
    */
   CRadians cAngle = cAccumulator.Angle();
   if(m_cGoStraightAngleRange.WithinMinBoundIncludedMaxBoundIncluded(cAngle) &&
      cAccumulator.Length() < m_fDelta ) {
      /* Go straight */
      m_pcWheels->SetLinearVelocity(m_fWheelVelocity, m_fWheelVelocity);
   }
   else {
      /* Turn, depending on the sign of the angle */
      if(cAngle.GetValue() > 0.0f) {
         m_pcWheels->SetLinearVelocity(m_fWheelVelocity, 0.0f);
      }
      else {
         m_pcWheels->SetLinearVelocity(0.0f, m_fWheelVelocity);
      }
   }

   
   PrintAnalytics();

   currentStep++;
}

void CFootBotDiffusion::PrintAnalytics() {
   const CCI_PositioningSensor::SReading& tPosReading = m_pcPositioning->GetReading();
   const Real w = tPosReading.Orientation.GetW();
   const Real z = tPosReading.Orientation.GetZ();
   const Real y = tPosReading.Orientation.GetY();
   const Real x = tPosReading.Orientation.GetX();
   const Real zAngle = atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));

   nlohmann::json j;
   j["id"] = GetId();
   j["step"] = printStep;
   j["fitness"] = 0;
   j["states"] = {false, true};
   j["position"] = { tPosReading.Position.GetX(), tPosReading.Position.GetY() };
   j["orientation"] = zAngle;
   j["proximity"] = {0, 0};
   j["boolean_network"] = nullptr;
   j["boolean_network"]["functions"] = { {false, true}, {false, true}};
   j["boolean_network"]["connections"] = { { 1, 2, 3}, { 1, 2, 3} };
   j["boolean_network"]["inputs"] = { 1, 2, 3};
   j["boolean_network"]["outputs"] = { 1, 2};
   j["boolean_network"]["overridden_output_functions"] = { {false, true}, {false, true}};

   //printf("%s\n", j.dump().c_str());
   fflush(stdout);
   printStep++;
}

CFootBotDiffusion::~CFootBotDiffusion() {
   delete bestBn;
   delete currentBn;
}

void CFootBotDiffusion::Reset() {
   
}

REGISTER_CONTROLLER(CFootBotDiffusion, "footbot_controller")
