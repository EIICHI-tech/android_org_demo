package jp.co.demo.pachinkodatalamp

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
    private lateinit var panel: DataLampView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        panel = DataLampView(this)
        setContentView(panel)
        if (Build.VERSION.SDK_INT >= 30) window.insetsController?.hide(WindowInsets.Type.statusBars())
    }
    override fun onDestroy() { panel.release(); super.onDestroy() }
}

class DataLampView(private val host: Activity) : View(host), TextToSpeech.OnInitListener {
    private enum class State { NORMAL, CALL, STAFF }
    private data class Hit(val r: RectF, val action: () -> Unit)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans", Typeface.BOLD) }
    private val mono = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
    private val hits = mutableListOf<Hit>()
    private val handler = Handler(Looper.getMainLooper())
    private var state = State.NORMAL
    private var page = 0
    private var phase = 0f
    private var sound = true
    private var ttsReady = false
    private val tts = TextToSpeech(host, this)
    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()).build()
    private val chime = pool.load(host, R.raw.call_chime, 1)
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply { duration = 1800; repeatCount = ValueAnimator.INFINITE; addUpdateListener { phase=it.animatedValue as Float; invalidate() }; start() }

    override fun onInit(status:Int) { if(status==TextToSpeech.SUCCESS){ ttsReady=tts.setLanguage(Locale.JAPAN)>=0; tts.setSpeechRate(.92f) } }
    override fun onDraw(c:Canvas){super.onDraw(c);hits.clear();val w=width.toFloat();val h=height.toFloat();drawBg(c,w,h);drawFrame(c,w,h);drawMain(c,w,h);drawBottom(c,w,h)}

    private fun drawBg(c:Canvas,w:Float,h:Float){
        p.shader=LinearGradient(0f,0f,0f,h,intArrayOf(Color.rgb(2,4,8),Color.rgb(10,13,20),Color.rgb(0,1,4)),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p);p.shader=null
    }
    private fun drawFrame(c:Canvas,w:Float,h:Float){
        val n=46
        for(i in 0 until n){val x=w*(i+.5f)/n;led(c,x,h*.027f,h*.020f,ledColor(i,n));led(c,x,h*.973f,h*.020f,ledColor(i+9,n))}
        val m=20
        for(i in 0 until m){val y=h*(i+.5f)/m;led(c,w*.014f,y,h*.020f,ledColor(i+18,m));led(c,w*.986f,y,h*.020f,ledColor(i+27,m))}
        p.style=Paint.Style.STROKE;p.strokeWidth=h*.010f;p.color=Color.rgb(25,35,48);c.drawRoundRect(RectF(w*.025f,h*.045f,w*.975f,h*.955f),h*.035f,h*.035f,p);p.style=Paint.Style.FILL
    }
    private fun led(c:Canvas,x:Float,y:Float,r:Float,col:Int){p.color=col;p.setShadowLayer(r*1.6f,0f,0f,col);c.drawCircle(x,y,r,p);p.clearShadowLayer()}
    private fun ledColor(i:Int,n:Int):Int{
        if(state==State.CALL) return if((phase*10).toInt()%2==0) Color.rgb(255,20,45) else Color.rgb(60,0,5)
        if(state==State.STAFF) return Color.HSVToColor(floatArrayOf(45f,.95f,.45f+.55f*((sin(phase*2*PI-i*.4)+1)/2).toFloat()))
        return Color.HSVToColor(floatArrayOf((i*360f/n+phase*360f)%360f,.88f,1f))
    }
    private fun drawMain(c:Canvas,w:Float,h:Float){
        val body=RectF(w*.055f,h*.085f,w*.945f,h*.805f);p.color=Color.rgb(7,9,13);c.drawRoundRect(body,h*.035f,h*.035f,p)
        val left=RectF(w*.075f,h*.115f,w*.330f,h*.650f); val right=RectF(w*.350f,h*.115f,w*.925f,h*.650f)
        p.color=Color.rgb(1,2,3);c.drawRoundRect(left,18f,18f,p);c.drawRoundRect(right,18f,18f,p)
        drawDigits(c,left,w,h); if(page==0) drawStats(c,right,w,h) else drawHistory(c,right,w,h)
    }
    private fun drawDigits(c:Canvas,r:RectF,w:Float,h:Float){
        val big=when(state){State.CALL->"CALL";State.STAFF->"STAFF";else->"237"}
        mono.textAlign=Paint.Align.CENTER;mono.textSize=if(state==State.NORMAL)h*.20f else h*.105f
        mono.color=when(state){State.CALL->Color.rgb(255,30,65);State.STAFF->Color.YELLOW;else->Color.rgb(255,55,110)}
        mono.setShadowLayer(15f,0f,0f,mono.color);c.drawText(big,r.centerX(),r.top+r.height()*.36f,mono);mono.clearShadowLayer()
        mono.textSize=h*.095f;mono.color=Color.rgb(70,255,130);c.drawText("330",r.centerX(),r.top+r.height()*.62f,mono)
        t.textSize=h*.026f;t.color=Color.rgb(120,255,160);c.drawText("スタート",r.centerX(),r.top+r.height()*.72f,t)
        t.textSize=h*.032f;t.color=Color.WHITE;c.drawText("台番号 777",r.centerX(),r.bottom-h*.045f,t);t.textAlign=Paint.Align.LEFT;mono.textAlign=Paint.Align.LEFT
    }
    private fun drawStats(c:Canvas,r:RectF,w:Float,h:Float){
        t.color=Color.WHITE;t.textSize=h*.034f;c.drawText("本日の遊技データ",r.left+w*.025f,r.top+h*.055f,t)
        stat(c,"大当り", "23", Color.rgb(255,40,60), r.left+w*.03f,r.top+h*.135f,h)
        stat(c,"確 変", "15", Color.rgb(255,145,30), r.left+w*.17f,r.top+h*.135f,h)
        stat(c,"最大出玉", "12,370", Color.rgb(255,220,40), r.left+w*.31f,r.top+h*.135f,h)
        stat(c,"回転数", "330", Color.rgb(60,255,125), r.left+w*.03f,r.top+h*.265f,h)
        stat(c,"前日", "28", Color.rgb(40,210,255), r.left+w*.17f,r.top+h*.265f,h)
        stat(c,"前々日", "18", Color.rgb(155,115,255), r.left+w*.31f,r.top+h*.265f,h)
        graph(c,RectF(r.left+w*.025f,r.top+h*.345f,r.right-w*.025f,r.bottom-h*.035f),w,h)
    }
    private fun stat(c:Canvas,label:String,value:String,col:Int,x:Float,y:Float,h:Float){t.textSize=h*.022f;t.color=Color.LTGRAY;c.drawText(label,x,y,t);mono.textSize=h*.065f;mono.color=col;mono.setShadowLayer(8f,0f,0f,col);c.drawText(value,x,y+h*.060f,mono);mono.clearShadowLayer()}
    private fun graph(c:Canvas,r:RectF,w:Float,h:Float){
        p.style=Paint.Style.STROKE;p.strokeWidth=1.5f;p.color=Color.rgb(50,70,85);for(i in 0..5){val y=r.top+r.height()*i/5;c.drawLine(r.left,y,r.right,y,p)}
        val vals=floatArrayOf(.72f,.62f,.68f,.48f,.55f,.35f,.44f,.22f,.29f,.14f,.19f,.08f)
        val path=Path();vals.forEachIndexed{i,v->val x=r.left+r.width()*i/(vals.size-1);val y=r.top+r.height()*v;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
        p.strokeWidth=4f;p.color=Color.rgb(0,220,255);p.setShadowLayer(9f,0f,0f,p.color);c.drawPath(path,p);p.clearShadowLayer();p.style=Paint.Style.FILL
    }
    private fun drawHistory(c:Canvas,r:RectF,w:Float,h:Float){
        t.color=Color.WHITE;t.textSize=h*.034f;c.drawText("大当り履歴",r.left+w*.025f,r.top+h*.055f,t)
        val headers=listOf("回","時刻","スタート","種別","出玉");val xs=floatArrayOf(.04f,.18f,.38f,.61f,.80f)
        headers.forEachIndexed{i,s->t.textSize=h*.024f;t.color=Color.rgb(80,220,255);c.drawText(s,r.left+r.width()*xs[i],r.top+h*.115f,t)}
        for(row in 0..5){val y=r.top+h*(.17f+row*.07f);p.color=if(row%2==0)Color.rgb(14,24,34)else Color.rgb(8,14,22);c.drawRect(r.left+w*.02f,y-h*.035f,r.right-w*.02f,y+h*.025f,p);val data=listOf("${row+1}","${10+row}:2${row}","${30+row*17}",if(row%2==0)"大当り" else "確変","${1250+row*310}");data.forEachIndexed{i,s->t.textSize=h*.027f;t.color=if(i==3)Color.rgb(255,80,100)else Color.WHITE;c.drawText(s,r.left+r.width()*xs[i],y,t)}}
    }
    private fun drawBottom(c:Canvas,w:Float,h:Float){
        val y=h*.835f;val bh=h*.085f;val gap=w*.012f;val bw=(w*.89f-gap*5)/6f;var x=w*.055f
        button(c,RectF(x,y,x+bw,y+bh),"呼 出",state==State.CALL,Color.rgb(210,25,50)){call()};x+=bw+gap
        button(c,RectF(x,y,x+bw,y+bh),"休 憩",false,Color.rgb(65,95,130)){};x+=bw+gap
        button(c,RectF(x,y,x+bw,y+bh),"データ切替",page==1,Color.rgb(30,130,195)){page=1-page;invalidate()};x+=bw+gap
        button(c,RectF(x,y,x+bw,y+bh),"スタッフ到着",state==State.STAFF,Color.rgb(205,145,15)){staff()};x+=bw+gap
        button(c,RectF(x,y,x+bw,y+bh),"音声 ${if(sound)"ON" else "OFF"}",sound,Color.rgb(75,85,180)){sound=!sound;invalidate()};x+=bw+gap
        button(c,RectF(x,y,x+bw,y+bh),"リセット",false,Color.rgb(25,150,90)){reset()}
    }
    private fun button(c:Canvas,r:RectF,s:String,on:Boolean,col:Int,f:()->Unit){p.color=if(on)col else Color.rgb(29,38,47);c.drawRoundRect(r,12f,12f,p);p.style=Paint.Style.STROKE;p.strokeWidth=3f;p.color=if(on)Color.WHITE else Color.rgb(120,145,160);c.drawRoundRect(r,12f,12f,p);p.style=Paint.Style.FILL;t.textAlign=Paint.Align.CENTER;t.textSize=r.height()*.28f;t.color=Color.WHITE;c.drawText(s,r.centerX(),r.centerY()-(t.ascent()+t.descent())/2,t);t.textAlign=Paint.Align.LEFT;hits+=Hit(r,f)}
    private fun call(){handler.removeCallbacksAndMessages(null);state=State.CALL;if(sound){pool.play(chime,1f,1f,1,0,1f);if(ttsReady)handler.postDelayed({tts.speak("お客様がお呼びです。担当スタッフは対応をお願いします。",TextToSpeech.QUEUE_FLUSH,null,"call")},850)};invalidate()}
    private fun staff(){handler.removeCallbacksAndMessages(null);state=State.STAFF;if(sound&&ttsReady)tts.speak("スタッフが対応します。",TextToSpeech.QUEUE_FLUSH,null,"staff");invalidate()}
    private fun reset(){handler.removeCallbacksAndMessages(null);state=State.NORMAL;page=0;tts.stop();invalidate()}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_UP){hits.lastOrNull{it.r.contains(e.x,e.y)}?.action?.invoke();performClick()};return true}
    override fun performClick():Boolean{super.performClick();return true}
    fun release(){anim.cancel();handler.removeCallbacksAndMessages(null);tts.stop();tts.shutdown();pool.release()}
}
