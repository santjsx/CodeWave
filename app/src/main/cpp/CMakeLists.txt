cmake_minimum_required(VERSION 3.22.1)

project("viper_dsp_engine")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Aggressive optimizations for real-time mobile audio processing
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math -Wall -Werror=return-type")

add_library(
    viper_dsp_engine
    SHARED
    ViperDspEngine.cpp
    ViperJniWrapper.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    viper_dsp_engine
    ${log-lib}
)
