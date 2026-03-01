#pragma once
#include <stddef.h>

#ifdef _WIN32
	#define VSL_API __declspec(dllexport)
#else
	#define VSL_API
#endif

#ifdef __cplusplus
extern "C" {
#endif

	VSL_API const char* vsl_version();

	// init/shutdown
	VSL_API int vsl_init(const char* model_path);
	VSL_API void vsl_shutdown();

	// transcribe
	VSL_API char* vsl_transcribe_wav(const char* wav_path);
	VSL_API char* vsl_transcribe_pcm_f32(const float* pcm, int n_samples);

	// memory
	VSL_API void vsl_free(void* p);

	// Error
	VSL_API const char* vsl_last_error();

#ifdef __cplusplus
}
#endif