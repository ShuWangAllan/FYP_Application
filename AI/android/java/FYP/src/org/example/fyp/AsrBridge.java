package org.example.fyp;

public class AsrVrudge {
    static 
    {
        System.loadLibrary("vsl_asr_plugin");
    }

    public native String transcribePcm16(short[] pcm, int sampleRate, int channelCount);
}
