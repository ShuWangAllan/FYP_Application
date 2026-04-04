// vsl_asr.cpp: implementation of the VSL ASR wrapper

#include "vsl_asr.h"
#include "vsl_audio.h"

#include <mutex>
#include <vector>
#include <fstream>
#include <sstream>
#include <iostream>
#include <cstdint>
#include <cstring>
#include <algorithm>
#include <thread>

#include "whisper.h"

namespace vsl {

    static std::mutex g_mutex;
    static whisper_context* g_ctx = nullptr;

    // Helpers
    static bool file_exists(const std::string& path) {
        std::ifstream f(path, std::ios::binary);
        return f.good();
    }

    // Public API
    const char* version() {
        return "vsl_asr wrapper 1.0 (whisper.cpp)";
    }

    bool init(const std::string& model_path) {
        std::lock_guard<std::mutex> lock(g_mutex);

        if (g_ctx) {
            // Already initialized
            return true;
        }

        if (!file_exists(model_path)) {
            std::cerr << "[VSL_ASR] Model file not found: " << model_path << "\n";
            return false;
        }

        whisper_context_params cparams = whisper_context_default_params();

        // CPU-only: avoids ggml backend abort issues on systems without proper GPU backend.
        cparams.use_gpu = false;
        cparams.flash_attn = false;

        g_ctx = whisper_init_from_file_with_params(model_path.c_str(), cparams);
        if (!g_ctx) {
            std::cerr << "[VSL_ASR] whisper_init_from_file_with_params failed.\n";
            return false;
        }

        std::cout << "[VSL_ASR] Model loaded successfully.\n";
        return true;
    }

    std::string transcribe_pcm(const std::vector<float>& audio_pcm) {
        std::lock_guard<std::mutex> lock(g_mutex);

        if (!g_ctx) {
            return "[VSL_ASR] Error: ASR not initialized. Call vsl::init(model_path) first.";
        }
        if (audio_pcm.empty()) {
            return "[VSL_ASR] Error: empty audio buffer.";
        }

        whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

        // Basic settings (tune later)
        params.print_progress = false;
        params.print_realtime = false;
        params.print_timestamps = false;
        params.translate = false;
        params.no_context = true;

        // Threads
        const unsigned hc = std::max(1u, std::thread::hardware_concurrency());
        params.n_threads = (int)hc;

        // Run
        int rc = whisper_full(g_ctx, params, audio_pcm.data(), (int)audio_pcm.size());
        if (rc != 0) {
            return "[VSL_ASR] Error: whisper_full failed.";
        }

        // Collect text
        const int n_segments = whisper_full_n_segments(g_ctx);
        std::string result;
        result.reserve(256);

        for (int i = 0; i < n_segments; ++i) {
            const char* seg = whisper_full_get_segment_text(g_ctx, i);
            if (seg) result += seg;
        }

        return result;
    }

    std::string transcribe_file(const std::string& audio_path) {
        if (!file_exists(audio_path)) {
            return "[VSL_ASR] Error: audio file not found: " + audio_path;
        }

        vsl::audio::DecodedAudio decoded;
        std::string err;

        if (!vsl::audio::decode_audio_file(audio_path, decoded, err)) {
            return std::string("[VSL_ASR] Error: decode failed: ") + err;
        }

        std::vector<float> f32;
        if (!vsl::audio::normalize_to_mono_f32(decoded, 16000, f32, err)) {
            return std::string("[VSL_ASR] Error: normalize failed: ") + err;
        }

        return transcribe_pcm(f32);
    }

    std::string transcribe_wav(const std::string& wav_path) {
        return transcribe_file(wav_path);
    }

    void shutdown() {
        std::lock_guard<std::mutex> lock(g_mutex);

        if (g_ctx) {
            whisper_free(g_ctx);
            g_ctx = nullptr;
        }
    }

} // namespace vsl