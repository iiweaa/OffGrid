cmake_minimum_required(VERSION 3.22.1)
project("offgrid_opus")

set(CMAKE_CXX_STANDARD 17)

# Build libopus as a static library (default), without tests/programs.
set(OPUS_BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(OPUS_BUILD_PROGRAMS OFF CACHE BOOL "" FORCE)
set(OPUS_BUILD_SHARED_LIBRARY OFF CACHE BOOL "" FORCE)

add_subdirectory(opus)

add_library(opus_jni SHARED opus_jni.cpp)

target_include_directories(opus_jni PRIVATE opus/include)

target_link_libraries(opus_jni opus log)
