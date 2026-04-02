package org.example.fyp;

public class Main {
    public static void main(String[] args)
    {
        String path = "test.m4a";
        String result = AudioTranscriber.inspectFile(path);
        System.out.println(result);
    }
}
