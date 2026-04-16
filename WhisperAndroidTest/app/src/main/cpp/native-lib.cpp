#include <jni.h>
#include <string>
#include "asr_engine.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_whisperandroidtest_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this*/)
{
    std::string msg = test_engine();
    return env -> NewStringUTF(msg.c_str());
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_whisperandroidtest_MainActivity_initModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath
        ){
    const char* pathChars = env->GetStringUTFChars(modelPath, nullptr);
    std::string modelPathStr = pathChars ? pathChars : "";
    env->ReleaseStringUTFChars(modelPath, pathChars);

    bool ok = init_model(modelPathStr);
    return ok ? JNI_TRUE : JNI_FALSE;
}