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
#include "utils.h"

#define LOG_DEBUG
#define isUpper() (m_pcPositioning->GetReading().Position.GetX() > 0);

/* Init class fields */
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
   stayUpper(false),
   myId(-1) {}


/* Global parameters */
bool configurationLoaded = false;

//SIMULATION
int TICKS_PER_SECOND;
int EXPERIMENT_LENGTH;
int ROBOT_COUNT;
bool PRINT_ANALYTICS;

// ADAPTATION
int EPOCH_LENGTH;
//network io
int MAX_INPUT_REWIRES;
Real INPUT_REWIRE_PROBABILITY;
int MAX_OUTPUT_REWIRES;
Real OUTPUT_REWIRE_PROBABILITY;
bool IO_NODE_OVERLAP_ON_REWIRE;
//netowrk
int MAX_CONNECTION_REWIRES;
Real CONNECTION_REWIRE_PROBABILITY;
bool CONNECTION_REWIRE_SELF_LOOPS;
int MAX_FUNCTION_BIT_FLIPS;
Real FUNCTION_BIT_FLIP_PROBABILITY;
bool KEEP_P_BALANCE;

//NETOWRK
int N, K;
Real P;
bool SELF_LOOPS;
//NETOWRK IO
bool OVERRIDE_OUTPUT_FUNCTIONS;
Real P_OVERRIDE;
bool IO_NODE_OVERLAP;

//OBJECTIVES
//forwrding
Real MAX_WHEELS_SPEED;
int WHEELS_NODES;
//obstacle_avoidance
Real PROXIMITY_THRESHOLD;
int PROXIMITY_NODES;
//half region
bool STAY_ON_HALF;
int REGION_NODES = 0;
Real PENALTY_FACTOR = 0;
bool RESET_REGION_EVERY_EPOCH = false;

int NETWORK_INPUT_COUNT, NETWORK_OUTPUT_COUNT;
nlohmann::json config;
//Bn** sharedBn;
//BnHandles** sharedBnIo;


/* Controller initialization */
void CFootBotDiffusion::Init(TConfigurationNode& t_node) {
   myId = std::stoi(GetId());
   m_pcWheels = GetActuator<CCI_DifferentialSteeringActuator>("differential_steering");
   m_pcProximity = GetSensor<CCI_FootBotProximitySensor>("footbot_proximity");
   m_pcPositioning = GetSensor<CCI_PositioningSensor>("positioning");
   m_pcLEDs = GetActuator<CCI_LEDsActuator>("leds");
   m_pcLEDs->SetAllColors(CColor::RED);

   /* Load the configuration */
   if(!configurationLoaded) {
      
      std::string configStr = "null";
      GetNodeAttributeOrDefault(t_node, "CONFIG", configStr, configStr);
      config = nlohmann::json::parse(configStr);

      //simulation
      TICKS_PER_SECOND              = config["simulation"]["ticks_per_seconds"].get<int>();
      EXPERIMENT_LENGTH             = config["simulation"]["experiment_length"].get<int>();
      PRINT_ANALYTICS               = config["simulation"]["print_analytics"].get<bool>();
      ROBOT_COUNT                   = config["simulation"]["robot_count"].get<int>();
      //sharedBn = new Bn*[ROBOT_COUNT];
      //sharedBnIo = new BnHandles*[ROBOT_COUNT];

      //adaptation
      EPOCH_LENGTH                  = config["adaptation"]["epoch_length"].get<int>();
      MAX_INPUT_REWIRES             = config["adaptation"]["network_io_mutation"]["max_input_rewires"].get<int>();
      INPUT_REWIRE_PROBABILITY      = config["adaptation"]["network_io_mutation"]["input_rewire_probability"].get<Real>();
      MAX_OUTPUT_REWIRES            = config["adaptation"]["network_io_mutation"]["max_output_rewires"].get<int>();
      OUTPUT_REWIRE_PROBABILITY     = config["adaptation"]["network_io_mutation"]["output_rewire_probability"].get<Real>();
      IO_NODE_OVERLAP_ON_REWIRE     = config["adaptation"]["network_io_mutation"]["allow_io_node_overlap"].get<bool>();
      MAX_CONNECTION_REWIRES        = config["adaptation"]["network_mutation"]["max_connection_rewires"].get<int>();
      CONNECTION_REWIRE_PROBABILITY = config["adaptation"]["network_mutation"]["connection_rewire_probability"].get<Real>();
      CONNECTION_REWIRE_SELF_LOOPS  = config["adaptation"]["network_mutation"]["self_loops"].get<bool>();
      MAX_FUNCTION_BIT_FLIPS        = config["adaptation"]["network_mutation"]["max_function_bit_flips"].get<int>();
      FUNCTION_BIT_FLIP_PROBABILITY = config["adaptation"]["network_mutation"]["function_bit_flips_probability"].get<Real>();
      KEEP_P_BALANCE                = config["adaptation"]["network_mutation"]["keep_p_balance"].get<bool>();

      //netowrk
      N                             = config["network"]["n"].get<int>();
      K                             = config["network"]["k"].get<int>();
      P                             = config["network"]["p"].get<Real>();
      SELF_LOOPS                    = config["network"]["self_loops"].get<bool>();
      //netowrk io
      OVERRIDE_OUTPUT_FUNCTIONS     = config["network"]["io"]["override_output_nodes"].get<bool>();
      P_OVERRIDE                    = config["network"]["io"]["override_outputs_p"].get<Real>();
      IO_NODE_OVERLAP               = config["network"]["io"]["allow_io_node_overlap"].get<bool>();

      //forwarding objective
      MAX_WHEELS_SPEED              = config["objective"]["forwarding"]["max_wheel_speed"].get<Real>();
      WHEELS_NODES                  = config["objective"]["forwarding"]["wheels_nodes"].get<int>();
      //obstacle avoidance objective
      PROXIMITY_THRESHOLD           = config["objective"]["obstacle_avoidance"]["proximity_threshold"].get<Real>();
      PROXIMITY_NODES               = config["objective"]["obstacle_avoidance"]["proximity_nodes"].get<int>();
      //half region variation
      STAY_ON_HALF = false;
      if(config["objective"]["half_region_variation"].is_object()) {
         STAY_ON_HALF               = true;
         PENALTY_FACTOR             = config["objective"]["half_region_variation"]["penalty_factor"].get<Real>();
         REGION_NODES               = config["objective"]["half_region_variation"]["region_nodes"].get<int>();
         RESET_REGION_EVERY_EPOCH   = config["objective"]["half_region_variation"]["reset_region_every_epoch"].get<bool>();
      }
      
      //netowrk io total nodes
      NETWORK_INPUT_COUNT = PROXIMITY_NODES + REGION_NODES;
      NETWORK_OUTPUT_COUNT = WHEELS_NODES;

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
      printf("[DEBUG]\t ROBOT_COUNT = %d\n", ROBOT_COUNT); 

      printf("[DEBUG]\t EPOCH_LENGTH = %d\n", EPOCH_LENGTH); 
      printf("[DEBUG]\t MAX_CONNECTION_REWIRES = %d\n", MAX_CONNECTION_REWIRES); 
      printf("[DEBUG]\t CONNECTION_REWIRE_PROBABILITY = %f\n", CONNECTION_REWIRE_PROBABILITY); 
      printf("[DEBUG]\t CONNECTION_REWIRE_SELF_LOOPS = %d\n", CONNECTION_REWIRE_SELF_LOOPS); 
      printf("[DEBUG]\t MAX_FUNCTION_BIT_FLIPS = %d\n", MAX_FUNCTION_BIT_FLIPS); 
      printf("[DEBUG]\t FUNCTION_BIT_FLIP_PROBABILITY = %f\n", FUNCTION_BIT_FLIP_PROBABILITY); 
      printf("[DEBUG]\t KEEP_P_BALANCE = %d\n", KEEP_P_BALANCE); 
      printf("[DEBUG]\t MAX_INPUT_REWIRES = %d\n", MAX_INPUT_REWIRES); 
      printf("[DEBUG]\t INPUT_REWIRE_PROBABILITY = %f\n", INPUT_REWIRE_PROBABILITY); 
      printf("[DEBUG]\t MAX_OUTPUT_REWIRES = %d\n", MAX_OUTPUT_REWIRES); 
      printf("[DEBUG]\t OUTPUT_REWIRE_PROBABILITY = %f\n", OUTPUT_REWIRE_PROBABILITY); 
      printf("[DEBUG]\t IO_NODE_OVERLAP_ON_REWIRE = %d\n", IO_NODE_OVERLAP_ON_REWIRE); 

      printf("[DEBUG]\t N = %d\n", N); 
      printf("[DEBUG]\t K = %d\n", K); 
      printf("[DEBUG]\t P = %f\n", P);
      printf("[DEBUG]\t SELF_LOOPS = %d\n", SELF_LOOPS); 
      printf("[DEBUG]\t OVERRIDE_OUTPUT_FUNCTIONS = %d\n", OVERRIDE_OUTPUT_FUNCTIONS); 
      printf("[DEBUG]\t P_OVERRIDE = %f\n", P_OVERRIDE); 
      printf("[DEBUG]\t IO_NODE_OVERLAP = %d\n", IO_NODE_OVERLAP); 

      printf("[DEBUG]\t MAX_WHEELS_SPEED = %f\n", MAX_WHEELS_SPEED); 
      printf("[DEBUG]\t WHEELS_NODES = %d\n", WHEELS_NODES); 
      printf("[DEBUG]\t PROXIMITY_THRESHOLD = %f\n", PROXIMITY_THRESHOLD); 
      printf("[DEBUG]\t PROXIMITY_NODES = %d\n", PROXIMITY_NODES); 
      printf("[DEBUG]\t STAY_ON_HALF = %d\n", STAY_ON_HALF); 
      printf("[DEBUG]\t REGION_NODES = %d\n", REGION_NODES); 
      printf("[DEBUG]\t PENALTY_FACTOR = %f\n", PENALTY_FACTOR); 
      printf("[DEBUG]\t RESET_REGION_EVERY_EPOCH = %d\n", RESET_REGION_EVERY_EPOCH); 
      
      printf("[DEBUG]\t NETWORK_INPUT_COUNT = %d\n", NETWORK_INPUT_COUNT); 
      printf("[DEBUG]\t NETWORK_OUTPUT_COUNT = %d\n", NETWORK_OUTPUT_COUNT); 
      #endif
   } //end configuration loading

   /* Network creation */
   bestBn = new Bn(N, K, P, SELF_LOOPS);
   bestHang = new BnHandles(NETWORK_INPUT_COUNT, NETWORK_OUTPUT_COUNT, bestBn, IO_NODE_OVERLAP, OVERRIDE_OUTPUT_FUNCTIONS, P_OVERRIDE);

   /* Load given network schema */
   if(config["network"]["initial_schema"].is_object()) {
      nlohmann::json connections = config["network"]["initial_schema"]["connections"];
      for(int n = 0; n < bestBn->N; n++) {
         for(int k = 0; k < bestBn->K; k++) {
            bestBn->SetConnectionIndex(n, k, connections[n][k].get<int>());
         }
      }
      nlohmann::json functions = config["network"]["initial_schema"]["functions"];
      for(int n = 0; n < bestBn->N; n++) {
         for(int k = 0; k < bestBn->K2; k++) {
            bestBn->SetTruthTableEntry(n, k, functions[n][k].get<bool>());
         }
      }
      nlohmann::json inputs = config["network"]["initial_schema"]["inputs"];
      for(int n = 0; n < bestHang->InputCount; n++) {
         bestHang->SetInputIndex(n, inputs[n].get<int>());
      }
      nlohmann::json outputs = config["network"]["initial_schema"]["outputs"];
      for(int n = 0; n < bestHang->OutputCount; n++) {
         bestHang->SetOutputIndex(n, outputs[n].get<int>());
      }
      nlohmann::json overriddenFunctions = config["network"]["initial_schema"]["overridden_output_functions"];
      if(overriddenFunctions.is_array()) {
         for(int n = 0; n < bestHang->OutputCount; n++) {
            for(int k = 0; k < bestBn->K2; k++) {
               bestHang->SetOverriddenTruthTableEntry(n, k, overriddenFunctions[n][k].get<bool>());
            }
         }
      }
      #ifdef LOG_DEBUG 
      printf("[DEBUG] [BOT %d] loaded initial netowrk schema\n", myId); 
      #endif
   }
   /* Load given network state*/
   if(config["network"]["initial_state"].is_array()) {
      nlohmann::json states = config["network"]["initial_state"];
      for(int n = 0; n < bestBn->N; n++) {
         bestBn->SetNodeState(n, states[n].get<int>());
      }
      #ifdef LOG_DEBUG 
      printf("[DEBUG] [BOT %d] loaded initial netowrk state\n", myId); 
      #endif
   }

   testBn = new Bn(N, K, P, SELF_LOOPS);
   testBn->CopyFrom(bestBn);
   testHang = new BnHandles(NETWORK_INPUT_COUNT, NETWORK_OUTPUT_COUNT, bestBn, IO_NODE_OVERLAP, OVERRIDE_OUTPUT_FUNCTIONS, P_OVERRIDE);
   testHang->CopyFrom(bestHang, bestBn);

   stayUpper = isUpper();
   #ifdef LOG_DEBUG 
   if(stayUpper && STAY_ON_HALF) {
      m_pcLEDs->SetAllColors(CColor::GREEN);
   }
   printf("[DEBUG] [BOT %d] initialized!\n", myId); 
   fflush(stdout);
   #endif
}


/* Feed run and evaluate the test network */
void CFootBotDiffusion::RunAndEvaluateNetwork() {

   // Feed network
   const CCI_FootBotProximitySensor::TReadings& proximityReadings = m_pcProximity->GetReadings();
   Real maxProximityValue = 0;
   int proximityGroupSize = proximityReadings.size() / PROXIMITY_NODES;
   for(int i = 0; i < PROXIMITY_NODES; i++) {
      Real max = 0;
      for(int c = 0; c < proximityGroupSize; c++) {
         Real value = proximityReadings[i * proximityGroupSize + c].Value;
         max = max > value ? max : value;
         maxProximityValue = maxProximityValue > value ? maxProximityValue : value;
      }
      testHang->PushInput(testBn, i, max > PROXIMITY_THRESHOLD);
   }
   bool isInCorrectHalf = stayUpper == isUpper();
   for(int i = 0; i < REGION_NODES; i++) {
      testHang->PushInput(testBn, PROXIMITY_NODES + i, isInCorrectHalf);
   }

   #ifdef LOG_DEBUG
   if(STAY_ON_HALF && isInCorrectHalf && stayUpper) {
      m_pcLEDs->SetAllColors(CColor::GREEN);
   } else if(STAY_ON_HALF && !isInCorrectHalf && stayUpper) {
      m_pcLEDs->SetAllColors(CColor::YELLOW);
   } else if(STAY_ON_HALF && isInCorrectHalf && !stayUpper) {
      m_pcLEDs->SetAllColors(CColor::RED);
   } else if(STAY_ON_HALF && !isInCorrectHalf && !stayUpper) {
      m_pcLEDs->SetAllColors(CColor::ORANGE);
   }
   #endif

   // Run network
   testBn->Step();

   // Run motors with outputs
   Real left = 0, right = 0;
   for(int i = 0; i < WHEELS_NODES / 2; i++) {
      left += testHang->GetOutput(testBn, i) ? (MAX_WHEELS_SPEED / (WHEELS_NODES / 2)): 0;
   }
   for(int i = 0; i < WHEELS_NODES / 2; i++) {
      right += testHang->GetOutput(testBn, i + WHEELS_NODES / 2) ? (MAX_WHEELS_SPEED / (WHEELS_NODES / 2)): 0;
   }
   m_pcWheels->SetLinearVelocity(left, right);
   
   // Evaluate fitness function
   left /= MAX_WHEELS_SPEED;
   right /= MAX_WHEELS_SPEED;
   Real speedFactor = (left + right) / 2;
   Real straightFactor = (1 - sqrt(abs(left - right)));
   Real proximityFactor = (1 - maxProximityValue);
   Real totalFactor = speedFactor * straightFactor * proximityFactor;
   Real fitness = 100 * totalFactor / EPOCH_LENGTH;
   if(STAY_ON_HALF && !isInCorrectHalf) {
      testNetworkFitness += PENALTY_FACTOR * fitness;
   } else {
      testNetworkFitness += fitness;
   }
}

/* Controller step */
void CFootBotDiffusion::ControlStep() {
   /* End of an epoch */
   if(currentStep >= EPOCH_LENGTH) {
      if(PRINT_ANALYTICS) PrintAnalytics(false);

      //SELECTION
      if(testNetworkFitness >= bestNetworkFitness) {
         bestBn->CopyFrom(testBn);
         bestHang->CopyFrom(testHang, bestBn);
         bestNetworkFitness = testNetworkFitness;
      } else { //rollback to previous best network
         testBn->CopyFrom(bestBn);
         testHang->CopyFrom(bestHang, bestBn);
      }
      //TODO reset states with p 0.5?

      //NETWORK MUTATION
      int connectionRewires = extract(MAX_CONNECTION_REWIRES, CONNECTION_REWIRE_PROBABILITY);
      int functinoBitFlips = extract(MAX_FUNCTION_BIT_FLIPS, FUNCTION_BIT_FLIP_PROBABILITY);
      if(connectionRewires > 0) testBn->RewiresConnections(connectionRewires, CONNECTION_REWIRE_SELF_LOOPS);
      if(functinoBitFlips > 0) testBn->MutesFunctions(functinoBitFlips, KEEP_P_BALANCE);

      //NETWORK IO REWIRES
      int inputRewires = extract(MAX_INPUT_REWIRES, INPUT_REWIRE_PROBABILITY);
      int outputRewires = extract(MAX_OUTPUT_REWIRES, OUTPUT_REWIRE_PROBABILITY);
      if(inputRewires > 0) testHang->Rewires(testBn, inputRewires, 0, IO_NODE_OVERLAP_ON_REWIRE);
      if(outputRewires > 0) testHang->Rewires(testBn, 0, outputRewires, IO_NODE_OVERLAP_ON_REWIRE);

      //RESET
      currentStep = 0;
      testNetworkFitness = 0;
      if(RESET_REGION_EVERY_EPOCH) {
         stayUpper = isUpper();
         #ifdef LOG_DEBUG 
         if(stayUpper && STAY_ON_HALF) {
            m_pcLEDs->SetAllColors(CColor::GREEN);
         } else {
            m_pcLEDs->SetAllColors(CColor::RED);
         }
         #endif
      }
   }

   if(currentStep == 0 && PRINT_ANALYTICS) PrintAnalytics(true);
   else if(PRINT_ANALYTICS) PrintAnalytics(false);

   RunAndEvaluateNetwork();
   currentStep++;

   if(myId == 0) {
      fflush(stdout);
   }
}

void CFootBotDiffusion::Destroy() {
   #ifdef LOG_DEBUG 
   printf("[DEBUG] [BOT %d] destroyed!\n", myId); 
   #endif
   if(currentStep > 0) { //can be called when a robot initialization fails
      if(PRINT_ANALYTICS) PrintAnalytics(false);
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

   /* 
   nlohmann::json jProximity;
   for(int i = 0 ; i < tProxReads.size(); i++) jProximity.push_back(round(tProxReads[i].Value, 2));
   j["proximity"] = jProximity;
   */

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

      if(testHang->HasOverriddenOutputFunctions()) {
         nlohmann::json jOverriddenFunctions = nlohmann::json::array();
         for(int n = 0; n < NETWORK_OUTPUT_COUNT; n++) {
            nlohmann::json jTruthTable = nlohmann::json::array();
            for(int k = 0; k < testBn->K2; k++)
               jTruthTable.push_back(testHang->GetOverriddenOutputFunctions(n, k));
            jOverriddenFunctions.push_back(jTruthTable);
         }
         j["boolean_network"]["overridden_output_functions"] = jOverriddenFunctions;
      }
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