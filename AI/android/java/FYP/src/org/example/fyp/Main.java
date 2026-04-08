package org.example.fyp;

public class Main {
    public static void main(String[] args)
    {
        short[] pcm = new short[] {100, 200, -100, -200, 300, -300};
        int sampleRate = 44100;
        int channelCount = 2;
        
        String modelPath = "C:\\Users\\ROG\\Documents\\GitHub\\FYP_Application\\AI\\models\\ggml-tiny.bin";

        try
        {
            AsrBridge bridge = new AsrBridge();
            
            boolean ok = bridge.initModel(modelPath);
            System.out.println("initModel result: " + ok);

            String result = bridge.transcribePcm16(pcm, sampleRate, channelCount);
            System.out.println("Native test result:" + result);
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }
    }
}
