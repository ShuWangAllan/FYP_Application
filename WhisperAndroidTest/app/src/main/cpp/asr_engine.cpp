#include "asr_engine.h"
#include <whisper.h>

static whisper_context * g_ctx = nullptr;

std::string test_engine()
{
    return "engine ready.";
}

bool init_model(const std::string& model_path)
{
    if (g_ctx != nullptr)
    {
        whisper_free(g_ctx);
        g_ctx = nullptr;
    }

    whisper_context_params params = whisper_context_default_params();
    g_ctx = whisper_init_from_file_with_params(model_path.c_str(), params);

    return g_ctx != nullptr;
}