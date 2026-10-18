package jp.co.demo.pachinkolamp

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.View
import java.util.Locale
import kotlin.math.*

class PachinkoLampView(context: Context) : View(context), TextToSpeech.OnInitListener {
    enum class State { STANDBY, CALLING, ASSIGNED, COMPLETED }
    enum class CallType(val label: String, val speech: String) {
        NORMAL("通常呼出", "お客様がお呼びです。担当スタッフは対応をお願いします。"),
        BALL("玉詰まり", "玉詰まりが発生しました。担当スタッフはご確認ください。"),
        COUNTER("計数機", "計数機の確認をお願いします。"),
        EMERGENCY("緊急", "緊急呼出です。スタッフは速やかに対応してください。")
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans", Typeface.BOLD) }
    private val thin = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans", Typeface.NORMAL) }
    private val handler = Handler(Looper.getMainLooper())
    private var phase = 0f
    private var state = State.STANDBY
    private var type = CallType.NORMAL
    private var soundEnabled = true
    private var autoProgress = true
    private var tableNo = 123
    private var ttsReady = false
    private val tts = TextToSpeech(context, this)
    private val soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val soundId = soundPool.load(context, R.raw.call_chime, 1)
    private var animator: ValueAnimator? = null
    private val buttons = mutableListOf<ButtonHit>()
    private data class ButtonHit(val rect: RectF, val action: () -> Unit)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = tts.setLanguage(Locale.JAPAN) >= TextToSpeech.LANG_AVAILABLE
            tts.setSpeechRate(0.95f)
            tts.setPitch(1.02f)
        }
    }

    fun startDemo() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1800; repeatCount = ValueAnimator.INFINITE
            addUpdateListener { phase = it.animatedValue as Float; invalidate() }
            start()
        }
    }
    fun stopDemo() { animator?.cancel(); handler.removeCallbacksAndMessages(null) }
    fun release() { stopDemo(); tts.stop(); tts.shutdown(); soundPool.release() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        buttons.clear()
        val w = width.toFloat(); val h = height.toFloat()
        drawBackground(c, w, h)
        drawHeader(c, w, h)
        drawLamp(c, w * .04f, h * .18f, w * .96f, h * .60f)
        drawControls(c, w, h)
    }

    private fun drawBackground(c: Canvas, w: Float, h: Float) {
        p.shader = LinearGradient(0f,0f,w,h, intArrayOf(Color.rgb(4,5,15), Color.rgb(11,13,31), Color.rgb(3,4,12)), null, Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,w,h,p); p.shader = null
        p.color = Color.argb(35, 70, 120, 255)
        for(i in 0..12) c.drawCircle(w*(i/12f), h*(.10f + .02f*sin(i.toFloat())), 2f + 3f*(i%3), p)
    }

    private fun drawHeader(c: Canvas, w: Float, h: Float) {
        thin.color = Color.rgb(125,155,220); thin.textSize = h*.028f
        c.drawText("PACHINKO HALL  •  SMART CALL SYSTEM", w*.04f, h*.075f, thin)
        text.color = Color.WHITE; text.textSize = h*.054f
        c.drawText("呼び出しランプ デモ", w*.04f, h*.135f, text)
        thin.textSize = h*.024f; thin.color = Color.LTGRAY
        val status = when(state){ State.STANDBY->"待機中"; State.CALLING->"呼出受付"; State.ASSIGNED->"スタッフ対応中"; State.COMPLETED->"対応完了" }
        c.drawText("台番号 $tableNo   |   $status", w*.72f, h*.105f, thin)
    }

    private fun drawLamp(c: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val outer = RectF(l,t,r,b); val radius=(b-t)*.20f
        p.color=Color.rgb(13,15,25); p.setShadowLayer(22f,0f,10f,Color.BLACK); setLayerType(LAYER_TYPE_SOFTWARE,p)
        c.drawRoundRect(outer,radius,radius,p); p.clearShadowLayer()
        val segments=34
        for(i in 0 until segments){
            val x=l+(r-l)*(i+.5f)/segments
            val edgeY= if(i%2==0) t+(b-t)*.12f else b-(b-t)*.12f
            val col=ledColor(i,segments)
            p.color=col; p.setShadowLayer(16f,0f,0f,col)
            c.drawCircle(x,edgeY,(b-t)*.055f,p)
        }
        for(i in 0 until 10){
            val y=t+(b-t)*(i+.5f)/10f
            val col=ledColor(i+segments,segments+10)
            p.color=col; p.setShadowLayer(14f,0f,0f,col)
            c.drawCircle(l+(r-l)*.035f,y,(b-t)*.052f,p)
            c.drawCircle(r-(r-l)*.035f,y,(b-t)*.052f,p)
        }
        p.clearShadowLayer()
        val panel=RectF(l+(r-l)*.11f,t+(b-t)*.18f,r-(r-l)*.11f,b-(b-t)*.18f)
        p.shader=LinearGradient(panel.left,panel.top,panel.left,panel.bottom,Color.rgb(24,27,40),Color.rgb(2,3,8),Shader.TileMode.CLAMP)
        c.drawRoundRect(panel,(b-t)*.11f,(b-t)*.11f,p); p.shader=null
        text.textAlign=Paint.Align.CENTER
        text.color=when(state){State.CALLING->Color.WHITE;State.ASSIGNED->Color.rgb(255,220,70);State.COMPLETED->Color.rgb(100,255,160);else->Color.rgb(140,185,255)}
        text.textSize=(b-t)*.26f
        val title=when(state){State.STANDBY->"待 機";State.CALLING->"呼 出";State.ASSIGNED->"対応中";State.COMPLETED->"完 了"}
        p.color=text.color; p.setShadowLayer(20f,0f,0f,text.color); text.setShadowLayer(16f,0f,0f,text.color)
        c.drawText(title,(l+r)/2f,t+(b-t)*.59f,text); text.clearShadowLayer(); p.clearShadowLayer()
        thin.textAlign=Paint.Align.CENTER; thin.color=Color.rgb(175,190,220); thin.textSize=(b-t)*.07f
        c.drawText(type.label,(l+r)/2f,t+(b-t)*.76f,thin)
        thin.textAlign=Paint.Align.LEFT; text.textAlign=Paint.Align.LEFT
    }

    private fun ledColor(i:Int,n:Int):Int {
        if(state==State.STANDBY) return Color.HSVToColor(floatArrayOf(205f+15f*sin(phase*PI*2).toFloat(),.85f,.50f+.35f*sin(phase*PI).toFloat()))
        if(state==State.CALLING){ val on=((phase*8).toInt()%2)==0; return if(on) Color.rgb(255,20,45) else Color.rgb(70,0,5) }
        if(state==State.ASSIGNED){ val v=.45f+.55f*((sin((phase*2*PI-i*.45))+1)/2).toFloat(); return Color.HSVToColor(floatArrayOf(48f,.98f,v)) }
        val hue=(i*360f/n+phase*360f)%360f; return Color.HSVToColor(floatArrayOf(hue,.92f,1f))
    }

    private fun drawControls(c:Canvas,w:Float,h:Float){
        val top=h*.66f; val gap=w*.012f; val bw=(w*.92f-gap*5)/6f; var x=w*.04f
        for(ct in CallType.entries){
            drawButton(c,RectF(x,top,x+bw,top+h*.09f),ct.label,ct==type){ type=ct; invalidate() }; x+=bw+gap
        }
        drawButton(c,RectF(x,top,x+bw,top+h*.09f),if(soundEnabled)"音声 ON" else "音声 OFF",soundEnabled){soundEnabled=!soundEnabled;invalidate()}; x+=bw+gap
        drawButton(c,RectF(x,top,x+bw,top+h*.09f),if(autoProgress)"自動進行 ON" else "自動進行 OFF",autoProgress){autoProgress=!autoProgress;invalidate()}

        val y=h*.79f; val bigW=w*.28f
        drawButton(c,RectF(w*.04f,y,w*.04f+bigW,y+h*.14f),"呼 出",state==State.CALLING,Color.rgb(210,20,50)){beginCall()}
        drawButton(c,RectF(w*.36f,y,w*.36f+bigW,y+h*.14f),"スタッフ到着",state==State.ASSIGNED,Color.rgb(220,160,15)){setAssigned()}
        drawButton(c,RectF(w*.68f,y,w*.68f+bigW,y+h*.14f),"対応完了 / リセット",state==State.COMPLETED,Color.rgb(20,155,90)){complete()}
    }

    private fun drawButton(c:Canvas,r:RectF,label:String,active:Boolean,color:Int=Color.rgb(40,70,125),action:()->Unit){
        p.color=if(active) color else Color.rgb(22,28,48); p.style=Paint.Style.FILL; c.drawRoundRect(r,18f,18f,p)
        p.style=Paint.Style.STROKE; p.strokeWidth=if(active)4f else 2f; p.color=if(active)Color.WHITE else Color.rgb(72,92,140); c.drawRoundRect(r,18f,18f,p);p.style=Paint.Style.FILL
        text.textAlign=Paint.Align.CENTER;text.color=Color.WHITE;text.textSize=r.height()*.30f
        c.drawText(label,r.centerX(),r.centerY()-((text.ascent()+text.descent())/2),text);text.textAlign=Paint.Align.LEFT
        buttons += ButtonHit(r,action)
    }

    private fun beginCall(){
        handler.removeCallbacksAndMessages(null); state=State.CALLING
        if(soundEnabled){ soundPool.play(soundId,1f,1f,1,0,1f); if(ttsReady) handler.postDelayed({tts.speak(type.speech,TextToSpeech.QUEUE_FLUSH,null,"call")},850) }
        if(autoProgress) handler.postDelayed({setAssigned()},7000)
        invalidate()
    }
    private fun setAssigned(){ handler.removeCallbacksAndMessages(null);state=State.ASSIGNED;if(autoProgress)handler.postDelayed({complete()},6000);invalidate() }
    private fun complete(){ handler.removeCallbacksAndMessages(null);state=State.COMPLETED;if(soundEnabled)soundPool.play(soundId,.65f,.65f,1,0,1.25f);handler.postDelayed({state=State.STANDBY;invalidate()},4500);invalidate() }

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.action==MotionEvent.ACTION_UP){ buttons.lastOrNull{it.rect.contains(e.x,e.y)}?.action?.invoke();performClick() }
        return true
    }
    override fun performClick():Boolean{super.performClick();return true}
}
