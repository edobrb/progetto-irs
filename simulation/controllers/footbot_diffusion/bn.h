#ifndef BN_H
#define BN_H

class Bn {
    public:
        Bn(int n, int k, double p, bool selfLoops) {
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
        void Step() {
            for (int n = 0; n < N; n++) oldStates[n] = states[n];
            for (int n = 0; n < N; n++) {
                int truthTableColumn = 0;
                int* nodeConnections = connections[n];
                for (int k = 0; k < K; k++) truthTableColumn += (1 << k) & (-oldStates[nodeConnections[k]]);
                states[n] = functions[n][truthTableColumn];
            }
        }
        ~Bn(){
            for (int i = 0; i < N; ++i) {
                delete [] functions[i];
                delete [] connections[i];
            }
            delete [] functions;
            delete [] connections;
            delete [] states;
            delete [] oldStates;
        }

        void CopyFrom(Bn* other) {
            for (int n = 0; n < N; n++) SetNodeState(n, other->GetNodeState(n));
            for (int n = 0; n < N; n++) SetOldNodeState(n, other->GetOldNodeState(n));
            for (int n = 0; n < N; n++) for (int k = 0; k < K; k++) SetConnectionIndex(n, k, other->GetConnectionIndex(n, k));
            for (int n = 0; n < N; n++) for (int k = 0; k < K2; k++) SetTruthTableEntry(n, k, other->GetTruthTableEntry(n, k));
        }
        inline bool GetOldNodeState(int index) {
            return oldStates[index];
        }
        inline void SetOldNodeState(int index, bool value) {
            oldStates[index] = value;
        }
        inline bool GetNodeState(int index) {
            return states[index];
        }
        inline void SetNodeState(int index, bool value){
            states[index] = value;
        }
        inline int GetConnectionIndex(int node, int index) {
            return connections[node][index];
        }
        inline void SetConnectionIndex(int node, int index, int value) {
            connections[node][index] = value;
        }
        inline bool GetTruthTableEntry(int node, int index) {
            return functions[node][index];
        }
        inline void SetTruthTableEntry(int node, int index, bool value) {
            functions[node][index] = value;
        }
        int N, K, K2;
    private:
        bool* states;
        bool* oldStates;
        bool** functions;
        int** connections;
        
};

#endif