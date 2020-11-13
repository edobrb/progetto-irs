#ifndef BN_HANDLES_H
#define BN_HANDLES_H

#include "bn.h"
#include <algorithm>

class BnHandles {
    public:
        BnHandles(int inputCount, int outputCount, Bn* bn, bool nodeOverlap, bool overrideOutputFunctions, double p) {
            InputCount = inputCount;
            OutputCount = outputCount;
            inputNodes = new int[InputCount];
            outputNodes = new int[OutputCount];
            overriddenOutputFunctions = overrideOutputFunctions ? new bool*[OutputCount] : nullptr;
            
            std::vector<int> nodes(bn->N);
            std::iota(std::begin(nodes), std::end(nodes), 0);

            for(int i = 0; i < InputCount; i++) {
                if(nodeOverlap) {
                    inputNodes[i] = rand() % bn->N; //Select random input node
                } else {
                    int extracted = nodes[rand() % nodes.size()];
                    inputNodes[i] = extracted; //Select random input node from free nodes
                    nodes.erase(std::remove(nodes.begin(), nodes.end(), extracted), nodes.end());
                }
            }
            for(int i = 0; i < OutputCount; i++) {
                if(nodeOverlap) {
                    outputNodes[i] = rand() % bn->N; //Select random output node
                } else {
                    int extracted = nodes[rand() % nodes.size()];
                    outputNodes[i] = extracted; //Select random output node from free nodes
                    nodes.erase(std::remove(nodes.begin(), nodes.end(), extracted), nodes.end());
                }

                if(overriddenOutputFunctions != nullptr) {
                    overriddenOutputFunctions[i] = new bool[bn->K2];

                    //p-(1-p) distribution
                    std::vector<int> toAssign(bn->K2);
                    std::iota(std::begin(toAssign), std::end(toAssign), 0);
                    for(int k = 0; k < (int)(bn->K2 * p + .5); k++) {
                        int extracted = toAssign[rand() % toAssign.size()];
                        toAssign.erase(std::remove(toAssign.begin(), toAssign.end(), extracted), toAssign.end());
                        overriddenOutputFunctions[i][extracted] = true;
                    }
                    for(int k = 0; k < (int)(bn->K2 * (1 - p)); k++) {
                        overriddenOutputFunctions[i][toAssign[k]] = false;
                    }
                }
            }
            
        }
        ~BnHandles(){
            if(overriddenOutputFunctions != nullptr) {
                for (int i = 0; i < OutputCount; ++i) {
                    delete [] overriddenOutputFunctions[i];
                }
                delete [] overriddenOutputFunctions;
            }
            delete [] inputNodes;
            delete [] outputNodes;
        }

        inline void SetInputIndex(int index, int value) {
            inputNodes[index] = value;
        }
        inline void SetOutputIndex(int index, int value) {
            outputNodes[index] = value;
        }
        inline void SetOverriddenTruthTableEntry(int index, int k, bool value) {
            overriddenOutputFunctions[index][k] = value;
        }
        inline void PushInput(Bn* bn, int index, bool value) {
            bn->SetNodeState(inputNodes[index], value);
        }
        inline bool GetOutput(Bn* bn, int index) {
            if(overriddenOutputFunctions == nullptr) return bn->GetNodeState(outputNodes[index]);
            else {
                int truthTableColumn = 0;
                int nodeIndex = outputNodes[index];
                for (int k = 0; k < bn->K; k++) {
                    int connection = bn->GetConnectionIndex(nodeIndex, k);
                    truthTableColumn += (1 << k) * (bn->GetOldNodeState(connection) ? 1 : 0);
                }
                return overriddenOutputFunctions[index][truthTableColumn];
            }
        }
        void Rewires(Bn* bn, int inputRewires, int outputRewires, bool nodeOverlap) {
            //Find free nodes
            std::vector<int> nodes(bn->N);
            std::iota(std::begin(nodes), std::end(nodes), 0);
            for(int i = 0; i < InputCount; i++) nodes.erase(std::remove(nodes.begin(), nodes.end(), inputNodes[i]), nodes.end());
            for(int i = 0; i < OutputCount; i++) nodes.erase(std::remove(nodes.begin(), nodes.end(), outputNodes[i]), nodes.end());

            //Select 'inputRewires' input nodes to rewire
            std::vector<int> inputsIndex(InputCount);
            std::iota(std::begin(inputsIndex), std::end(inputsIndex), 0);
            std::vector<int> inputToRewires(inputRewires);
            for(int i = 0; i < inputRewires; i++) {
                int extracted = inputsIndex[rand() % inputsIndex.size()];
                inputToRewires[i] = extracted;
                inputsIndex.erase(std::remove(inputsIndex.begin(), inputsIndex.end(), extracted), inputsIndex.end());
                //nodes.push_back(inputNodes[extracted]);
            }

            //Select 'outputRewires' input nodes to rewire
            std::vector<int> outputsIndex(OutputCount);
            std::iota(std::begin(outputsIndex), std::end(outputsIndex), 0);
            std::vector<int> outputToRewires(outputRewires);
            for(int i = 0; i < outputRewires; i++) {
                int extracted = outputsIndex[rand() % outputsIndex.size()];
                outputToRewires[i] = extracted;
                outputsIndex.erase(std::remove(outputsIndex.begin(), outputsIndex.end(), extracted), outputsIndex.end());
                //nodes.push_back(outputNodes[extracted]);
            }

            //Rewires the node by selecting one by one from the pool 'nodes'
            for(int i = 0; i < inputRewires; i++) {
                int extracted = nodes[rand() % nodes.size()];
                nodes.erase(std::remove(nodes.begin(), nodes.end(), extracted), nodes.end());
                inputNodes[inputToRewires[i]] = nodeOverlap ? rand() % bn->N : extracted;
            }
            for(int i = 0; i < outputRewires; i++) {
                int extracted = nodes[rand() % nodes.size()];
                nodes.erase(std::remove(nodes.begin(), nodes.end(), extracted), nodes.end());
                outputNodes[outputToRewires[i]] = nodeOverlap ? rand() % bn->N : extracted;
            }
        }
        void CopyFrom(BnHandles* hang, Bn* bn) {
            for(int i = 0; i < InputCount; i++) inputNodes[i] = hang->inputNodes[i];
            for(int i = 0; i < OutputCount; i++) outputNodes[i] = hang->outputNodes[i];
            if(overriddenOutputFunctions != nullptr)
                for(int i = 0; i < OutputCount; i++) for(int k = 0; k < bn->K2; k++) overriddenOutputFunctions[i][k] = hang->overriddenOutputFunctions[i][k];
        }
        
        int InputCount, OutputCount;
        inline int GetInputNodeIndex(int index) {
            return inputNodes[index];
        }
        inline int GetOutputNodeIndex(int index) {
            return outputNodes[index];
        }
        inline bool GetOverriddenOutputFunctions(int n, int k) {
            return overriddenOutputFunctions[n][k];
        }
        inline bool HasOverriddenOutputFunctions() {
            return overriddenOutputFunctions != nullptr;
        }
    private:
        int* inputNodes;
        int* outputNodes;
        bool** overriddenOutputFunctions;
};

#endif