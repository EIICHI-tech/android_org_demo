package com.example.voicecamai
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.voicecamai.databinding.ActivityMainBinding
class MainActivity:AppCompatActivity(),VoiceRecognitionManager.Listener {private lateinit var b:ActivityMainBinding;private lateinit var voice:VoiceRecognitionManager;private lateinit var tts:TtsManager;private lateinit var camera:CameraController;private var awake=false;private var speaking=false
 private val permission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){if(it.values.all{v->v})init()else b.stateText.text="カメラとマイク権限が必要です"}
 override fun onCreate(s:Bundle?){super.onCreate(s);b=ActivityMainBinding.inflate(layoutInflater);setContentView(b.root);val ps=listOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO);if(ps.all{ContextCompat.checkSelfPermission(this,it)==PackageManager.PERMISSION_GRANTED})init()else permission.launch(ps.toTypedArray())}
 private fun init(){camera=CameraController(this,this,b.previewView){b.resultText.text=it};camera.start();tts=TtsManager(this);voice=VoiceRecognitionManager(this,this);voice.start()}
 override fun onState(s:String){runOnUiThread{b.stateText.text=s}};override fun onPartial(s:String){runOnUiThread{b.resultText.text="聞き取り中: $s"}};override fun onDiagnostic(s:String){runOnUiThread{b.debugText.text=s}}
 override fun onFinal(s:String){runOnUiThread{if(speaking)return@runOnUiThread;b.resultText.text="認識:「$s」";handle(VoiceCommandProcessor.parse(s,awake))}}
 private fun say(text:String,action:(()->Unit)?=null){speaking=true;voice.pauseForTts();b.stateText.text="🔊 $text";tts.speak(text){action?.invoke();speaking=false;voice.resumeAfterTts()}}
 private fun handle(c:VoiceCommand){when(c){VoiceCommand.Wake->{awake=true;say("はい、どうしますか")};VoiceCommand.Capture->say("撮影します"){camera.take()};VoiceCommand.Timer->{speaking=true;voice.pauseForTts();b.stateText.text="3";Handler(Looper.getMainLooper()).postDelayed({b.stateText.text="2"},1000);Handler(Looper.getMainLooper()).postDelayed({b.stateText.text="1"},2000);Handler(Looper.getMainLooper()).postDelayed({camera.take();speaking=false;voice.resumeAfterTts()},3000)};VoiceCommand.ZoomIn->{camera.zoom(.5f);say("ズームします")};VoiceCommand.ZoomOut->{camera.zoom(-.5f);say("ズームアウトします")};VoiceCommand.Brighter->{camera.expose(1);say("明るくします")};VoiceCommand.Darker->{camera.expose(-1);say("暗くします")};VoiceCommand.Switch->{camera.switch();say("カメラを切り替えます")};VoiceCommand.Center->say("顔を枠の中央に合わせてください");VoiceCommand.Skin->{camera.expose(1);say("肌を明るくします")};VoiceCommand.Exit->{awake=false;say("音声操作を終了します")};VoiceCommand.Unknown->{if(awake)say("もう一度お願いします")}}
 }
 override fun onDestroy(){super.onDestroy();if(::voice.isInitialized)voice.destroy();if(::tts.isInitialized)tts.shutdown()}}
