#include "vsl_asr_plugin.h"
#include "vsl_asr.h"

#include <jni.h>
#include <mutex>
#include <string>
#include <vector>
#include <cstring>
#include <cstdlib>
#include <sstream>
#include <algorithm>
#include <cstdint>

static std::mutex g_mtx;
static thread_local std::string g_last_err;

static char* dup_cstr(const std::string& s)
{
	char* p = (char*)std::malloc(s.size() + 1);
	if (!p) return nullptr;
	std::memcpy(p, s.c_str(), s.size() + 1);
	return p;
}

static std::vector<float> pcm16_to_f32(const std::vector<int16_t>& pcm16)
{
	std::vector<float> out;
	out.reserve(pcm16.size());

	for (int16_t s : pcm16)
	{
		out.push_back(std::clamp((float)s / 32768.0f, -1.0f, 1.0f));
	}

	return out;
}

static std::vector<float> to_mono(const std::vector<float>& interleaved, int channels)
{
	if (channels <= 1)
	{
		return interleaved;
	}

	std::vector<float> mono;
	mono.reserve(interleaved.size() / channels);

	size_t frames = interleaved.size() / (size_t)channels;
	for (size_t i = 0; i < frames; i++)
	{
		float sum = 0.0f;
		for (int ch = 0; ch < channels; ++ch)
		{
			sum += interleaved[i * channels + ch];
		}
		mono.push_back(sum / (float)channels);
	}
	return mono;
}

static std::vector<float> resample_linear(
	const std::vector<float>& mono,
	int src_rate,
	int dst_rate)
{
	if (src_rate <= 0 || dst_rate <= 0 || mono.empty())
	{
		return {};
	}

	if (src_rate == dst_rate)
	{
		return mono;
	}

	const double ratio = (double)dst_rate / (double)src_rate;
	const size_t out_count = (size_t)std::ceil(mono.size() * ratio);

	std::vector<float> out(out_count);

	for (size_t i = 0; i < out_count; ++i)
	{
		const double src_pos = (double)i / ratio;
		const size_t idx0 = (size_t)src_pos;
		const size_t idx1 = std::min(idx0 + 1, mono.size() - 1);
		const double frac = src_pos - (double)idx0;

		const float s0 = mono[idx0];
		const float s1 = mono[idx1];
		out[i] = (float)(s0 + (s1 - s0) * frac);
	}

	return out;
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

	JNIEXPORT jstring JNICALL Java_org_example_fyp_AsrBridge_transcribePcm16
	(
		JNIEnv* env,
		jobject /* thiz */,
		jshortArray pcmArray,
		jint sampleRate,
		jint channelCount
	)
	{
		if (pcmArray == nullptr)
		{
			return env->NewStringUTF("native error: pcmArray is null");
		}

		jsize len = env->GetArrayLength(pcmArray);
		if (len <= 0)
		{
			return env->NewStringUTF("native error: pcmArray is empty");
		}

		std::vector<int16_t> pcm16((size_t)len);
		env->GetShortArrayRegion(
			pcmArray,
			0,
			len,
			reinterpret_cast<jshort*>(pcm16.data())
		);

		auto f32 = pcm16_to_f32(pcm16);
		auto mono = to_mono(f32, (int)channelCount);
		auto mono16k = resample_linear(mono, (int)sampleRate, 16000);

		if (mono16k.empty())
		{
			return env->NewStringUTF("native error: audio normalize failed");
		}

		std::lock_guard<std::mutex> lock(g_mtx);
		std::string out = vsl::transcribe_pcm(mono16k);

		if (out.empty())
		{
			return env->NewStringUTF("native error: empty transcription result");
		}

		return env->NewStringUTF(out.c_str());
	}

	JNIEXPORT jboolean JNICALL Java_org_example_fyp_AsrBridge_initModel
	(
		JNIEnv* env,
		jobject /* thiz */,
		jstring modelPath
	)
	{
		if (modelPath == nullptr)
		{
			g_last_err = "modelPath is null";
			return JNI_FALSE;
		}

		const char* pathChars = env->GetStringUTFChars(modelPath, nullptr);
		if (!pathChars)
		{
			g_last_err = "GetStringUTFChars failed";
			return JNI_FALSE;
		}

		std::string path(pathChars);
		env->ReleaseStringUTFChars(modelPath, pathChars);

		std::lock_guard<std::mutex> lock(g_mtx);

		if (!vsl::init(path))
		{
			g_last_err = "vsl::init failed";
			return JNI_FALSE;
		}

		g_last_err.clear();
		return JNI_TRUE;
	}
}