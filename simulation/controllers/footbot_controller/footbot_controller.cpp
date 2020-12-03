#include "footbot_controller.h"
#include <argos3/core/utility/configuration/argos_configuration.h>
#include <argos3/core/utility/math/vector2.h>
#include "json.hpp"
#include <iostream>
#include <cmath>
#include "bn_io.h"
#include "bn.h"
#include "utils.h"

//#define LOG_DEBUG
#define isUpper() (m_pcPositioning->GetReading().Position.GetX() > 0);
#define isOnNest() (m_pcPositioning->GetReading().Position.GetY() > 1);
#define isOnGather() (m_pcPositioning->GetReading().Position.GetY() < -1);

CFootBotBn::CFootBotBn() :
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
   bestIO(NULL),
   testIO(NULL),
   stayUpper(false),
   hasGather(false),
   myId(-1),
   currentEpoch(0),
   m_pcLights(NULL) {}

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
bool ONLY_DISTINCT_CONNECTIONS_ON_REWIRE = false;

//NETOWRK
int N, K;
Real P;
bool SELF_LOOPS;
bool ONLY_DISTINCT_CONNECTIONS = false;
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

//half region variant
int RUN_VARIANT = 0;
bool STAY_ON_HALF = false;
int REGION_NODES = 0;
Real PENALTY_FACTOR = 0;
bool RESET_REGION_EVERY_EPOCH = false;

//foraging variant
int FORAGING_VARIANT = 1;
bool FORAGING = false;
int LIGHT_NODES = 0;
Real LIGHT_THRESHOLD = 0;


int NETWORK_INPUT_COUNT = 0, NETWORK_OUTPUT_COUNT = 0;
nlohmann::json config;

int VARIANT = 0;

bool HALF_REWIRE_MUTATION = false;


//Bn** sharedBn;
//BnIO** sharedBnIo;


/* Controller initialization */
void CFootBotBn::Init(TConfigurationNode& t_node) {
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

      //variant
      if(config["other"].is_object()) {
         if(config["other"]["variant"].is_string()) {
            VARIANT = (config["other"]["variant"].get<std::string>() == "foraging") ? FORAGING_VARIANT : VARIANT;
         }

         HALF_REWIRE_MUTATION = config["other"]["half_rewire_mutation"].is_string();
      }

      //simulation
      TICKS_PER_SECOND              = config["simulation"]["ticks_per_seconds"].get<int>();
      EXPERIMENT_LENGTH             = config["simulation"]["experiment_length"].get<int>() * TICKS_PER_SECOND;
      PRINT_ANALYTICS               = config["simulation"]["print_analytics"].get<bool>();
      ROBOT_COUNT                   = config["simulation"]["robot_count"].get<int>();

      //adaptation
      EPOCH_LENGTH                  = config["adaptation"]["epoch_length"].get<int>() * TICKS_PER_SECOND;
      MAX_INPUT_REWIRES             = config["adaptation"]["network_io_mutation"]["max_input_rewires"].get<int>();
      INPUT_REWIRE_PROBABILITY      = config["adaptation"]["network_io_mutation"]["input_rewire_probability"].get<Real>();
      MAX_OUTPUT_REWIRES            = config["adaptation"]["network_io_mutation"]["max_output_rewires"].get<int>();
      OUTPUT_REWIRE_PROBABILITY     = config["adaptation"]["network_io_mutation"]["output_rewire_probability"].get<Real>();
      IO_NODE_OVERLAP_ON_REWIRE     = config["adaptation"]["network_io_mutation"]["allow_io_node_overlap"].get<bool>();
      MAX_CONNECTION_REWIRES        = config["adaptation"]["network_mutation"]["max_connection_rewires"].get<int>();
      CONNECTION_REWIRE_PROBABILITY = config["adaptation"]["network_mutation"]["connection_rewire_probability"].get<Real>();
      CONNECTION_REWIRE_SELF_LOOPS  = config["adaptation"]["network_mutation"]["self_loops"].get<bool>();
      ONLY_DISTINCT_CONNECTIONS_ON_REWIRE = config["adaptation"]["network_mutation"]["only_distinct_connections"].get<bool>();
      MAX_FUNCTION_BIT_FLIPS        = config["adaptation"]["network_mutation"]["max_function_bit_flips"].get<int>();
      FUNCTION_BIT_FLIP_PROBABILITY = config["adaptation"]["network_mutation"]["function_bit_flips_probability"].get<Real>();
      KEEP_P_BALANCE                = config["adaptation"]["network_mutation"]["keep_p_balance"].get<bool>();

      //netowrk
      N                             = config["network"]["n"].get<int>();
      K                             = config["network"]["k"].get<int>();
      P                             = config["network"]["p"].get<Real>();
      SELF_LOOPS                    = config["network"]["self_loops"].get<bool>();
      ONLY_DISTINCT_CONNECTIONS     = config["network"]["only_distinct_connections"].get<bool>();
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
      if(VARIANT == RUN_VARIANT && config["objective"]["half_region_variation"].is_object()) {
         if(config["objective"]["half_region_variation"].is_object()) {
            STAY_ON_HALF               = true;
            PENALTY_FACTOR             = config["objective"]["half_region_variation"]["penalty_factor"].get<Real>();
            REGION_NODES               = config["objective"]["half_region_variation"]["region_nodes"].get<int>();
            RESET_REGION_EVERY_EPOCH   = config["objective"]["half_region_variation"]["reset_region_every_epoch"].get<bool>();
            NETWORK_INPUT_COUNT        += REGION_NODES;
         }
      } 
      //foraging variant
      else if(VARIANT == FORAGING_VARIANT) {
         FORAGING             = true;
         NETWORK_INPUT_COUNT  += 3; //is on nest, is on gather, has food
         NETWORK_OUTPUT_COUNT += 1; //hold food
         LIGHT_NODES          = std::stoi(config["other"]["light_nodes"].get<std::string>());
         LIGHT_THRESHOLD      = (Real)std::stod(config["other"]["light_threshold"].get<std::string>());
         NETWORK_INPUT_COUNT  += LIGHT_NODES;
      }
      
      //netowrk io total nodes
      NETWORK_INPUT_COUNT += PROXIMITY_NODES;
      NETWORK_OUTPUT_COUNT += WHEELS_NODES;

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
      printf("[DEBUG]\t ONLY_DISTINCT_CONNECTIONS_ON_REWIRE = %d\n", ONLY_DISTINCT_CONNECTIONS_ON_REWIRE); 
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
      printf("[DEBUG]\t ONLY_DISTINCT_CONNECTIONS = %d\n", ONLY_DISTINCT_CONNECTIONS); 
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

      printf("[DEBUG]\t VARIANT = %d\n", VARIANT);
      printf("[DEBUG]\t HALF_REWIRE_MUTATION = %d\n", HALF_REWIRE_MUTATION);
      #endif
   } //end configuration loading

   /* Network creation */
   bestBn = new Bn(N, K, P, SELF_LOOPS, ONLY_DISTINCT_CONNECTIONS);
   bestIO = new BnIO(NETWORK_INPUT_COUNT, NETWORK_OUTPUT_COUNT, bestBn, IO_NODE_OVERLAP, OVERRIDE_OUTPUT_FUNCTIONS, P_OVERRIDE);

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
      for(int n = 0; n < bestIO->InputCount; n++) {
         bestIO->SetInputIndex(n, inputs[n].get<int>());
      }
      nlohmann::json outputs = config["network"]["initial_schema"]["outputs"];
      for(int n = 0; n < bestIO->OutputCount; n++) {
         bestIO->SetOutputIndex(n, outputs[n].get<int>());
      }
      nlohmann::json overriddenFunctions = config["network"]["initial_schema"]["overridden_output_functions"];
      if(overriddenFunctions.is_array()) {
         for(int n = 0; n < bestIO->OutputCount; n++) {
            for(int k = 0; k < bestBn->K2; k++) {
               bestIO->SetOverriddenTruthTableEntry(n, k, overriddenFunctions[n][k].get<bool>());
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

   testBn = new Bn(N, K, P, SELF_LOOPS, ONLY_DISTINCT_CONNECTIONS);
   testBn->CopyFrom(bestBn);
   testIO = new BnIO(NETWORK_INPUT_COUNT, NETWORK_OUTPUT_COUNT, bestBn, IO_NODE_OVERLAP, OVERRIDE_OUTPUT_FUNCTIONS, P_OVERRIDE);
   testIO->CopyFrom(bestIO, bestBn);

   stayUpper = isUpper();
   if(VARIANT == FORAGING_VARIANT) {
      m_pcLights = GetSensor<CCI_FootBotLightSensor>("footbot_light");
   }
   #ifdef LOG_DEBUG 
   if(stayUpper && STAY_ON_HALF) {
      m_pcLEDs->SetAllColors(CColor::GREEN);
   }
   printf("[DEBUG] [BOT %d] initialized!\n", myId); 
   fflush(stdout);
   #endif
}


/* Feed run and evaluate the test network */
void CFootBotBn::RunAndEvaluateNetwork() {

   // Feed network
   int nextInputNode = 0;
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
      testIO->PushInput(testBn, nextInputNode++, max > PROXIMITY_THRESHOLD);
   }

   bool isInCorrectHalf = stayUpper == isUpper();
   bool isOnNest = isOnNest();
   bool isOnGather = isOnGather();

   if(STAY_ON_HALF) {
      for(int i = 0; i < REGION_NODES; i++) {
         testIO->PushInput(testBn, nextInputNode++, isInCorrectHalf);
      }
      #ifdef LOG_DEBUG
      if(isInCorrectHalf && stayUpper) {
         m_pcLEDs->SetAllColors(CColor::GREEN);
      } else if(!isInCorrectHalf && stayUpper) {
         m_pcLEDs->SetAllColors(CColor::RED);
      } else if(isInCorrectHalf && !stayUpper) {
         m_pcLEDs->SetAllColors(CColor::BLUE);
      } else if(!isInCorrectHalf && !stayUpper) {
         m_pcLEDs->SetAllColors(CColor::RED);
      }
      #endif
   } 

   if(FORAGING) {
      const CCI_FootBotLightSensor::TReadings& lightReadings = m_pcLights->GetReadings();
      int lightGroupSize = lightReadings.size() / LIGHT_NODES;
      for(int i = 0; i < LIGHT_NODES; i++) {
         Real max = 0;
         for(int c = 0; c < lightGroupSize; c++) {
            Real value = lightReadings[i * lightGroupSize + c].Value;
            max = max > value ? max : value;
         }
         testIO->PushInput(testBn, nextInputNode++, max > LIGHT_THRESHOLD);
      }
      testIO->PushInput(testBn, nextInputNode++, isOnNest);
      testIO->PushInput(testBn, nextInputNode++, isOnGather);
      testIO->PushInput(testBn, nextInputNode++, hasGather);
   }

   // Run network
   testBn->Step();

   // Run motors with outputs
   int nextOutputNode = 0;
   Real left = 0, right = 0;
   for(int i = 0; i < WHEELS_NODES / 2; i++) {
      left += testIO->GetOutput(testBn, nextOutputNode++) ? (MAX_WHEELS_SPEED / (WHEELS_NODES / 2)): 0;
   }
   for(int i = 0; i < WHEELS_NODES / 2; i++) {
      right += testIO->GetOutput(testBn, nextOutputNode++) ? (MAX_WHEELS_SPEED / (WHEELS_NODES / 2)): 0;
   }
   m_pcWheels->SetLinearVelocity(left, right);
   bool holdingFood = (FORAGING && testIO->GetOutput(testBn, nextOutputNode++));
   
   // Evaluate fitness function
   left /= MAX_WHEELS_SPEED;
   right /= MAX_WHEELS_SPEED;
   

   if(VARIANT == RUN_VARIANT) {
      Real speedFactor = (left + right) / 2;
      Real straightFactor = (1 - sqrt(abs(left - right)));
      Real proximityFactor = (1 - maxProximityValue);
      Real totalFactor = speedFactor * straightFactor * proximityFactor;
      Real runFitness = 100 * totalFactor / EPOCH_LENGTH;

      if(STAY_ON_HALF && !isInCorrectHalf) {
         testNetworkFitness += PENALTY_FACTOR * runFitness;
      } else {
         testNetworkFitness += runFitness;
      }
   } else if(VARIANT == FORAGING_VARIANT) {

      if(hasGather && !holdingFood) { //drop
         hasGather = false; 
         testNetworkFitness += isOnNest ? 50 : -100;
      } else if(isOnGather && !hasGather && holdingFood) { //take
         hasGather = true;
         testNetworkFitness += 50;
      }

      Real speedFactor = (left + right) / 2;
      Real straightFactor = (1 - sqrt(abs(left - right)));
      Real proximityFactor = (1 - maxProximityValue * 2);
      Real totalFactor = speedFactor * straightFactor * proximityFactor;
      Real runFitness = 100 * totalFactor / EPOCH_LENGTH;

      testNetworkFitness += runFitness;

      #ifdef LOG_DEBUG
      if(isOnNest) {
         m_pcLEDs->SetAllColors(CColor::GREEN);
      } else if(isOnGather) {
         m_pcLEDs->SetAllColors(CColor::RED);
      } else {
         m_pcLEDs->SetAllColors(CColor::GRAY50);
      }
      if(hasGather) {
         for(int l = 0; l < 12; l += 2) m_pcLEDs->SetSingleColor(l, CColor::WHITE);
      } 
      #endif
   }
   
}

/* Controller step */
void CFootBotBn::ControlStep() {
   /* End of an epoch */
   if(currentStep >= EPOCH_LENGTH) {
      if(PRINT_ANALYTICS) PrintAnalytics(false);

      //SELECTION
      if(testNetworkFitness >= bestNetworkFitness) {
         bestBn->CopyFrom(testBn);
         bestIO->CopyFrom(testIO, bestBn);
         bestNetworkFitness = testNetworkFitness;
      } else { //rollback to previous best network
         testBn->CopyFrom(bestBn);
         testIO->CopyFrom(bestIO, bestBn);
      }
      //TODO reset states with p 0.5?

       
      bool canMutate = !HALF_REWIRE_MUTATION || (currentEpoch >= (EXPERIMENT_LENGTH / EPOCH_LENGTH) / 2);
      bool canRewire = !HALF_REWIRE_MUTATION || (currentEpoch < (EXPERIMENT_LENGTH / EPOCH_LENGTH) / 2);
      //NETWORK MUTATION
      if(canMutate) {
         int connectionRewires = extract(MAX_CONNECTION_REWIRES, CONNECTION_REWIRE_PROBABILITY);
         int functinoBitFlips = extract(MAX_FUNCTION_BIT_FLIPS, FUNCTION_BIT_FLIP_PROBABILITY);
         if(connectionRewires > 0) testBn->RewiresConnections(connectionRewires, CONNECTION_REWIRE_SELF_LOOPS, ONLY_DISTINCT_CONNECTIONS_ON_REWIRE);
         if(functinoBitFlips > 0) testBn->MutesFunctions(functinoBitFlips, KEEP_P_BALANCE);
      }
      //NETWORK IO REWIRES
      if(canRewire) {
         int inputRewires = extract(MAX_INPUT_REWIRES, INPUT_REWIRE_PROBABILITY);
         int outputRewires = extract(MAX_OUTPUT_REWIRES, OUTPUT_REWIRE_PROBABILITY);
         if(inputRewires > 0) testIO->Rewires(testBn, inputRewires, 0, IO_NODE_OVERLAP_ON_REWIRE);
         if(outputRewires > 0) testIO->Rewires(testBn, 0, outputRewires, IO_NODE_OVERLAP_ON_REWIRE);
      }

      //RESET
      currentStep = 0;
      testNetworkFitness = 0;
      hasGather = false;
      if(RESET_REGION_EVERY_EPOCH && STAY_ON_HALF) {
         stayUpper = isUpper();
         #ifdef LOG_DEBUG 
         if(stayUpper) {
            m_pcLEDs->SetAllColors(CColor::GREEN);
         } else {
            m_pcLEDs->SetAllColors(CColor::BLUE);
         }
         #endif
      }

      currentEpoch++;
   }

   if(currentStep == 0 && PRINT_ANALYTICS) PrintAnalytics(true);
   else if(PRINT_ANALYTICS) PrintAnalytics(false);

   RunAndEvaluateNetwork();
   currentStep++;

   if(myId == 0) {
      fflush(stdout);
   }
}

void CFootBotBn::Destroy() {
   #ifdef LOG_DEBUG 
   printf("[DEBUG] [BOT %d] destroyed!\n", myId); 
   #endif
   if(currentStep > 0) { //can be called when a robot initialization fails
      if(PRINT_ANALYTICS) PrintAnalytics(false);
      fflush(stdout);
   }
}

void CFootBotBn::PrintAnalytics(bool printBnSchema) {
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
   j["location"] = { round(tPosReading.Position.GetX(), 4), round(tPosReading.Position.GetY(), 4), round(zAngle, 4) };

   if(!printBnSchema) {
      nlohmann::json jInputs = nlohmann::json::array();
      for(int n = 0; n < testIO->InputCount; n++) jInputs.push_back(testBn->GetOldNodeState(testIO->GetInputNodeIndex(n)));
      j["inputs"] = jInputs;
   } else {
      j["boolean_network"] = nullptr;
      nlohmann::json jFunctions = nlohmann::json::array();
      for(int n = 0; n < testBn->N; n++) {
         nlohmann::json jTruthTable = nlohmann::json::array();
         for(int k = 0; k < testBn->K2; k++)
            jTruthTable.push_back(testBn->GetTruthTableEntry(n, k));
         jFunctions.push_back(jTruthTable);
      }
      j["boolean_network"]["functions"] = jFunctions;

      nlohmann::json jStates = nlohmann::json::array();
      for(int n = 0; n < testBn->N; n++) jStates.push_back(testBn->GetNodeState(n));
      j["boolean_network"]["states"] = jStates;

      nlohmann::json jConnections = nlohmann::json::array();
      for(int n = 0; n < testBn->N; n++) {
         nlohmann::json jNodeConnections = nlohmann::json::array();
         for(int k = 0; k < testBn->K; k++)
            jNodeConnections.push_back(testBn->GetConnectionIndex(n, k));
         jConnections.push_back(jNodeConnections);
      }
      j["boolean_network"]["connections"] = jConnections;

      nlohmann::json jInput = nlohmann::json::array();
      for(int i = 0; i < NETWORK_INPUT_COUNT; i++) jInput.push_back(testIO->GetInputNodeIndex(i));
      j["boolean_network"]["inputs"] = jInput;
      nlohmann::json jOutputs = nlohmann::json::array();
      for(int i = 0; i < NETWORK_OUTPUT_COUNT; i++) jOutputs.push_back(testIO->GetOutputNodeIndex(i));
      j["boolean_network"]["outputs"] = jOutputs;

      if(testIO->HasOverriddenOutputFunctions()) {
         nlohmann::json jOverriddenFunctions = nlohmann::json::array();
         for(int n = 0; n < NETWORK_OUTPUT_COUNT; n++) {
            nlohmann::json jTruthTable = nlohmann::json::array();
            for(int k = 0; k < testBn->K2; k++)
               jTruthTable.push_back(testIO->GetOverriddenOutputFunctions(n, k));
            jOverriddenFunctions.push_back(jTruthTable);
         }
         j["boolean_network"]["overridden_output_functions"] = jOverriddenFunctions;
      }
   }

   printf("%s\n", j.dump().c_str());
   printStep++;
}

CFootBotBn::~CFootBotBn() {
   delete bestBn;
   delete testBn;
   delete bestIO;
   delete testIO;
}

void CFootBotBn::Reset() {
   
}

REGISTER_CONTROLLER(CFootBotBn, "footbot_controller")