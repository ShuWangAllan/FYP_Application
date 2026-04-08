package org.example.fyp;

public class AsrBridge {
    static 
    {
        System.load("C:\\Users\\ROG\\Documents\\GitHub\\FYP_Application\\AI\\vsl_asr\\out\\build\\x64-debug\\vsl_asr\\vsl_asr_plugin.dll");
    }
    public native boolean initModel(String modelPath);

    public native String transcribePcm16(short[] pcm, int sampleRate, int channelCount);
}
