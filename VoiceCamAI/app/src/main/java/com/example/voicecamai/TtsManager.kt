package com.example.voicecamai
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
class TtsManager(context:Context, private val onDone:()->Unit): TextToSpeech.OnInitListener {
 private val tts=TextToSpeech(context,this); private var ready=false
 override fun onInit(status:Int){ ready=status==TextToSpeech.SUCCESS; if(ready){tts.language=Locale.JAPAN; tts.setSpeechRate(1.0f); tts.setOnUtteranceProgressListener(object:UtteranceProgressListener(){override fun onStart(id:String){}; override fun onDone(id:String)=onDone(); @Deprecated("Deprecated") override fun onError(id:String)=onDone()})} }
 fun speak(text:String){ if(ready) tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"voicecam-${System.nanoTime()}") else onDone() }
 fun shutdown()=tts.shutdown()
}
