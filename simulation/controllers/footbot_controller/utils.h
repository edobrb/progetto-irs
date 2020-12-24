#ifndef UTILS_H
#define UTILS_H
#include <argos3/core/utility/datatypes/datatypes.h>
#include <cmath>

/**
 * Counts how much wins its get with p probability per attempt.
 **/
int extract(int n, Real p) {
   int r = 0;
   for(int i = 0; i < n; i++) r += ((double)rand() / RAND_MAX) < p ? 1 : 0;
   return r;
}

/**
 * Rounds v with d decimal places.
 **/ 
inline Real round(Real v, int d) {
    double p = pow(10, d);
    return (Real)(floor(v * p + 0.5) / p);
}

/**
 * Gives the time t in which the event occur based on given frequency (lambda)
 * Eg. if lambda = 5 it means that the events fires 5 times / sec with an exponential probability.
 * E[X] = 1 / lambda, Var[X] = 1 / lambda^2.
 **/
Real eventTime(Real lambda) {
    double p = ((double)rand() / RAND_MAX);
    return -log(1 - p) / lambda;
}

#endif