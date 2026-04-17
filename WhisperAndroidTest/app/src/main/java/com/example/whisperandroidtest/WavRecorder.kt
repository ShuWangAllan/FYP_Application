package com.example.whisperandroidtest

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread

class WavRecorder(
    private val outputFile: File,
    private val sampleRate: Int = 16000
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    fun start(): Boolean {
        if (isRecording) return false

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return false
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2
        )

        val recorder = audioRecord ?: return false
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            return false
        }

        outputFile.parentFile?.mkdirs()

        RandomAccessFile(outputFile, "rw").use { raf ->
            raf.setLength(0)
            writeWavHeader(raf, sampleRate, 1, 16, 0)
        }

        recorder.startRecording()
        isRecording = true

        recordingThread = thread(start = true) {
            val buffer = ByteArray(minBufferSize)
            RandomAccessFile(outputFile, "rw").use { raf ->
                raf.seek(44)
                var totalAudioLen = 0L

                while (isRecording) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        raf.write(buffer, 0, read)
                        totalAudioLen += read
                    }
                }

                raf.seek(0)
                writeWavHeader(raf, sampleRate, 1, 16, totalAudioLen)
            }
        }

        return true
    }

    fun stop(): File? {
        if (!isRecording) return null

        isRecording = false

        try {
            recordingThread?.join()
        } catch (_: Exception) {
        }

        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }

        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }

        audioRecord = null
        recordingThread = null

        return outputFile
    }

    private fun writeWavHeader(
        raf: RandomAccessFile,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        totalAudioLen: Long
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        raf.writeBytes("RIFF")
        raf.writeInt(Integer.reverseBytes(totalDataLen.toInt()))
        raf.writeBytes("WAVE")

        raf.writeBytes("fmt ")
        raf.writeInt(Integer.reverseBytes(16))
        raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
        raf.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
        raf.writeInt(Integer.reverseBytes(sampleRate))
        raf.writeInt(Integer.reverseBytes(byteRate))
        raf.writeShort(java.lang.Short.reverseBytes((channels * bitsPerSample / 8).toShort()).toInt())
        raf.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())

        raf.writeBytes("data")
        raf.writeInt(Integer.reverseBytes(totalAudioLen.toInt()))
    }
}