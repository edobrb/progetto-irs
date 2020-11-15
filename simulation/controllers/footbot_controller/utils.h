#ifndef UTILS_H
#define UTILS_H
#include <argos3/core/utility/datatypes/datatypes.h>
#include <cmath>

int extract(int n, Real p) {
   int r = 0;
   for(int i = 0; i < n; i++) r += ((double)rand() / RAND_MAX) < p ? 1 : 0;
   return r;
}
inline Real round(Real v, int d) {
    return (Real)(((int)(v * pow(10, d) + .5)) / pow(10, d));
}

#endif