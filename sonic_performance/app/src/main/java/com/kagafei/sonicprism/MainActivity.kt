package com.kagafei.sonicprism

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

private val Navy = Color(0xFF050815)
private val Panel = Color(0xFF0C1326)
private val Cyan = Color(0xFF42E8FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(background=Navy, surface=Panel, primary=Cyan)) { Dashboard() } }
    }
}

@Composable
private fun Dashboard(vm: AudioFeatureViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var permissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted) vm.start()
    }
    DisposableEffect(Unit) { onDispose { vm.stop() } }

    Column(Modifier.fillMaxSize().background(Navy).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
            Column {
                Text("SONIC PRISM", color=Cyan, style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
                Text("G510 real-time audio feature dashboard", color=Color(0xFF9CAECC))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically) {
                StatusPill(if(s.voiceActive) "VOICE" else "SILENCE", if(s.voiceActive) Color(0xFF45FF9A) else Color(0xFF718096))
                StatusPill("${s.fps.roundToInt()} FPS", Color(0xFFFFD45C))
                Button(onClick={ if(s.running) vm.stop() else if(permissionGranted) vm.start() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) { Text(if(s.running) "STOP" else "START") }
                OutlinedButton(onClick=vm::clear) { Text("CLEAR") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1.75f).fillMaxHeight(), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                PanelBox("MEL SPECTROGRAM  |  64 BANDS  |  50-8000 Hz", Modifier.weight(1f).fillMaxWidth()) {
                    MelSpectrogram(s.melHistory, Modifier.fillMaxSize())
                }
                PanelBox("LIVE WAVEFORM", Modifier.height(120.dp).fillMaxWidth()) {
                    Waveform(s.waveform, Modifier.fillMaxSize())
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FeatureCard("ENERGY", "${s.energyPercent.roundToInt()} %", s.energyPercent/100f, Color(0xFFFF4D8D), Modifier.weight(1f))
                    FeatureCard("PITCH", if(s.pitchHz>0) "${s.pitchHz.roundToInt()} Hz" else "--", s.pitchHz/420f, Color(0xFF8D7CFF), Modifier.weight(1f))
                }
                Row(Modifier.weight(1f), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FeatureCard("CENTROID", "${s.centroidHz.roundToInt()} Hz", s.centroidHz/8000f, Color(0xFF00D9FF), Modifier.weight(1f))
                    FeatureCard("ROLLOFF 85%", "${s.rolloffHz.roundToInt()} Hz", s.rolloffHz/8000f, Color(0xFFFFB84D), Modifier.weight(1f))
                }
                Row(Modifier.weight(1f), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FeatureCard("ZCR", "%.3f".format(s.zcr), s.zcr*5f, Color(0xFF61FFCA), Modifier.weight(1f))
                    FeatureCard("FLUX", "%.3f".format(s.flux), s.flux*3f, Color(0xFFFF6EDF), Modifier.weight(1f))
                }
                Row(Modifier.weight(1f), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FeatureCard("FLATNESS", "%.3f".format(s.flatness), s.flatness, Color(0xFF4CA6FF), Modifier.weight(1f))
                    FeatureCard("CREST", "%.2f".format(s.crestFactor), s.crestFactor/10f, Color(0xFFFF675C), Modifier.weight(1f))
                }
                PanelBox("SYSTEM / INPUT + ZRAM PERFORMANCE", Modifier.height(168.dp).fillMaxWidth()) {
                    Column(Modifier.fillMaxSize().padding(4.dp), verticalArrangement=Arrangement.SpaceEvenly) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceAround) {
                            MiniMetric("LEVEL", "${s.levelDb.roundToInt()} dBFS", dbColor(s.levelDb))
                            MiniMetric("CPU", "${s.cpuPercent.roundToInt()} %", Color(0xFF46DBFF))
                            MiniMetric("APP RAM", "${s.appRamMb.roundToInt()} MB", Color(0xFFB58CFF))
                            MiniMetric("MEM PRESS", "${(s.memoryPressure*100).roundToInt()} %", pressureColor(s.memoryPressure))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceAround) {
                            MiniMetric("ZRAM", if(s.zramEnabled) "ON" else "N/A", if(s.zramEnabled) Color(0xFF49F5A7) else Color.Gray)
                            MiniMetric("USED/TOTAL", "${s.zramUsedMb.roundToInt()}/${s.zramTotalMb.roundToInt()} MB", Color(0xFFFFD45C))
                            MiniMetric("RATIO", if(s.compressionRatio>0) "%.2fx".format(s.compressionRatio) else "N/A", Color(0xFFFF6EDF))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceAround) {
                            MiniMetric("SWAP IN", "%.1f p/s".format(s.swapInPerSec), Color(0xFF42E8FF))
                            MiniMetric("SWAP OUT", "%.1f p/s".format(s.swapOutPerSec), Color(0xFFFF8C5A))
                            MiniMetric("SOURCE", s.zramSource, Color(0xFF9FB1CF))
                        }
                    }
                }
            }
        }
        if(s.error != null) Text(s.error!!, color=Color(0xFFFF637D), modifier=Modifier.padding(top=6.dp))
    }
}

@Composable private fun StatusPill(text:String,color:Color){ Surface(color=color.copy(alpha=.16f), shape=RoundedCornerShape(50), modifier=Modifier.border(1.dp,color.copy(alpha=.7f),RoundedCornerShape(50))) { Text(text,color=color,modifier=Modifier.padding(horizontal=12.dp,vertical=7.dp),fontWeight=FontWeight.Bold) } }
@Composable private fun PanelBox(title:String, modifier:Modifier, content:@Composable BoxScope.()->Unit){ Column(modifier.background(Panel,RoundedCornerShape(12.dp)).border(1.dp,Color(0xFF1B2D4B),RoundedCornerShape(12.dp)).padding(8.dp)){ Text(title,color=Color(0xFFAFC3E5),style=MaterialTheme.typography.labelMedium); Spacer(Modifier.height(5.dp)); Box(Modifier.weight(1f).fillMaxWidth(),content=content) } }
@Composable private fun FeatureCard(title:String,value:String,level:Float,color:Color,modifier:Modifier){ Column(modifier.fillMaxHeight().background(Panel,RoundedCornerShape(12.dp)).border(1.dp,color.copy(alpha=.45f),RoundedCornerShape(12.dp)).padding(9.dp),verticalArrangement=Arrangement.SpaceBetween){ Text(title,color=Color(0xFF9FB1CF),style=MaterialTheme.typography.labelMedium); Text(value,color=color,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold); LinearProgressIndicator(progress={level.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(6.dp),color=color,trackColor=color.copy(alpha=.15f)) } }
@Composable private fun MiniMetric(label:String,value:String,color:Color){ Column(horizontalAlignment=Alignment.CenterHorizontally){Text(label,color=Color(0xFF8293AF),style=MaterialTheme.typography.labelSmall);Text(value,color=color,fontWeight=FontWeight.Bold)} }

@Composable private fun MelSpectrogram(history:List<FloatArray>,modifier:Modifier){ Canvas(modifier.background(Color.Black)){ if(history.isEmpty())return@Canvas;val cw=size.width/history.size;val rh=size.height/history.first().size;history.forEachIndexed{x,col->col.forEachIndexed{y,db->drawRect(prismColor(db),Offset(x*cw,size.height-(y+1)*rh),Size(cw+1,rh+1))}};for(i in 1..3)drawLine(Color(0x3356CFFF),Offset(0f,size.height*i/4),Offset(size.width,size.height*i/4),1f)} }
@Composable private fun Waveform(wave:FloatArray,modifier:Modifier){ Canvas(modifier.background(Color(0xFF03060C))){ if(wave.size<2)return@Canvas;val p=Path();wave.forEachIndexed{i,v->val x=i.toFloat()/(wave.size-1)*size.width;val y=size.height/2-v.coerceIn(-1f,1f)*size.height*.45f;if(i==0)p.moveTo(x,y)else p.lineTo(x,y)};drawLine(Color(0x3320E8FF),Offset(0f,size.height/2),Offset(size.width,size.height/2));drawPath(p,Cyan,style=Stroke(2f))} }
private fun prismColor(db:Float):Color{val t=((db+80f)/80f).coerceIn(0f,1f);return when{t<.12f->Color(0xFF050018);t<.25f->lerp(Color(0xFF32105F),Color(0xFF2448DD),(t-.12f)/.13f);t<.42f->lerp(Color(0xFF2448DD),Color(0xFF00D9FF),(t-.25f)/.17f);t<.58f->lerp(Color(0xFF00D9FF),Color(0xFF22F28C),(t-.42f)/.16f);t<.75f->lerp(Color(0xFF22F28C),Color(0xFFFFE14D),(t-.58f)/.17f);t<.9f->lerp(Color(0xFFFFE14D),Color(0xFFFF3068),(t-.75f)/.15f);else->lerp(Color(0xFFFF3068),Color.White,(t-.9f)/.1f)}}
private fun lerp(a:Color,b:Color,t:Float)=Color(a.red+(b.red-a.red)*t,a.green+(b.green-a.green)*t,a.blue+(b.blue-a.blue)*t,1f)
private fun pressureColor(v:Float)=when{v>.85f->Color(0xFFFF405D);v>.65f->Color(0xFFFFC847);else->Color(0xFF49F5A7)}
private fun dbColor(db:Float)=when{db>-6->Color(0xFFFF405D);db>-18->Color(0xFFFFC847);else->Color(0xFF49F5A7)}
