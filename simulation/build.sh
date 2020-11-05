#!/bin/bash

mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make && cd .. && argos3 -c experiments/diffusion_10.argos
