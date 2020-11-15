#ifndef BN_IO_H
#define BN_IO_H
#include "bn.h"

class BnIO {
    public:
        BnIO(int inputCount, int outputCount, Bn* bn, bool nodeOverlap, bool overrideOutputFunctions, double p);
        ~BnIO();
        void CopyFrom(BnIO* io, Bn* bn);

        //IO
        void PushInput(Bn* bn, int index, bool value);
        bool GetOutput(Bn* bn, int index);

        //mutation
        void Rewires(Bn* bn, int inputRewires, int outputRewires, bool nodeOverlap);
        
        //interface
        void SetInputIndex(int index, int value);
        void SetOutputIndex(int index, int value);
        void SetOverriddenTruthTableEntry(int index, int k, bool value);
        int GetInputNodeIndex(int index);
        int GetOutputNodeIndex(int index);
        bool GetOverriddenOutputFunctions(int n, int k);
        bool HasOverriddenOutputFunctions();
        int InputCount, OutputCount;
    private:
        int* inputNodes;
        int* outputNodes;
        bool** overriddenOutputFunctions;
};

#endif
