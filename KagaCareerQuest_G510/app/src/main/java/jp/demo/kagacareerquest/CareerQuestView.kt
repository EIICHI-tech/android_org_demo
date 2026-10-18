package jp.demo.kagacareerquest

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import java.util.Locale

class CareerQuestView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bg = Color.rgb(7, 26, 43)
    private val panel = Color.rgb(18, 43, 63)
    private val mint = Color.rgb(0, 183, 168)
    private val gold = Color.rgb(255, 190, 65)
    private val white = Color.rgb(242, 248, 252)
    private val muted = Color.rgb(163, 185, 199)

    private var page = 0
    private var scenario = 0
    private var selectedCourse = "FAE"
    private var selected = -1
    private var showResult = false
    private val scores = linkedMapOf("技術理解" to 0, "顧客対応" to 0, "連携" to 0, "リスク管理" to 0)
    private val hitAreas = mutableListOf<Pair<RectF, () -> Unit>>()

    data class Choice(val text: String, val gain: IntArray, val feedback: String)
    data class Scenario(val tag: String, val title: String, val body: String, val choices: List<Choice>)

    private val scenarios = listOf(
        Scenario("展示会", "デモ直前の画像認識トラブル", "開始直前、カメラ認識が不安定になりました。あなたはどう行動しますか？", listOf(
            Choice("ログと接続状態を確認し、原因を切り分ける", intArrayOf(3,1,1,2), "事実を確認してから行動することで、再現性のある解決につながります。"),
            Choice("担当を分け、並行して調査と来場者対応を進める", intArrayOf(1,2,3,2), "役割分担により、技術対応と顧客体験を両立できます。"),
            Choice("安定版デモへ切り替え、説明内容を調整する", intArrayOf(0,3,1,3), "事業継続を優先する判断です。復旧後の振り返りも重要です。")
        )),
        Scenario("顧客提案", "要件が曖昧なAI相談", "顧客から『AIで何か面白いことを』と相談されました。最初の一手は？", listOf(
            Choice("現場課題・利用者・成功条件を質問する", intArrayOf(1,3,1,2), "目的と評価条件を明確にすると、AI導入が手段目的化しにくくなります。"),
            Choice("G510で動く複数の短いデモを見せる", intArrayOf(2,2,1,1), "具体例は対話を促します。次に課題と成功条件を確認しましょう。"),
            Choice("小規模PoCと評価項目を提案する", intArrayOf(2,2,2,3), "検証範囲を限定し、判断材料を作る進め方です。")
        )),
        Scenario("プロジェクト", "納期と品質のバランス", "追加要望が入り、すべて対応すると納期リスクが高まります。どう進めますか？", listOf(
            Choice("必須・任意に分け、影響を共有して優先順位を決める", intArrayOf(1,2,3,3), "関係者と判断材料を共有し、合意形成する方法です。"),
            Choice("重要機能だけ実装し、残りは次期候補にする", intArrayOf(2,1,1,3), "スコープを守れます。変更理由を記録するとさらに有効です。"),
            Choice("試作で技術難度を確認してから再見積もりする", intArrayOf(3,1,2,2), "不確実性を小さくしてから計画を更新する方法です。")
        ))
    )

    override fun onDraw(c: Canvas) {
        super.onDraw(c); c.drawColor(bg); hitAreas.clear()
        when(page) { 0 -> drawHome(c); 1 -> drawCourse(c); 2 -> drawScenario(c); 3 -> drawSummary(c) }
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = white, bold: Boolean = false) {
        paint.color=color; paint.textSize=size; paint.typeface=if(bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        c.drawText(s,x,y,paint)
    }
    private fun round(c: Canvas, r: RectF, color: Int, radius: Float=28f) { paint.color=color; c.drawRoundRect(r,radius,radius,paint) }
    private fun button(c: Canvas, r: RectF, label: String, primary:Boolean=false, action:()->Unit) {
        round(c,r,if(primary) mint else panel,22f); text(c,label,r.left+28,r.centerY()+10,28f,white,true); hitAreas += r to action
    }
    private fun header(c: Canvas, step:String) {
        text(c,"KAGA CAREER QUEST",52f,62f,25f,mint,true); text(c,step,width-270f,62f,19f,muted)
    }

    private fun drawHome(c: Canvas) {
        val w=width.toFloat(); val h=height.toFloat()
        paint.shader=LinearGradient(0f,0f,w,h,Color.rgb(8,35,57),Color.rgb(6,18,32),Shader.TileMode.CLAMP); c.drawRect(0f,0f,w,h,paint); paint.shader=null
        text(c,"G510 OFFLINE AI DEMO",60f,70f,20f,mint,true)
        text(c,"選択から学ぶ",60f,h*.35f,55f,white,true)
        text(c,"キャリア育成シミュレーション",60f,h*.35f+67f,55f,white,true)
        text(c,"架空の業務シナリオを体験し、判断の観点を学ぶ研修デモ",64f,h*.35f+118f,24f,muted)
        button(c,RectF(64f,h-160f,430f,h-84f),"体験を開始",true){ page=1; invalidate() }
        round(c,RectF(w-430f,120f,w-70f,h-80f),panel,38f)
        text(c,"AI EDGE",w-374f,190f,22f,gold,true)
        arrayOf("01  課題を読む","02  対応を選ぶ","03  学びを確認").forEachIndexed{i,s-> text(c,s,w-374f,270f+i*78f,26f,white,i==0)}
        text(c,"※実在社員の評価・昇進予測には使用しません",w-400f,h-115f,15f,muted)
    }

    private fun drawCourse(c: Canvas) {
        header(c,"STEP 1 / 3")
        text(c,"体験するコースを選択",55f,135f,42f,white,true)
        text(c,"どのコースでも、同じ3つの判断シナリオを体験できます。",58f,178f,20f,muted)
        val labels=listOf("FAE" to "技術と顧客をつなぐ", "営業" to "課題を価値へ変える", "開発" to "品質と速度を両立", "PM" to "チームで成果を出す")
        val cardW=(width-150f)/2f; val cardH=120f
        labels.forEachIndexed{i,p->
            val col=i%2; val row=i/2; val r=RectF(55f+col*(cardW+40),225f+row*(cardH+32),55f+col*(cardW+40)+cardW,225f+row*(cardH+32)+cardH)
            round(c,r,if(selectedCourse==p.first) Color.rgb(21,72,82) else panel,25f); text(c,p.first,r.left+26,r.top+45,28f,if(selectedCourse==p.first) mint else white,true); text(c,p.second,r.left+26,r.top+84,20f,muted)
            hitAreas += r to { selectedCourse=p.first; invalidate() }
        }
        button(c,RectF(width-370f,height-105f,width-55f,height-38f),"このコースで開始",true){ page=2; invalidate() }
    }

    private fun drawScenario(c: Canvas) {
        header(c,"STEP 2 / 3   ${scenario+1} / ${scenarios.size}")
        val s=scenarios[scenario]
        round(c,RectF(50f,102f,width-50f,195f),Color.rgb(13,54,72),24f)
        text(c,s.tag,75f,139f,17f,mint,true); text(c,s.title,75f,177f,31f,white,true)
        text(c,s.body,58f,244f,23f,white)
        s.choices.forEachIndexed{i,ch->
            val r=RectF(55f,285f+i*96f,width-55f,365f+i*96f)
            val color=if(selected==i) Color.rgb(24,92,96) else panel
            round(c,r,color,20f); round(c,RectF(r.left+17,r.top+18,r.left+62,r.bottom-18),if(selected==i) mint else Color.rgb(49,76,93),15f)
            text(c,('A'.code+i).toChar().toString(),r.left+31,r.centerY()+9,22f,white,true); text(c,ch.text,r.left+85,r.centerY()+9,23f,white)
            hitAreas += r to { selected=i; showResult=false; invalidate() }
        }
        if(selected>=0) {
            val rr=RectF(55f,height-150f,width-430f,height-60f); round(c,rr,Color.rgb(24,55,65),20f)
            text(c,scenarios[scenario].choices[selected].feedback,rr.left+24,rr.centerY()+8,18f,white)
            button(c,RectF(width-390f,height-145f,width-55f,height-65f),if(scenario==scenarios.lastIndex) "結果を見る" else "次のシナリオ",true){ applyChoice(); if(scenario==scenarios.lastIndex) page=3 else {scenario++; selected=-1}; invalidate() }
        }
    }

    private fun applyChoice() {
        if(selected<0 || showResult) return
        val g=scenarios[scenario].choices[selected].gain
        scores.keys.forEachIndexed{i,k-> scores[k]=scores.getValue(k)+g[i] }
        showResult=true
    }

    private fun drawSummary(c: Canvas) {
        header(c,"STEP 3 / 3")
        text(c,"体験レポート",55f,132f,42f,white,true)
        text(c,"$selectedCourse コース",58f,171f,21f,mint,true)
        val max=9f
        scores.entries.forEachIndexed{i,e->
            val y=235f+i*80f; text(c,e.key,60f,y,21f,white,true); round(c,RectF(230f,y-23,width-165f,y+9),Color.rgb(37,63,78),16f)
            val right=230f+(width-395f)*(e.value/max); round(c,RectF(230f,y-23,right,y+9),if(i%2==0) mint else gold,16f)
            text(c,String.format(Locale.US,"%d",e.value),width-125f,y+3,25f,white,true)
        }
        round(c,RectF(55f,height-185f,width-420f,height-60f),panel,22f)
        text(c,"学びのポイント",78f,height-143f,19f,gold,true)
        text(c,"正解を決めるのではなく、状況・影響・関係者を整理して判断することが重要です。",78f,height-101f,19f,white)
        button(c,RectF(width-380f,height-150f,width-55f,height-70f),"もう一度プレイ",true){ resetGame(); invalidate() }
    }

    private fun resetGame(){ page=0; scenario=0; selected=-1; showResult=false; scores.keys.forEach{scores[it]=0} }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(e.action==MotionEvent.ACTION_UP) { hitAreas.lastOrNull{it.first.contains(e.x,e.y)}?.second?.invoke(); performClick() }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
