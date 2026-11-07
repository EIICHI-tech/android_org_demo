package com.example.hielevator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    companion object { const val TAG = "HiElevatorSpeech"; const val MIN_FLOOR = 1; const val MAX_FLOOR = 15 }
    private lateinit var grid: GridLayout
    private lateinit var currentFloorText: TextView
    private lateinit var statusText: TextView
    private lateinit var heardText: TextView
    private val buttons = mutableMapOf<Int, Button>()
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var listening = false
    private var speaking = false
    private var waitingForFloor = false
    private lateinit var speechIntent: Intent

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) scheduleListen(500) else setStatus("マイク権限が必要です")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main)
        grid=findViewById(R.id.floorGrid); currentFloorText=findViewById(R.id.currentFloorText)
        statusText=findViewById(R.id.statusText); heardText=findViewById(R.id.heardText)
        createButtons(); setupControls(); tts=TextToSpeech(this,this); setupRecognizer()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) scheduleListen(700) else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun createButtons() {
        for (floor in MAX_FLOOR downTo MIN_FLOOR) {
            val b=Button(this).apply { text="${floor}F"; textSize=14f; setTextColor(Color.rgb(30,45,52)); gravity=Gravity.CENTER; isAllCaps=false; background=getDrawable(R.drawable.floor_button); setPadding(0,0,0,0); setOnClickListener{ acceptFloor(floor) } }
            val lp=GridLayout.LayoutParams().apply { width=dp(48); height=dp(48); columnSpec=GridLayout.spec(GridLayout.UNDEFINED, 1f); rowSpec=GridLayout.spec(GridLayout.UNDEFINED, 1f); setGravity(Gravity.CENTER); setMargins(dp(8),dp(3),dp(8),dp(3)) }
            grid.addView(b,lp); buttons[floor]=b
        }
    }

    private fun setupControls() {
        findViewById<Button>(R.id.openButton).setOnClickListener { announce("ドアを開きます", false) }
        findViewById<Button>(R.id.closeButton).setOnClickListener { announce("ドアを閉じます", false) }
        findViewById<Button>(R.id.alarmButton).setOnClickListener { announce("係員を呼び出します", false) }
    }

    private fun setupRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { setStatus("音声認識サービスがありません"); return }
        recognizer=SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(object:RecognitionListener {
            override fun onReadyForSpeech(p:Bundle?){ listening=true; Log.d(TAG,"READY mode=${if(waitingForFloor) "FLOOR" else "WAKE"}"); heardText.text=if(waitingForFloor) "階数を聞いています" else "待機中: Hi エレベーター" }
            override fun onBeginningOfSpeech(){ Log.d(TAG,"BEGIN"); heardText.text="音声入力中..." }
            override fun onRmsChanged(v:Float) { if(v>4f) Log.v(TAG,"RMS=$v") }
            override fun onBufferReceived(b:ByteArray?){}
            override fun onEndOfSpeech(){ listening=false; Log.d(TAG,"END"); heardText.text="認識中..." }
            override fun onError(e:Int){ listening=false; Log.w(TAG,"ERROR=$e ${errorName(e)} mode=${if(waitingForFloor) "FLOOR" else "WAKE"}"); if(!speaking) scheduleListen(if(e==SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1400 else 800) }
            override fun onResults(r:Bundle?){ listening=false; val list=r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty(); Log.d(TAG,"RESULT=$list"); process(list) }
            override fun onPartialResults(r:Bundle?){ val list=r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty(); if(list.isNotEmpty()){ heardText.text="認識: ${list.first()}"; Log.v(TAG,"PARTIAL=$list") } }
            override fun onEvent(t:Int,p:Bundle?){}
        }) }
        speechIntent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ja-JP"); putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true); putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,1400L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,900L)
        }
    }

    private fun process(list:List<String>) {
        if(list.isEmpty()){ scheduleListen(700); return }
        heardText.text="認識: ${list.first()}"
        val all=list.joinToString(" "); val wake=list.any{isWake(it)}; val floor=extractFloor(all)
        when {
            floor!=null && (waitingForFloor||wake) -> acceptFloor(floor)
            wake -> { waitingForFloor=true; setStatus("はい、何階ですか？"); announce("はい、何階ですか", true) }
            waitingForFloor -> { setStatus("1階から15階でお話しください"); announce("1階から15階でお話しください", true) }
            else -> scheduleListen(500)
        }
    }

    private fun acceptFloor(floor:Int) {
        if(floor !in MIN_FLOOR..MAX_FLOOR){ setStatus("1階から15階を指定してください"); announce("1階から15階を指定してください",true); return }
        waitingForFloor=false; buttons.values.forEach{it.isSelected=false}; buttons[floor]?.isSelected=true
        currentFloorText.text=floor.toString(); setStatus("${floor}階を受け付けました"); heardText.text="行き先: ${floor}F"
        Log.d(TAG,"FLOOR=$floor"); announce("${floor}階を受け付けました。${floor}階へ参ります",false)
    }

    private fun announce(text:String, listenForFloorAfter:Boolean) {
        handler.removeCallbacksAndMessages(null); recognizer?.cancel(); listening=false
        if(!ttsReady){ speaking=false; waitingForFloor=listenForFloorAfter; scheduleListen(700); return }
        speaking=true; waitingForFloor=listenForFloorAfter
        val id=if(listenForFloorAfter) "ASK_FLOOR" else "ANNOUNCE"
        Log.d(TAG,"TTS_START request=$id text=$text"); tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,id)
    }

    private fun scheduleListen(delay:Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if(!isFinishing && !isDestroyed && !listening && !speaking && checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){
                try { recognizer?.cancel(); recognizer?.startListening(speechIntent); Log.d(TAG,"START_LISTEN mode=${if(waitingForFloor) "FLOOR" else "WAKE"}") }
                catch(e:Exception){ Log.e(TAG,"startListening failed",e); scheduleListen(1500) }
            }
        },delay)
    }

    private fun isWake(s:String):Boolean { val n=norm(s); return n.contains("hiエレベーター")||n.contains("ハイエレベーター")||n.contains("はいエレベーター")||n=="エレベーター" }
    private fun extractFloor(s0:String):Int? {
        val s=norm(s0).replace("１","1").replace("２","2").replace("３","3").replace("４","4").replace("５","5").replace("６","6").replace("７","7").replace("８","8").replace("９","9").replace("０","0")
        Regex("(?:^|[^0-9])(1[0-5]|[1-9])(?:f|階|回|$)").find(s)?.groupValues?.get(1)?.toIntOrNull()?.let{return it}
        val words=mapOf("十五" to 15,"十四" to 14,"十三" to 13,"十二" to 12,"十一" to 11,"十" to 10,"九" to 9,"八" to 8,"七" to 7,"六" to 6,"五" to 5,"四" to 4,"三" to 3,"二" to 2,"一" to 1)
        return words.entries.firstOrNull{s.contains(it.key+"階")||s==it.key}?.value
    }
    private fun norm(s:String)=s.lowercase(Locale.JAPAN).replace(" ","").replace("　","")
    private fun errorName(e:Int)=when(e){1->"NETWORK_TIMEOUT";2->"NETWORK";3->"AUDIO";4->"SERVER";5->"CLIENT";6->"SPEECH_TIMEOUT";7->"NO_MATCH";8->"BUSY";9->"INSUFFICIENT_PERMISSIONS";else->"UNKNOWN"}
    private fun setStatus(s:String){statusText.text=s}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    override fun onInit(status:Int) {
        if(status==TextToSpeech.SUCCESS){ val r=tts?.setLanguage(Locale.JAPAN); ttsReady=r!=TextToSpeech.LANG_MISSING_DATA&&r!=TextToSpeech.LANG_NOT_SUPPORTED; tts?.setSpeechRate(.9f)
            tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
                override fun onStart(id:String?){ speaking=true; Log.d(TAG,"TTS_ON_START id=$id") }
                override fun onDone(id:String?){ Log.d(TAG,"TTS_DONE id=$id"); runOnUiThread{ speaking=false; scheduleListen(if(id=="ASK_FLOOR") 450 else 900) } }
                @Deprecated("Deprecated in Java") override fun onError(id:String?){ Log.e(TAG,"TTS_ERROR id=$id"); runOnUiThread{speaking=false;scheduleListen(900)} }
            })
        } else { Log.e(TAG,"TTS init failed=$status"); ttsReady=false }
    }
    override fun onDestroy(){ handler.removeCallbacksAndMessages(null); recognizer?.cancel(); recognizer?.destroy(); tts?.stop(); tts?.shutdown(); super.onDestroy() }
}
