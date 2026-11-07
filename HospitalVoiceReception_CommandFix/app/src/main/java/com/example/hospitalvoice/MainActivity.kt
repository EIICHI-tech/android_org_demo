package com.example.hospitalvoice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

enum class Mode { WAKE, COMMAND }
enum class VoiceState { WAITING, LISTENING, SPEAKING, ERROR }
data class Dept(val name:String,val place:String,val hours:String,val words:List<String>)

val depts=listOf(
 Dept("内科","1階 A受付","8:30〜11:30 / 13:00〜16:00",listOf("内科","発熱","熱","せき","咳","腹痛")),
 Dept("外科","1階 B受付","8:30〜11:30",listOf("外科","けが","怪我","切った")),
 Dept("整形外科","2階 C受付","8:30〜11:30 / 13:00〜15:30",listOf("整形","骨","腰","膝")),
 Dept("皮膚科","2階 D受付","9:00〜11:30 / 14:00〜16:00",listOf("皮膚","かゆい","湿疹")),
 Dept("眼科","2階 E受付","8:30〜11:00 / 13:30〜15:30",listOf("眼科","目","見えにくい")),
 Dept("耳鼻咽喉科","3階 F受付","8:30〜11:30 / 14:00〜16:00",listOf("耳鼻","耳","鼻","のど","喉")),
 Dept("小児科","3階 G受付","8:30〜11:30 / 13:00〜16:00",listOf("小児","子供","子ども")),
 Dept("歯科・口腔外科","3階 H受付","9:00〜11:30 / 13:30〜16:00",listOf("歯科","歯","口腔"))
)

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}

class VoiceEngine(
 private val a:ComponentActivity,
 private val state:(VoiceState)->Unit,
 private val partial:(String)->Unit,
 private val result:(String)->Unit,
 private val error:(String)->Unit,
 private val wake:()->Unit
){
 private val h=Handler(Looper.getMainLooper())
 private var sr:SpeechRecognizer?=null
 private var tts:TextToSpeech?=null
 private var mode=Mode.WAKE
 private var afterTts=Mode.WAKE
 private var generation=0
 private var dead=false
 init{
  tts=TextToSpeech(a){status->if(status==TextToSpeech.SUCCESS){tts?.language=Locale.JAPAN;tts?.setSpeechRate(.92f);tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
   override fun onStart(id:String?){}
   override fun onDone(id:String?){h.post{if(!dead){state(VoiceState.WAITING);schedule(afterTts,650)}}}
   override fun onError(id:String?){h.post{if(!dead)schedule(afterTts,650)}}
  })}}
 }
 fun startWake(delay:Long=0)=schedule(Mode.WAKE,delay)
 private fun schedule(m:Mode,delay:Long){h.removeCallbacksAndMessages(null);h.postDelayed({listen(m)},delay)}
 private fun listen(m:Mode){
  if(dead||tts?.isSpeaking==true)return
  if(!SpeechRecognizer.isRecognitionAvailable(a)){error("音声認識サービスがありません");return}
  mode=m;generation++;val session=generation
  sr?.cancel();sr?.destroy()
  sr=SpeechRecognizer.createSpeechRecognizer(a).apply{
   setRecognitionListener(object:RecognitionListener{
    override fun onReadyForSpeech(p:Bundle?){if(session==generation)state(VoiceState.LISTENING)}
    override fun onBeginningOfSpeech(){};override fun onRmsChanged(v:Float){};override fun onBufferReceived(b:ByteArray?){};override fun onEndOfSpeech(){}
    override fun onError(e:Int){if(session!=generation||dead)return;if(e==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){state(VoiceState.ERROR);error("マイク権限が必要です")}else{state(VoiceState.WAITING);schedule(mode,700)}}
    override fun onResults(b:Bundle?){
     if(session!=generation||dead)return
     val list=b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
     val text=list.firstOrNull().orEmpty()
     if(text.isBlank()){schedule(mode,400);return}
     if(mode==Mode.WAKE){if(list.any(::isWake)){generation++;sr?.cancel();wake()}else schedule(Mode.WAKE,250)}else result(text)
    }
    override fun onPartialResults(b:Bundle?){if(session==generation)b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(partial)}
    override fun onEvent(t:Int,p:Bundle?){}
   })
   startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ja-JP")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5)
    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,if(m==Mode.COMMAND)1600L else 750L)
    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,if(m==Mode.COMMAND)1100L else 500L)
    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,if(m==Mode.COMMAND)900L else 400L)
   })
  }
 }
 private fun isWake(x:String):Boolean{val n=x.lowercase(Locale.JAPAN).replace(" ","").replace("　","");return listOf("hi病院","hiびょういん","ハイ病院","はい病院","ハイびょういん","はいびょういん").any(n::contains)}
 fun sayThenCommand(x:String)=say(x,Mode.COMMAND)
 fun sayThenWake(x:String)=say(x,Mode.WAKE)
 private fun say(x:String,next:Mode){afterTts=next;generation++;sr?.cancel();sr?.destroy();sr=null;state(VoiceState.SPEAKING);if(tts?.speak(x,TextToSpeech.QUEUE_FLUSH,null,"u_${System.nanoTime()}")==TextToSpeech.ERROR)schedule(next,700)}
 fun close(){dead=true;generation++;h.removeCallbacksAndMessages(null);sr?.cancel();sr?.destroy();tts?.stop();tts?.shutdown()}
}

@Composable fun App(){
 val a=androidx.compose.ui.platform.LocalContext.current as ComponentActivity
 var vs by remember{mutableStateOf(VoiceState.WAITING)}
 var msg by remember{mutableStateOf("『HI 病院』と呼びかけてください")}
 var heard by remember{mutableStateOf("")}
 var ticket by remember{mutableIntStateOf(23)}
 var selected by remember{mutableStateOf<Dept?>(null)}
 var initial by remember{mutableStateOf(true)}
 lateinit var v:VoiceEngine
 v=remember{VoiceEngine(a,{vs=it},{heard=it},{x->heard=x;process(x,{msg=it},{selected=it},{initial=it},{ticket++;v.sayThenWake(it)})},{msg=it},{heard="HI 病院";msg="はい、お伺いします。ご用件をお話しください";v.sayThenCommand(msg)})}
 DisposableEffect(Unit){onDispose{v.close()}}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)v.startWake()else msg="マイク権限を許可してください"}
 LaunchedEffect(Unit){if(a.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)v.startWake()else permission.launch(Manifest.permission.RECORD_AUDIO)}
 MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF08776F),background=Color(0xFFF3FAF8))){
  Column(Modifier.fillMaxSize().background(Color(0xFFF3FAF8))){
   Row(Modifier.fillMaxWidth().background(Color(0xFF08776F)).padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text("＋",fontSize=30.sp,color=Color.White,fontWeight=FontWeight.Bold);Column(Modifier.weight(1f)){Text("病院 音声受付",fontSize=23.sp,color=Color.White,fontWeight=FontWeight.Bold);Text("完全ハンズフリー",color=Color.White,fontSize=12.sp)};Text(when(vs){VoiceState.LISTENING->"● 聞き取り中";VoiceState.SPEAKING->"● ご案内中";VoiceState.ERROR->"● エラー";else->"● 待機中"},color=Color.White,fontSize=12.sp)}
   Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally){
    Box(Modifier.size(190.dp).background(Color(0xFFD8F1ED),CircleShape),contentAlignment=Alignment.Center){Text(if(vs==VoiceState.SPEAKING)"👩‍⚕️🗣️" else "👩‍⚕️",fontSize=82.sp)}
    Spacer(Modifier.height(12.dp));Card(colors=CardDefaults.cardColors(containerColor=Color.White)){Text(msg,Modifier.fillMaxWidth().padding(18.dp),fontSize=22.sp,lineHeight=30.sp,textAlign=TextAlign.Center,fontWeight=FontWeight.Bold)}
    if(heard.isNotBlank()){Spacer(Modifier.height(10.dp));Text("認識結果：$heard",Modifier.fillMaxWidth().background(Color(0xFFE2F2EF),RoundedCornerShape(12.dp)).padding(12.dp),fontSize=17.sp)}
    Spacer(Modifier.height(14.dp));Text(if(vs==VoiceState.LISTENING)"音声を聞いています" else "『HI 病院』→ 応答終了後に用件を話してください",color=Color(0xFF08776F),fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
    selected?.let{d->Spacer(Modifier.height(14.dp));Card(colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("受付完了",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Color(0xFF08776F));Text("A${ticket.toString().padStart(3,'0')}",fontSize=58.sp,fontWeight=FontWeight.ExtraBold);Text(d.name,fontSize=27.sp,fontWeight=FontWeight.Bold);Text(d.place,fontSize=22.sp);Text(if(initial)"初診受付" else "再診受付",color=Color.Gray)}}}
    Spacer(Modifier.height(14.dp));Text("音声例",fontSize=19.sp,fontWeight=FontWeight.Bold);Text("初診です／再診です／内科をお願いします／皮膚がかゆい／受付時間を教えて／薬はどこですか／会計はどこですか",Modifier.padding(8.dp),textAlign=TextAlign.Center,lineHeight=24.sp)
    depts.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{d->Card(Modifier.weight(1f).padding(vertical=4.dp).clickable{selected=d;msg="${d.name}は${d.place}です";v.sayThenWake(msg)},colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(d.name,fontWeight=FontWeight.Bold);Text(d.place,fontSize=12.sp)}}};if(row.size==1)Spacer(Modifier.weight(1f))}}
    Text("※診断アプリではありません。緊急時は職員または119へ連絡してください。",fontSize=11.sp,color=Color.Gray,textAlign=TextAlign.Center,modifier=Modifier.padding(12.dp))
   }
  }
 }
}



private fun process(x:String,setMsg:(String)->Unit,setDept:(Dept?)->Unit,setInitial:(Boolean)->Unit,say:(String)->Unit){
 val t=x.lowercase(Locale.JAPAN)
 if(listOf("胸が痛","息ができ","意識がない","大量に出血","倒れた","助けて","救急").any(t::contains)){val m="緊急の可能性があります。お近くの職員を呼んでください";setMsg(m);say(m);return}
 val isInitial=listOf("初診","初めて","初回","初めて来").any(t::contains)
 val isRevisit=listOf("再診","予約","二回目","2回目").any(t::contains)
 if(isInitial){setInitial(true);val m="初診ですね。受診したい診療科をお話しください";setMsg(m);say(m);return}
 if(isRevisit){setInitial(false);val m="再診ですね。予約している診療科をお話しください";setMsg(m);say(m);return}
 val d=depts.firstOrNull{it.words.any(t::contains)}
 if(d!=null){setDept(d);val m="${d.name}の受付が完了しました。${d.place}へお進みください";setMsg(m);say(m);return}
 val m=when{t.contains("受付時間")||t.contains("何時")->"診療科名と受付時間を続けてお話しください";t.contains("薬")||t.contains("処方箋")->"院内処方は1階薬受渡窓口、院外処方は処方箋交付窓口です";t.contains("会計")->"会計は1階総合受付の右側です";t.contains("トイレ")->"トイレは1階総合受付の左側です";else->"『$x』と認識しました。初診、再診、または診療科をお話しください"}
 setMsg(m);say(m)
}
