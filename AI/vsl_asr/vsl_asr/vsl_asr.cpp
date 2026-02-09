// vsl_asr.cpp: 定义应用程序的入口点。

#include "vsl_asr.h"

#include <mutex>
#include <vector>
#include <fstream>
#include <sstream>
#include <iostream>
#include <cstdint>
#include <cstring>

#include "whisper.h"

namespace
{
    std::mutex g_asr_mutex;
    whisper_context* g_ctx = nullptr;

    // ---- little-endian readers ----
    static uint16_t read_u16(std::ifstream& in) {
        uint8_t b[2]; in.read(reinterpret_cast<char*>(b), 2);
        return uint16_t(b[0]) | (uint16_t(b[1]) << 8);
    }

    static uint32_t read_u32(std::ifstream& in) {
        uint8_t b[4]; in.read(reinterpret_cast<char*>(b), 4);
        return uint32_t(b[0]) |
            (uint32_t(b[1]) << 8) |
            (uint32_t(b[2]) << 16) |
            (uint32_t(b[3]) << 24);
    }

    // only support：WAV (RIFF) / mono / 16kHz / PCM16 or float32
    static bool load_wav_mono_16k(const std::string& wav_path, std::vector<float>& out_pcm) {
        std::ifstream in(wav_path, std::ios::binary);
        if (!in) return false;

        char riff[4]; in.read(riff, 4);
        if (in.gcount() != 4 || std::memcmp(riff, "RIFF", 4) != 0) return false;

        (void)read_u32(in); // file size

        char wave[4]; in.read(wave, 4);
        if (in.gcount() != 4 || std::memcmp(wave, "WAVE", 4) != 0) return false;

        uint16_t audio_format = 0;       // 1=PCM, 3=float
        uint16_t num_channels = 0;
        uint32_t sample_rate = 0;
        uint16_t bits_per_sample = 0;
        std::vector<uint8_t> data;

        while (in && !in.eof()) {
            char chunk_id[4];
            in.read(chunk_id, 4);
            if (in.gcount() != 4) break;

            uint32_t chunk_size = read_u32(in);

            if (std::memcmp(chunk_id, "fmt ", 4) == 0) {
                audio_format = read_u16(in);
                num_channels = read_u16(in);
                sample_rate = read_u32(in);
                (void)read_u32(in); // byte rate
                (void)read_u16(in); // block align
                bits_per_sample = read_u16(in);

                // skip extra fmt bytes if any
                if (chunk_size > 16) {
                    in.seekg(chunk_size - 16, std::ios::cur);
                }
            }
            else if (std::memcmp(chunk_id, "data", 4) == 0) {
                data.resize(chunk_size);
                in.read(reinterpret_cast<char*>(data.data()), chunk_size);
            }
            else {
                // skip unknown chunk
                in.seekg(chunk_size, std::ios::cur);
            }
        }

        if (data.empty()) return false;
        if (num_channels != 1) return false;
        if (sample_rate != 16000) return false;

        out_pcm.clear();

        if (audio_format == 1 && bits_per_sample == 16) {
            // PCM 16-bit signed
            const size_t n = data.size() / 2;
            out_pcm.resize(n);
            for (size_t i = 0; i < n; ++i) {
                int16_t s;
                std::memcpy(&s, &data[i * 2], 2);
                out_pcm[i] = float(s) / 32768.0f;
            }
            return true;
        }

        if (audio_format == 3 && bits_per_sample == 32) {
            // IEEE float 32-bit
            const size_t n = data.size() / 4;
            out_pcm.resize(n);
            for (size_t i = 0; i < n; ++i) {
                float f;
                std::memcpy(&f, &data[i * 4], 4);
                out_pcm[i] = f;
            }
            return true;
        }

        return false;
    }
}

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
        std::cerr << "[VSL_ASR] Whisper already initialized.\n";
        return true;
    }

    g_ctx = whisper_init_from_file(model_path.c_str());

    if (!g_ctx)
    {
        std::cerr << "[VSL_ASR] Failed to load whisper model: " << model_path << std::endl;
        return false;
    }

    std::cout << "[VSL_ASR] Whisper model loaded successfully.\n";
    return true;
}

// run ASR once at least
std::string vsl_asr_transcribe(const std::vector<float>& audio_pcm)
{
    std::lock_guard<std::mutex> lock(g_asr_mutex);

    if (!g_ctx)
    {
        std::cerr << "[VSL_ASR] Whisper not initialized.\n";
        return "";
    }

    if (audio_pcm.empty())
    {
        std::cerr << "[VSL_ASR] Empty audio buffer.\n";
        return "";
    }

    // Default params
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.translate = false;

    // Using Chinese by default
    params.language = "zh";

    int ret = whisper_full(g_ctx, params, audio_pcm.data(), static_cast<int>(audio_pcm.size()));

    if (ret != 0)
    {
        std::cerr << "[VSL_ASR] whisper_full failed.\n";
        return "";
    }

    // load result
    std::string result;
    const int n_segment = whisper_full_n_segments(g_ctx);

    for (int i = 0; i < n_segment; ++i)
    {
        const char* text = whisper_full_get_segment_text(g_ctx, i);
        if (text) result += text;
    }

    return result;
}

namespace vsl
{
    std::string version()
    {
        return vsl_version();
    }

    bool init(const std::string& model_path)
    {
        return vsl_asr_init(model_path);
    }

    std::string transcribe(const std::vector<float>& audio_pcm)
    {
        return vsl_asr_transcribe(audio_pcm);
    }

    std::string transcribe_wav(const std::string& model_path, const std::string& wav_path)
    {
        // init (idempotent)
        if (!init(model_path)) {
            return "";
        }

        std::vector<float> pcm;
        if (!load_wav_mono_16k(wav_path, pcm)) {
            std::cerr << "[VSL_ASR] Failed to load wav (need mono 16kHz PCM16/float32): "
                << wav_path << std::endl;
            return "";
        }

        return transcribe(pcm);
    }

    void shutdown()
    {
        std::lock_guard<std::mutex> lock(g_asr_mutex);

        if (g_ctx)
        {
            whisper_free(g_ctx);
            g_ctx = nullptr;
        }
    }

    const char* vsl_version()
    {
        return "VSL_ASR v0.1";
    }
} // namespace vsl
