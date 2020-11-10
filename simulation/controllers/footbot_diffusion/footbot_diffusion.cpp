/* Include the controller definition */
#include "footbot_diffusion.h"
/* Function definitions for XML parsing */
#include <argos3/core/utility/configuration/argos_configuration.h>
/* 2D vector definition */
#include <argos3/core/utility/math/vector2.h>
#include "json.hpp"
#include <iostream>
#include <cmath>
#include "bn_handles.h"
#include "bn.h"

#define round(v, d) ((int)(v * pow(10, d) + .5) / ((Real)pow(10, d)))
#define LOG_DEBUG
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
   testHang(NULL),
   stayUpper(false) {}

/****************************************/
/****************************************/

bool configurationLoaded = false;
int TICKS_PER_SECOND;
int EXPERIMENT_LENGTH;
int NETWORK_TEST_STEPS;
bool PRINT_ANALYTICS;

Real PROXIMITY_THRESHOLD;
Real MAX_WHEELS_SPEED;
bool STAY_ON_HALF;
bool FEED_POSITION;

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

//VARIATIONS
int VARIATIONS;
#define VARIATION(n) ((VARIATIONS & n) > 0))
#define OBSTACLE_AVOIDANCE        VARIATION(1)
#define WALK_STRAIGHT_LINE        VARIATION(2)
#define RESTRICT_TO_HALF_ARENA    VARIATION(4)
#define FEED_HALF_ARENA_POSITION  VARIATION(8)
#define EVOLVE_BN_HANGS           VARIATION(128)
#define EVOLVE_BN_FUNCTIONS       VARIATION(256)
#define EVOLVE_BN_CONNECTION      VARIATION(512)



void CFootBotDiffusion::Init(TConfigurationNode& t_node) {
   m_pcWheels = GetActuator<CCI_DifferentialSteeringActuator>("differential_steering");
   m_pcProximity = GetSensor<CCI_FootBotProximitySensor>("footbot_proximity");
   m_pcPositioning = GetSensor<CCI_PositioningSensor>("positioning");
   m_pcLEDs = GetActuator<CCI_LEDsActuator>("leds");
   m_pcLEDs->SetAllColors(CColor::RED);

   /*
    * Parse the configuration json
    */
   if(!configurationLoaded) {
      std::string configStr = "null";
      GetNodeAttributeOrDefault(t_node, "CONFIG", configStr, configStr);
      config = nlohmann::json::parse(configStr);

      TICKS_PER_SECOND = config["simulation"]["ticks_per_seconds"].get<int>();
      EXPERIMENT_LENGTH = config["simulation"]["experiment_length"].get<int>();
      PRINT_ANALYTICS = config["simulation"]["print_analytics"].get<bool>();
      NETWORK_TEST_STEPS = config["simulation"]["network_test_steps"].get<int>(); //TODO: into task variation
      

      PROXIMITY_THRESHOLD = config["robot"]["proximity_threshold"].get<Real>();
      MAX_WHEELS_SPEED = config["robot"]["max_wheel_speed"].get<Real>() * TICKS_PER_SECOND;
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


      //Select random seed
      uint seed = 0;
      if(config["simulation"]["controllers_random_seed"].is_number_unsigned()) {
         seed = config["simulation"]["controllers_random_seed"].get<uint>();
         #ifdef LOG_DEBUG 
         printf("[DEBUG] using given value as random seed (%d)\n", seed); 
         #endif
         
      } else {
         seed = time(NULL);
         #ifdef LOG_DEBUG 
         printf("[DEBUG] using random value as random seed (%d)\n", seed); 
         #endif
      }
      srand(seed);
      configurationLoaded = true;
      #ifdef LOG_DEBUG 
      printf("[DEBUG] configuration loaded:\n"); 
      printf("[DEBUG]\t TICKS_PER_SECOND = %d\n", TICKS_PER_SECOND); 
      printf("[DEBUG]\t EXPERIMENT_LENGTH = %d\n", EXPERIMENT_LENGTH); 
      printf("[DEBUG]\t PRINT_ANALYTICS = %d\n", PRINT_ANALYTICS); 
      printf("[DEBUG]\t NETWORK_TEST_STEPS = %d\n", NETWORK_TEST_STEPS); 
      printf("[DEBUG]\t PROXIMITY_THRESHOLD = %f\n", PROXIMITY_THRESHOLD); 
      printf("[DEBUG]\t MAX_WHEELS_SPEED = %f\n", MAX_WHEELS_SPEED); 
      printf("[DEBUG]\t STAY_ON_HALF = %d\n", STAY_ON_HALF); 
      printf("[DEBUG]\t FEED_POSITION = %d\n", FEED_POSITION); 
      printf("[DEBUG]\t MAX_INPUT_REWIRES = %d\n", MAX_INPUT_REWIRES); 
      printf("[DEBUG]\t INPUT_REWIRES_PROBABILITY = %f\n", INPUT_REWIRES_PROBABILITY); 
      printf("[DEBUG]\t MAX_OUTPUT_REWIRES = %d\n", MAX_OUTPUT_REWIRES); 
      printf("[DEBUG]\t OUTPUT_REWIRES_PROBABILITY = %f\n", OUTPUT_REWIRES_PROBABILITY); 
      printf("[DEBUG]\t N = %d\n", N); 
      printf("[DEBUG]\t K = %d\n", K); 
      printf("[DEBUG]\t P = %f\n", P);
      printf("[DEBUG]\t NETWORK_INPUT_COUNT = %d\n", NETWORK_INPUT_COUNT); 
      printf("[DEBUG]\t NETWORK_OUTPUT_COUNT = %d\n", NETWORK_OUTPUT_COUNT); 
      printf("[DEBUG]\t SELF_LOOPS = %d\n", SELF_LOOPS); 
      printf("[DEBUG]\t OVERRIDE_OUTPUT_FUNCTIONS = %d\n", OVERRIDE_OUTPUT_FUNCTIONS);  
      #endif
   }

   bestBn = new Bn(N, K, P, SELF_LOOPS);
   bestHang = new BnHandles(NETWORK_INPUT_COUNT, NETWORK_OUTPUT_COUNT, bestBn, IO_NODE_OVERLAP, OVERRIDE_OUTPUT_FUNCTIONS, P_OVERRIDE);
   if(config["bn"]["initial"].is_object()) {
      nlohmann::json connections = config["bn"]["initial"]["connections"];
      for(int n = 0; n < bestBn->N; n++) {
         for(int k = 0; k < bestBn->K; k++) {
            bestBn->SetConnectionIndex(n, k, connections[n][k].get<int>());
         }
      }
      nlohmann::json functions = config["bn"]["initial"]["functions"];
      for(int n = 0; n < bestBn->N; n++) {
         for(int k = 0; k < bestBn->K2; k++) {
            bestBn->SetTruthTableEntry(n, k, functions[n][k].get<bool>());
         }
      }
      nlohmann::json inputs = config["bn"]["initial"]["inputs"];
      for(int n = 0; n < bestHang->InputCount; n++) {
         bestHang->SetInputIndex(n, inputs[n].get<int>());
      }
      nlohmann::json outputs = config["bn"]["initial"]["outputs"];
      for(int n = 0; n < bestHang->OutputCount; n++) {
         bestHang->SetOutputIndex(n, outputs[n].get<int>());
      }
      nlohmann::json overriddenFunctions = config["bn"]["initial"]["overridden_output_functions"];
      if(overriddenFunctions.is_array()) {
         for(int n = 0; n < bestHang->OutputCount; n++) {
            for(int k = 0; k < bestBn->K2; k++) {
               bestHang->SetOverriddenTruthTableEntry(n, k, overriddenFunctions[n][k].get<bool>());
            }
         }
      }
      //nlohmann::json states = config["bn"]["initial"]["states"];
      for(int n = 0; n < bestBn->N; n++) {
         //bestBn->SetNodeState(n, states[n].get<int>());
      }
      #ifdef LOG_DEBUG 
      printf("[DEBUG] [BOT %s] loaded initial bn\n", GetId().c_str()); 
      #endif
   }

   testBn = new Bn(N, K, P, SELF_LOOPS);
   testBn->CopyFrom(bestBn);
   testHang = new BnHandles(NETWORK_INPUT_COUNT, NETWORK_OUTPUT_COUNT, bestBn, IO_NODE_OVERLAP, OVERRIDE_OUTPUT_FUNCTIONS, P_OVERRIDE);
   testHang->CopyFrom(bestHang, bestBn);

   stayUpper = m_pcPositioning->GetReading().Position.GetX() > 0;
   if(stayUpper && STAY_ON_HALF) {
      m_pcLEDs->SetAllColors(CColor::GREEN);
   }
   #ifdef LOG_DEBUG 
   printf("[DEBUG] [BOT %s] initialized!\n", GetId().c_str()); 
   #endif
}

/****************************************/
/****************************************/

void CFootBotDiffusion::RunAndEvaluateNetwork() {

   // Feed network
   const CCI_FootBotProximitySensor::TReadings& proximityReadings = m_pcProximity->GetReadings();
   int proximityInputCount = NETWORK_INPUT_COUNT - (FEED_POSITION ? 1 : 0);
   Real maxProximityValue = 0;
   int proximityGroupSize = proximityReadings.size() / proximityInputCount;
   for(int i = 0; i < proximityInputCount; i++) {
      Real max = 0;
      for(int c = 0; c < proximityGroupSize; c++) {
         Real value = proximityReadings[i * proximityGroupSize + c].Value;
         max = max > value ? max : value;
         maxProximityValue = maxProximityValue > value ? maxProximityValue : value;
      }
      testHang->PushInput(testBn, i, max > PROXIMITY_THRESHOLD);
   }
   bool isInCorrectHalf = stayUpper == (m_pcPositioning->GetReading().Position.GetX() > 0);
   if(FEED_POSITION) {
      testHang->PushInput(testBn, NETWORK_INPUT_COUNT - 1, isInCorrectHalf);
   }

   // Run network
   testBn->Step();

   // Run motors with outputs
   Real left = testHang->GetOutput(testBn, 0) ? MAX_WHEELS_SPEED : 0;
   Real right = testHang->GetOutput(testBn, 1) ? MAX_WHEELS_SPEED : 0;
   m_pcWheels->SetLinearVelocity(left, right);

   // Evaluate fitness function
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
      } else { //rollback to previous best network
         testBn->CopyFrom(bestBn);
         testHang->CopyFrom(bestHang, bestBn);
      }
      //if(variation is X) ...
      int inputRewires;
      int outputRewires;
      do {
         inputRewires = 0;
         outputRewires = 0;
         for(int i = 0; i < MAX_INPUT_REWIRES; i++) inputRewires += ((double)rand() / RAND_MAX) < INPUT_REWIRES_PROBABILITY ? 1 : 0;
         for(int i = 0; i < MAX_OUTPUT_REWIRES; i++) outputRewires += ((double)rand() / RAND_MAX) < OUTPUT_REWIRES_PROBABILITY ? 1 : 0;
      } while(inputRewires == 0 && outputRewires == 0);
      testHang->Rewires(testBn, inputRewires, 0, IO_NODE_OVERLAP);
      testHang->Rewires(testBn, 0, outputRewires, IO_NODE_OVERLAP);
      currentStep = 0;
      testNetworkFitness = 0;
   }

   if(currentStep == 0 && PRINT_ANALYTICS) PrintAnalytics(true);
   else if(PRINT_ANALYTICS) PrintAnalytics(false);

   RunAndEvaluateNetwork();
   currentStep++;
}

void CFootBotDiffusion::Destroy() {
   #ifdef LOG_DEBUG 
   printf("[DEBUG] [BOT %s] destroyed!\n", GetId().c_str()); 
   #endif
   if(currentStep > 0) { //can be called when a robot initialization fails
      if(PRINT_ANALYTICS) PrintAnalytics(true);
      fflush(stdout);
   }
}

void CFootBotDiffusion::PrintAnalytics(bool printBnSchema) {
   const CCI_PositioningSensor::SReading& tPosReading = m_pcPositioning->GetReading();
   const CCI_FootBotProximitySensor::TReadings& tProxReads = m_pcProximity->GetReadings();
   //const CCI_FootBotProximitySensor::TReadings& tProxReads = m_pcProximity->GetReadings();
   const Real w = tPosReading.Orientation.GetW();
   const Real z = tPosReading.Orientation.GetZ();
   const Real y = tPosReading.Orientation.GetY();
   const Real x = tPosReading.Orientation.GetX();
   const Real zAngle = atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));

   nlohmann::json j;
   j["id"] = GetId();
   j["step"] = printStep;
   j["fitness"] = round(testNetworkFitness, 4);
   nlohmann::json jStates = nlohmann::json::array();
   for(int n = 0; n < testBn->N; n++) jStates.push_back(testBn->GetNodeState(n));
   j["states"] = jStates;
   j["location"] = { round(tPosReading.Position.GetX(), 4), round(tPosReading.Position.GetY(), 4), round(zAngle, 4) };


  /* nlohmann::json jProximity;
   for(int i = 0 ; i < tProxReads.size(); i++) jProximity.push_back(round(tProxReads[i].Value, 2));
   j["proximity"] = jProximity;*/

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