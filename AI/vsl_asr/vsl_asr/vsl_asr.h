// vsl_asr.h: 标准系统包含文件的包含文件
// 或项目特定的包含文件。

#pragma once

#include <iostream>
#include <string>

// TODO: 

namespace vsl
{
	const char* vsl_version();

	// init: load model
	// model pat
	bool init(const std::string& model_path);

	// load wav file
	std::string transcribe_wav(const std::string& model_path, const std::string& wav_path);

	// release sources
	void shutdown();
}