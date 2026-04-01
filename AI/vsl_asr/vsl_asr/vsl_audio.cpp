#include "vsl_audio.h"

#include <algorithm>
#include <cmath>
#include <fstream>
#include <sstream>
#include <cstring>
#include <cstdint>

namespace vsl::audio
{
	static bool file_exists(const std::string& path)
	{
		std::ifstream f(path, std::ios::binary);
		return f.good();
	}

	static uint32_t read_u32_le(std::ifstream& in)
	{
		uint8_t b[4]{};
		in.read(reinterpret_cast<char*>(b), 4);
		return (uint32_t)b[0]
			| ((uint32_t)b[1] << 8)
			| ((uint32_t)b[2] << 16)
			| ((uint32_t)b[3] << 24);
	}

	static uint16_t read_u16_le(std::ifstream& in)
	{
		uint8_t b[2]{};
		in.read(reinterpret_cast<char*>(b), 2);
		return (uint16_t)b[0] | ((uint16_t)b[1] << 8);
	}

	static bool decode_wav_pcm16(const std::string& path, DecodedAudio& out, std::string err)
	{
		std::ifstream in(path, std::ios::binary);
		if (!in.good())
		{
			err = "Cannot open file: " + path;
			return false;
		}

		char riff[4]{};
		in.read(riff, 4);
		if (std::strncmp(riff, "RIFF", 4) != 0)
		{
			err = "Not a RIFF file.";
			return false;
		}

		(void)read_u32_le(in);

		char wave[4]{};
		in.read(wave, 4);
		if (std::strncmp(wave, "WAVE", 4) != 0)
		{
			err = "NOT a WAVE file";
			return false;
		}

        bool got_fmt = false;
        bool got_data = false;

        uint16_t audio_format = 0;
        uint16_t num_channels = 0;
        uint32_t sample_rate = 0;
        uint16_t bits_per_sample = 0;

        std::vector<int16_t> pcm16;

        while (in.good() && !(got_fmt && got_data)) {
            char chunk_id[4]{};
            in.read(chunk_id, 4);
            if (!in.good()) break;

            uint32_t chunk_size = read_u32_le(in);
            if (!in.good()) break;

            if (std::strncmp(chunk_id, "fmt ", 4) == 0) {
                audio_format = read_u16_le(in);
                num_channels = read_u16_le(in);
                sample_rate = read_u32_le(in);
                (void)read_u32_le(in);
                (void)read_u16_le(in);
                bits_per_sample = read_u16_le(in);

                const uint32_t read_bytes = 2 + 2 + 4 + 4 + 2 + 2;
                if (chunk_size > read_bytes) {
                    in.seekg(chunk_size - read_bytes, std::ios::cur);
                }

                got_fmt = true;
            }
            else if (std::strncmp(chunk_id, "data", 4) == 0) {
                if (chunk_size % 2 != 0) {
                    err = "Invalid WAV data chunk size.";
                    return false;
                }

                pcm16.resize(chunk_size / 2);
                in.read(reinterpret_cast<char*>(pcm16.data()), chunk_size);
                got_data = true;
            }
            else {
                in.seekg(chunk_size, std::ios::cur);
            }

            if (chunk_size % 2 == 1) {
                in.seekg(1, std::ios::cur);
            }
        }

        if (!got_fmt || !got_data) {
            err = "Missing fmt or data chunk.";
            return false;
        }

        if (audio_format != 1) {
            err = "Only PCM WAV is currently supported in fallback decoder.";
            return false;
        }

        if (bits_per_sample != 16) {
            err = "Only 16-bit PCM WAV is currently supported in fallback decoder.";
            return false;
        }

        out.sample_rate = static_cast<int>(sample_rate);
        out.channels = static_cast<int>(num_channels);
        out.samples.resize(pcm16.size());

        for (size_t i = 0; i < pcm16.size(); ++i) {
            out.samples[i] = std::clamp(static_cast<float>(pcm16[i]) / 32768.0f, -1.0f, 1.0f);
        }

        return true;
    }

    bool decode_audio_file(const std::string& path, DecodedAudio& out, std::string& err) {
        if (!file_exists(path)) {
            err = "Audio file not found: " + path;
            return false;
        }

        // 目前先做 WAV fallback
        // 下一步这里替换成 FFmpeg 解码即可
        return decode_wav_pcm16(path, out, err);
    }

    std::vector<float> to_mono(const std::vector<float>& interleaved, int channels) {
        if (channels <= 1) {
            return interleaved;
        }

        std::vector<float> mono;
        mono.reserve(interleaved.size() / channels);

        const size_t frames = interleaved.size() / static_cast<size_t>(channels);
        for (size_t i = 0; i < frames; ++i) {
            float sum = 0.0f;
            for (int ch = 0; ch < channels; ++ch) {
                sum += interleaved[i * channels + ch];
            }
            mono.push_back(sum / static_cast<float>(channels));
        }

        return mono;
    }

    std::vector<float> resample_linear(
        const std::vector<float>& mono,
        int src_rate,
        int dst_rate
    ) {
        if (src_rate <= 0 || dst_rate <= 0 || mono.empty()) {
            return {};
        }

        if (src_rate == dst_rate) {
            return mono;
        }

        const double ratio = static_cast<double>(dst_rate) / static_cast<double>(src_rate);
        const size_t out_count = static_cast<size_t>(std::ceil(mono.size() * ratio));

        std::vector<float> out;
        out.resize(out_count);

        for (size_t i = 0; i < out_count; ++i) {
            const double src_pos = static_cast<double>(i) / ratio;
            const size_t idx0 = static_cast<size_t>(src_pos);
            const size_t idx1 = std::min(idx0 + 1, mono.size() - 1);
            const double frac = src_pos - static_cast<double>(idx0);

            const float s0 = mono[idx0];
            const float s1 = mono[idx1];
            out[i] = static_cast<float>(s0 + (s1 - s0) * frac);
        }

        return out;
    }

    bool normalize_to_mono_f32(
        const DecodedAudio& in,
        int dst_rate,
        std::vector<float>& out,
        std::string& err
    ) {
        if (in.sample_rate <= 0) {
            err = "Invalid input sample rate.";
            return false;
        }
        if (in.channels <= 0) {
            err = "Invalid input channels.";
            return false;
        }
        if (in.samples.empty()) {
            err = "Empty decoded audio.";
            return false;
        }

        auto mono = to_mono(in.samples, in.channels);
        out = resample_linear(mono, in.sample_rate, dst_rate);

        if (out.empty()) {
            err = "Normalization failed.";
            return false;
        }

        return true;
    }
}