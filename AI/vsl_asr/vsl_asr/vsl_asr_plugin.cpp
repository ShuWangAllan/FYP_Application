#include "vsl_asr_plugin.h"
#include "vsl_asr.h"

#include <mutex>
#include <string>
#include <vector>
#include <cstring>
#include <cstdlib>

static std::mutex g_mtx;
static thread_local std::string g_last_err;

static char* dup_cstr(const std::string& s)
{
	char* p = (char*)std::malloc(s.size() + 1);
	if (!p) return nullptr;
	std::memcpy(p, s.c_str(), s.size() + 1);
	return p;
}

extern "C"
{
	const char* vsl_version()
	{
		return vsl::version();
	}

	int vsl_init(const char* model_path)
	{
		if (!model_path)
		{
			g_last_err = "model_path is null";
			return 0;
		}

		std::lock_guard<std::mutex> lock(g_mtx);

		if (!vsl::init(std::string(model_path)))
		{
			g_last_err = "vsl::init failed (Check model path and runtime deps)";
			return 0;
		}
		g_last_err.clear();
		return 1;
	}

	void vsl_shutdown()
	{
		std::lock_guard<std::mutex> lock(g_mtx);
		vsl::shutdown();
	}

	char* vsl_transcribe_wav(const char* wav_path)
	{
		if (!wav_path)
		{
			g_last_err = "wav_path is null";
			return nullptr;
		}

		std::lock_guard<std::mutex> lock(g_mtx);

		std::string out = vsl::transcribe_wav(std::string(wav_path));
		if (out.rfind("[VSL_ASR] Error:", 0) == 0)
		{
			g_last_err = out;
			return nullptr;
		}

		g_last_err.clear();
		return dup_cstr(out);
	}

	char* vsl_transcribe_pcm_f32(const float* pcm, int n_samples)
	{
		if (!pcm || n_samples <= 0)
		{
			g_last_err = "pcm is null or n_samples <= 0";
			return nullptr;
		}
		std::lock_guard<std::mutex> lock(g_mtx);

		std::vector<float> v(pcm, pcm + n_samples);
		std::string out = vsl::transcribe_pcm(v);

		if (out.rfind("[VSL_ASR] Error:", 0) == 0)
		{
			g_last_err = out;
			return nullptr;
		}

		g_last_err.clear();
		return dup_cstr(out);
	}

	void vsl_free(void* p)
	{
		std::free(p);
	}

	const char* vsl_last_error()
	{
		return g_last_err.c_str();
	}
}