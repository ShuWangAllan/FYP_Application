#pragma once

#include <string>
#include <vector>

namespace vsl::audio
{
	struct DecodedAudio
	{
		int sample_rate = 0;
		int channels = 0;
		std::vector<float> samples; // interleaved if channels > 1;
	};

	// Decode any audio files
	bool decode_audio_file(const std::string& path, DecodedAudio& out, std::string& err);

	// change channel into 1
	std::vector<float> to_mono(const std::vector <float>& interleaved, int channels);

	std::vector<float> resample_linear(
		const std::vector<float>& mono,
		int dst_rate,
		std::vector<float>& out,
		std::string& err
	);
} // namespace vsl::audio