/* Include the controller definition */
#include "footbot_diffusion.h"
/* Function definitions for XML parsing */
#include <argos3/core/utility/configuration/argos_configuration.h>
/* 2D vector definition */
#include <argos3/core/utility/math/vector2.h>
#include "json.hpp"
#include <iostream>
#include <cmath>
#include "bn_hang.h"
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
   bestBn(NULL),
   testBn(NULL),
   bestHang(NULL),
   testHang(NULL) {}

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
int N, K;
double P;
int NETWORK_INPUT_COUNT, NETWORK_OUTPUT_COUNT;
bool SELF_LOOPS, OVERRIDE_OUTPUT_FUNCTIONS;
double P_OVERRIDE;
bool IO_NODE_OVERLAP;

void CFootBotDiffusion::Init(TConfigurationNode& t_node) {
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
   STAY_ON_HALF = config["robot"]["stay_on_half"].get<bool>();                      //TODO: into task variation
   FEED_POSITION = STAY_ON_HALF && config["robot"]["feed_position"].get<bool>();     //TODO: into task variation

   //TODO: into task variation
   MAX_INPUT_REWIRES = config["bn"]["max_input_rewires"].get<int>();
   INPUT_REWIRES_PROBABILITY = config["bn"]["input_rewires_probability"].get<Real>();
   MAX_OUTPUT_REWIRES = config["bn"]["max_output_rewires"].get<int>();
   OUTPUT_REWIRES_PROBABILITY = config["bn"]["output_rewires_probability"].get<Real>();

   
   N = config["bn"]["options"]["node_count"].get<int>();
   K = config["bn"]["options"]["nodes_input_count"].get<int>();
   P = config["bn"]["options"]["bias"].get<double>();
   NETWORK_INPUT_COUNT = config["bn"]["options"]["network_inputs_count"].get<int>() + (FEED_POSITION ? 1 : 0);
   NETWORK_OUTPUT_COUNT = config["bn"]["options"]["network_outputs_count"].get<int>();
   SELF_LOOPS = config["bn"]["options"]["self_loops"].get<bool>();
   OVERRIDE_OUTPUT_FUNCTIONS = config["bn"]["options"]["override_output_nodes_bias"].get<bool>();
   P_OVERRIDE = 0.5;
   IO_NODE_OVERLAP = false;



   if(GetId() == "fb0") {
      srand (time(NULL));
   }
   bestBn = new Bn(N, K, P, SELF_LOOPS);
   testBn = new Bn(N, K, P, SELF_LOOPS);
   testBn->CopyFrom(bestBn);
   bestHang = new BnHang(NETWORK_INPUT_COUNT, NETWORK_OUTPUT_COUNT, bestBn, IO_NODE_OVERLAP, OVERRIDE_OUTPUT_FUNCTIONS, P_OVERRIDE);
   testHang = new BnHang(NETWORK_INPUT_COUNT, NETWORK_OUTPUT_COUNT, bestBn, IO_NODE_OVERLAP, OVERRIDE_OUTPUT_FUNCTIONS, P_OVERRIDE);
   testHang->CopyFrom(bestHang, bestBn);


   stay_upper = m_pcPositioning->GetReading().Position.GetX() > 0;
}

/****************************************/
/****************************************/

void CFootBotDiffusion::RunAndEvaluateNetwork() {

   //Feed network
   const CCI_FootBotProximitySensor::TReadings& proximityReadings = m_pcProximity->GetReadings();
   int proximityInputCount = NETWORK_INPUT_COUNT - FEED_POSITION ? 1 : 0;
   Real maxProximityValue = 0;
   int proximityGroupSize = proximityReadings.size() / proximityInputCount;
   for(int i = 0; i < proximityInputCount; i++) {
      Real sum = 0;
      for(size_t c = 0; c < proximityGroupSize; c++) {
         sum += proximityReadings[i].Value;
         if(proximityReadings[i].Value > maxProximityValue) maxProximityValue = proximityReadings[i].Value;
      }
      sum = sum / proximityGroupSize;
      testHang->PushInput(testBn, i, sum > PROXIMITY_THRESHOLD);
   }
   bool isInCorrectHalf = stay_upper == (m_pcPositioning->GetReading().Position.GetX() > 0);
   if(FEED_POSITION) {
      testHang->PushInput(testBn, NETWORK_INPUT_COUNT - 1, isInCorrectHalf);
   }

   //Run network
   testBn->Step();

   //Run motors with output
   Real left = testHang->GetOutput(testBn, 0) ? MAX_WHEELS_SPEED : 0;
   Real right = testHang->GetOutput(testBn, 1) ? MAX_WHEELS_SPEED : 0;
   m_pcWheels->SetLinearVelocity(left, right);

   // Fitness function
   if(STAY_ON_HALF && !isInCorrectHalf) {
      testNetworkFitness += 0;
   } else {
      left /= MAX_WHEELS_SPEED;
      right /= MAX_WHEELS_SPEED;
      Real speedFactor = (left + right) / 2;
      Real straightFactor = (1 - sqrt(abs(left - right)));
      Real proximityFactor = (1 - maxProximityValue);
      Real totalFactor = speedFactor * straightFactor * proximityFactor;
      testNetworkFitness += 100 * totalFactor / NETWORK_TEST_STEPS;
   }
}

void CFootBotDiffusion::ControlStep() {
   if(currentStep >= NETWORK_TEST_STEPS) {
      if(PRINT_ANALYTICS) PrintAnalytics(false);
      if(testNetworkFitness >= bestNetworkFitness) {
         bestBn->CopyFrom(testBn);
         bestHang->CopyFrom(testHang, bestBn);
         bestNetworkFitness = testNetworkFitness;
      }
      //if(variation is X) ...
      int inputRewires = 0;
      for(int i = 0; i < MAX_INPUT_REWIRES; i++) inputRewires += ((double)rand() / RAND_MAX) < INPUT_REWIRES_PROBABILITY ? 1 : 0;
      int outputRewires = 0;
      for(int i = 0; i < MAX_OUTPUT_REWIRES; i++) outputRewires += ((double)rand() / RAND_MAX) < OUTPUT_REWIRES_PROBABILITY ? 1 : 0;
      testHang->Rewires(testBn, inputRewires, outputRewires, IO_NODE_OVERLAP);
      currentStep = 0;
      testNetworkFitness = 0;
   }

   if(currentStep == 0 && PRINT_ANALYTICS) PrintAnalytics(true);
   else if(PRINT_ANALYTICS) PrintAnalytics(false);

   RunAndEvaluateNetwork();
   currentStep++;
}

void CFootBotDiffusion::Destroy() {
   if(currentStep > 0) { //can be called when a robot initialization fails
      if(PRINT_ANALYTICS) PrintAnalytics(true);
      fflush(stdout);
   }
}

void CFootBotDiffusion::PrintAnalytics(bool printBnSchema) {
   const CCI_PositioningSensor::SReading& tPosReading = m_pcPositioning->GetReading();
   const CCI_FootBotProximitySensor::TReadings& tProxReads = m_pcProximity->GetReadings();
   const Real w = tPosReading.Orientation.GetW();
   const Real z = tPosReading.Orientation.GetZ();
   const Real y = tPosReading.Orientation.GetY();
   const Real x = tPosReading.Orientation.GetX();
   const Real zAngle = atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));

   nlohmann::json j;
   j["id"] = GetId();
   j["step"] = printStep;
   j["fitness"] = testNetworkFitness;
   nlohmann::json jStates = nlohmann::json::array();
   for(int n = 0; n < testBn->N; n++) jStates.push_back(testBn->GetNodeState(n));
   j["states"] = jStates;
   j["position"] = { tPosReading.Position.GetX(), tPosReading.Position.GetY() };
   j["orientation"] = zAngle;

   //can be derived from arena, robot size, position and orientation if needed
   //nlohmann::json jProximity;
   //for(int i = 0 ; i < tProxReads.size(); i++) jProximity.push_back(tProxReads[i].Value);
   //j["proximity"] = jProximity;

   if(printBnSchema) {
      j["boolean_network"] = nullptr;
      nlohmann::json jFunctions = nlohmann::json::array();
      for(int n = 0; n < testBn->N; n++) {
         nlohmann::json jTruthTable = nlohmann::json::array();
         for(int k = 0; k < testBn->K2; k++)
            jTruthTable.push_back(testBn->GetTruthTableEntry(n, k));
         jFunctions.push_back(jTruthTable);
      }
      j["boolean_network"]["functions"] = jFunctions;
      nlohmann::json jConnections = nlohmann::json::array();
      for(int n = 0; n < testBn->N; n++) {
         nlohmann::json jNodeConnections = nlohmann::json::array();
         for(int k = 0; k < testBn->K; k++)
            jNodeConnections.push_back(testBn->GetConnectionIndex(n, k));
         jConnections.push_back(jNodeConnections);
      }
      j["boolean_network"]["connections"] = jConnections;

      nlohmann::json jInput = nlohmann::json::array();
      for(int i = 0; i < NETWORK_INPUT_COUNT; i++) jInput.push_back(testHang->GetInputNodeIndex(i));
      j["boolean_network"]["inputs"] = jInput;
      nlohmann::json jOutputs = nlohmann::json::array();
      for(int i = 0; i < NETWORK_OUTPUT_COUNT; i++) jOutputs.push_back(testHang->GetOutputNodeIndex(i));
      j["boolean_network"]["outputs"] = jOutputs;

      nlohmann::json jOverriddenFunctions = nlohmann::json::array();
      for(int n = 0; n < NETWORK_OUTPUT_COUNT; n++) {
         nlohmann::json jTruthTable = nlohmann::json::array();
         for(int k = 0; k < testBn->K2; k++)
            jTruthTable.push_back(testHang->GetOverriddenOutputFunctions(n, k));
         jOverriddenFunctions.push_back(jTruthTable);
      }
      j["boolean_network"]["overridden_output_functions"] = jOverriddenFunctions;
   }

   printf("%s\n", j.dump().c_str());
   printStep++;
}

CFootBotDiffusion::~CFootBotDiffusion() {
   delete bestBn;
   delete testBn;
   delete bestHang;
   delete testHang;
}

void CFootBotDiffusion::Reset() {
   
}

REGISTER_CONTROLLER(CFootBotDiffusion, "footbot_controller")
