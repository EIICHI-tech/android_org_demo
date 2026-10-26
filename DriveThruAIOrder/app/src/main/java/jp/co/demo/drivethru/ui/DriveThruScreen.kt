package jp.co.demo.drivethru.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.co.demo.drivethru.*

private val Red=Color(0xFFB51227);private val Yellow=Color(0xFFF7C600)
@Composable fun DriveThruScreen(vm:MainViewModel){val ui by vm.ui.collectAsState();MaterialTheme{Surface(color = Color(0xFFF4F0E7)){Column(Modifier.fillMaxSize()){
 Row(Modifier.fillMaxWidth().background(Red).padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text("GENIO DRIVE ORDER",color = Color.White,fontSize = 24.sp,fontWeight = FontWeight.Black);Text("「HI GENIO」から音声だけで注文",color = Color.White)};AssistChip(onClick={},label={Text(if(ui.listening)"● 音声待機中" else "停止中")})}
 if(ui.state==OrderState.COMPLETED)Done(ui.orderNumber){vm.reset()} else Row(Modifier.weight(1f).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Column(Modifier.weight(1.7f)){Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFF3C4))){Column(Modifier.fillMaxWidth().padding(12.dp)){Text(ui.message,fontWeight=FontWeight.Bold,fontSize=18.sp);Text("認識: ${ui.heard}",fontSize=13.sp)}};Spacer(Modifier.height(10.dp));LazyVerticalGrid(GridCells.Fixed(3),verticalArrangement=Arrangement.spacedBy(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(vm.products){p->Card(onClick={vm.add(p)},shape=RoundedCornerShape(14.dp)){Column{Image(painterResource(p.imageRes),p.name,Modifier.fillMaxWidth().aspectRatio(1.35f),contentScale=ContentScale.Crop);Column(Modifier.padding(8.dp)){Text(p.name,fontWeight=FontWeight.Bold);Text("¥${p.price}",color=Red,fontWeight=FontWeight.Bold)}}}}}}
  Card(Modifier.weight(.8f).fillMaxHeight()){Column(Modifier.padding(14.dp)){Text("ご注文内容",fontSize=20.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){if(ui.cart.isEmpty())Text("まだ商品はありません",color=Color.Gray);ui.cart.forEach{l->Row(Modifier.fillMaxWidth().padding(vertical=7.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("${l.product.name} ×${l.quantity}",Modifier.weight(1f));TextButton(onClick={vm.remove(l.product)}){Text("削除")}}}};HorizontalDivider();val total=ui.cart.sumOf{it.product.price*it.quantity};Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("合計",fontWeight = FontWeight.Bold);Text("¥$total",fontSize=22.sp,fontWeight=FontWeight.Black,color=Red)};Button(onClick={vm.confirm()},enabled=ui.cart.isNotEmpty(),colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Color.Black),modifier=Modifier.fillMaxWidth()){Text("注文内容を確認")}}}
 }} }
}}
@Composable private fun Done(no:Long?,reset:()->Unit){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("✓",fontSize=70.sp,color=Color(0xFF16883F));Text("ご注文ありがとうございました",fontSize=30.sp,fontWeight=FontWeight.Black);Text("注文番号 ${no?:"--"}",fontSize=26.sp,color=Red,fontWeight=FontWeight.Bold);Text("受取口へお進みください");Spacer(Modifier.height(20.dp));Button(onClick=reset){Text("次のお客様")}}}
