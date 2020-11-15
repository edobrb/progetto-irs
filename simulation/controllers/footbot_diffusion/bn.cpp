#include "bn.h"

#include <cmath>
#include <vector>

Bn::Bn(int n, int k, double p, bool selfLoops) {
    N = n;
    K = k;
    K2 = 1 << k;

    states = new bool[n];
    oldStates = new bool[n];

    functions = new bool*[n];
    connections = new int*[n];
    for (int i = 0; i < n; i++) {
        states[i] = rand() % 2 == 0; //50-50 initial states
        oldStates[i] = false;
        functions[i] = new bool[K2];
        connections[i] = new int[k];
        for (int j = 0; j < K2; j++) functions[i][j] = ((double)rand() / RAND_MAX) < p; //p biased function
        for (int j = 0; j < k; j++) {
            do {
                connections[i][j] = rand() % n; //Random topology
            } while(!selfLoops && connections[i][j] == i);
        }
    }
}
Bn::~Bn(){
    for (int i = 0; i < N; ++i) {
        delete [] functions[i];
        delete [] connections[i];
    }
    delete [] functions;
    delete [] connections;
    delete [] states;
    delete [] oldStates;
}
void Bn::Step() {
    for (int n = 0; n < N; n++) oldStates[n] = states[n];
    for (int n = 0; n < N; n++) {
        int truthTableColumn = 0;
        int* nodeConnections = connections[n];
        for (int k = 0; k < K; k++) truthTableColumn += (1 << k) & (-oldStates[nodeConnections[k]]);
        states[n] = functions[n][truthTableColumn];
    }
}
void Bn::CopyFrom(Bn* other) {
    for (int n = 0; n < N; n++) SetNodeState(n, other->GetNodeState(n));
    for (int n = 0; n < N; n++) SetOldNodeState(n, other->GetOldNodeState(n));
    for (int n = 0; n < N; n++) for (int k = 0; k < K; k++) SetConnectionIndex(n, k, other->GetConnectionIndex(n, k));
    for (int n = 0; n < N; n++) for (int k = 0; k < K2; k++) SetTruthTableEntry(n, k, other->GetTruthTableEntry(n, k));
}
void Bn::RewiresConnections(int count, bool selfLoops) {
    std::vector<int> edited(count);
    for(int i = 0; i < count; i++) {
        int n = rand() % N;
        int k = rand() % K;
        bool alreadyEdited = false;
        for(int j = 0; j < i && !alreadyEdited; j++) {
            alreadyEdited = (edited[j] == (n * K + k));
        }
        if(alreadyEdited) i--; //retry
        else {
            int node;
            do {
                node = rand() % N;
            } while(!selfLoops && node == n);
            SetConnectionIndex(n, k, node);
            edited[i] = n * K + k;
        }
    }          
}
void Bn::MutesFunctions(int count, bool keepBalanced) {      
    std::vector<int> edited(count);
    for(int i = 0; i < count; i++) {
        int n = rand() % N;
        int k = rand() % K2;
        bool alreadyEdited = false;
        for(int j = 0; j < i && !alreadyEdited; j++) {
            alreadyEdited = (edited[j] == (n * K2 + k));
        }
        if(alreadyEdited) i--; //retry
        else {
            bool value = !GetTruthTableEntry(n, k);
            SetTruthTableEntry(n, k, value);
            if(keepBalanced) {
                for(int j = 1; j < K2; j++) {
                    if(GetTruthTableEntry(n, (j + k) % K2) == value) {
                        SetTruthTableEntry(n, (j + k) % K2, !value);
                        break;
                    }
                }
            }
            edited[i] = n * K2 + k;
        }
    }   
}
bool Bn::GetOldNodeState(int index) {
    return oldStates[index];
}
void Bn::SetOldNodeState(int index, bool value) {
    oldStates[index] = value;
}
bool Bn::GetNodeState(int index) {
    return states[index];
}
void Bn::SetNodeState(int index, bool value){
    states[index] = value;
}
int Bn::GetConnectionIndex(int node, int index) {
    return connections[node][index];
}
void Bn::SetConnectionIndex(int node, int index, int value) {
    connections[node][index] = value;
}
bool Bn::GetTruthTableEntry(int node, int index) {
    return functions[node][index];
}
void Bn::SetTruthTableEntry(int node, int index, bool value) {
    functions[node][index] = value;
}
