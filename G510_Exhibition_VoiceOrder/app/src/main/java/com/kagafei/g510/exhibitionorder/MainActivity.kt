package com.kagafei.g510.exhibitionorder

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.max

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
 data class Menu(val name:String,val price:Int,val image:Int,val aliases:List<String>)
 private val menus = listOf(
  Menu("照り焼きバーガー",780,R.drawable.menu_burger,listOf("バーガー","ハンバーガー","照り焼き")),
  Menu("クラブサンド",680,R.drawable.menu_sandwich,listOf("サンド","サンドイッチ","クラブサンド")),
  Menu("カフェラテ",450,R.drawable.menu_coffee,listOf("コーヒー","ラテ","カフェラテ")),
  Menu("いちごパフェ",620,R.drawable.menu_parfait,listOf("パフェ","いちご","デザート")))
 private val cart=linkedMapOf<Menu,Int>()
 private lateinit var tts:TextToSpeech
 private lateinit var guide:TextView; private lateinit var peopleBadge:TextView; private lateinit var cameraStatus:TextView
 private lateinit var conversation:TextView; private lateinit var orderText:TextView; private lateinit var totalText:TextView; private lateinit var input:EditText
 private var detectedPeople=0; private var greeted=false; private var pendingConfirmation=false
 private val cameraExecutor=Executors.newSingleThreadExecutor()
 private var lastAnalysis=0L

 private val permissions=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ r ->
  if(r[Manifest.permission.CAMERA]==true) startCamera() else cameraStatus.text="カメラ権限なし: 手動設定を使用"
 }
 private val speech=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){r->
  if(r.resultCode==Activity.RESULT_OK){
   val s=r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
   if(s.isNotBlank()){input.setText(s); handleUtterance(s)} else fallback("認識結果が空でした。画面から選択できます。")
  } else fallback("音声入力を完了できませんでした。画面から選択できます。")
 }
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main)
  tts=TextToSpeech(this,this); bind(); createMenuCards(); conversation.movementMethod=ScrollingMovementMethod()
  findViewById<Button>(R.id.voiceButton).setOnClickListener{startVoice()}; findViewById<Button>(R.id.sendButton).setOnClickListener{handleUtterance(input.text.toString())}
  findViewById<Button>(R.id.confirmButton).setOnClickListener{confirmOrder()}; findViewById<Button>(R.id.resetButton).setOnClickListener{resetSession()}
  findViewById<Button>(R.id.manualPeopleButton).setOnClickListener{showPeopleDialog()}
  permissions.launch(arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO))
 }
 private fun bind(){guide=findViewById(R.id.guideText);peopleBadge=findViewById(R.id.peopleBadge);cameraStatus=findViewById(R.id.cameraStatus);conversation=findViewById(R.id.conversationText);orderText=findViewById(R.id.orderText);totalText=findViewById(R.id.totalText);input=findViewById(R.id.manualInput)}
 override fun onInit(s:Int){if(s==TextToSpeech.SUCCESS){tts.language=Locale.JAPAN;tts.setSpeechRate(0.95f);speak("いらっしゃいませ。人数を確認しています。")}}
 private fun createMenuCards(){val box=findViewById<LinearLayout>(R.id.menuContainer); menus.forEach{m->
  val v=LayoutInflater.from(this).inflate(R.layout.menu_card,box,false);v.findViewById<ImageView>(R.id.menuImage).setImageResource(m.image);v.findViewById<TextView>(R.id.menuName).text=m.name;v.findViewById<TextView>(R.id.menuPrice).text="¥${m.price}";v.setOnClickListener{add(m,1);reply("${m.name}を1点追加しました。ほかにご注文はありますか？")};box.addView(v)}}
 private fun startVoice(){
  if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){permissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO));return}
  // G510対策: isRecognitionAvailable()を使わず、認識Intentを直接起動する。
  val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ja-JP");putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);putExtra(RecognizerIntent.EXTRA_PROMPT,"ご注文をお話しください")}
  try{speech.launch(i);guide.text="音声を聞いています…"}catch(e:ActivityNotFoundException){Log.w(TAG,"Recognizer activity missing",e);fallback("音声画面を起動できません。写真メニューまたは手入力をご利用ください。") }catch(e:Exception){Log.e(TAG,"Recognizer launch failed",e);fallback("音声入力を開始できません。写真メニューまたは手入力をご利用ください。")}
 }
 private fun handleUtterance(raw:String){val text=raw.trim();if(text.isBlank())return;append("お客様: $text")
  if(text.contains("おすすめ")){reply("おすすめは照り焼きバーガーです。追加しますか？");return}
  if(text.contains("はい")&&pendingConfirmation){completeOrder();return}
  if((text.contains("いいえ")||text.contains("違"))&&pendingConfirmation){pendingConfirmation=false;reply("承知しました。変更する商品をお話しください。");return}
  if(text.contains("会計")||text.contains("確定")||text.contains("以上")){confirmOrder();return}
  if(text.contains("取消")||text.contains("キャンセル")||text.contains("削除")){cart.clear();renderCart();reply("注文を取り消しました。最初からご注文ください。");return}
  val menu=menus.firstOrNull{m->m.aliases.any{text.contains(it)}||text.contains(m.name)}
  if(menu!=null){val qty=parseQuantity(text);add(menu,qty);reply("${menu.name}を${qty}点追加しました。ほかにご注文はありますか？")}
  else reply("商品名を確認できませんでした。写真をタップするか、バーガー、サンドイッチ、カフェラテ、パフェのようにお話しください。")
  input.text.clear()
 }
 private fun parseQuantity(t:String):Int{val map=mapOf("一" to 1,"1" to 1,"ひとつ" to 1,"二" to 2,"2" to 2,"ふたつ" to 2,"三" to 3,"3" to 3,"四" to 4,"4" to 4);return map.entries.firstOrNull{t.contains(it.key)}?.value?:1}
 private fun add(m:Menu,q:Int){cart[m]=(cart[m]?:0)+max(1,q);renderCart()}
 private fun renderCart(){if(cart.isEmpty()){orderText.text="まだ注文はありません";totalText.text="合計 ¥0";return};orderText.text=cart.entries.joinToString("\n\n"){"${it.key.name}  × ${it.value}    ¥${it.key.price*it.value}"};totalText.text="合計 ¥${cart.entries.sumOf{it.key.price*it.value}}"}
 private fun confirmOrder(){if(cart.isEmpty()){reply("ご注文がまだありません。商品をお選びください。");return};pendingConfirmation=true;val summary=cart.entries.joinToString("、"){"${it.key.name}${it.value}点"};reply("ご注文は、${summary}、合計${cart.entries.sumOf{it.key.price*it.value}}円です。よろしければ、はい、とお答えください。")}
 private fun completeOrder(){pendingConfirmation=false;reply("ご注文を承りました。ありがとうございます。番号札をお持ちになってお待ちください。");findViewById<Button>(R.id.confirmButton).isEnabled=false}
 private fun reply(s:String){guide.text=s;append("AIガイド: $s");speak(s)}
 private fun append(s:String){conversation.append("\n\n$s");conversation.post{conversation.scrollTo(0,conversation.bottom)}}
 private fun speak(s:String){if(::tts.isInitialized)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"guide")}
 private fun fallback(s:String){guide.text=s;append("システム: $s");input.requestFocus()}
 private fun showPeopleDialog(){val a=(1..6).map{"${it}名"}.toTypedArray();AlertDialog.Builder(this).setTitle("人数を選択").setItems(a){_,i->onPeople(i+1)}.show()}
 private fun onPeople(n:Int){detectedPeople=n;peopleBadge.text="人数検知: ${n}名";cameraStatus.text="${n}名を検知";if(!greeted&&n>0){greeted=true;reply("いらっしゃいませ。${n}名でご来店ですね。写真をタップするか、マイクボタンからご注文ください。")}}
 private fun startCamera(){val f=ProcessCameraProvider.getInstance(this);f.addListener({val p=f.get();val preview=Preview.Builder().build().also{it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.previewView).surfaceProvider)};val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();val detector=FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build());analysis.setAnalyzer(cameraExecutor){proxy->
   val now=System.currentTimeMillis();if(now-lastAnalysis<650){proxy.close();return@setAnalyzer};lastAnalysis=now
   val media=proxy.image;if(media==null){proxy.close();return@setAnalyzer};val image=InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees);detector.process(image).addOnSuccessListener{faces->runOnUiThread{onPeople(faces.size)}}.addOnCompleteListener{proxy.close()}}
   try{p.unbindAll();val selector=if(p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA))CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA;p.bindToLifecycle(this,selector,preview,analysis);cameraStatus.text="AI人数検知中"}catch(e:Exception){Log.e(TAG,"Camera bind failed",e);cameraStatus.text="カメラ開始失敗: 手動設定を使用"}},ContextCompat.getMainExecutor(this))}
 private fun resetSession(){cart.clear();detectedPeople=0;greeted=false;pendingConfirmation=false;renderCart();peopleBadge.text="人数検知: 待機中";conversation.text="会話履歴";findViewById<Button>(R.id.confirmButton).isEnabled=true;reply("いらっしゃいませ。人数を確認しています。")}
 override fun onDestroy(){tts.stop();tts.shutdown();cameraExecutor.shutdown();super.onDestroy()}
 companion object{const val TAG="G510Exhibition"}
}
