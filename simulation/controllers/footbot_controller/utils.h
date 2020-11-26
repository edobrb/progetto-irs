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
    double p = pow(10, d);
    return (Real)(floor(v * p + 0.5) / p);
}

#endif