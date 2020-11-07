#ifndef BN_HANG_H
#define BN_HANG_H

#include "bn.h"
#include <algorithm>

class BnHang {
    public:
        BnHang(int inputCount, int outputCount, Bn* bn, bool nodeOverlap, bool overrideOutputFunctions, double p) {
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
                    for(int k = 0; k < bn->K2; k++) {
                        overriddenOutputFunctions[i][k] = ((double)rand() / RAND_MAX) < p;
                    }
                }
            }
            
        }
        ~BnHang(){
            if(overriddenOutputFunctions != nullptr) {
                for (int i = 0; i < OutputCount; ++i) {
                    delete [] overriddenOutputFunctions[i];
                }
                delete [] overriddenOutputFunctions;
            }
            delete [] inputNodes;
            delete [] outputNodes;
        }

        inline void PushInput(Bn* bn, int index, bool value) {
            bn->SetNodeState(inputNodes[index], value);
        }
        inline bool GetOutput(Bn* bn, int index) {
            if(overriddenOutputFunctions == nullptr) return bn->GetNodeState(outputNodes[index]);
            else {
                int truthTableColumns = 0;
                for (int k = 0; k < bn->K; k++) {
                    int connection = bn->GetConnectionIndex(outputNodes[index], k);
                    truthTableColumns += (1 << k) * (bn->GetOldNodeState(connection) ? 1 : 0);
                }
                return overriddenOutputFunctions[index][truthTableColumns];
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
                nodes.push_back(inputNodes[extracted]);
            }

            //Select 'outputRewires' input nodes to rewire
            std::vector<int> outputsIndex(OutputCount);
            std::iota(std::begin(outputsIndex), std::end(outputsIndex), 0);
            std::vector<int> outputToRewires(outputRewires);
            for(int i = 0; i < outputRewires; i++) {
                int extracted = outputsIndex[rand() % outputsIndex.size()];
                outputToRewires[i] = extracted;
                outputsIndex.erase(std::remove(outputsIndex.begin(), outputsIndex.end(), extracted), outputsIndex.end());
                nodes.push_back(outputNodes[extracted]);
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
        void CopyFrom(BnHang* hang, Bn* bn) {
            for(int i = 0; i < InputCount; i++) inputNodes[i] = hang->inputNodes[i];
            for(int i = 0; i < OutputCount; i++) outputNodes[i] = hang->outputNodes[i];
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
    private:
        int* inputNodes;
        int* outputNodes;
        bool** overriddenOutputFunctions;
};

#endif