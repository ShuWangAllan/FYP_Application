#include <iostream>
#include <fstream>
#include <windows.h>
#include "vsl_asr.h"

int main() {
    SetConsoleOutputCP(CP_UTF8);
    SetConsoleCP(CP_UTF8);

    // TODO: change to your actual paths
    const std::string model_path =
        "C:/Users/ROG/Documents/GitHub/FYP_Application/AI/models/ggml-tiny.bin";

    const std::string wav_path =
        "C:/Users/ROG/Documents/GitHub/FYP_Application/AI/vsl_asr/vsl_asr/test.wav"; // put test.wav next to your exe, or give an absolute path

    std::cout << "VSL ASR Version: " << vsl::version() << "\n";
    std::cout << "Loading model: " << model_path << "\n";

    if (!vsl::init(model_path)) {
        std::cerr << "Init failed.\n";
        return 1;
    }

    std::cout << "Transcribing wav: " << wav_path << "\n";
    std::string text = vsl::transcribe_wav(wav_path);

    std::cout << "Result:\n" << text << "\n";

    std::ofstream fout("result_utf8.txt", std::ios::binary);
    fout << text;
    fout.close();

    vsl::shutdown();
    return 0;
}