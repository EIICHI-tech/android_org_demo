package com.example.hospitalvoice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HospitalApp() }
    }
}

enum class VoiceState { IDLE, LISTENING, SPEAKING, ERROR }
enum class Screen { HOME, DEPARTMENTS, GUIDE, HOURS, MEDICINE, COMPLETE, EMERGENCY }

data class Department(val name: String, val floor: String, val desk: String, val hours: String, val keywords: List<String>)

private val departments = listOf(
    Department("内科", "1階", "A受付", "8:30〜11:30 / 13:00〜16:00", listOf("内科", "熱", "せき", "咳", "風邪", "腹痛")),
    Department("外科", "1階", "B受付", "8:30〜11:30", listOf("外科", "けが", "怪我", "切った", "出血")),
    Department("整形外科", "2階", "C受付", "8:30〜11:30 / 13:00〜15:30", listOf("整形", "骨", "腰", "膝", "足を痛め")),
    Department("皮膚科", "2階", "D受付", "9:00〜11:30 / 14:00〜16:00", listOf("皮膚", "かゆい", "湿疹", "赤い")),
    Department("眼科", "2階", "E受付", "8:30〜11:00 / 13:30〜15:30", listOf("眼科", "目", "見えにくい")),
    Department("耳鼻咽喉科", "3階", "F受付", "8:30〜11:30 / 14:00〜16:00", listOf("耳鼻", "耳", "鼻", "のど", "喉")),
    Department("小児科", "3階", "G受付", "8:30〜11:30 / 13:00〜16:00", listOf("小児", "子ども", "子供")),
    Department("歯科・口腔外科", "3階", "H受付", "9:00〜11:30 / 13:30〜16:00", listOf("歯科", "歯", "口腔"))
)

private class VoiceController(
    private val activity: ComponentActivity,
    private val onState: (VoiceState) -> Unit,
    private val onPartial: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(activity) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.JAPAN
                tts?.setSpeechRate(0.92f)
            }
        }
    }

    fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            onError("この端末では音声認識サービスを利用できません")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { onState(VoiceState.LISTENING) }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { onState(VoiceState.IDLE) }
                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "聞き取れませんでした。もう一度お話しください"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "音声が聞こえませんでした"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "音声認識ネットワークを確認してください"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "マイクの権限が必要です"
                        else -> "音声認識エラー: $error"
                    }
                    onState(VoiceState.ERROR); onError(msg)
                }
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    onState(VoiceState.IDLE)
                    if (text.isNotBlank()) onResult(text) else onError("聞き取れませんでした")
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial)
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            })
        }
    }

    fun speak(text: String) {
        onState(VoiceState.SPEAKING)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hospital_reply")
        android.os.Handler(activity.mainLooper).postDelayed({ onState(VoiceState.IDLE) }, (text.length * 95L).coerceIn(1200L, 6500L))
    }

    fun release() { recognizer?.destroy(); tts?.shutdown() }
}

@Composable
fun HospitalApp() {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    var screen by remember { mutableStateOf(Screen.HOME) }
    var voiceState by remember { mutableStateOf(VoiceState.IDLE) }
    var recognized by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("こんにちは。病院受付です。本日はどのようなご用件でしょうか？") }
    var selected by remember { mutableStateOf<Department?>(null) }
    var ticket by remember { mutableIntStateOf(23) }
    var initialVisit by remember { mutableStateOf(true) }

    lateinit var controller: VoiceController
    controller = remember {
        VoiceController(activity, { voiceState = it }, { recognized = it }, { text ->
            recognized = text
            handleSpeech(text, { screen = it }, { message = it }, { selected = it }, { initialVisit = it }, { controller.speak(it) })
        }, { message = it })
    }
    DisposableEffect(Unit) { onDispose { controller.release() } }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) controller.listen() else message = "マイクの権限を許可してください"
    }
    fun startListening() {
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) controller.listen()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    fun say(text: String) { message = text; controller.speak(text) }

    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF0B7A75), secondary = Color(0xFF3F8FA5), background = Color(0xFFF4FAF9))) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF4FAF9)) {
            Column(Modifier.fillMaxSize()) {
                Header(voiceState)
                when (screen) {
                    Screen.EMERGENCY -> EmergencyScreen({ screen = Screen.HOME; say("総合受付に戻りました") })
                    Screen.COMPLETE -> CompleteScreen(selected, ticket, initialVisit, { say("受付番号はA${ticket.toString().padStart(3, '0')}。${selected?.name ?: "総合受付"}、${selected?.floor ?: "1階"}${selected?.desk ?: ""}へお進みください") }, { screen = Screen.HOME; recognized = ""; say("受付を終了しました。ありがとうございました") })
                    else -> ReceptionBody(screen, voiceState, message, recognized, selected,
                        onMic = { startListening() },
                        onMenu = { destination ->
                            screen = destination
                            val prompt = when(destination) {
                                Screen.DEPARTMENTS -> "受診したい診療科を選ぶか、音声でお話しください"
                                Screen.GUIDE -> "ご案内する場所をお話しください"
                                Screen.HOURS -> "受付時間を確認する診療科を選んでください"
                                Screen.MEDICINE -> "お薬の受け取りについてご案内します"
                                else -> "ご用件をお話しください"
                            }
                            say(prompt)
                        },
                        onDepartment = { dep -> selected = dep; screen = Screen.COMPLETE; ticket++; say("${dep.name}の受付が完了しました。${dep.floor}${dep.desk}へお進みください") },
                        onFirst = { initialVisit = true; screen = Screen.DEPARTMENTS; say("初診ですね。受診したい診療科を教えてください") },
                        onRevisit = { initialVisit = false; screen = Screen.DEPARTMENTS; say("再診ですね。予約している診療科を教えてください") },
                        onHome = { screen = Screen.HOME; recognized = ""; say("最初の画面に戻りました") },
                        onStaff = { say("職員を呼び出しました。この画面はデモ表示です") })
                }
            }
        }
    }
}

private fun handleSpeech(text: String, setScreen: (Screen)->Unit, setMessage:(String)->Unit, setDepartment:(Department)->Unit, setInitial:(Boolean)->Unit, speak:(String)->Unit) {
    val t = text.lowercase(Locale.JAPAN)
    val emergency = listOf("胸が痛", "息ができ", "意識がない", "大量に出血", "倒れ", "助けて", "救急")
    if (emergency.any { t.contains(it) }) { setScreen(Screen.EMERGENCY); speak("緊急の可能性があります。お近くの職員を呼んでください"); return }
    if (t.contains("最初") || t.contains("ホーム") || t.contains("戻って")) { setScreen(Screen.HOME); speak("最初の画面に戻りました"); return }
    if (t.contains("初診") || t.contains("初めて")) { setInitial(true); setScreen(Screen.DEPARTMENTS); speak("初診ですね。受診したい診療科を教えてください"); return }
    if (t.contains("再診") || t.contains("予約")) { setInitial(false); setScreen(Screen.DEPARTMENTS); speak("再診ですね。診療科を教えてください"); return }
    if (t.contains("薬") || t.contains("処方箋")) { setScreen(Screen.MEDICINE); speak("院内処方は1階の薬受渡窓口、院外処方は会計後に処方箋交付窓口へお進みください"); return }
    if (t.contains("受付時間") || t.contains("何時")) { setScreen(Screen.HOURS); speak("診療科ごとの受付時間を表示します"); return }
    val dep = departments.firstOrNull { d -> d.keywords.any { t.contains(it) } }
    if (dep != null) { setDepartment(dep); setScreen(Screen.COMPLETE); speak("${dep.name}をご案内します。${dep.floor}${dep.desk}へお進みください"); return }
    if (t.contains("トイレ")) { setScreen(Screen.GUIDE); speak("トイレは1階総合受付の左側です"); return }
    if (t.contains("会計")) { setScreen(Screen.GUIDE); speak("会計は1階正面の会計窓口です"); return }
    setMessage("「$text」と認識しました。診療科、初診、再診、院内案内などをお話しください")
    speak("内容を確認できませんでした。診療科、初診、再診、院内案内などをお話しください")
}

@Composable private fun Header(state: VoiceState) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF0B7A75)).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("＋", fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f).padding(start = 10.dp)) { Text("病院 音声受付", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("Hospital Voice Reception", color = Color(0xFFD5F5F1), fontSize = 12.sp) }
        val s = when(state){ VoiceState.LISTENING->"聞き取り中"; VoiceState.SPEAKING->"ご案内中"; VoiceState.ERROR->"再入力"; else->"受付できます" }
        Text("● $s", color = Color.White, fontSize = 12.sp)
    }
}

@Composable private fun ReceptionBody(screen: Screen, state: VoiceState, message: String, recognized: String, selected: Department?, onMic:()->Unit, onMenu:(Screen)->Unit, onDepartment:(Department)->Unit, onFirst:()->Unit, onRevisit:()->Unit, onHome:()->Unit, onStaff:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(state)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Text(message, Modifier.padding(18.dp), fontSize = 22.sp, lineHeight = 30.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        if (recognized.isNotBlank()) Text("認識結果：$recognized", Modifier.fillMaxWidth().background(Color(0xFFE8F4F2), RoundedCornerShape(12.dp)).padding(12.dp), fontSize = 17.sp)
        Spacer(Modifier.height(12.dp))
        MicButton(state, onMic)
        Spacer(Modifier.height(12.dp))
        when(screen) {
            Screen.DEPARTMENTS, Screen.HOURS -> DepartmentGrid(screen == Screen.HOURS, onDepartment)
            Screen.GUIDE -> GuidePanel()
            Screen.MEDICINE -> MedicinePanel()
            else -> HomeMenu(onFirst, onRevisit, onMenu)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onHome, Modifier.weight(1f)) { Text("最初から") }
            Button(onClick = onStaff, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE5A45))) { Text("職員を呼ぶ") }
        }
        Text("※ 本アプリは受付・案内デモです。診断や治療判断は行いません。", Modifier.padding(top = 10.dp), fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable private fun Avatar(state: VoiceState) {
    val infinite = rememberInfiniteTransition(label="avatar")
    val pulse by infinite.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label="pulse")
    Box(Modifier.height(260.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size((210*pulse).dp, (245*pulse).dp)) {
            drawOval(Color(0xFFDDF3F0), topLeft=Offset(size.width*.08f,size.height*.03f), size=Size(size.width*.84f,size.height*.88f))
            drawCircle(Color(0xFF3A2C2A), size.width*.25f, Offset(size.width*.5f,size.height*.29f))
            drawCircle(Color(0xFFFFD8C7), size.width*.205f, Offset(size.width*.5f,size.height*.31f))
            drawCircle(Color(0xFF333333), size.width*.014f, Offset(size.width*.44f,size.height*.29f)); drawCircle(Color(0xFF333333), size.width*.014f, Offset(size.width*.56f,size.height*.29f))
            val mouthOpen = state == VoiceState.SPEAKING
            if(mouthOpen) drawOval(Color(0xFFB44A55), Offset(size.width*.47f,size.height*.37f), Size(size.width*.06f,size.height*.04f)) else drawArc(Color(0xFFB44A55), 10f,160f,false, Offset(size.width*.46f,size.height*.35f), Size(size.width*.08f,size.height*.05f), style=Stroke(4f))
            drawRoundRect(Color.White, Offset(size.width*.25f,size.height*.49f), Size(size.width*.5f,size.height*.35f), androidx.compose.ui.geometry.CornerRadius(24f,24f))
            drawRect(Color(0xFF79CFC4), Offset(size.width*.47f,size.height*.49f), Size(size.width*.06f,size.height*.35f))
            drawCircle(Color(0xFF0B7A75), size.width*.045f, Offset(size.width*.5f,size.height*.62f))
            drawLine(Color(0xFF0B7A75), Offset(size.width*.5f,size.height*.58f), Offset(size.width*.5f,size.height*.66f), strokeWidth=5f)
        }
        Text(when(state){VoiceState.LISTENING->"聞いています";VoiceState.SPEAKING->"ご案内中";VoiceState.ERROR->"もう一度お願いします";else->"受付アシスタント"}, Modifier.align(Alignment.BottomCenter).background(Color.White.copy(alpha=.9f), RoundedCornerShape(20.dp)).padding(horizontal=16.dp,vertical=7.dp), color=Color(0xFF0B7A75), fontWeight=FontWeight.Bold)
    }
}

@Composable private fun MicButton(state: VoiceState, onClick:()->Unit) {
    val color = when(state){VoiceState.LISTENING->Color(0xFF2E9E68);VoiceState.SPEAKING->Color(0xFF7756B3);VoiceState.ERROR->Color(0xFFCF7B32);else->Color(0xFF0B7A75)}
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp).background(color, CircleShape).border(7.dp,color.copy(alpha=.22f),CircleShape).clickable(onClick=onClick), contentAlignment=Alignment.Center) { Text("🎤", fontSize=38.sp) }
        Text(if(state==VoiceState.LISTENING) "音声を聞いています" else "タップしてお話しください", Modifier.padding(top=6.dp), fontWeight=FontWeight.Bold, color=color)
    }
}

@Composable private fun HomeMenu(onFirst:()->Unit,onRevisit:()->Unit,onMenu:(Screen)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        MenuRow("初診受付", "再診受付", onFirst, onRevisit)
        MenuRow("診療科", "院内案内", {onMenu(Screen.DEPARTMENTS)}, {onMenu(Screen.GUIDE)})
        MenuRow("受付時間", "薬の受け渡し", {onMenu(Screen.HOURS)}, {onMenu(Screen.MEDICINE)})
    }
}
@Composable private fun MenuRow(a:String,b:String,onA:()->Unit,onB:()->Unit){ Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ LargeTile(a,onA,Modifier.weight(1f));LargeTile(b,onB,Modifier.weight(1f)) } }
@Composable private fun LargeTile(text:String,onClick:()->Unit,modifier:Modifier=Modifier){ Card(modifier.height(68.dp).clickable(onClick=onClick), colors=CardDefaults.cardColors(containerColor=Color.White), elevation=CardDefaults.cardElevation(2.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(text,fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color(0xFF174A48),textAlign=TextAlign.Center)}} }

@Composable private fun DepartmentGrid(showHours:Boolean,onDepartment:(Department)->Unit){ Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ departments.chunked(2).forEach{ row-> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ row.forEach{d->Card(Modifier.weight(1f).height(if(showHours)86.dp else 62.dp).clickable{onDepartment(d)},colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxSize().padding(8.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Text(d.name,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center);if(showHours)Text(d.hours,fontSize=11.sp,textAlign=TextAlign.Center,color=Color.DarkGray)}}};if(row.size==1)Spacer(Modifier.weight(1f)) } } } }
@Composable private fun GuidePanel(){ Card(colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(18.dp)){Text("院内案内",fontSize=21.sp,fontWeight=FontWeight.Bold);Text("• 総合受付：1階 正面\n• 会計：1階 総合受付右側\n• トイレ：1階 総合受付左側\n• エレベーター：中央ホール\n• 薬受渡窓口：1階 会計横",fontSize=17.sp,lineHeight=27.sp)}} }
@Composable private fun MedicinePanel(){ Card(colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(18.dp)){Text("薬の受け渡し",fontSize=21.sp,fontWeight=FontWeight.Bold);Text("院内処方：会計後、1階薬受渡窓口へ\n\n院外処方：会計後、処方箋交付窓口へ\n\n薬の名前・用量・服用方法は薬剤師に確認してください。",fontSize=17.sp,lineHeight=25.sp)}} }

@Composable private fun CompleteScreen(dep:Department?,ticket:Int,initial:Boolean,onRepeat:()->Unit,onFinish:()->Unit){Column(Modifier.fillMaxSize().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){Avatar(VoiceState.IDLE);Text("受付が完了しました",fontSize=29.sp,fontWeight=FontWeight.Bold,color=Color(0xFF0B7A75));Card(colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(4.dp)){Column(Modifier.fillMaxWidth().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("受付番号",fontSize=18.sp);Text("A${ticket.toString().padStart(3,'0')}",fontSize=72.sp,fontWeight=FontWeight.ExtraBold,color=Color(0xFF0B7A75));Text(dep?.name?:"総合受付",fontSize=30.sp,fontWeight=FontWeight.Bold);Text("${dep?.floor?:"1階"} ${dep?.desk?:"総合受付"}",fontSize=24.sp);Text(if(initial)"初診受付" else "再診受付",fontSize=15.sp,color=Color.Gray)}};Button(onClick=onRepeat,Modifier.fillMaxWidth().height(56.dp)){Text("案内をもう一度聞く",fontSize=18.sp)};OutlinedButton(onClick=onFinish,Modifier.fillMaxWidth().height(56.dp)){Text("受付を終了する",fontSize=18.sp)}} }
@Composable private fun EmergencyScreen(onBack:()->Unit){Column(Modifier.fillMaxSize().background(Color(0xFFFFF2F0)).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){Text("⚠",fontSize=90.sp);Text("職員を呼んでください",fontSize=34.sp,fontWeight=FontWeight.ExtraBold,color=Color(0xFFB3261E),textAlign=TextAlign.Center);Text("緊急の可能性があります。\n受付操作を続けず、その場でお近くの職員にお知らせください。",fontSize=22.sp,lineHeight=32.sp,textAlign=TextAlign.Center);Button(onClick={},Modifier.fillMaxWidth().height(90.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFB3261E))){Text("職員呼出（デモ）",fontSize=25.sp,fontWeight=FontWeight.Bold)};TextButton(onClick=onBack){Text("誤検出の場合は最初に戻る")}} }
