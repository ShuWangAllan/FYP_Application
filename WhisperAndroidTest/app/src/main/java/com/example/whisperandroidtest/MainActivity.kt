package com.example.whisperandroidtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("whisperandroidtest")
        }
    }

    external fun stringFromJNI(): String
    external fun initModel(modelPath: String): Boolean
    external fun transcribeFile(wavPath: String): String

    private var wavRecorder: WavRecorder? = null
    private val recordPermissionCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                VisibleSpokenLanguageApp()
            }
        }
    }

    @Composable
    fun VisibleSpokenLanguageApp() {
        var currentScreen by remember { mutableStateOf("welcome") }
        var headerText by remember { mutableStateOf("Visible Spoken Language — Prototype") }
        var statusText by remember { mutableStateOf("Ready") }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF101012)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Header(headerText = headerText, statusText = statusText)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentScreen) {
                        "welcome" -> WelcomeScreen(
                            onStart = { currentScreen = "practice" },
                            onInfo = { currentScreen = "info" },
                            onExit = { finish() }
                        )

                        "info" -> InfoScreen(
                            onBack = { currentScreen = "welcome" },
                            onPractice = { currentScreen = "practice" }
                        )

                        "practice" -> PracticeScreen(
                            setStatus = { statusText = it },
                            onBack = { currentScreen = "welcome" }
                        )
                    }
                }

                Footer()
            }
        }
    }

    @Composable
    fun Header(headerText: String, statusText: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF1C1C21))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headerText,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = statusText,
                color = Color(0xFFCCD6E6),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @Composable
    fun Footer() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(Color(0xFF1C1C21))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Android Prototype",
                color = Color(0xFFB3BFCC),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    @Composable
    fun WelcomeScreen(
        onStart: () -> Unit,
        onInfo: () -> Unit,
        onExit: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101012))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Visible Spoken Language",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "让语言学习更具视觉化：通过语音与面部识别，\n在手机上训练发音、声调与口型。",
                color = Color(0xFFE6EBF5),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text("开始学习")
                }
                Button(onClick = onInfo, modifier = Modifier.fillMaxWidth()) {
                    Text("功能介绍")
                }
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                    Text("退出")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    @Composable
    fun InfoScreen(
        onBack: () -> Unit,
        onPractice: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101012))
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "功能介绍",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "• 发音练习：实时显示音高曲线与目标四声轮廓的偏差（后续接入）\n\n" +
                        "• 口型提示：相机捕捉面部关键点，给出口形反馈（后续接入）\n\n" +
                        "• 学习单元：按音节与声调组织练习（后续接入）\n\n" +
                        "• 进度记录：保存每次练习结果与曲线（后续接入）",
                color = Color(0xFFE6EBF5),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.weight(1f, fill = true))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("返回")
                }
                Button(onClick = onPractice, modifier = Modifier.weight(1f)) {
                    Text("去练习")
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PracticeScreen(
        setStatus: (String) -> Unit,
        onBack: () -> Unit
    ) {
        var syllable by remember { mutableStateOf("") }
        var selectedTone by remember { mutableStateOf("Tone 1") }
        var feedbackText by remember { mutableStateOf("Idle") }
        var running by remember { mutableStateOf(false) }

        val toneOptions = listOf("Tone 1", "Tone 2", "Tone 3", "Tone 4")
        var toneExpanded by remember { mutableStateOf(false) }

        fun resetState() {
            running = false
            feedbackText = "Idle"
            setStatus("Ready")
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101012))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxHeight()
                    .background(Color(0xFF1F1F24), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Camera Preview (占位)",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF26262E), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF3A3A44), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Camera Area", color = Color(0xFFCCD6E6))
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = syllable,
                    onValueChange = { syllable = it },
                    label = { Text("Syllable (e.g. ma)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                )

                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = toneExpanded,
                    onExpandedChange = { toneExpanded = !toneExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTone,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tone") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = toneExpanded,
                        onDismissRequest = { toneExpanded = false }
                    ) {
                        toneOptions.forEach { tone ->
                            DropdownMenuItem(
                                text = { Text(tone) },
                                onClick = {
                                    selectedTone = tone
                                    toneExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (!running) {
                            if (!hasRecordPermission()) {
                                feedbackText = "Need record permission"
                                setStatus("Requesting permission...")
                                requestRecordPermission()
                                return@Button
                            }

                            val outFile = File(filesDir, "recorded.wav")
                            wavRecorder = WavRecorder(outFile)

                            val started = wavRecorder?.start() == true
                            if (started) {
                                running = true
                                feedbackText = "Recording..."
                                setStatus("Recording...")
                            } else {
                                feedbackText = "Record start failed"
                                setStatus("Error")
                            }
                        } else {
                            running = false
                            setStatus("Processing...")

                            val recordedFile = wavRecorder?.stop()
                            wavRecorder = null

                            if (recordedFile == null || !recordedFile.exists()) {
                                feedbackText = "Record file storage failed"
                                setStatus("Error")
                                return@Button
                            }

                            thread {
                                try {
                                    val modelFile = copyAssetToInternalStorage("ggml-tiny.bin")
                                    if (modelFile == null) {
                                        runOnUiThread {
                                            feedbackText = "Model copy failed"
                                            setStatus("Error")
                                        }
                                        return@thread
                                    }

                                    runOnUiThread { setStatus("Loading model...") }
                                    val initOk = initModel(modelFile.absolutePath)
                                    if (!initOk) {
                                        runOnUiThread {
                                            feedbackText = "Model init failed"
                                            setStatus("Error")
                                        }
                                        return@thread
                                    }

                                    runOnUiThread { setStatus("Transcribing...") }
                                    val result = transcribeFile(recordedFile.absolutePath)

                                    runOnUiThread {
                                        feedbackText = result
                                        setStatus("Done")
                                    }
                                } catch (e: Exception) {
                                    runOnUiThread {
                                        feedbackText = "Exception: ${e.message}"
                                        setStatus("Error")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (running) "Stop" else "Start")
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxHeight()
                    .background(Color(0xFF1F1F24), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Feedback:",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.width(90.dp)
                    )
                    Text(
                        text = feedbackText,
                        color = Color(0xFF4CE680),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Pitch Contour (占位示意)",
                    color = Color(0xFFE6EBF5),
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF26262E), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF3A3A44), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "将来这里画音高曲线 / 目标曲线",
                        color = Color(0xFFCCD6E6)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                        Text("返回")
                    }
                    OutlinedButton(
                        onClick = { resetState() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("重置")
                    }
                }
            }
        }
    }

    private fun hasRecordPermission(): Boolean{
        return checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordPermission(){
        requestPermissions(
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            recordPermissionCode
        )
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