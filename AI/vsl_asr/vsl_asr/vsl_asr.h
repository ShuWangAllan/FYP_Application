#pragma once

#include <string>
#include <vector>

struct AudioBuffer {
	int sample_rate = 0;
	int channels = 0;
	std::vector<float> samples; // interleaved if channel > 1;
};

namespace vsl {

	// Simple version string for your wrapper.
	const char* version();

	// Load Whisper model once (CPU-only in this wrapper)
	bool init(const std::string& model_path);

	// Keep old API for compatibility
	std::string transcribe_wav(const std::string& wav_path);

	// Transcribe audio from PCM float samples
	// Requirements:
	// - mono
	// - 16000 Hz
	// - float samples in [-1, 1]
	std::string transcribe_pcm(const std::vector<float>& audio_pcm);

	std::string transcribe_file(const std::string& audio_path);

	// Release model context
	void shutdown();

} // namespace vsl