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

    private val AppBg = Color(0xFF101012)
    private val PanelBg = Color(0xFF1F1F24)
    private val BoxBg = Color(0xFF26262E)

    private val PrimaryText = Color.White
    private val SecondaryText = Color(0xFFE6EBF5)
    private val HintText = Color(0xFFCCD6E6)
    private val AccentGreen = Color(0xFF4CE680)
    private val BorderColor = Color(0xFF3A3A44)

    companion object {
        init {
            System.loadLibrary("whisperandroidtest")
        }
    }

    enum class PracticeState {
        Idle,
        Recording,
        Transcribing
    }

    data class CharInfo(
        val hanzi: String,
        val pinyin: String,
        val tone: Int
    )

    enum class CompareStatus{
        Correct,
        ToneMismatch,
        PronMismatch,
        Missing,
        Extra
    }

    data class CompareItem(
        val targetChar: String?,
        val recognizedChar: String?,
        val status: CompareStatus,
        val message: String
    )

    data class LearningTarget(
        val english: String,
        val chinese: String,
        val pinyin: String
    )

    private val charMap = mapOf(
        "你" to CharInfo("你", "ni", 3),
        "好" to CharInfo("好", "hao", 3),
        "妈" to CharInfo("妈", "ma", 1),
        "麻" to CharInfo("麻", "ma", 2),
        "马" to CharInfo("马", "ma", 3),
        "骂" to CharInfo("骂", "ma", 4),
        "我" to CharInfo("我", "wo", 3),
        "喜" to CharInfo("喜", "xi", 3),
        "欢" to CharInfo("欢", "huan", 1),
        "汉" to CharInfo("汉", "han", 4),
        "语" to CharInfo("语", "yu", 3),
        "谢" to CharInfo("谢", "xie", 4),
        "中" to CharInfo("中", "zhong", 1),
        "国" to CharInfo("国", "guo", 2),
        "学" to CharInfo("学", "xue", 2),
        "习" to CharInfo("习", "xi", 2)
    )

    private val englishTargetMap = mapOf(
        "hello" to LearningTarget("hello", "你好", "nǐ hǎo"),
        "thank you" to LearningTarget("thank you", "谢谢", "xiè xie"),
        "mother" to LearningTarget("mother", "妈妈", "mā ma"),
        "china" to LearningTarget("china", "中国", "zhōng guó"),
        "study" to LearningTarget("study", "学习", "xué xí"),
        "i like chinese" to LearningTarget("I like Chinese", "我喜欢汉语", "wǒ xǐ huān hàn yǔ")
    )

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
            color = AppBg
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
                .background(PanelBg)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headerText,
                color = PrimaryText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = statusText,
                color = HintText,
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
                .background(PanelBg)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Android Prototype",
                color = HintText,
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
                .background(AppBg)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Visible Spoken Language",
                color = PrimaryText,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "让语言学习更具视觉化：通过语音与面部识别，\n在手机上训练发音、声调与口型。",
                color = SecondaryText,
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
                .background(AppBg)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "功能介绍",
                color = PrimaryText,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "• 发音练习：录音后进行基础转录与发音训练\n\n" +
                        "• 声调练习：结合四声进行基础朗读训练\n\n" +
                        "• 学习单元：按由易到难的语言难度组织练习\n\n" +
                        "• 进度扩展：后续可加入曲线、口型与记录功能",
                color = SecondaryText,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "学习路径（由易到难）",
                color = PrimaryText,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BoxBg, RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text =
                        "Stage 1: Single Syllable\n" +
                                "从最基础的单音节开始，例如 ma / ba / da。\n" +
                                "目标：先让学习者熟悉音节发音。\n\n" +
                                "Stage 2: Tone Practice\n" +
                                "练习同一音节的四声变化，例如 mā / má / mǎ / mà。\n" +
                                "目标：强化声调辨识与控制。\n\n" +
                                "Stage 3: Word Practice\n" +
                                "进入双音节或简单词语，例如 妈妈 / 中国 / 学习。\n" +
                                "目标：把单音节能力迁移到真实词语。\n\n" +
                                "Stage 4: Sentence Practice\n" +
                                "练习简单短句，例如 你好 / 我喜欢汉语。\n" +
                                "目标：提升连贯表达与完整语音输出。",
                    color = HintText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
        var englishInput by remember { mutableStateOf("hello") }
        var selectedTone by remember { mutableStateOf("Tone 1") }
        var feedbackText by remember { mutableStateOf("Idle") }
        var correctionText by remember { mutableStateOf("暂无纠错结果") }
        var practiceState by remember { mutableStateOf(PracticeState.Idle) }

        var targetEnglish by remember { mutableStateOf("hello") }
        var targetChinese by remember { mutableStateOf("你好") }
        var targetPinyin by remember { mutableStateOf("nǐ hǎo") }

        val toneOptions = listOf("Tone 1", "Tone 2", "Tone 3", "Tone 4")
        var toneExpanded by remember { mutableStateOf(false) }

        fun updateTargetFromEnglish(input: String) {
            val key = input.trim().lowercase()
            val matched = englishTargetMap[key]
            if (matched != null) {
                targetEnglish = matched.english
                targetChinese = matched.chinese
                targetPinyin = matched.pinyin
            } else {
                targetEnglish = input
                targetChinese = "未收录该英文目标"
                targetPinyin = "-"
            }
        }

        fun resetState() {
            if (practiceState == PracticeState.Transcribing) return

            try {
                wavRecorder?.stop()
            } catch (_: Exception) {
            }
            wavRecorder = null

            practiceState = PracticeState.Idle
            feedbackText = "Idle"
            correctionText = "暂无纠错结果"
            setStatus("Ready")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBg)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PanelBg, RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Learning Target",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BoxBg, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "English: $targetEnglish",
                            color = PrimaryText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Chinese: $targetChinese",
                            color = AccentGreen,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Pinyin: $targetPinyin",
                            color = HintText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text(
                    text = "Learning Notes",
                    color = SecondaryText,
                    style = MaterialTheme.typography.titleMedium
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BoxBg, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "请先输入英文目标，系统会自动生成中文与拼音。然后点击 Start 进行录音练习。完成后系统会返回基础转写与纠错结果。",
                        color = HintText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "Feedback",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(BoxBg, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = feedbackText,
                        color = AccentGreen,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Text(
                    text = "Correction Result",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(BoxBg, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = correctionText,
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                OutlinedTextField(
                    value = englishInput,
                    onValueChange = {
                        englishInput = it
                        updateTargetFromEnglish(it)
                    },
                    label = { Text("English Input") },
                    placeholder = { Text("例如：hello / thank you / I like Chinese", color = HintText) },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                )

                ExposedDropdownMenuBox(
                    expanded = toneExpanded,
                    onExpandedChange = { toneExpanded = !toneExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTone,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tone") },
                        colors = appTextFieldColors(),
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
                                text = { Text(tone, color = PrimaryText) },
                                onClick = {
                                    selectedTone = tone
                                    toneExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        when (practiceState) {
                            PracticeState.Idle -> {
                                if (!hasRecordPermission()) {
                                    feedbackText = "需要录音权限"
                                    setStatus("Requesting permission...")
                                    requestRecordPermission()
                                    return@Button
                                }

                                if (targetChinese.isBlank() || targetChinese == "未收录该英文目标") {
                                    feedbackText = "请先输入已收录的英文目标"
                                    correctionText = "暂无纠错结果"
                                    setStatus("Target not found")
                                    return@Button
                                }

                                clearTemporaryAudioFiles()

                                try {
                                    wavRecorder?.stop()
                                } catch (_: Exception) {
                                }
                                wavRecorder = null

                                val outFile = File(filesDir, "recorded.wav")
                                wavRecorder = WavRecorder(outFile)

                                val started = wavRecorder?.start() == true
                                if (started) {
                                    practiceState = PracticeState.Recording
                                    feedbackText = "Recording..."
                                    setStatus("Recording...")
                                } else {
                                    feedbackText = "录音启动失败"
                                    setStatus("Error")
                                    practiceState = PracticeState.Idle
                                }
                            }

                            PracticeState.Recording -> {
                                practiceState = PracticeState.Transcribing
                                feedbackText = "Stopping recording..."
                                setStatus("Processing...")

                                val recordedFile = wavRecorder?.stop()
                                wavRecorder = null

                                if (recordedFile == null || !recordedFile.exists()) {
                                    deleteFileSilently(recordedFile)
                                    feedbackText = "录音文件生成失败"
                                    setStatus("Error")
                                    practiceState = PracticeState.Idle
                                    return@Button
                                }

                                thread {
                                    try {
                                        val modelFile = copyAssetToInternalStorage("ggml-tiny.bin")
                                        if (modelFile == null) {
                                            deleteFileSilently(recordedFile)
                                            runOnUiThread {
                                                feedbackText = "Model copy failed"
                                                setStatus("Error")
                                                practiceState = PracticeState.Idle
                                            }
                                            return@thread
                                        }

                                        runOnUiThread {
                                            setStatus("Loading model...")
                                            feedbackText = "Loading model..."
                                        }

                                        val initOk = initModel(modelFile.absolutePath)
                                        if (!initOk) {
                                            deleteFileSilently(recordedFile)
                                            runOnUiThread {
                                                feedbackText = "Model init failed"
                                                setStatus("Error")
                                                practiceState = PracticeState.Idle
                                            }
                                            return@thread
                                        }

                                        runOnUiThread {
                                            setStatus("Transcribing...")
                                            feedbackText = "Transcribing..."
                                        }

                                        val result = transcribeFile(recordedFile.absolutePath)
                                        val compareItems = compareChinese(targetChinese, result)
                                        val compareText = formatCompareResult(compareItems)

                                        deleteFileSilently(recordedFile)

                                        runOnUiThread {
                                            feedbackText = result
                                            correctionText = compareText
                                            setStatus("Done")
                                            practiceState = PracticeState.Idle
                                        }
                                    } catch (e: Exception) {
                                        deleteFileSilently(recordedFile)
                                        runOnUiThread {
                                            feedbackText = "Exception: ${e.message}"
                                            correctionText = "暂无纠错结果"
                                            setStatus("Error")
                                            practiceState = PracticeState.Idle
                                        }
                                    }
                                }
                            }

                            PracticeState.Transcribing -> {
                                feedbackText = "Please wait, transcribing..."
                                setStatus("Transcribing...")
                            }
                        }
                    },
                    enabled = practiceState != PracticeState.Transcribing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (practiceState) {
                            PracticeState.Idle -> "Start"
                            PracticeState.Recording -> "Stop"
                            PracticeState.Transcribing -> "Processing..."
                        }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onBack,
                        enabled = practiceState != PracticeState.Transcribing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("返回")
                    }

                    OutlinedButton(
                        onClick = {
                            englishInput = "hello"
                            targetEnglish = "hello"
                            targetChinese = "你好"
                            targetPinyin = "nǐ hǎo"
                            resetState()
                        },
                        enabled = practiceState != PracticeState.Transcribing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("重置")
                    }
                }
            }
        }
    }

    @Composable
    private fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PrimaryText,
        unfocusedTextColor = PrimaryText,
        disabledTextColor = HintText,
        focusedContainerColor = BoxBg,
        unfocusedContainerColor = BoxBg,
        disabledContainerColor = BoxBg,
        cursorColor = AccentGreen,
        focusedBorderColor = AccentGreen,
        unfocusedBorderColor = BorderColor,
        disabledBorderColor = BorderColor,
        focusedLabelColor = SecondaryText,
        unfocusedLabelColor = HintText,
        disabledLabelColor = HintText,
        focusedPlaceholderColor = HintText,
        unfocusedPlaceholderColor = HintText,
        disabledPlaceholderColor = HintText
    )

    private fun hasRecordPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordPermission() {
        requestPermissions(
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            recordPermissionCode
        )
    }

    private fun deleteFileSilently(file: File?) {
        if (file == null) return
        try {
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    private fun clearTemporaryAudioFiles() {
        deleteFileSilently(File(filesDir, "recorded.wav"))
        deleteFileSilently(File(filesDir, "test_short.wav"))
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

    private fun compareChinese(target: String, recognized: String): List<CompareItem> {
        val targetChars = target.map { it.toString() }
        val recognizedChars = recognized.map { it.toString() }

        val maxLen = maxOf(targetChars.size, recognizedChars.size)
        val results = mutableListOf<CompareItem>()

        for (i in 0 until maxLen) {
            val t: String? = targetChars.getOrNull(i)
            val r: String? = recognizedChars.getOrNull(i)

            when {
                t != null && r == null -> {
                    results.add(
                        CompareItem(
                            targetChar = t,
                            recognizedChar = null,
                            status = CompareStatus.Missing,
                            message = "$t：漏读"
                        )
                    )
                }

                t == null && r != null -> {
                    results.add(
                        CompareItem(
                            targetChar = null,
                            recognizedChar = r,
                            status = CompareStatus.Extra,
                            message = "$r：多读"
                        )
                    )
                }

                t != null && r != null -> {
                    if (t == r) {
                        results.add(
                            CompareItem(
                                targetChar = t,
                                recognizedChar = r,
                                status = CompareStatus.Correct,
                                message = "$t：正确"
                            )
                        )
                    } else {
                        val tInfo = charMap[t]
                        val rInfo = charMap[r]

                        if (tInfo != null && rInfo != null) {
                            when {
                                tInfo.pinyin == rInfo.pinyin && tInfo.tone == rInfo.tone -> {
                                    results.add(
                                        CompareItem(
                                            targetChar = t,
                                            recognizedChar = r,
                                            status = CompareStatus.Correct,
                                            message = "$t/$r：同音同调，判定正确"
                                        )
                                    )
                                }

                                tInfo.pinyin == rInfo.pinyin && tInfo.tone != rInfo.tone -> {
                                    results.add(
                                        CompareItem(
                                            targetChar = t,
                                            recognizedChar = r,
                                            status = CompareStatus.ToneMismatch,
                                            message = "$r：声调错误，目标“$t”应为第 ${tInfo.tone} 声"
                                        )
                                    )
                                }

                                else -> {
                                    results.add(
                                        CompareItem(
                                            targetChar = t,
                                            recognizedChar = r,
                                            status = CompareStatus.PronMismatch,
                                            message = "$r：发音错误，目标应为“$t”"
                                        )
                                    )
                                }
                            }
                        } else {
                            results.add(
                                CompareItem(
                                    targetChar = t,
                                    recognizedChar = r,
                                    status = CompareStatus.PronMismatch,
                                    message = "$r：无法匹配拼音，按错误处理，目标应为“$t”"
                                )
                            )
                        }
                    }
                }
            }
        }

        return results
    }

    private fun formatCompareResult(items: List<CompareItem>): String {
        return items.joinToString("\n") { it.message }
    }
}