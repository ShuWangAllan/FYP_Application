package com.example.whisperandroidtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("whisperandroidtest")
        }
    }

    external fun stringFromJNI(): String
    external fun initModel(modelPath: String): Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val testMsg = stringFromJNI()

        val copiedFile = copyAssetToInternalStorage("ggml-tiny.bin")
        val initOk = if (copiedFile != null) {
            initModel(copiedFile.absolutePath)
        } else {
            false
        }

        val finalText = if (initOk && copiedFile != null) {
            "$testMsg\nfile copied: ${copiedFile.absolutePath}\ninit model success"
        } else {
            "$testMsg\ninit model failed"
        }

        setContent {
            Text(text = finalText)
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