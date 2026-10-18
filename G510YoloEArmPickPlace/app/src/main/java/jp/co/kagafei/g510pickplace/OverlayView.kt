package jp.co.kagafei.g510pickplace

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class OverlayView(context:Context):View(context){
    var detections:List<Detection> = emptyList(); set(v){field=v;postInvalidate()}
    var selected:Detection?=null; set(v){field=v;postInvalidate()}
    var onSelect:(Detection)->Unit={}
    private val box=Paint().apply{style=Paint.Style.STROKE;strokeWidth=4f;color=Color.CYAN}
    private val sel=Paint().apply{style=Paint.Style.STROKE;strokeWidth=8f;color=Color.YELLOW}
    private val text=Paint().apply{color=Color.WHITE;textSize=28f;typeface=Typeface.DEFAULT_BOLD}
    override fun onDraw(c:Canvas){super.onDraw(c);detections.forEach{d->val r=RectF(d.box.left*width,d.box.top*height,d.box.right*width,d.box.bottom*height);c.drawRect(r,if(d==selected)sel else box);c.drawText("${d.label} ${(d.score*100).toInt()}%",r.left,r.top.coerceAtLeast(30f),text)}}
    override fun onTouchEvent(e:MotionEvent):Boolean { if(e.action==MotionEvent.ACTION_UP){val x=e.x/width;val y=e.y/height;detections.filter{it.box.contains(x,y)}.maxByOrNull{it.score}?.let{onSelect(it)};performClick()};return true }
    override fun performClick():Boolean{super.performClick();return true}
}
