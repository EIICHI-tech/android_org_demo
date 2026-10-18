package jp.co.kagafei.earmg510
import android.content.*
import android.hardware.usb.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
class MainActivity:ComponentActivity(){private val v:EArmViewModel by viewModels();private val r=object:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){when(i.action){EArmUsbClient.PERMISSION->{val d=if(Build.VERSION.SDK_INT>=33)i.getParcelableExtra(UsbManager.EXTRA_DEVICE,UsbDevice::class.java)else @Suppress("DEPRECATION") i.getParcelableExtra(UsbManager.EXTRA_DEVICE);if(i.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false)&&d!=null)v.connect(d)};UsbManager.ACTION_USB_DEVICE_DETACHED->v.disconnect()}}};override fun onCreate(b:Bundle?){super.onCreate(b);val f=IntentFilter().apply{addAction(EArmUsbClient.PERMISSION);addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)};if(Build.VERSION.SDK_INT>=33)registerReceiver(r,f,RECEIVER_NOT_EXPORTED)else @Suppress("DEPRECATION") registerReceiver(r,f);setContent{App(v)};v.discover()};override fun onNewIntent(i:Intent){super.onNewIntent(i);v.discover()};override fun onStop(){v.stop("画面離脱のため安全停止");super.onStop()};override fun onDestroy(){unregisterReceiver(r);super.onDestroy()}}
val Navy=Color(0xFF071626);val PanelC=Color(0xFF102B40);val Cyan=Color(0xFF00D4FF);val Red=Color(0xFFFF3B4D);val Green=Color(0xFF15D69A)
@Composable fun App(v:EArmViewModel){val on by v.on.collectAsStateWithLifecycle();val msg by v.msg.collectAsStateWithLifecycle();val a by v.act.collectAsStateWithLifecycle();var bz by remember{mutableStateOf(false)};MaterialTheme(colorScheme=darkColorScheme()){Surface(Modifier.fillMaxSize(),color=Navy){Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("eArm G510 USB-C CONTROLLER",fontSize=25.sp,fontWeight=FontWeight.Black);Text(msg,color=if(on)Green else Color.Yellow)};Text(if(on)"● USB ONLINE" else "● USB OFFLINE",color=if(on)Green else Red,fontWeight=FontWeight.Bold);Spacer(Modifier.width(12.dp));OutlinedButton(onClick=v::discover){Text("再接続")}};Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(12.dp)){P("台座","左","A_MINUS","右","A_PLUS",a,v,Modifier.weight(1f));P("上腕","上げる","B_MINUS","下げる","B_PLUS",a,v,Modifier.weight(1f));P("前腕","上げる","C_PLUS","下げる","C_MINUS",a,v,Modifier.weight(1f));P("グリッパー","開く","D_MINUS","閉じる","D_PLUS",a,v,Modifier.weight(1f))};Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Button(onClick={bz=!bz;v.buzzer(bz)},Modifier.weight(1f).height(58.dp)){Text(if(bz)"ブザーOFF" else "ブザーON")};Button(onClick={v.stop()},Modifier.weight(2f).height(58.dp),colors=ButtonDefaults.buttonColors(containerColor=Red)){Text("緊急停止",fontSize=22.sp,fontWeight=FontWeight.Black)}}}}}}
@Composable fun P(t:String,l:String,lc:String,r:String,rc:String,a:String?,v:EArmViewModel,m:Modifier){Column(m.background(PanelC,RoundedCornerShape(20.dp)).padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(t,fontSize=21.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp));H(l,lc,a==lc,v,Modifier.weight(1f).fillMaxWidth());Spacer(Modifier.height(10.dp));H(r,rc,a==rc,v,Modifier.weight(1f).fillMaxWidth())}}
@Composable fun H(s:String,c:String,p:Boolean,v:EArmViewModel,m:Modifier){Box(m.background(if(p)Cyan else Color(0xFF1D526D),RoundedCornerShape(16.dp)).pointerInput(c){awaitEachGesture{awaitFirstDown(false);v.press(c);try{waitForUpOrCancellation()}finally{v.release(c)}}},contentAlignment=Alignment.Center){Text(s,color=if(p)Navy else Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)}}