CXX=g++
CXX_FLAGS=--std=c++11 -Wall -Werror -Wno-deprecated-declarations

ifeq ($(shell uname),Darwin)
LD_FLAGS=-framework OpenCL
else
LD_FLAGS=-lcl
endif

SOURCE_DIR=./src/
RESOURCE_DIR=./resources/
BINARY_DIR=./bin/

TARGET=main

all: ${BINARY_DIR}${TARGET}

${BINARY_DIR}${TARGET}: ${SOURCE_DIR}${TARGET}.cpp ${SOURCE_DIR}matrix_t.h ${SOURCE_DIR}cl.hpp
	$(CXX) $(CXX_FLAGS) $(LD_FLAGS) ${SOURCE_DIR}${TARGET}.cpp -o ${BINARY_DIR}${TARGET}
	cp ${RESOURCE_DIR}* ${BINARY_DIR}

clean:
	rm ${BINARY_DIR}*
