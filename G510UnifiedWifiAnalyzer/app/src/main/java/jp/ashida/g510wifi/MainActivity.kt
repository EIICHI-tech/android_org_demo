package jp.ashida.g510wifi
import android.Manifest
import android.app.Application
import android.content.*
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity:ComponentActivity(){
 private val perms=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}
 override fun onCreate(b:Bundle?){super.onCreate(b);request();setContent{MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF42D6FF),secondary=Color(0xFF55E39A),background=Color(0xFF07111F),surface=Color(0xFF101D2C))){WifiApp()}}}
 private fun request(){val p=mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION);if(Build.VERSION.SDK_INT>=33)p+=Manifest.permission.NEARBY_WIFI_DEVICES;perms.launch(p.toTypedArray())}
}
data class Ap(val ssid:String,val bssid:String,val rssi:Int,val freq:Int,val ch:Int,val width:String,val security:String,val ageMs:Long)
data class Conn(val ssid:String="-",val bssid:String="-",val rssi:Int=-127,val freq:Int=0,val tx:Int=0,val rx:Int=0,val ip:String="-",val gateway:String="-",val dns:String="-")
data class Device(val ip:String,val latency:Long,val name:String="")
data class Sample(val time:String,val label:String,val rssi:Int)
data class State(val aps:List<Ap> = emptyList(),val conn:Conn=Conn(),val devices:List<Device> = emptyList(),val samples:List<Sample> = emptyList(),val scanning:Boolean=false,val lanScanning:Boolean=false,val note:String="待機中")
class WifiVm(a:Application):AndroidViewModel(a){
 val state=MutableStateFlow(State());private val wm=a.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager;private val cm=a.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
 private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){load(i?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED,false)==true)}}
 init{a.registerReceiver(receiver,IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));connection();load(false)}
 fun scan(){if(ContextCompat.checkSelfPermission(getApplication(),Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){state.value=state.value.copy(note="位置情報権限を許可してください");return};state.value=state.value.copy(scanning=true,note="Wi-Fiスキャン要求中");val ok=try{wm.startScan()}catch(e:Exception){false};if(!ok){load(false);state.value=state.value.copy(note="スキャン制限中。キャッシュ結果を表示")}}
 @Suppress("DEPRECATION") private fun connection(){val n=cm.activeNetwork;val lp=n?.let(cm::getLinkProperties);val wi=wm.connectionInfo;val tx=if(Build.VERSION.SDK_INT>=29)wi.txLinkSpeedMbps else 0;val rx=if(Build.VERSION.SDK_INT>=29)wi.rxLinkSpeedMbps else 0;state.value=state.value.copy(conn=Conn(wi.ssid?.trim('"')?:"-",wi.bssid?:"-",wi.rssi,wi.frequency,tx,rx,wi.ipAddress.let{"${it and 255}.${it shr 8 and 255}.${it shr 16 and 255}.${it shr 24 and 255}"},lp?.routes?.firstOrNull{it.isDefaultRoute}?.gateway?.hostAddress?:"-",lp?.dnsServers?.joinToString{it.hostAddress?:""}?:"-"))}
 @Suppress("DEPRECATION") private fun load(fresh:Boolean){try{val now=SystemClock.elapsedRealtime();val x=wm.scanResults.map{Ap(if(it.SSID.isNullOrBlank())"<hidden>" else it.SSID,it.BSSID,it.level,it.frequency,channel(it.frequency),width(it.channelWidth),security(it.capabilities),((now*1000-it.timestamp)/1000).coerceAtLeast(0))}.sortedByDescending{it.rssi};connection();state.value=state.value.copy(aps=x,scanning=false,note=if(fresh)"${x.size} APを更新" else "${x.size} AP（キャッシュを含む）")}catch(e:SecurityException){state.value=state.value.copy(scanning=false,note="権限または位置情報設定を確認してください")}}
 fun sample(label:String){val c=state.value.conn;if(c.rssi>-127)state.value=state.value.copy(samples=state.value.samples+Sample(SimpleDateFormat("HH:mm:ss",Locale.JAPAN).format(Date()),label.ifBlank{"地点${state.value.samples.size+1}"},c.rssi),note="測定地点を追加")}
 fun scanLan(){if(state.value.lanScanning)return;viewModelScope.launch(Dispatchers.IO){val ip=state.value.conn.ip;val base=ip.substringBeforeLast('.',"");if(base.isBlank()){state.value=state.value.copy(note="Wi-Fi IPv4を取得できません");return@launch};state.value=state.value.copy(lanScanning=true,devices=emptyList(),note="$base.0/24 を探索中");val found=Collections.synchronizedList(mutableListOf<Device>());coroutineScope{(1..254).map{i->async{val host="$base.$i";try{val a=InetAddress.getByName(host);val st=System.currentTimeMillis();if(a.isReachable(250))found+=Device(host,System.currentTimeMillis()-st,try{a.canonicalHostName}catch(_:Exception){""})}catch(_:Exception){}}}.awaitAll()};state.value=state.value.copy(lanScanning=false,devices=found.sortedBy{it.ip.substringAfterLast('.').toInt()},note="${found.size}台を検出")}}
 override fun onCleared(){try{getApplication<Application>().unregisterReceiver(receiver)}catch(_:Exception){}}
 companion object{fun channel(f:Int)=when(f){2484->14;in 2412..2472->(f-2407)/5;in 5000..5895->(f-5000)/5;in 5955..7115->(f-5950)/5;else->0};fun width(w:Int)=when(w){ScanResult.CHANNEL_WIDTH_20MHZ->"20";ScanResult.CHANNEL_WIDTH_40MHZ->"40";ScanResult.CHANNEL_WIDTH_80MHZ->"80";ScanResult.CHANNEL_WIDTH_160MHZ->"160";else->"?"};fun security(c:String)=when{c.contains("WPA3")||c.contains("SAE")->"WPA3";c.contains("WPA2")->"WPA2";c.contains("WPA")->"WPA";c.contains("WEP")->"WEP";else->"Open"}}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun WifiApp(vm:WifiVm=viewModel()){val s by vm.state.collectAsState();var tab by remember{mutableIntStateOf(0)};Scaffold(topBar={TopAppBar(title={Text("G510 Unified Wi-Fi Analyzer",fontWeight=FontWeight.Bold)},actions={Text(s.note,style=MaterialTheme.typography.bodySmall);IconButton(vm::scan){Icon(Icons.Default.Refresh,"scan")}})}){p->Column(Modifier.padding(p).fillMaxSize()){TabRow(tab){listOf("DASHBOARD","CHANNELS","ACCESS POINTS","LAN/FING","SURVEY").forEachIndexed{i,n->Tab(selected=tab==i,onClick={tab=i},text={Text(n)})}};when(tab){0->Dashboard(s);1->Channels(s.aps);2->ApList(s.aps);3->Lan(s,vm);else->Survey(s,vm)}}}}
@Composable fun Dashboard(s:State){Row(Modifier.padding(12.dp).fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(12.dp)){Column(Modifier.weight(.38f),verticalArrangement=Arrangement.spacedBy(10.dp)){Card{Column(Modifier.padding(14.dp)){Text("接続中",fontWeight=FontWeight.Bold);Text(s.conn.ssid,style=MaterialTheme.typography.headlineSmall,color=Color(0xFF42D6FF));Text("${s.conn.rssi} dBm  CH ${WifiVm.channel(s.conn.freq)}  ${s.conn.freq} MHz");Text("Link TX ${s.conn.tx} / RX ${s.conn.rx} Mbps");Text("IP ${s.conn.ip}");Text("GW ${s.conn.gateway}");Text("DNS ${s.conn.dns}")}};Quality(s.conn.rssi);Card{Column(Modifier.padding(12.dp)){Text("診断",fontWeight=FontWeight.Bold);Text(recommend(s.aps,s.conn),color=Color(0xFFFFCB6B))}}};Column(Modifier.weight(.62f)){Text("強いアクセスポイント Top 12",fontWeight=FontWeight.Bold);LazyColumn{items(s.aps.take(12)){ApRow(it)}}}}}
@Composable fun Quality(r:Int){val q=((r+100)/70f).coerceIn(0f,1f);Card{Column(Modifier.padding(12.dp)){Text("Signal Quality",fontWeight=FontWeight.Bold);LinearProgressIndicator(progress={q},modifier=Modifier.fillMaxWidth().height(14.dp),color=if(r>=-60)Color(0xFF55E39A)else if(r>=-75)Color(0xFFFFCB6B)else Color(0xFFFF6B7A));Text("$r dBm  ${if(r>=-50)"Excellent"else if(r>=-60)"Good"else if(r>=-70)"Fair"else"Poor"}")}}}
@Composable fun ApList(aps:List<Ap>){LazyColumn(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){items(aps){ApRow(it)}}}
@Composable fun ApRow(a:Ap){ListItem(headlineContent={Text(a.ssid,fontWeight=FontWeight.Bold)},supportingContent={Text("${a.bssid}  ${a.security}  age ${a.ageMs} ms")},leadingContent={Text("${a.rssi}\ndBm",color=if(a.rssi>=-60)Color(0xFF55E39A)else if(a.rssi>=-75)Color(0xFFFFCB6B)else Color(0xFFFF6B7A))},trailingContent={Text("CH ${a.ch}\n${a.freq} MHz\n${a.width} MHz")})}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Channels(aps:List<Ap>){var band by remember{mutableIntStateOf(0)};Column(Modifier.padding(12.dp)){Row{FilterChip(selected=band==0,onClick={band=0},label={Text("2.4 GHz")});Spacer(Modifier.width(8.dp));FilterChip(selected=band==1,onClick={band=1},label={Text("5 GHz")});Spacer(Modifier.width(8.dp));FilterChip(selected=band==2,onClick={band=2},label={Text("6 GHz")})};val x=aps.filter{if(band==0)it.freq<3000 else if(band==1)it.freq in 4900..5900 else it.freq>5900};Card(Modifier.fillMaxSize().padding(top=8.dp)){ChannelChart(x)}}}
@Composable fun ChannelChart(aps:List<Ap>){Canvas(Modifier.fillMaxSize().padding(35.dp)){if(aps.isEmpty())return@Canvas;val min=aps.minOf{it.ch}.coerceAtMost(if(aps.first().freq<3000)1 else 36);val max=aps.maxOf{it.ch}.coerceAtLeast(if(aps.first().freq<3000)14 else min+32);aps.take(40).forEachIndexed{i,a->val center=(a.ch-min).toFloat()/(max-min).coerceAtLeast(1)*size.width;val half=(if(a.width=="40")4 else if(a.width=="80")8 else 2).toFloat()/(max-min).coerceAtLeast(1)*size.width;val y=size.height-(a.rssi+100).coerceIn(0,70)/70f*size.height;val p=Path().apply{moveTo((center-half).coerceAtLeast(0f),size.height);quadraticBezierTo(center,y,(center+half).coerceAtMost(size.width),size.height)};drawPath(p,listOf(Color.Cyan,Color.Green,Color.Magenta,Color.Yellow,Color.Red)[i%5],style=androidx.compose.ui.graphics.drawscope.Stroke(3f))};for(i in 0..7){val y=size.height*i/7;drawLine(Color(0xFF30445A),Offset(0f,y),Offset(size.width,y),1f)}}}
@Composable fun Lan(s:State,vm:WifiVm){Column(Modifier.padding(12.dp)){Button(vm::scanLan,enabled=!s.lanScanning){Text(if(s.lanScanning)"LAN探索中..."else"同一 /24 LANを探索")};Text("自分が管理するLANで使用してください。Androidの到達性応答だけを表示します。",color=Color(0xFFFFCB6B));LazyColumn{items(s.devices){ListItem(headlineContent={Text(it.ip,fontFamily=FontFamily.Monospace)},supportingContent={Text(if(it.name!=it.ip)it.name else "応答あり")},trailingContent={Text("${it.latency} ms")})}}}}
@Composable fun Survey(s:State,vm:WifiVm){var label by remember{mutableStateOf("")};Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("簡易サイトサーベイ",fontWeight=FontWeight.Bold);Row{OutlinedTextField(label,{label=it},label={Text("地点名 例: 会議室A")});Spacer(Modifier.width(8.dp));Button({vm.sample(label);label=""}){Text("現在RSSIを記録")}};Text("地点ごとの測定値。実際の平面図ヒートマップは、座標入力または図面画像の追加が必要です。");LazyColumn{items(s.samples){ListItem(headlineContent={Text(it.label)},supportingContent={Text(it.time)},trailingContent={Text("${it.rssi} dBm",color=if(it.rssi>=-60)Color.Green else if(it.rssi>=-75)Color.Yellow else Color.Red)})}}}}
fun recommend(a:List<Ap>,c:Conn):String{val band=a.filter{if(c.freq<3000)it.freq<3000 else it.freq in 4900..5900};val counts=band.groupingBy{it.ch}.eachCount();val crowded=counts.maxByOrNull{it.value};val best=if(c.freq<3000)listOf(1,6,11).minByOrNull{counts[it]?:0}else (36..165 step 4).minByOrNull{counts[it]?:0};return "現在CH ${WifiVm.channel(c.freq)}。最多はCH ${crowded?.key?:"-"} (${crowded?.value?:0} AP)。候補CH ${best?:"-"}。RSSI ${c.rssi} dBm${if(c.rssi< -70)"のためAP配置またはアンテナを確認"else"で受信品質は実用範囲"}。"}
