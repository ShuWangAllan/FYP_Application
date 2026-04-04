package org.example.fyp;

public class AudioTranscriber {
    public static String inspectFile(String path)
    {
        try 
        {
            DecodedPcm decoded = AudioDecoder.decodeToPcm16(path);
            return "decode ok\n"
                    + "sampleRate = " + decoded.sampleRate + "\n"
                    + "channelCount = " + decoded.channelCount + "\n"
                    + "pcm16.length = " + decoded.pcm16.length;
        }
        catch (Exception e)
        {
            return "decode failed: " + e.getMessage();
        }
    }

    public static String testNative(short[] pcm, int sampleRate, int channelCount)
    {
        try
        {
            AsrBridge bridge = new AsrBridge();
            return bridge.transcribePcm16(pcm, sampleRate, channelCount);
        }
        catch(Exception e)
        {
            return "transcribe failed: " + e.getMessage();
        }
    }
}
