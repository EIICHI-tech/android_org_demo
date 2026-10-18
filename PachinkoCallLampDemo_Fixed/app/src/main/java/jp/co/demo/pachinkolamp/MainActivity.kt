package jp.co.demo.pachinkolamp

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.*
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.*
import android.speech.tts.TextToSpeech
import android.view.*
import java.util.Locale
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var demo: LampView
    override fun onCreate(b: Bundle?) { super.onCreate(b); demo=LampView(this); setContentView(demo) }
    override fun onDestroy(){ demo.release(); super.onDestroy() }
}

class LampView(private val a: Activity): View(a), TextToSpeech.OnInitListener {
    private enum class Mode { WAIT, CALL, STAFF, DONE }
    private data class Hit(val r:RectF,val f:()->Unit)
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val tx=Paint(Paint.ANTI_ALIAS_FLAG).apply{typeface=Typeface.create("sans",Typeface.BOLD)}
    private val hits=mutableListOf<Hit>(); private var mode=Mode.WAIT; private var kind="通常呼出"; private var phase=0f
    private val h=Handler(Looper.getMainLooper()); private var ttsOk=false; private val tts=TextToSpeech(a,this)
    private val sp=SoundPool.Builder().setMaxStreams(2).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()).build()
    private val chime=sp.load(a,R.raw.call_chime,1)
    init { ValueAnimator.ofFloat(0f,1f).apply{duration=1600;repeatCount=ValueAnimator.INFINITE;addUpdateListener{phase=it.animatedValue as Float;invalidate()};start()} }
    override fun onInit(s:Int){if(s==TextToSpeech.SUCCESS){ttsOk=tts.setLanguage(Locale.JAPAN)>=0;tts.setSpeechRate(.95f)}}
    override fun onDraw(c:Canvas){super.onDraw(c);hits.clear();val w=width.toFloat();val hh=height.toFloat()
        p.shader=LinearGradient(0f,0f,w,hh,Color.rgb(3,5,14),Color.rgb(12,15,35),Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,hh,p);p.shader=null
        tx.color=Color.WHITE;tx.textSize=hh*.055f;c.drawText("呼び出しランプ デモ",w*.04f,hh*.10f,tx)
        tx.textSize=hh*.03f;tx.color=Color.rgb(150,180,235);c.drawText("台番号 123   |   ${status()}",w*.70f,hh*.095f,tx)
        lamp(c,RectF(w*.04f,hh*.16f,w*.96f,hh*.60f))
        val names=listOf("通常呼出","玉詰まり","計数機","緊急");val gap=w*.015f;val bw=(w*.92f-gap*3)/4
        names.forEachIndexed{i,n->button(c,RectF(w*.04f+i*(bw+gap),hh*.66f,w*.04f+i*(bw+gap)+bw,hh*.75f),n,n==kind){kind=n;invalidate()}}
        val y=hh*.80f; val big=w*.28f
        button(c,RectF(w*.04f,y,w*.04f+big,hh*.95f),"呼 出",mode==Mode.CALL,Color.rgb(210,20,55)){call()}
        button(c,RectF(w*.36f,y,w*.36f+big,hh*.95f),"スタッフ到着",mode==Mode.STAFF,Color.rgb(220,155,15)){staff()}
        button(c,RectF(w*.68f,y,w*.68f+big,hh*.95f),"対応完了",mode==Mode.DONE,Color.rgb(15,160,90)){done()}
    }
    private fun status()=when(mode){Mode.WAIT->"待機中";Mode.CALL->"呼出受付";Mode.STAFF->"スタッフ対応中";Mode.DONE->"対応完了"}
    private fun lamp(c:Canvas,r:RectF){p.color=Color.rgb(12,14,25);c.drawRoundRect(r,60f,60f,p);val n=34
        for(i in 0 until n){val x=r.left+r.width()*(i+.5f)/n;val col=color(i,n);p.color=col;p.setShadowLayer(18f,0f,0f,col);c.drawCircle(x,if(i%2==0)r.top+r.height()*.12f else r.bottom-r.height()*.12f,r.height()*.055f,p)}
        p.clearShadowLayer();val inner=RectF(r.left+r.width()*.11f,r.top+r.height()*.19f,r.right-r.width()*.11f,r.bottom-r.height()*.19f);p.color=Color.rgb(3,4,9);c.drawRoundRect(inner,45f,45f,p)
        tx.textAlign=Paint.Align.CENTER;tx.textSize=r.height()*.27f;tx.color=when(mode){Mode.CALL->Color.WHITE;Mode.STAFF->Color.YELLOW;Mode.DONE->Color.rgb(90,255,150);else->Color.rgb(140,190,255)};tx.setShadowLayer(18f,0f,0f,tx.color);c.drawText(when(mode){Mode.WAIT->"待 機";Mode.CALL->"呼 出";Mode.STAFF->"対応中";Mode.DONE->"完 了"},r.centerX(),r.centerY()+r.height()*.08f,tx);tx.clearShadowLayer();tx.textAlign=Paint.Align.LEFT}
    private fun color(i:Int,n:Int):Int=when(mode){
        Mode.WAIT->Color.HSVToColor(floatArrayOf(210f,.9f,.45f+.35f*sin(phase*PI).toFloat()))
        Mode.CALL->if((phase*8).toInt()%2==0)Color.rgb(255,20,45) else Color.rgb(65,0,5)
        Mode.STAFF->Color.HSVToColor(floatArrayOf(48f,.95f,.45f+.5f*((sin(phase*2*PI-i*.4)+1)/2).toFloat()))
        Mode.DONE->Color.HSVToColor(floatArrayOf((i*360f/n+phase*360)%360,.9f,1f))}
    private fun button(c:Canvas,r:RectF,s:String,on:Boolean,col:Int=Color.rgb(35,65,120),f:()->Unit){p.color=if(on)col else Color.rgb(22,28,48);c.drawRoundRect(r,18f,18f,p);p.style=Paint.Style.STROKE;p.strokeWidth=if(on)4f else 2f;p.color=if(on)Color.WHITE else Color.rgb(75,95,140);c.drawRoundRect(r,18f,18f,p);p.style=Paint.Style.FILL;tx.textAlign=Paint.Align.CENTER;tx.textSize=r.height()*.30f;tx.color=Color.WHITE;c.drawText(s,r.centerX(),r.centerY()-(tx.ascent()+tx.descent())/2,tx);tx.textAlign=Paint.Align.LEFT;hits+=Hit(r,f)}
    private fun call(){h.removeCallbacksAndMessages(null);mode=Mode.CALL;sp.play(chime,1f,1f,1,0,1f);if(ttsOk)h.postDelayed({tts.speak(speech(),TextToSpeech.QUEUE_FLUSH,null,"call")},800);h.postDelayed({staff()},7000);invalidate()}
    private fun speech()=when(kind){"玉詰まり"->"玉詰まりが発生しました。担当スタッフはご確認ください。";"計数機"->"計数機の確認をお願いします。";"緊急"->"緊急呼出です。スタッフは速やかに対応してください。";else->"お客様がお呼びです。担当スタッフは対応をお願いします。"}
    private fun staff(){h.removeCallbacksAndMessages(null);mode=Mode.STAFF;h.postDelayed({done()},6000);invalidate()}
    private fun done(){h.removeCallbacksAndMessages(null);mode=Mode.DONE;sp.play(chime,.6f,.6f,1,0,1.2f);h.postDelayed({mode=Mode.WAIT;invalidate()},4500);invalidate()}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_UP){hits.lastOrNull{it.r.contains(e.x,e.y)}?.f?.invoke();performClick()};return true}
    override fun performClick():Boolean{super.performClick();return true}
    fun release(){h.removeCallbacksAndMessages(null);tts.stop();tts.shutdown();sp.release()}
}
