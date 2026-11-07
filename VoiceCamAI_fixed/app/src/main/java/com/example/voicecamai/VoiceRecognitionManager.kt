package com.example.voicecamai
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale
class VoiceRecognitionManager(private val context:Context,private val listener:Listener):RecognitionListener {
 interface Listener { fun onState(s:String); fun onPartial(s:String); fun onFinal(s:String); fun onDiagnostic(s:String) }
 private enum class State { IDLE,LISTENING,PAUSED,DESTROYED }
 private val tag="VoiceCamSpeech"; private val h=Handler(Looper.getMainLooper()); private var sr:SpeechRecognizer?=null; private var state=State.IDLE; private var retry=0
 private val restart=Runnable{startSession()}
 private val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ja-JP");putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,1400L);putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,900L)}
 fun start(){if(!SpeechRecognizer.isRecognitionAvailable(context)){diag("isRecognitionAvailable=false");listener.onState("音声認識サービスがありません");return};diag("isRecognitionAvailable=true");state=State.IDLE;schedule(500)}
 fun pauseForTts(){h.removeCallbacks(restart);state=State.PAUSED;diag("PAUSED for TTS")}
 fun resumeAfterTts(){if(state==State.DESTROYED)return;state=State.IDLE;schedule(800)}
 private fun ensure(){if(sr==null){sr=SpeechRecognizer.createSpeechRecognizer(context);sr!!.setRecognitionListener(this);diag("recognizer created")}}
 private fun startSession(){if(state!=State.IDLE)return;try{ensure();state=State.LISTENING;diag("startListening");listener.onState("🎤 お話しください");sr!!.startListening(intent)}catch(e:Exception){diag("start failed: ${e.message}");state=State.IDLE;schedule(1500)}}
 private fun schedule(ms:Long){h.removeCallbacks(restart);if(state==State.IDLE)h.postDelayed(restart,ms)}
 private fun terminal(error:Int?=null){if(state==State.PAUSED||state==State.DESTROYED)return;state=State.IDLE;val delay=when(error){SpeechRecognizer.ERROR_RECOGNIZER_BUSY->2000L;SpeechRecognizer.ERROR_AUDIO,SpeechRecognizer.ERROR_NETWORK->1500L;else->1000L};schedule(delay)}
 private fun diag(s:String){Log.i(tag,s);listener.onDiagnostic(s)}
 fun destroy(){h.removeCallbacksAndMessages(null);state=State.DESTROYED;sr?.destroy();sr=null;diag("destroy")}
 override fun onReadyForSpeech(p:Bundle?){diag("onReadyForSpeech")};override fun onBeginningOfSpeech(){diag("onBeginningOfSpeech")};override fun onRmsChanged(v:Float){};override fun onBufferReceived(b:ByteArray?){};override fun onEndOfSpeech(){diag("onEndOfSpeech; waiting terminal callback");listener.onState("認識処理中")}
 override fun onPartialResults(b:Bundle?){b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let{listener.onPartial(it)}}
 override fun onResults(b:Bundle?){val all=b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty();diag("onResults=$all");state=State.IDLE;retry=0;if(all.isNotEmpty())listener.onFinal(all.first())else schedule(1000)}
 override fun onError(e:Int){diag("onError=$e ${name(e)}");retry++;terminal(e)}
 private fun name(e:Int)=when(e){1->"NETWORK_TIMEOUT";2->"NETWORK";3->"AUDIO";4->"SERVER";5->"CLIENT";6->"SPEECH_TIMEOUT";7->"NO_MATCH";8->"BUSY";9->"PERMISSION";else->"OTHER"}
 override fun onEvent(t:Int,p:Bundle?){}
}
