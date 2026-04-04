cmake_minimum_required(VERSION 3.16)
project(vsl_asr LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Build whisper.cpp & ggml as STATIC to avoid runtime missing DLLs
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

set(WHISPER_ROOT ${CMAKE_CURRENT_LIST_DIR}/../../runtime/whisper.cpp)
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

# Build DLL plugin (for Python)
add_library(vsl_asr_plugin SHARED
    vsl_asr.cpp
    vsl_asr.h
    vsl_asr_plugin.cpp
    vsl_asr_plugin.h
)

target_include_directories(vsl_asr_plugin PRIVATE
    ${WHISPER_ROOT}/include
)

target_link_libraries(vsl_asr_plugin PRIVATE ${_WHISPER_TARGET})

if (MSVC)
    target_compile_options(vsl_asr_plugin PRIVATE /utf-8)
endif()

# Build demo executable (for debugging)
add_executable(vsl_asr_demo
    main.cpp
)

target_link_libraries(vsl_asr_demo PRIVATE vsl_asr_plugin)

if (MSVC)
    target_compile_options(vsl_asr_demo PRIVATE /utf-8)
endif()