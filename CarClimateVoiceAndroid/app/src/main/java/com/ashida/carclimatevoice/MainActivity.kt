package com.ashida.carclimatevoice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val vm: ClimateViewModel by viewModels()
    private var pendingStart: (() -> Unit)? = null
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) pendingStart?.invoke() else vm.processCommand("")
        pendingStart = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CarClimateApp(vm) { action -> requestMic(action) } }
    }

    private fun requestMic(action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) action()
        else { pendingStart = action; permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    }
}

@Composable
fun CarClimateApp(vm: ClimateViewModel, requestMic: (() -> Unit) -> Unit) {
    var partial by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var controller by remember { mutableStateOf<VoiceController?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    DisposableEffect(Unit) {
        controller = VoiceController(
            context,
            onPartial = { partial = it },
            onFinal = { text -> partial = text; controller?.speak(vm.processCommand(text)) },
            onListening = { listening = it },
            onError = { partial = it; listening = false }
        )
        onDispose { controller?.release() }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, secondary = Orange)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF020B11), Color(0xFF0B2530)))).systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                Header()
                Row(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    NavigationPanel(Modifier.weight(1.25f))
                    ClimatePanel(vm, Modifier.weight(1f))
                }
                VoicePanel(vm, partial, listening,
                    onMic = { requestMic { if (listening) controller?.stop() else { partial = ""; controller?.start() } } },
                    onSpeak = { controller?.speak(vm.state.response) })
                BottomBar()
            }
        }
    }
}

private val Cyan = Color(0xFF19D9F1)
private val Orange = Color(0xFFFFA52F)
private val Panel = Color(0x99101820)

@Composable private fun Header() {
    Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha=.35f)).padding(horizontal=20.dp, vertical=9.dp), verticalAlignment=Alignment.CenterVertically) {
        Icon(Icons.Default.DirectionsCar, null, tint=Cyan); Spacer(Modifier.width(8.dp))
        Text("VOICE COCKPIT", fontWeight=FontWeight.Bold, letterSpacing=1.5.sp)
        Spacer(Modifier.weight(1f)); Icon(Icons.Default.Navigation, null, tint=Cyan); Spacer(Modifier.width(6.dp)); Text("ナビ案内中")
        Spacer(Modifier.weight(1f)); Text("ANDROID AUTO DEMO", color=Cyan, fontWeight=FontWeight.Bold)
    }
}

@Composable private fun NavigationPanel(modifier: Modifier) {
    Card(modifier, shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Panel)) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row { Icon(Icons.Default.Map, null, tint=Cyan); Spacer(Modifier.width(8.dp)); Text("NAVIGATION", color=Cyan, fontWeight=FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("2.4 km", fontWeight=FontWeight.Bold) }
            Canvas(Modifier.weight(1f).fillMaxWidth().padding(vertical=8.dp)) {
                val path=Path().apply { moveTo(size.width*.08f,size.height*.8f); cubicTo(size.width*.35f,size.height*.35f,size.width*.62f,size.height*.9f,size.width*.92f,size.height*.18f) }
                drawPath(path,Cyan,style=Stroke(width=16f,cap=StrokeCap.Round))
                drawCircle(Color.White,22f,Offset(size.width*.55f,size.height*.53f)); drawCircle(Cyan,12f,Offset(size.width*.55f,size.height*.53f))
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column { Text("次の交差点",fontSize=11.sp,color=Color.Gray); Text("寝屋川市駅方面へ直進",fontWeight=FontWeight.Bold) }
                Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowUpward,null,tint=Cyan,modifier=Modifier.size(42.dp))
            }
        }
    }
}

@Composable private fun ClimatePanel(vm: ClimateViewModel, modifier: Modifier) {
    val s=vm.state
    Card(modifier,shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
        Column(Modifier.fillMaxSize().padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.Air,tint=Cyan,contentDescription=null); Spacer(Modifier.width(7.dp)); Text("CLIMATE",color=Cyan,fontWeight=FontWeight.Bold); Spacer(Modifier.weight(1f)); IconButton(vm::togglePower) { Icon(Icons.Default.PowerSettingsNew,null,tint=if(s.powerOn) Cyan else Color.Gray) } }
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                RoundAction(Icons.Default.Remove){vm.temperature(-.5f)}
                Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("%.1f".format(s.temperature),fontSize=51.sp,fontWeight=FontWeight.Bold); Text("°C",color=Cyan) }
                RoundAction(Icons.Default.Add){vm.temperature(.5f)}
            }
            Text("風量",fontSize=11.sp,color=Color.Gray)
            Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(5.dp)) { (1..7).forEach { n -> Box(Modifier.width(13.dp).height((8+n*4).dp).background(if(n<=s.fanLevel) Cyan else Color.DarkGray,RoundedCornerShape(5.dp)).clickable{vm.setFan(n)}) } }
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) { ToggleButton("AUTO",s.autoOn,vm::toggleAuto,Modifier.weight(1f)); ToggleButton("A/C",s.acOn,vm::toggleAc,Modifier.weight(1f)); ToggleButton("内気",s.recirculationOn,vm::toggleRecirculation,Modifier.weight(1f)) }
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) { IconToggle(Icons.Default.Waves,s.frontDefrostOn,vm::toggleFrontDefrost,Modifier.weight(1f)); IconToggle(Icons.Default.ViewWeek,s.rearDefrostOn,vm::toggleRearDefrost,Modifier.weight(1f)) }
        }
    }
}

@Composable private fun VoicePanel(vm: ClimateViewModel, partial:String, listening:Boolean,onMic:()->Unit,onSpeak:()->Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=4.dp),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.Black.copy(alpha=.42f))) {
        Row(Modifier.padding(horizontal=20.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
            FilledIconButton(onMic,Modifier.size(68.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=if(listening) Color.Red else Cyan)) { Icon(if(listening) Icons.Default.GraphicEq else Icons.Default.Mic,null,Modifier.size(31.dp)) }
            Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) {
                Text(if(listening) "お話しください…" else "音声エアコン操作",color=if(listening) Color.Red else Cyan,fontWeight=FontWeight.Bold)
                Text(if(partial.isBlank()) vm.state.response else partial,maxLines=2,overflow=TextOverflow.Ellipsis,fontSize=17.sp)
                Text("例: 24度にして / 暑い / 風量を最大にして / 内気循環",fontSize=11.sp,color=Color.Gray,maxLines=1)
            }
            IconButton(onSpeak) { Icon(Icons.Default.VolumeUp,null,tint=Cyan) }
        }
    }
}

@Composable private fun BottomBar() { Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha=.5f)).padding(horizontal=30.dp,vertical=7.dp),horizontalArrangement=Arrangement.SpaceAround) { NavItem(Icons.Default.Map,"ナビ",false); NavItem(Icons.Default.Air,"空調",true); NavItem(Icons.Default.MusicNote,"メディア",false); NavItem(Icons.Default.Phone,"電話",false); NavItem(Icons.Default.Settings,"設定",false) } }
@Composable private fun NavItem(icon:ImageVector,title:String,selected:Boolean){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,tint=if(selected) Cyan else Color.Gray);Text(title,fontSize=10.sp,color=if(selected) Cyan else Color.Gray)}}
@Composable private fun RoundAction(icon:ImageVector,onClick:()->Unit){FilledIconButton(onClick,colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color.White.copy(alpha=.1f))){Icon(icon,null)}}
@Composable private fun ToggleButton(label:String,on:Boolean,click:()->Unit,modifier:Modifier){Button(click,modifier,contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.buttonColors(containerColor=if(on) Cyan else Color.DarkGray,contentColor=if(on) Color.Black else Color.White)){Text(label,fontSize=11.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun IconToggle(icon:ImageVector,on:Boolean,click:()->Unit,modifier:Modifier){Button(click,modifier,colors=ButtonDefaults.buttonColors(containerColor=if(on) Orange else Color.DarkGray,contentColor=if(on) Color.Black else Color.White)){Icon(icon,null)}}
