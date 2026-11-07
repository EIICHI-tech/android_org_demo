package com.example.voicecamai
import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.voicecamai.databinding.ActivityMainBinding
class MainActivity:AppCompatActivity(),VoiceRecognitionManager.Listener{
 private lateinit var b:ActivityMainBinding; private lateinit var voice:VoiceRecognitionManager; private lateinit var tts:TtsManager; private lateinit var camera:CameraController; private var awake=false; private var speaking=false
 private val permissions=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){r->if(r.values.all{it})initialize()else b.stateText.text="カメラとマイクの権限が必要です"}
 override fun onCreate(s:Bundle?){super.onCreate(s);b=ActivityMainBinding.inflate(layoutInflater);setContentView(b.root);if(listOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO).all{ContextCompat.checkSelfPermission(this,it)==PackageManager.PERMISSION_GRANTED})initialize()else permissions.launch(arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO))}
 private fun initialize(){camera=CameraController(this,this,b.previewView){b.resultText.text=it};camera.start();tts=TtsManager(this){runOnUiThread{speaking=false;voice.resume()}};voice=VoiceRecognitionManager(this,this);voice.start();b.stateText.text="🎤 「Hi カメラ」を待っています"}
 override fun onState(text:String){runOnUiThread{b.stateText.text=text}}
 override fun onText(text:String){runOnUiThread{if(speaking)return@runOnUiThread;b.resultText.text="認識:「$text」";handle(VoiceCommandProcessor.parse(text,awake))}}
 override fun onFatal(message:String){runOnUiThread{b.stateText.text=message}}
 private fun say(text:String,after:(()->Unit)?=null){speaking=true;voice.pause();b.stateText.text="🔊 $text";if(after==null)tts.speak(text)else{tts.speak(text);Handler(Looper.getMainLooper()).postDelayed({after()},650)}}
 private fun handle(c:VoiceCommand){when(c){VoiceCommand.Wake->{awake=true;say("はい、どうしますか？")};VoiceCommand.Capture->say("撮影します"){camera.take()};VoiceCommand.TimedCapture->{speaking=true;voice.pause();b.stateText.text="3";Handler(Looper.getMainLooper()).postDelayed({b.stateText.text="2"},1000);Handler(Looper.getMainLooper()).postDelayed({b.stateText.text="1"},2000);Handler(Looper.getMainLooper()).postDelayed({camera.take();speaking=false;voice.resume()},3000)};VoiceCommand.ZoomIn->{camera.zoom(.5f);say("ズームします")};VoiceCommand.ZoomOut->{camera.zoom(-.5f);say("ズームアウトします")};VoiceCommand.Brighter->{camera.expose(1);say("明るくします")};VoiceCommand.Darker->{camera.expose(-1);say("暗くします")};VoiceCommand.SwitchCamera->{camera.switch();say("カメラを切り替えます")};VoiceCommand.CenterFace->{b.faceGuide.animate().alpha(1f).setDuration(200);say("顔を枠の中央に合わせてください")};VoiceCommand.SkinBright->{camera.expose(1);say("肌が明るく見えるように調整します")};VoiceCommand.Exit->{awake=false;say("音声操作を終了します。ハイカメラで再開できます")};is VoiceCommand.Unknown->{if(awake)say("すみません。もう一度お願いします")}}
 }
 override fun onDestroy(){super.onDestroy();if(::voice.isInitialized)voice.destroy();if(::tts.isInitialized)tts.shutdown()}
}
