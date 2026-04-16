cmake_minimum_required(VERSION 3.22.1)

project("whisperandroidtest")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Build whisper.cpp & ggml as STATIC
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

# Path to your whisper.cpp source tree
set(WHISPER_ROOT ${CMAKE_CURRENT_LIST_DIR}/../../../../../AI/runtime/whisper.cpp)

message(STATUS "WHISPER_ROOT = ${WHISPER_ROOT}")

if (NOT EXISTS ${WHISPER_ROOT}/CMakeLists.txt)
    message(FATAL_ERROR "whisper.cpp CMakeLists.txt not found at: ${WHISPER_ROOT}")
endif()

add_subdirectory(${WHISPER_ROOT} ${CMAKE_BINARY_DIR}/whisper_build)

# Detect whisper target name
set(_WHISPER_TARGET "")
if (TARGET whisper)
    set(_WHISPER_TARGET whisper)
elseif (TARGET whispercpp)
    set(_WHISPER_TARGET whispercpp)
elseif (TARGET whisper-lib)
    set(_WHISPER_TARGET whisper-lib)
elseif (TARGET libwhisper)
    set(_WHISPER_TARGET libwhisper)
endif()

if (_WHISPER_TARGET STREQUAL "")
    message(FATAL_ERROR
        "Could not find whisper.cpp target. Check ${WHISPER_ROOT}/CMakeLists.txt for add_library(...) name."
    )
endif()

message(STATUS "Using whisper target: ${_WHISPER_TARGET}")

add_library(
        whisperandroidtest
        SHARED
        native-lib.cpp
        asr_engine.cpp
)

target_include_directories(
        whisperandroidtest
        PRIVATE
        ${WHISPER_ROOT}/include
)

find_library(
        log-lib
        log
)

target_link_libraries(
        whisperandroidtest
        PRIVATE
        ${log-lib}
        ${_WHISPER_TARGET}
)