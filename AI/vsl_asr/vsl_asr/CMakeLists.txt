# CMakeList.txt: vsl_asr 的 CMake 项目，在此处包括源代码并定义
# 项目特定的逻辑。
#

# 将源代码添加到此项目的可执行文件。
add_executable (vsl_asr "vsl_asr.cpp" "vsl_asr.h" "main.cpp"
${CMAKE_CURRENT_LIST_DIR}/../../runtime/whisper/src/whisper.cpp
)

target_include_directories(vsl_asr PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../../runtime/whisper/include)

if (CMAKE_VERSION VERSION_GREATER 3.12)
  set_property(TARGET vsl_asr PROPERTY CXX_STANDARD 20)
endif()

if (MSVC)
  target_compile_options(vsl_asr PRIVATE /utf-8)
endif()


# TODO: 如有需要，请添加测试并安装目标。
