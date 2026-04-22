cmake_minimum_required(VERSION 3.22.1)

project("vula")

add_library(
        vula-native
        SHARED
        vula-native.cpp
        sha256.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        vula-native
        ${log-lib})
