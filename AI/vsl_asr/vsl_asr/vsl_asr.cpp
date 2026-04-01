// vsl_asr.cpp: 定义应用程序的入口点。

#include "vsl_asr.h"

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

    static uint32_t read_u32_le(std::ifstream& in) {
        uint8_t b[4]{};
        in.read(reinterpret_cast<char*>(b), 4);
        return (uint32_t)b[0] | ((uint32_t)b[1] << 8) | ((uint32_t)b[2] << 16) | ((uint32_t)b[3] << 24);
    }

    static uint16_t read_u16_le(std::ifstream& in) {
        uint8_t b[2]{};
        in.read(reinterpret_cast<char*>(b), 2);
        return (uint16_t)b[0] | ((uint16_t)b[1] << 8);
    }

    struct WavPcm16 {
        int sample_rate = 0;
        int channels = 0;
        std::vector<int16_t> samples; // interleaved if channels>1 (but we reject >1)
    };

    // Minimal WAV parser: PCM16 only
    static bool load_wav_pcm16_mono_16k(const std::string& wav_path, WavPcm16& out, std::string& err) {
        std::ifstream in(wav_path, std::ios::binary);
        if (!in.good()) {
            err = "Cannot open wav file: " + wav_path;
            return false;
        }

        char riff[4]{};
        in.read(riff, 4);
        if (std::strncmp(riff, "RIFF", 4) != 0) {
            err = "Not a RIFF file.";
            return false;
        }

        (void)read_u32_le(in); // file size

        char wave[4]{};
        in.read(wave, 4);
        if (std::strncmp(wave, "WAVE", 4) != 0) {
            err = "Not a WAVE file.";
            return false;
        }

        bool got_fmt = false;
        bool got_data = false;

        uint16_t audio_format = 0;
        uint16_t num_channels = 0;
        uint32_t sample_rate = 0;
        uint16_t bits_per_sample = 0;

        std::vector<int16_t> pcm;

        while (in.good() && !(got_fmt && got_data)) {
            char chunk_id[4]{};
            in.read(chunk_id, 4);
            if (!in.good()) break;

            uint32_t chunk_size = read_u32_le(in);
            if (!in.good()) break;

            if (std::strncmp(chunk_id, "fmt ", 4) == 0) {
                // fmt chunk
                audio_format = read_u16_le(in);
                num_channels = read_u16_le(in);
                sample_rate = read_u32_le(in);
                (void)read_u32_le(in); // byte rate
                (void)read_u16_le(in); // block align
                bits_per_sample = read_u16_le(in);

                // Skip any remaining fmt bytes
                uint32_t read_bytes = 2 + 2 + 4 + 4 + 2 + 2;
                if (chunk_size > read_bytes) {
                    in.seekg(chunk_size - read_bytes, std::ios::cur);
                }

                got_fmt = true;
            }
            else if (std::strncmp(chunk_id, "data", 4) == 0) {
                // data chunk
                if (!got_fmt) {
                    // some files have fmt before data, but still: handle gracefully
                }

                // We only support PCM16
                if (chunk_size % 2 != 0) {
                    err = "Invalid data chunk size (not multiple of 2).";
                    return false;
                }

                pcm.resize(chunk_size / 2);
                in.read(reinterpret_cast<char*>(pcm.data()), chunk_size);
                got_data = true;
            }
            else {
                // skip unknown chunk
                in.seekg(chunk_size, std::ios::cur);
            }

            // chunk sizes are padded to even
            if (chunk_size % 2 == 1) {
                in.seekg(1, std::ios::cur);
            }
        }

        if (!got_fmt || !got_data) {
            err = "Missing fmt or data chunk in WAV.";
            return false;
        }

        if (audio_format != 1) {
            err = "Unsupported WAV format: only PCM (format=1) supported.";
            return false;
        }
        if (bits_per_sample != 16) {
            err = "Unsupported WAV bit depth: only 16-bit supported.";
            return false;
        }
        if (num_channels != 1) {
            err = "Unsupported channel count: only mono (1 channel) supported.";
            return false;
        }
        if (sample_rate != 16000) {
            std::ostringstream oss;
            oss << "Unsupported sample rate: " << sample_rate << ". Only 16000 Hz supported.";
            err = oss.str();
            return false;
        }

        out.sample_rate = (int)sample_rate;
        out.channels = (int)num_channels;
        out.samples = std::move(pcm);
        return true;
    }

    static std::vector<float> pcm16_to_f32(const std::vector<int16_t>& pcm16) {
        std::vector<float> f;
        f.reserve(pcm16.size());
        for (int16_t s : pcm16) {
            f.push_back(std::clamp((float)s / 32768.0f, -1.0f, 1.0f));
        }
        return f;
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

    std::string transcribe_wav(const std::string& wav_path) {
        if (!file_exists(wav_path)) {
            return "[VSL_ASR] Error: WAV file not found: " + wav_path;
        }

        WavPcm16 wav;
        std::string err;
        if (!load_wav_pcm16_mono_16k(wav_path, wav, err)) {
            return std::string("[VSL_ASR] Error: WAV load failed: ") + err;
        }

        auto f32 = pcm16_to_f32(wav.samples);
        return transcribe_pcm(f32);
    }

    void shutdown() {
        std::lock_guard<std::mutex> lock(g_mutex);

        if (g_ctx) {
            whisper_free(g_ctx);
            g_ctx = nullptr;
        }
    }

} // namespace vsl
