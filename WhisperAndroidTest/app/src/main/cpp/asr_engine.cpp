#include "asr_engine.h"
#include "vsl_audio.h"
#include <whisper.h>
#include <vector>

static whisper_context * g_ctx = nullptr;

std::string test_engine()
{
    return "engine ready.";
}

bool init_model(const std::string& model_path)
{
    if (g_ctx != nullptr)
    {
        whisper_free(g_ctx);
        g_ctx = nullptr;
    }

    whisper_context_params params = whisper_context_default_params();
    g_ctx = whisper_init_from_file_with_params(model_path.c_str(), params);

    return g_ctx != nullptr;
}

std::string transcribe_wav(const std::string& wav_path)
{
    if (g_ctx == nullptr)
    {
        return "error: model not initialized";
    }

    vsl::audio::DecodedAudio decoded;
    std::string err;
    if (!vsl::audio::decode_audio_file(wav_path, decoded, err))
    {
        return "error: decode failed: " + err;
    }

    std::vector<float> mono;
    if (!vsl::audio::normalize_to_mono_f32(decoded, 16000, mono, err))
    {
        return "error: normalize failed: " + err;
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_progress = false;
    params.print_special = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.translate = false;
    params.language = "zh";

    if (whisper_full(g_ctx, params, mono.data(), static_cast<int>(mono.size())) != 0)
    {
        return "error: whisper_full failed";
    }

    const int n_segments = whisper_full_n_segments(g_ctx);
    std::string result;
    for (int i = 0; i < n_segments; ++i)
    {
        const char* text = whisper_full_get_segment_text(g_ctx, i);
        if (text) result += text;
    }

    if (result.empty())
    {
        return "(empty result)";
    }

    return result;
}