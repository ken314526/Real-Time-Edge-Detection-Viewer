cmake_minimum_required(VERSION 3.22.1)
project(visionprocessor)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(OpenCV REQUIRED core imgproc)

add_library(
        visionprocessor
        SHARED
        Processor.cpp
)

target_include_directories(
        visionprocessor
        PRIVATE
        ${OpenCV_INCLUDE_DIRS}
)

target_link_libraries(
        visionprocessor
        ${OpenCV_LIBS}
        log
        android
)

