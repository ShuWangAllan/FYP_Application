package org.example.fyp;

public class Main {
    public static void main(String[] args)
    {
        short[] pcm = new short[44100];
        int sampleRate = 44100;
        int channelCount = 2;

        for (int i = 0; i < pcm.length; i++)
        {
            pcm[i] = (short)(Math.sin(i * 0.05) * 2000);
        }
        
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
