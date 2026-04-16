package com.example.whisperandroidtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("whisperandroidtest")
        }
    }

    external fun stringFromJNI(): String
    external fun initModel(modelPath: String): Boolean
    external fun transcribeFile(wavPath: String): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var finalText = "onCreate entered"

        try {
            val testMsg = stringFromJNI()
            finalText = "native ok: $testMsg"

            val modelFile = copyAssetToInternalStorage("ggml-tiny.bin")
            if (modelFile == null) {
                finalText += "\nmodel copy failed"
            } else {
                finalText += "\nmodel file: ${modelFile.absolutePath}"

                val wavFile = copyAssetToInternalStorage("test_short.wav")
                if (wavFile == null) {
                    finalText += "\nwav copy failed"
                } else {
                    finalText += "\nwav file: ${wavFile.absolutePath}"

                    val initOk = initModel(modelFile.absolutePath)
                    finalText += "\ninit: $initOk"

                    if (initOk) {
                        val result = transcribeFile(wavFile.absolutePath)
                        finalText += "\nresult:\n$result"
                    }
                }
            }
        } catch (e: Exception) {
            finalText = "exception:\n${e.javaClass.simpleName}\n${e.message}"
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = finalText,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    private fun copyAssetToInternalStorage(fileName: String): File? {
        return try {
            val outFile = File(filesDir, fileName)
            assets.open(fileName).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}