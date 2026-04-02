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
}
