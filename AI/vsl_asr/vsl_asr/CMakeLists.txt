cmake_minimum_required(VERSION 3.16)

# 你的 whisper.cpp 整包根目录（AI/runtime/whisper.cpp）
set(WHISPER_ROOT ${CMAKE_CURRENT_LIST_DIR}/../../runtime/whisper.cpp)
message(STATUS "WHISPER_ROOT = ${WHISPER_ROOT}")

# 收集 whisper + ggml 源码（必须包含 .c 和 .cpp）
file(GLOB WHISPER_SOURCES
    ${WHISPER_ROOT}/*.c
    ${WHISPER_ROOT}/*.cpp
    ${WHISPER_ROOT}/src/*.c
    ${WHISPER_ROOT}/src/*.cpp
)


file(GLOB GGML_SOURCES
    ${WHISPER_ROOT}/ggml/src/*.c
    ${WHISPER_ROOT}/ggml/src/*.cpp
)

add_executable(vsl_asr
    vsl_asr.cpp
    vsl_asr.h
    main.cpp
    ${WHISPER_SOURCES}
    ${GGML_SOURCES}
)

# 头文件目录：whisper.h 在 whisper.cpp/include
target_include_directories(vsl_asr PRIVATE
    ${WHISPER_ROOT}/include
    ${WHISPER_ROOT}/ggml/include
    ${WHISPER_ROOT}/src
    ${WHISPER_ROOT}
)


# Windows/MSVC：用 UTF-8 编译源文件
if (MSVC)
    target_compile_options(vsl_asr PRIVATE /utf-8)
endif()

# C++ 标准
set_property(TARGET vsl_asr PROPERTY CXX_STANDARD 20)

target_compile_definitions(vsl_asr PRIVATE
    GGML_VERSION=\"unknown\"
    GGML_COMMIT=\"unknown\"
)

target_compile_definitions(vsl_asr PRIVATE
    WHISPER_VERSION=\"unknown\"
)
