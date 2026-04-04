#pragma once

#include <string>
#include <vector>

namespace vsl {

	// Simple version string for your wrapper.
	const char* version();

	// Load Whisper model once (CPU-only in this wrapper)
	bool init(const std::string& model_path);

	// Transcribe audio from WAV file
	// Requirements (current minimal parser):
	// - RIFF/WAVE
	// - PCM int16
	// - Mono (1 channel)
	// - 16000 Hz
	std::string transcribe_wav(const std::string& wav_path);

	// Transcribe audio from PCM float samples
	// Requirements:
	// - mono
	// - 16000 Hz
	// - float samples in [-1, 1]
	std::string transcribe_pcm(const std::vector<float>& audio_pcm);

	// Release model context
	void shutdown();

} // namespace vsl