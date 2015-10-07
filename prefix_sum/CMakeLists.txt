cmake_minimum_required(VERSION 3.3)
project(prefix_sum)

set(CMAKE_MODULE_PATH .)
find_package(OpenCL REQUIRED)
include_directories(${OPENCL_INCLUDE_DIRS})

if(NOT OPENCL_LIBRARIES)
    set(OPENCL_LIBRARIES /System/Library/Frameworks/OpenCL.framework)
endif(NOT OPENCL_LIBRARIES)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Werror -Wno-deprecated-declarations")

set(CMAKE_SOURCE_DIR ./src)
set(CMAKE_RESOURCE_DIR ./resources)
set(SOURCE_FILES ${CMAKE_SOURCE_DIR}/main.cpp)

add_executable(${PROJECT_NAME} ${SOURCE_FILES})
target_link_libraries(${PROJECT_NAME} ${OPENCL_LIBRARIES})

configure_file(${CMAKE_RESOURCE_DIR}/prefix_sum.cl ${CMAKE_BINARY_DIR}/prefix_sum.cl COPYONLY)
configure_file(${CMAKE_RESOURCE_DIR}/input.txt ${CMAKE_BINARY_DIR}/input.txt COPYONLY)