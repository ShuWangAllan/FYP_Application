package org.example.fyp;

public class Main {
    public static void main(String[] args)
    {
        short[] pcm = new short[] {100, 200, -100, -200, 300, -300};
        int sampleRate = 44100;
        int channelCount = 2;

        String result = AudioTranscriber.testNative(pcm, sampleRate, channelCount);
        System.out.println(result);
    }
}
