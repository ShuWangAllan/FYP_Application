#include <iostream>
#include <fstream>
#include "vsl_asr.h"

// For test
static bool file_exists(const std::string& path) {
	std::ifstream f(path, std::ios::binary);
	return f.good();
}


int main()
{
	/*vsl::init("AI/models/ggml-tiny.bin");*/
	
	std::string model_path = "C:/Users/ROG/Documents/GitHub/FYP_Application/AI/models/ggml-tiny.bin";
	/*std::string wav_path = "text.wav";*/

	/*auto text = vsl::transcribe_wav(model_path, wav_path);*/


	// Mini test
	std::cout << "Loading Model: " << model_path << "\n";
	bool ok = vsl::init(model_path);
	std::cout << "Init: " << (ok ? "ok" : "Failed") << "\n";

	vsl::shutdown();

	return 0;
}