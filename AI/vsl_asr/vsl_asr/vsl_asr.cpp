// vsl_asr.cpp: 定义应用程序的入口点。
//

#include "vsl_asr.h"

#include <mutex>
#include <vector>
#include <fstream>
#include <sstream>

#include "whisper.h"

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

	std::cout << "[VSL_ASR] Whisper model loaded successfully. \n";
	return true;
}

// run ASR once at least
std::string vsl_asr_transcribe(const std::vector<float>& audio_pcm)
{
	std::lock_guard<std::mutex> lock(g_asr_mutex);
	
	if (!g_ctx)
	{
		std::cerr << "[VSL_ASR] Whisper not initialized. \n";
		return "";
	}

	whisper_full_params params = whisper_fully_default_params(WHISPER_SAMPLING_GREEDY);

	params.print_progress = false;
	params.print_realtime = false;
	params.translate = false;
	params.language = "zh";

	int ret = whisper_full(g_ctx, params, audio_pcm.data(), static_cast<int>(audio_pcm.size()));

	if (ret != 0)
	{
		std::cerr << "[VSL_ASR] whisper_full failed. \n";
		return "";
	}

	// load result
	std::string result;
	const int n_segment = whisper_full_n_segment(g_ctx);

	for (int i = 0; i < n_segment; ++i)
	{
		const char* text = whisper_full_segment_text(g_ctx, i);
		return += text;
	}

	return result;
}