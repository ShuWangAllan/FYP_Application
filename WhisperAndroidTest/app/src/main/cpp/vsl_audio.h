#pragma once

#include <string>
#include <vector>

namespace vsl::audio
{
	struct DecodedAudio
	{
		int sample_rate = 0;
		int channels = 0;
		std::vector<float> samples;
	};

	bool decode_audio_file(const std::string& path, DecodedAudio& out, std::string& err);

	std::vector<float> to_mono(const std::vector<float>& interleaved, int channels);

	std::vector<float> resample_linear(
			const std::vector<float>& mono,
			int src_rate,
			int dst_rate
	);

	bool normalize_to_mono_f32(
			const DecodedAudio& in,
			int dst_rate,
			std::vector<float>& out,
			std::string& err
	);
}