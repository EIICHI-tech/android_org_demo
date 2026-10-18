package jp.co.demo.g510amr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.PriorityQueue
import kotlin.math.abs

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Mint, background = Bg, surface = Panel)) { AmrDemo() } }
 }
}

data class Cell(val x:Int,val y:Int)
data class SearchResult(val path:List<Cell>,val explored:Int)
enum class EditMode(val label:String){ START("出発点"), GOAL("目的地"), OBSTACLE("障害物") }
enum class RunState { READY, RUNNING, PAUSED, ARRIVED, NO_ROUTE }

val Bg=Color(0xFF071018); val Panel=Color(0xFF0D202B); val Panel2=Color(0xFF13313D)
val Mint=Color(0xFF24D8A0); val Blue=Color(0xFF4DA8FF); val Orange=Color(0xFFFFB84D)
val Red=Color(0xFFFF6B6B); val Text=Color(0xFFF2F8FA); val Sub=Color(0xFF8FA9B4)
const val COLS=20; const val ROWS=12

fun defaultObstacles()=mutableSetOf<Cell>().apply {
 for(y in 1..8) add(Cell(5,y)); remove(Cell(5,4))
 for(x in 8..15) add(Cell(x,3)); remove(Cell(11,3))
 for(y in 5..10) add(Cell(14,y)); remove(Cell(14,8))
 for(x in 2..9) add(Cell(x,9)); remove(Cell(7,9))
}

@Composable fun AmrDemo(){
 var start by remember{mutableStateOf(Cell(1,1))}; var goal by remember{mutableStateOf(Cell(18,10))}
 var obstacles by remember{mutableStateOf(defaultObstacles().toSet())}; var mode by remember{mutableStateOf(EditMode.OBSTACLE)}
 var result by remember{mutableStateOf(aStar(start,goal,obstacles))}; var state by remember{mutableStateOf(RunState.READY)}
 var robotIndex by remember{mutableIntStateOf(0)}; var battery by remember{mutableIntStateOf(87)}
 fun search(){ result=aStar(start,goal,obstacles); robotIndex=0; state=if(result.path.isEmpty()) RunState.NO_ROUTE else RunState.READY }
 LaunchedEffect(state,result.path){
  while(state==RunState.RUNNING && robotIndex<result.path.lastIndex){ delay(380); robotIndex++; if(robotIndex%3==0) battery=(battery-1).coerceAtLeast(10) }
  if(state==RunState.RUNNING && result.path.isNotEmpty() && robotIndex>=result.path.lastIndex) state=RunState.ARRIVED
 }
 Column(Modifier.fillMaxSize().background(Bg).padding(14.dp)){
  Header(state,battery)
  Spacer(Modifier.height(10.dp))
  Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   Column(Modifier.width(230.dp).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(10.dp)){
    ControlPanel(mode,{mode=it},state, onSearch={search()}, onRun={state=when(state){RunState.RUNNING->RunState.PAUSED;RunState.PAUSED,RunState.READY->RunState.RUNNING;RunState.ARRIVED->{robotIndex=0;RunState.RUNNING};else->state}}, onReset={start=Cell(1,1);goal=Cell(18,10);obstacles=defaultObstacles();battery=87;search()})
    LegendPanel(Modifier.weight(1f))
   }
   MapPanel(start,goal,obstacles,result.path,robotIndex,state,mode,{c->
    when(mode){
     EditMode.START->if(c!=goal&&!obstacles.contains(c)){start=c;search()}
     EditMode.GOAL->if(c!=start&&!obstacles.contains(c)){goal=c;search()}
     EditMode.OBSTACLE->if(c!=start&&c!=goal){obstacles=if(obstacles.contains(c)) obstacles-c else obstacles+c;search()}
    }
   },Modifier.weight(1f).fillMaxHeight())
   StatusPanel(result,battery,state,robotIndex,Modifier.width(245.dp).fillMaxHeight())
  }
 }
}

@Composable fun Header(state:RunState,battery:Int){
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
  Column(Modifier.weight(1f)){Text("G510 EDGE CONTROL",color=Mint,fontSize=11.sp,fontWeight=FontWeight.Bold);Text("自動搬送ロボット ルート検索",color=Text,fontSize=24.sp,fontWeight=FontWeight.Bold)}
  StatusChip("AMR-01",Blue); Spacer(Modifier.width(8.dp)); StatusChip(stateLabel(state),if(state==RunState.NO_ROUTE) Red else Mint); Spacer(Modifier.width(8.dp));StatusChip("BAT $battery%",if(battery<30) Red else Orange)
 }
}
fun stateLabel(s:RunState)=when(s){RunState.READY->"待機";RunState.RUNNING->"搬送中";RunState.PAUSED->"一時停止";RunState.ARRIVED->"到着";RunState.NO_ROUTE->"経路なし"}
@Composable fun StatusChip(t:String,c:Color){Surface(color=c.copy(alpha=.14f),shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.dp,c)){Text(t,color=c,modifier=Modifier.padding(horizontal=13.dp,vertical=7.dp),fontSize=12.sp,fontWeight=FontWeight.Bold)}}

@Composable fun ControlPanel(mode:EditMode,onMode:(EditMode)->Unit,state:RunState,onSearch:()->Unit,onRun:()->Unit,onReset:()->Unit){
 PanelBox{Text("ルート設定",color=Text,fontWeight=FontWeight.Bold,fontSize=16.sp);Text("マップをタップして編集",color=Sub,fontSize=11.sp);Spacer(Modifier.height(8.dp))
  EditMode.entries.forEach{m->Box(Modifier.fillMaxWidth().padding(vertical=3.dp).background(if(mode==m) Mint.copy(alpha=.18f) else Panel2,RoundedCornerShape(10.dp)).border(1.dp,if(mode==m) Mint else Color.Transparent,RoundedCornerShape(10.dp)).clickable{onMode(m)}.padding(10.dp)){Text((if(m==EditMode.START) "S  " else if(m==EditMode.GOAL) "G  " else "■  ")+m.label,color=if(mode==m) Mint else Text,fontSize=13.sp,fontWeight=FontWeight.Bold)}}
  Spacer(Modifier.height(7.dp));Button(onClick=onSearch,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Blue)){Text("経路を再検索")}
  Button(onClick=onRun,enabled=state!=RunState.NO_ROUTE,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Mint,contentColor=Bg)){Text(if(state==RunState.RUNNING) "一時停止" else if(state==RunState.PAUSED) "走行再開" else "搬送開始",fontWeight=FontWeight.Bold)}
  OutlinedButton(onClick=onReset,modifier=Modifier.fillMaxWidth()){Text("初期状態に戻す")}
 }}

@Composable fun LegendPanel(modifier:Modifier){PanelBox(modifier){Text("操作ガイド",color=Text,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Legend("緑","検索ルート",Mint);Legend("青","出発地点",Blue);Legend("橙","目的地点",Orange);Legend("赤","障害物",Red);Spacer(Modifier.height(8.dp));Text("障害物モードで通路を塞ぐと、A*が自動的に迂回路を再計算します。",color=Sub,fontSize=11.sp,lineHeight=16.sp)}}
@Composable fun Legend(a:String,b:String,c:Color){Row(Modifier.padding(vertical=3.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(11.dp).background(c,RoundedCornerShape(3.dp)));Spacer(Modifier.width(8.dp));Text("$a：$b",color=Text,fontSize=11.sp)}}

@Composable fun MapPanel(start:Cell,goal:Cell,obs:Set<Cell>,path:List<Cell>,robotIndex:Int,state:RunState,mode:EditMode,onTap:(Cell)->Unit,modifier:Modifier){
 PanelBox(modifier){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("第1工場 搬送エリア",color=Text,fontWeight=FontWeight.Bold,fontSize=16.sp);Text("編集: ${mode.label}",color=Mint,fontSize=11.sp)};Spacer(Modifier.height(8.dp))
  Canvas(Modifier.fillMaxSize().pointerInput(start,goal,obs,mode){detectTapGestures{p->val cw=size.width/COLS;val ch=size.height/ROWS;onTap(Cell((p.x/cw).toInt().coerceIn(0,COLS-1),(p.y/ch).toInt().coerceIn(0,ROWS-1)))}}){
   val cw=size.width/COLS;val ch=size.height/ROWS
   drawRect(Color(0xFF091820))
   for(x in 0..COLS)drawLine(Color(0xFF1C3440),Offset(x*cw,0f),Offset(x*cw,size.height),1f)
   for(y in 0..ROWS)drawLine(Color(0xFF1C3440),Offset(0f,y*ch),Offset(size.width,y*ch),1f)
   // station zones
   drawRect(Blue.copy(alpha=.08f),Offset(0f,0f),Size(cw*4,ch*3));drawRect(Orange.copy(alpha=.08f),Offset(cw*16,ch*9),Size(cw*4,ch*3))
   obs.forEach{c->drawRoundRect(Red.copy(alpha=.78f),Offset(c.x*cw+2,c.y*ch+2),Size(cw-4,ch-4),cornerRadius=androidx.compose.ui.geometry.CornerRadius(5f))}
   if(path.size>1){val pp=Path();path.forEachIndexed{i,c->val q=Offset((c.x+.5f)*cw,(c.y+.5f)*ch);if(i==0)pp.moveTo(q.x,q.y)else pp.lineTo(q.x,q.y)};drawPath(pp,Mint,style=Stroke(width=(cw.coerceAtMost(ch)*.26f),cap=StrokeCap.Round));path.forEach{c->drawCircle(Color.White,cw.coerceAtMost(ch)*.075f,Offset((c.x+.5f)*cw,(c.y+.5f)*ch))}}
   fun marker(c:Cell,color:Color,label:String){val center=Offset((c.x+.5f)*cw,(c.y+.5f)*ch);drawCircle(color,cw.coerceAtMost(ch)*.35f,center);drawCircle(Color.White,cw.coerceAtMost(ch)*.25f,center,style=Stroke(2f))}
   marker(start,Blue,"S");marker(goal,Orange,"G")
   if(path.isNotEmpty()){val r=path[robotIndex.coerceIn(0,path.lastIndex)];val center=Offset((r.x+.5f)*cw,(r.y+.5f)*ch);drawCircle(Color.Black,cw.coerceAtMost(ch)*.32f,center);drawRoundRect(Mint,Offset(center.x-cw*.28f,center.y-ch*.22f),Size(cw*.56f,ch*.44f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(7f));drawCircle(Color.White,cw*.06f,Offset(center.x-cw*.12f,center.y));drawCircle(Color.White,cw*.06f,Offset(center.x+cw*.12f,center.y))}
  }
 }}

@Composable fun StatusPanel(r:SearchResult,battery:Int,state:RunState,index:Int,modifier:Modifier){PanelBox(modifier){Text("搬送ステータス",color=Text,fontWeight=FontWeight.Bold,fontSize=16.sp);Spacer(Modifier.height(10.dp));Metric("状態",stateLabel(state),if(state==RunState.NO_ROUTE)Red else Mint);Metric("ルート距離","${((r.path.size-1).coerceAtLeast(0)*1.2).format1()} m",Blue);Metric("推定時間","${((r.path.size-1).coerceAtLeast(0)*2)} 秒",Orange);Metric("探索ノード","${r.explored}",Text);Metric("進捗",if(r.path.isEmpty()) "0%" else "${index*100/r.path.lastIndex.coerceAtLeast(1)}%",Mint);Spacer(Modifier.height(8.dp));Text("バッテリー",color=Sub,fontSize=11.sp);LinearProgressIndicator(progress={battery/100f},modifier=Modifier.fillMaxWidth().height(9.dp),color=if(battery<30)Red else Mint,trackColor=Panel2);Spacer(Modifier.height(13.dp));Text("イベントログ",color=Text,fontWeight=FontWeight.Bold,fontSize=13.sp);LogLine("LiDAR", "正常",Mint);LogLine("前方経路",if(state==RunState.NO_ROUTE)"閉塞" else "クリア",if(state==RunState.NO_ROUTE)Red else Mint);LogLine("通信", "Wi-Fi接続",Blue);LogLine("安全速度", "0.8 m/s",Orange)}}
fun Double.format1()="%.1f".format(this)
@Composable fun Metric(k:String,v:String,c:Color){Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(k,color=Sub,fontSize=12.sp);Text(v,color=c,fontSize=14.sp,fontWeight=FontWeight.Bold)};HorizontalDivider(color=Color(0xFF1B3945))}
@Composable fun LogLine(k:String,v:String,c:Color){Row(Modifier.fillMaxWidth().padding(vertical=5.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(k,color=Sub,fontSize=11.sp);Text("● $v",color=c,fontSize=11.sp)}}
@Composable fun PanelBox(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Surface(modifier=modifier,color=Panel,shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF1B3A46))){Column(Modifier.fillMaxSize().padding(14.dp),content=content)}}

fun aStar(start:Cell,goal:Cell,blocked:Set<Cell>):SearchResult{
 data class Node(val c:Cell,val g:Int,val f:Int)
 val open=PriorityQueue<Node>(compareBy<Node>{it.f}.thenBy{it.g});val came=mutableMapOf<Cell,Cell>();val g=mutableMapOf(start to 0);val closed=mutableSetOf<Cell>();var explored=0
 open.add(Node(start,0,abs(start.x-goal.x)+abs(start.y-goal.y)))
 while(open.isNotEmpty()){
  val cur=open.poll().c;if(!closed.add(cur))continue;explored++
  if(cur==goal){val p=mutableListOf(goal);var q=goal;while(q!=start){q=came[q]?:break;p.add(q)};return SearchResult(p.reversed(),explored)}
  listOf(Cell(cur.x+1,cur.y),Cell(cur.x-1,cur.y),Cell(cur.x,cur.y+1),Cell(cur.x,cur.y-1)).filter{it.x in 0 until COLS&&it.y in 0 until ROWS&&!blocked.contains(it)}.forEach{n->val ng=g.getValue(cur)+1;if(ng<(g[n]?:Int.MAX_VALUE)){came[n]=cur;g[n]=ng;open.add(Node(n,ng,ng+abs(n.x-goal.x)+abs(n.y-goal.y)))}}
 }
 return SearchResult(emptyList(),explored)
}
