package org.example.fyp;

public class Main {
    public static void main(String[] args)
    {
        short[] pcm = new short[] {100, 200, -100, -200, 300, -300};
        int sampleRate = 44100;
        int channelCount = 2;
        
        try
        {
            AsrBridge bridge = new AsrBridge();
            String result = bridge.transcribePcm16(pcm, sampleRate, channelCount);
            System.out.println("Native test result: " + result);
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }
    }
}
