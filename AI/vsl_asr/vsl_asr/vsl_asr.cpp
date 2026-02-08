// vsl_asr.cpp: 定义应用程序的入口点。
//

#include "vsl_asr.h"

#include <mutex>
#include <vector>
#include <fstream>
#include <sstream>

#include "../whisper.h"

using namespace std;

std::string vsl_version()
{
	return "VSL_ASR v0.1";
}

// init ASR(load whisper model)
bool vsl_asr_init(const std::string& model_path)
{
	std::lock_guard<std::mutex> lock(g_asr_mutex);

	if (g_ctx != nullptr)
	{
		std::cerr << "[VSL_ASR] Whisper already initialized. \n";
		return true;
	}

	g_ctx = whisper_init_from_file(model_path.c_str());

	if (!g_ctx)
	{
		std::cerr << "[VSL_ASR] Failed to load whisper model: " << model_path << std::endl;
		return false;
	}
}
