package com.example.voicecamai
import android.content.*
import android.os.*
import android.speech.*
import java.util.Locale
class VoiceRecognitionManager(private val context:Context, private val listener:Listener):RecognitionListener {
 interface Listener { fun onState(text:String); fun onText(text:String); fun onFatal(message:String) }
 private val main=Handler(Looper.getMainLooper()); private var recognizer:SpeechRecognizer?=null; private var running=false; private var paused=false
 private val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.JAPAN.toLanguageTag()); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true); putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5); putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,900L) }
 fun start(){ paused=false; schedule(100) }
 fun pause(){ paused=true; running=false; recognizer?.cancel(); listener.onState("🔊 応答中") }
 fun resume(){ paused=false; schedule(350) }
 private fun ensure(){ if(recognizer==null){recognizer=SpeechRecognizer.createSpeechRecognizer(context); recognizer!!.setRecognitionListener(this)} }
 private fun schedule(delay:Long=600){ main.removeCallbacksAndMessages(null); main.postDelayed({ if(!paused&&!running){ try{ensure(); running=true; listener.onState("🎤 Listening..."); recognizer!!.startListening(intent)}catch(e:Exception){running=false;schedule(1000)} }},delay) }
 fun destroy(){paused=true;main.removeCallbacksAndMessages(null);recognizer?.destroy();recognizer=null}
 override fun onReadyForSpeech(p:Bundle?){listener.onState("🎤 お話しください")}; override fun onBeginningOfSpeech(){}; override fun onRmsChanged(v:Float){}; override fun onBufferReceived(b:ByteArray?){}; override fun onEndOfSpeech(){running=false;listener.onState("認識中...")}
 override fun onPartialResults(r:Bundle?){r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let{listener.onText(it)}}
 override fun onResults(r:Bundle?){running=false; val text=r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull(); if(!text.isNullOrBlank()) listener.onText(text); if(!paused)schedule(300)}
 override fun onError(error:Int){running=false; when(error){SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS->listener.onFatal("マイク権限がありません"); SpeechRecognizer.ERROR_RECOGNIZER_BUSY->{recognizer?.destroy();recognizer=null;schedule(1200)}; else->if(!paused)schedule(if(error==SpeechRecognizer.ERROR_NO_MATCH||error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT)300 else 900)}}
 override fun onEvent(t:Int,p:Bundle?){}
}
