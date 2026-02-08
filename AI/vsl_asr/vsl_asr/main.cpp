#include <iostream>
#include "vsl_asr.h"

int main()
{
	vsl::init("AI/models/ggml-tiny.bin");
	std::cout << vsl::transcribe_wav("test.wav") << std::endl;
	vsl::shutdown();
}