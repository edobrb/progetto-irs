#ifndef BN_H
#define BN_H

//#include "argos3/core/utility/datatypes/datatypes.h"
class Bn {
    public:
        Bn(int n, int k, double p) {
            N = n;
            K = k;
            K2 = 1 << k;

            states = new bool[n];
            oldStates = new bool[n];

            functions = new bool*[n];
            connections = new int*[n];
            for (int i = 0; i < n; i++) {
                states[i] = rand() % 2 == 0;
                oldStates[i] = false;

                functions[i] = new bool[K2];
                connections[i] = new int[k];

                for (int j = 0; j < K2; j++) functions[i][j] = ((double)rand() / RAND_MAX) < p;
                for (int j = 0; j < k; j++) connections[i][j] = rand() % n;
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
        void SetNode(int index, bool value){
            states[index] = value;
        }
        bool GetNode(int index) {
            return states[index];
        }
        Bn* Clone() {
            Bn* newBn = new Bn(N, K, 0);
            for (int n = 0; n < N; n++) {
                newBn->states[n] = states[n];
                newBn->oldStates[n] = oldStates[n];
                for (int k = 0; k < K; k++) newBn->connections[n][k] = connections[n][k];
                for (int k = 0; k < K2; k++) newBn->functions[n][k] = functions[n][k];
            }
            return newBn;
        }
        void Step() {
            for (int n = 0; n < N; n++) oldStates[n] = states[n];
            for (int n = 0; n < N; n++) {
                int f = 0;
                for (int k = 0; k < K; k++) {//if (connections[n, k] != -1)
                    f += (1 << k) * (oldStates[connections[n][k]] ? 1 : 0);
                    //f += (1 << k) & (-oldStates[connections[n][k]]);
                }
                states[n] = functions[n][f];
            }
        }
    private:
        bool* states;
        bool* oldStates;
        bool** functions;
        int** connections;
        int N, K, K2;
};

#endif