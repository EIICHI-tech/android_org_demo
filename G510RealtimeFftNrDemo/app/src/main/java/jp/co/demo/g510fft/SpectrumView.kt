package jp.co.demo.g510fft

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class SpectrumView @JvmOverloads constructor(c: Context, a: AttributeSet? = null): View(c, a) {
    @Volatile var raw = FloatArray(512) { -120f }
    @Volatile var nr = FloatArray(512) { -120f }
    var sampleRate = 48000
    var nrEnabled = true
    private var cursorX = -1f
    private val grid = Paint(1).apply { color = Color.rgb(45,65,78); strokeWidth = 1f }
    private val text = Paint(1).apply { color = Color.LTGRAY; textSize = 27f }
    private val rawPaint = Paint(1).apply { color = Color.rgb(160,170,175); strokeWidth = 3f; style = Paint.Style.STROKE }
    private val nrPaint = Paint(1).apply { color = Color.rgb(40,190,255); strokeWidth = 4f; style = Paint.Style.STROKE }

    override fun onDraw(c: Canvas) {
        c.drawColor(Color.rgb(7,16,24))
        val l=75f; val r=width-25f; val t=35f; val b=height-55f
        for (db in -100..0 step 20) {
            val y = b - (db + 100) / 100f * (b-t)
            c.drawLine(l,y,r,y,grid); c.drawText("$db",8f,y+9,text)
        }
        val maxHz=12000
        for (hz in 0..maxHz step 2000) {
            val x=l+hz/maxHz.toFloat()*(r-l); c.drawLine(x,t,x,b,grid); c.drawText("${hz/1000}k",x-15,b+35,text)
        }
        drawSpectrum(c, raw, rawPaint, l,r,t,b,maxHz)
        if(nrEnabled) drawSpectrum(c,nr,nrPaint,l,r,t,b,maxHz)
        c.drawText("RAW",l+15,t+25,rawPaint.apply{style=Paint.Style.FILL})
        if(nrEnabled)c.drawText("NR",l+110,t+25,nrPaint.apply{style=Paint.Style.FILL})
        rawPaint.style=Paint.Style.STROKE; nrPaint.style=Paint.Style.STROKE
        if(cursorX in l..r){
            c.drawLine(cursorX,t,cursorX,b,Paint(1).apply{color=Color.YELLOW})
            val hz=((cursorX-l)/(r-l)*maxHz).roundToInt().coerceIn(0,maxHz)
            val idx=(hz.toDouble()/sampleRate*raw.size*2).roundToInt().coerceIn(raw.indices)
            val s="${hz} Hz  RAW ${"%.1f".format(raw[idx])} dBFS" + if(nrEnabled) "  NR ${"%.1f".format(nr[idx])} dBFS" else ""}
            c.drawText(s,l+10,b-15,text.apply{color=Color.YELLOW}); text.color=Color.LTGRAY
        }
    }
    private fun drawSpectrum(c:Canvas,d:FloatArray,p:Paint,l:Float,r:Float,t:Float,b:Float,maxHz:Int){
        val path=Path(); val maxBin=(maxHz.toDouble()/sampleRate*d.size*2).roundToInt().coerceIn(1,d.size-1)
        for(xi in 0..(r-l).toInt()){
            val bin=(xi/(r-l)*maxBin).roundToInt().coerceIn(d.indices)
            val x=l+xi; val y=b-((d[bin]+100f)/100f).coerceIn(0f,1f)*(b-t)
            if(xi==0)path.moveTo(x,y) else path.lineTo(x,y)
        }; c.drawPath(path,p)
    }
    override fun onTouchEvent(e:MotionEvent):Boolean { cursorX=e.x; invalidate(); return true }
}
