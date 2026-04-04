package org.example.fyp;

public class DecodedPcm {
    public int sampleRate;
    public int channelCount;
    public short[] pcm16;

    public DecodedPcm(int sampleRate, int channelCount, short[] pcm16)
    {
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.pcm16 = pcm16;
    }
}
