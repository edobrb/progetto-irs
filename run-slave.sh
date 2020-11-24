#!/bin/bash

git checkout develop
git pull origin develop
cd simulation
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make
cd ../../analyzer
sbt "runMain Remote working_dir=../simulation client=true address=$1 port=$2 threads=$3"
cd ..
./$0 $1 $2 $3
