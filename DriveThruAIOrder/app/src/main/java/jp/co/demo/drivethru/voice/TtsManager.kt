package jp.co.demo.drivethru.voice
import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
class TtsManager(context:Context){
 private var ready=false
 private lateinit var tts:TextToSpeech
 init { tts=TextToSpeech(context){status->if(status==TextToSpeech.SUCCESS){tts.language=Locale.JAPAN;ready=true}} }
 fun speak(text:String){if(ready)tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"genio")}
 fun shutdown(){if(::tts.isInitialized)tts.shutdown()}
}
