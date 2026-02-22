cmake_minimum_required(VERSION 3.16)
project(vsl_asr LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

message(STATUS "WHISPER_ROOT exists? $<BOOL:${WHISPER_ROOT}>")

# whisper.cpp root
set(WHISPER_ROOT ${CMAKE_CURRENT_LIST_DIR}/../../runtime/whisper.cpp)
message(STATUS "WHISPER_ROOT = ${WHISPER_ROOT}")

# --- Add whisper.cpp as a subproject ---
# Build into a separate binary dir under your build tree
add_subdirectory(${WHISPER_ROOT} ${CMAKE_BINARY_DIR}/whisper_build)

# --- Your executable ---
add_executable(vsl_asr
    main.cpp
    vsl_asr.cpp
    vsl_asr.h
)

# Includes for whisper public headers
target_include_directories(vsl_asr PRIVATE
    ${WHISPER_ROOT}/include
)

# Windows/MSVC: UTF-8 source encoding
if (MSVC)
    target_compile_options(vsl_asr PRIVATE /utf-8)
endif()

# -------- Force CPU-only usage from your wrapper side --------
# (Even if whisper supports GPU, your code can stay CPU-only by params.use_gpu=false)
# Still, you can add extra safe defs here if you want:
target_compile_definitions(vsl_asr PRIVATE
    GGML_VERSION="unknown"
    GGML_COMMIT="unknown"
    WHISPER_VERSION="unknown"
)

# --- Link against whisper.cpp target ---
# Different versions export different target names. Try common ones.
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
        "Could not find whisper.cpp CMake target. "
        "Expected one of: whisper, whispercpp, whisper-lib, libwhisper. "
        "Open ${WHISPER_ROOT}/CMakeLists.txt to check the exported target name."
    )
endif()

if (WIN32)
    add_custom_command(TARGET vsl_asr POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:${_WHISPER_TARGET}>
        $<TARGET_FILE_DIR:vsl_asr>
    )
endif()

message(STATUS "Linking vsl_asr with whisper target: ${_WHISPER_TARGET}")
target_link_libraries(vsl_asr PRIVATE ${_WHISPER_TARGET})