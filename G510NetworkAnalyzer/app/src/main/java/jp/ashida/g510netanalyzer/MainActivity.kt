package jp.ashida.g510netanalyzer
import android.app.Application
import android.content.Context
import android.net.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.*
import kotlin.system.measureTimeMillis

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{MaterialTheme{App()}}}}
data class Net(val type:String="未接続",val iface:String="-",val ip:String="-",val dns:String="-",val gw:String="-",val valid:Boolean=false,val down:Int=0,val up:Int=0)
data class Result(val type:String,val target:String,val detail:String,val ms:Long?,val ok:Boolean)
data class State(val net:Net=Net(),val list:List<Result> = emptyList(),val running:Boolean=false)
class VM(a:Application):AndroidViewModel(a){
 private val cm=a.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
 val state=MutableStateFlow(State()); private var job:Job?=null
 private val cb=object:ConnectivityManager.NetworkCallback(){override fun onAvailable(n:Network)=refresh();override fun onLost(n:Network)=refresh();override fun onCapabilitiesChanged(n:Network,c:NetworkCapabilities)=refresh();override fun onLinkPropertiesChanged(n:Network,l:LinkProperties)=refresh()}
 init{cm.registerDefaultNetworkCallback(cb);refresh()}
 fun refresh(){viewModelScope.launch(Dispatchers.IO){val n=cm.activeNetwork;val c=n?.let(cm::getNetworkCapabilities);val l=n?.let(cm::getLinkProperties);val t=when{c==null->"未接続";c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)->"Ethernet";c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->"Wi-Fi";c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->"Mobile";c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)->"VPN";else->"その他"};state.value=state.value.copy(net=Net(t,l?.interfaceName?:"-",l?.linkAddresses?.joinToString()?:"-",l?.dnsServers?.joinToString{it.hostAddress?:""}?:"-",l?.routes?.firstOrNull{it.isDefaultRoute}?.gateway?.hostAddress?:"-",c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true,c?.linkDownstreamBandwidthKbps?:0,c?.linkUpstreamBandwidthKbps?:0))}}
 private fun run(type:String,target:String,f:()->Triple<Boolean,String,Long>){viewModelScope.launch(Dispatchers.IO){val r=try{val x=f();Result(type,target,x.second,x.third,x.first)}catch(e:Exception){Result(type,target,e.message?:"error",null,false)};state.value=state.value.copy(list=listOf(r)+state.value.list.take(99))}}
 fun ping(h:String)=run("PING",h){val a=InetAddress.getByName(h);var ok=false;val ms=measureTimeMillis{ok=a.isReachable(2000)};Triple(ok,if(ok)"到達可能" else "応答なし",ms)}
 fun dns(h:String)=run("DNS",h){var s="";val ms=measureTimeMillis{s=InetAddress.getAllByName(h).joinToString{it.hostAddress?:""}};Triple(s.isNotEmpty(),s,ms)}
 fun tcp(h:String,p:Int)=run("TCP","$h:$p"){Socket().use{var ok=false;val ms=measureTimeMillis{it.connect(InetSocketAddress(h,p),2000);ok=true};Triple(ok,"接続成功",ms)}}
 fun http(u:String)=run("HTTP",u){var code=0;val ms=measureTimeMillis{val c=URL(u).openConnection() as HttpURLConnection;c.requestMethod="HEAD";c.connectTimeout=3000;c.readTimeout=3000;code=c.responseCode;c.disconnect()};Triple(code in 200..399,"HTTP $code",ms)}
 fun toggle(h:String){if(job!=null){job?.cancel();job=null;state.value=state.value.copy(running=false)}else{state.value=state.value.copy(running=true);job=viewModelScope.launch{while(isActive){ping(h);delay(3000)}}}}
 override fun onCleared(){cm.unregisterNetworkCallback(cb);job?.cancel()}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(v:VM=viewModel()){
    val s by v.state.collectAsState()
    var host by remember{mutableStateOf("8.8.8.8")}
    var name by remember{mutableStateOf("www.google.com")}
    var port by remember{mutableStateOf("443")}
    var url by remember{mutableStateOf("https://www.google.com/generate_204")}
    
    Scaffold(topBar={TopAppBar(title={Text("G510 Network Analyzer")})}){pad->
        LazyColumn(Modifier.padding(pad).padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            item{
                Card(colors=CardDefaults.cardColors(containerColor=if(s.net.valid)Color(0xFFE8F5E9) else Color(0xFFFFF3E0))){
                    Column(Modifier.padding(14.dp)){
                        Text(if(s.net.valid)"接続 OK" else "未検証／未接続",fontWeight=FontWeight.Bold)
                        Text("${s.net.type} • ${s.net.iface}")
                        Text("IP: ${s.net.ip}")
                        Text("Gateway: ${s.net.gw}")
                        Text("DNS: ${s.net.dns}")
                        Text("推定帯域 ↓${s.net.down/1000} / ↑${s.net.up/1000} Mbps")
                    }
                }
            }
            item{
                Card{
                    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        OutlinedTextField(host,{host=it},label={Text("Ping先")},modifier=Modifier.fillMaxWidth())
                        Button({v.ping(host)},Modifier.fillMaxWidth()){Text("Ping")}
                        OutlinedTextField(name,{name=it},label={Text("ホスト名")},modifier=Modifier.fillMaxWidth())
                        Row{
                            Button({v.dns(name)}){Text("DNS")}
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(port,{port=it.filter(Char::isDigit)},label={Text("Port")},modifier=Modifier.width(120.dp))
                            Spacer(Modifier.width(8.dp))
                            Button({v.tcp(name,port.toIntOrNull()?:443)}){Text("TCP")}
                        }
                        OutlinedTextField(url,{url=it},label={Text("URL")},modifier=Modifier.fillMaxWidth())
                        Button({v.http(url)},Modifier.fillMaxWidth()){Text("HTTP応答")}
                        Button({v.toggle(host)},Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=if(s.running)Color.Red else Color(0xFF00897B))){
                            Text(if(s.running)"連続監視を停止" else "3秒間隔で連続監視")
                        }
                    }
                }
            }
            if(s.list.isNotEmpty()) {
                item{
                    Chart(s.list.mapNotNull{it.ms}.take(30).reversed())
                }
            }
            items(s.list){
                ListItem(
                    headlineContent={Text("${it.type} ${it.target}")},
                    supportingContent={Text(it.detail)},
                    trailingContent={Text(it.ms?.let{x->"$x ms"}?:"-",color=if(it.ok)Color(0xFF2E7D32) else Color.Red)}
                )
            }
        }
    }
}
@Composable fun Chart(v:List<Long>){Card{Column(Modifier.padding(12.dp)){Text("レイテンシ推移",fontWeight=FontWeight.Bold);Canvas(Modifier.fillMaxWidth().height(100.dp)){if(v.size>1){val m=(v.maxOrNull()?:1).coerceAtLeast(1);val p=Path();v.forEachIndexed{i,x->val px=size.width*i/(v.size-1);val py=size.height-x.toFloat()/m*size.height;if(i==0)p.moveTo(px,py)else p.lineTo(px,py)};drawPath(p,Color.Blue,style=androidx.compose.ui.graphics.drawscope.Stroke(4f))}}}}}
