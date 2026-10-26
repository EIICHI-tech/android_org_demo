package jp.co.demo.drivethru.voice

import android.content.*
import android.os.*
import android.speech.*

class SpeechRecognizerManager(private val context:Context,private val events:Events):RecognitionListener{
 interface Events{fun onReady();fun onText(text:String);fun onErrorMessage(message:String)}
 private var recognizer:SpeechRecognizer?=null
 private var stopped=false
 private val handler=Handler(Looper.getMainLooper())
 fun start(){stopped=false;if(!SpeechRecognizer.isRecognitionAvailable(context)){events.onErrorMessage("Google音声認識サービスを利用できません");return}
  if(recognizer==null) recognizer=SpeechRecognizer.createSpeechRecognizer(context).also{it.setRecognitionListener(this)}
  listen()
 }
 fun stop(){stopped=true;handler.removeCallbacksAndMessages(null);recognizer?.cancel()}
 fun destroy(){stop();recognizer?.destroy();recognizer=null}
 private fun listen(){if(stopped)return; val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
  putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ja-JP");putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3)
 }; runCatching{recognizer?.startListening(i)}.onFailure{retry("音声認識を再起動します")}}
 private fun retry(message:String){events.onErrorMessage(message);if(!stopped)handler.postDelayed({listen()},900)}
 override fun onReadyForSpeech(params:Bundle?){events.onReady()}
 override fun onResults(results:Bundle?){val t=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull();if(!t.isNullOrBlank())events.onText(t);handler.postDelayed({listen()},350)}
 override fun onPartialResults(partialResults:Bundle?){}
 override fun onError(error:Int){if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){events.onErrorMessage("マイク権限が必要です");return};retry("待機中")}
 override fun onBeginningOfSpeech(){};override fun onRmsChanged(rmsdB:Float){};override fun onBufferReceived(buffer:ByteArray?){};override fun onEndOfSpeech(){};override fun onEvent(eventType:Int,params:Bundle?){}
}
