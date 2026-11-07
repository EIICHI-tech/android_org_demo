package com.example.vocalswitchkaraoke

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { KaraokeApp() } }
}

enum class PlayMode { ORIGINAL, KARAOKE, VOCAL_ONLY, MIX }

class KaraokeViewModel(app: Application) : AndroidViewModel(app) {
 private val ctx=app.applicationContext
 private val _status=MutableStateFlow("YouTubeを読み込み、伴奏とボーカルWAVを選択してください")
 val status: StateFlow<String> = _status
 var webView: WebView?=null
 var instrumentalUri by mutableStateOf<Uri?>(null); private set
 var vocalUri by mutableStateOf<Uri?>(null); private set
 var mode by mutableStateOf(PlayMode.ORIGINAL); private set
 var musicVol by mutableFloatStateOf(.85f)
 var vocalVol by mutableFloatStateOf(.70f)
 var syncOffsetMs by mutableIntStateOf(0)
 var ytTimeMs by mutableLongStateOf(0L)
 var ytState by mutableIntStateOf(-1)
 var videoId by mutableStateOf("")
 private var inst: MediaPlayer?=null; private var vocal: MediaPlayer?=null
 private var prepared=0
 val recorder=WavRecorder(ctx) { _status.value=it }

 fun setInstrumental(uri:Uri){ instrumentalUri=uri; ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); preparePlayers() }
 fun setVocal(uri:Uri){ vocalUri=uri; ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); preparePlayers() }
 private fun prep(uri:Uri, done:()->Unit)=MediaPlayer().apply{
  setDataSource(ctx,uri); setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
  setOnPreparedListener{done()}; setOnCompletionListener{ webView?.evaluateJavascript("pauseVideo()",null) }; prepareAsync()
 }
 fun preparePlayers(){
  val a=instrumentalUri; val v=vocalUri; if(a==null||v==null)return
  releasePlayers(); prepared=0; _status.value="音源を準備中…"
  inst=prep(a){prepared++; readyCheck()}; vocal=prep(v){prepared++; readyCheck()}
 }
 private fun readyCheck(){if(prepared==2){applyVolumes();_status.value="音源準備完了"}}
 fun loadVideo(input:String){ val id=extractVideoId(input); if(id==null){_status.value="YouTube URLまたは11文字の動画IDを確認してください";return}; videoId=id; webView?.evaluateJavascript("loadVideo('$id')",null); _status.value="YouTubeを読み込みました" }
 fun updateMode(m:PlayMode){mode=m;when(m){
  PlayMode.ORIGINAL->{pauseLocal(); webView?.evaluateJavascript("unmutePlayer()",null)}
  else->{webView?.evaluateJavascript("mutePlayer()",null); syncToYoutube(); if(ytState==1)startLocal()}
 };applyVolumes()}
 fun togglePlay(){if(ytState==1)webView?.evaluateJavascript("pauseVideo()",null) else webView?.evaluateJavascript("playVideo()",null)}
 fun onYoutubeState(s:Int){ytState=s;when(s){1->if(mode!=PlayMode.ORIGINAL){syncToYoutube();startLocal()};2,0->pauseLocal()}}
 fun onYoutubeTime(sec:Double,state:Int){ytTimeMs=(sec*1000).toLong();ytState=state;if(state==1&&mode!=PlayMode.ORIGINAL){val p=inst?.currentPosition?:return;val target=(ytTimeMs+syncOffsetMs).coerceAtLeast(0L);if(abs(target-p)>180L)seekLocal(target)}}
 fun syncToYoutube(){seekLocal((ytTimeMs+syncOffsetMs).coerceAtLeast(0L))}
 private fun seekLocal(ms:Long){inst?.seekTo(ms,MediaPlayer.SEEK_CLOSEST);vocal?.seekTo(ms,MediaPlayer.SEEK_CLOSEST)}
 private fun startLocal(){if(prepared<2){_status.value="先に2つのWAVを選択してください";return};inst?.start();vocal?.start()}
 private fun pauseLocal(){inst?.pause();vocal?.pause()}
 fun applyVolumes(){inst?.setVolume(if(mode==PlayMode.VOCAL_ONLY)0f else musicVol,if(mode==PlayMode.VOCAL_ONLY)0f else musicVol);val vv=if(mode==PlayMode.KARAOKE)0f else vocalVol;vocal?.setVolume(vv,vv)}
 private fun releasePlayers(){inst?.release();vocal?.release();inst=null;vocal=null}
 override fun onCleared(){releasePlayers();recorder.stop()}
 private fun extractVideoId(s:String):String?{val t=s.trim();if(Regex("^[A-Za-z0-9_-]{11}$").matches(t))return t;val patterns=listOf("[?&]v=([A-Za-z0-9_-]{11})","youtu\\.be/([A-Za-z0-9_-]{11})","shorts/([A-Za-z0-9_-]{11})","embed/([A-Za-z0-9_-]{11})");return patterns.firstNotNullOfOrNull{Regex(it).find(t)?.groupValues?.get(1)}}
}

@SuppressLint("SetJavaScriptEnabled")
@Composable fun KaraokeApp(vm:KaraokeViewModel= viewModel()){
 val ctx=LocalContext.current; val status by vm.status.collectAsState(); var url by remember{mutableStateOf("")}
 var granted by remember{mutableStateOf(ContextCompat.checkSelfPermission(ctx,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)}
 val mic=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it;if(it)vm.recorder.start()}
 val pickInst=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(vm::setInstrumental)}
 val pickVocal=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(vm::setVocal)}
 MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF00D4FF))){
  Row(Modifier.fillMaxSize().background(Color(0xFF10151C)).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   AndroidView(modifier=Modifier.weight(1.5f).fillMaxHeight(),factory={c->WebView(c).apply{
    settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.mediaPlaybackRequiresUserGesture=false;webChromeClient=WebChromeClient();webViewClient=WebViewClient()
    addJavascriptInterface(object{ @JavascriptInterface fun onReady(){};@JavascriptInterface fun onState(s:Int){post{vm.onYoutubeState(s)}};@JavascriptInterface fun onTime(t:Double,s:Int){post{vm.onYoutubeTime(t,s)}}},"Android")
    loadUrl("file:///android_asset/youtube_player.html");vm.webView=this
   }},update={vm.webView=it})
   Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("Vocal Switch Karaoke",style=MaterialTheme.typography.headlineSmall,color=Color.White)
    OutlinedTextField(url,{url=it},label={Text("YouTube URL / Video ID")},singleLine=true,modifier=Modifier.fillMaxWidth())
    Button({vm.loadVideo(url)},Modifier.fillMaxWidth()){Text("YouTubeを読み込む")}
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Button({pickInst.launch(arrayOf("audio/wav","audio/*"))}){Text("伴奏WAV")};Button({pickVocal.launch(arrayOf("audio/wav","audio/*"))}){Text("歌声WAV")}}
    Text("伴奏: ${vm.instrumentalUri?.lastPathSegment?:"未選択"}",color=Color.LightGray,maxLines=1);Text("歌声: ${vm.vocalUri?.lastPathSegment?:"未選択"}",color=Color.LightGray,maxLines=1)
    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){PlayMode.entries.forEach{m->FilterChip(selected=vm.mode==m,onClick={vm.updateMode(m)},label={Text(m.name)})}}
    Text("伴奏音量 ${(vm.musicVol*100).toInt()}%",color=Color.White);Slider(vm.musicVol,{vm.musicVol=it;vm.applyVolumes()})
    Text("歌手音声 ${(vm.vocalVol*100).toInt()}%",color=Color.White);Slider(vm.vocalVol,{vm.vocalVol=it;vm.applyVolumes()})
    Text("同期補正 ${vm.syncOffsetMs} ms",color=Color.White);Slider(vm.syncOffsetMs.toFloat(),{vm.syncOffsetMs=it.toInt();vm.syncToYoutube()},valueRange=-500f..500f,steps=19)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(vm::togglePlay){Text("再生 / 一時停止")};Button({if(vm.recorder.recording)vm.recorder.stop() else if(granted)vm.recorder.start() else mic.launch(Manifest.permission.RECORD_AUDIO)}){Text(if(vm.recorder.recording)"録音停止" else "マイク録音")}}
    Text(status,color=Color(0xFF9FE7FF));Text("YouTube ${vm.ytTimeMs/1000.0}s / state ${vm.ytState}",color=Color.Gray)
    Text("注: YouTube音声を抽出しません。権利処理済みの分離WAVを同期再生します。",color=Color(0xFFFFCC80),style=MaterialTheme.typography.bodySmall)
   }
  }
 }
}

class WavRecorder(private val context:Context,private val report:(String)->Unit){
 private val active=AtomicBoolean(false);var recording by mutableStateOf(false);private set;private var thread:Thread?=null;private var audio:AudioRecord?=null
 fun start(){if(active.get())return;val rate=48000;val min=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){report("マイク権限が必要です");return}
  val dir=context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?:context.filesDir;val out=File(dir,"karaoke_${System.currentTimeMillis()}.wav");audio=AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,min*2);active.set(true);recording=true
  thread=Thread{
   var bytes=0L
   val buf=ByteArray(min)
   RandomAccessFile(out,"rw").use{f->
    writeHeader(f,rate,0)
    audio?.startRecording()
    while(active.get()){
     val n=audio?.read(buf,0,buf.size)?:0
     if(n>0){f.write(buf,0,n);bytes+=n}
    }
    audio?.stop()
    f.seek(0)
    writeHeader(f,rate,bytes)
   }
   audio?.release()
   audio=null
   recording=false
   report("録音保存: ${out.absolutePath}")
  }.also{it.start()}
  report("録音中…")
 }
 fun stop(){if(!active.getAndSet(false))return;thread?.join(1500)}
 private fun writeHeader(f: RandomAccessFile, rate: Int, data: Long) {
  fun s(v: String) = f.write(v.toByteArray(Charsets.US_ASCII))
  fun i(v: Long) {
   f.write(byteArrayOf(
    (v and 255).toByte(),
    ((v shr 8) and 255).toByte(),
    ((v shr 16) and 255).toByte(),
    ((v shr 24) and 255).toByte()
   ))
  }
  fun h(v: Int) {
   f.write(byteArrayOf(
    (v and 255).toByte(),
    ((v shr 8) and 255).toByte()
   ))
  }
  s("RIFF")
  i(36 + data)
  s("WAVEfmt ")
  i(16)
  h(1)
  h(1)
  i(rate.toLong())
  i((rate * 2).toLong())
  h(2)
  h(16)
  s("data")
  i(data)
 }
}
