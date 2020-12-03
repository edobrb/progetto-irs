#ifndef BN_H
#define BN_H

class Bn {
    public:
        Bn(int n, int k, double p, bool selfLoops, bool onlyDistinctConnection);
        ~Bn();
        void CopyFrom(Bn* other);

        //update
        void Step();

        //Mutation
        void RewiresConnections(int count, bool selfLoops, bool onlyDistinctConnection);
        void MutesFunctions(int count, bool keepBalanced);
        void ResetStates(double p);

        //Interface
        bool GetOldNodeState(int index);
        void SetOldNodeState(int index, bool value);
        bool GetNodeState(int index) ;
        void SetNodeState(int index, bool value);
        int GetConnectionIndex(int node, int index);
        void SetConnectionIndex(int node, int index, int value);
        bool GetTruthTableEntry(int node, int index);
        void SetTruthTableEntry(int node, int index, bool value);
        int N, K, K2;
    private:
        bool* states;
        bool* oldStates;
        bool** functions;
        int** connections;
};

#endif