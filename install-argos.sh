#!/bin/bash

cd ..
sudo apt-get install cmake libfreeimage-dev libfreeimageplus-dev qt5-default freeglut3-dev libxi-dev libxmu-dev liblua5.3-dev lua5.3 doxygen graphviz graphviz-dev asciidoc g++ git
git clone https://github.com/ilpincy/argos3.git argos3
cd argos3
git reset --hard 4376baa6fee9921601acc48101b4cab5385e6f94
mkdir build_simulator
cd build_simulator
cmake -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_INSTALL_PREFIX=/usr/local \
        -DARGOS_BUILD_FOR=simulator \
        -DARGOS_BUILD_NATIVE=ON \
        -DARGOS_THREADSAFE_LOG=ON \
        -DARGOS_DYNAMIC_LOADING=ON \
        -DARGOS_USE_DOUBLE=ON \
        -DARGOS_DOCUMENTATION=ON \
        -DARGOS_INSTALL_LDSOCONF=ON \
        ../src
make doc
sudo make install
sudo ldconfig
argos3 --version
