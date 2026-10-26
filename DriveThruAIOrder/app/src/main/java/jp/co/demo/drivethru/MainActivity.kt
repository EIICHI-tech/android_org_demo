package jp.co.demo.drivethru

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import jp.co.demo.drivethru.ui.DriveThruScreen
import jp.co.demo.drivethru.voice.*

class MainActivity:ComponentActivity(),SpeechRecognizerManager.Events{
 private val vm:MainViewModel by viewModels();private lateinit var speech:SpeechRecognizerManager;private lateinit var tts:TtsManager
 private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){if(it)speech.start()}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);speech=SpeechRecognizerManager(this,this);tts=TtsManager(this);setContent{DriveThruScreen(vm)}
  if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)speech.start() else permission.launch(Manifest.permission.RECORD_AUDIO)
 }
 override fun onReady(){vm.listening(true)}
 override fun onText(text:String){vm.voice(text)?.let{spoken->speech.stop();tts.speak(spoken);window.decorView.postDelayed({speech.start()},2600)}}
 override fun onErrorMessage(message:String){vm.listening(false)}
 override fun onDestroy(){speech.destroy();tts.shutdown();super.onDestroy()}
}
