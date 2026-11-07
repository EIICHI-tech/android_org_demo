package com.example.voicecamai
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
class TtsManager(context:Context):TextToSpeech.OnInitListener {
 private val tts=TextToSpeech(context,this); private val main=Handler(Looper.getMainLooper()); private var ready=false; private var completion:(()->Unit)?=null
 override fun onInit(status:Int){ ready=status==TextToSpeech.SUCCESS; if(ready){tts.language=Locale.JAPAN;tts.setSpeechRate(1f);tts.setOnUtteranceProgressListener(object:UtteranceProgressListener(){override fun onStart(id:String){};override fun onDone(id:String)=finish();@Deprecated("Deprecated") override fun onError(id:String)=finish()})}}
 private fun finish(){main.post{completion?.invoke();completion=null}}
 fun speak(text:String,onDone:()->Unit){completion=onDone;if(ready)tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"vc-${System.nanoTime()}")else onDone()}
 fun shutdown(){tts.stop();tts.shutdown()}
}
