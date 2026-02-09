#include <iostream>
#include "vsl_asr.h"

int main()
{
	vsl::init("AI/models/ggml-tiny.bin");
	
	std::string model_path = "models/ggml-tiny.bin";
	std::string wav_path = "text.wav";

	auto text = vsl::transcribe_wav(model_path, wav_path);

	vsl::shutdown();
}