#pragma once
#include <string>

std::string test_engine();
bool init_model(const std::string& model_path);
std::string transcribe_wav(const std::string& wav_path);