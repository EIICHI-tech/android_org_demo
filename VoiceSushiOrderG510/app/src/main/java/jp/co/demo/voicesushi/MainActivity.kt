package jp.co.demo.voicesushi

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

data class MenuItem(val id:Int,val name:String,val price:Int,val emoji:String,val keywords:List<String>)
data class CartLine(val item:MenuItem,val count:Int)

private val menu = listOf(
    MenuItem(1,"まぐろ",120,"🍣",listOf("まぐろ","マグロ","鮪")),
    MenuItem(2,"サーモン",150,"🍣",listOf("サーモン")),
    MenuItem(3,"えび",140,"🦐",listOf("えび","エビ","海老")),
    MenuItem(4,"たまご",110,"🥚",listOf("たまご","卵","玉子")),
    MenuItem(5,"いくら",220,"🔴",listOf("いくら","イクラ")),
    MenuItem(6,"鉄火巻",180,"🍙",listOf("鉄火巻","てっかまき")),
    MenuItem(7,"味噌汁",160,"🍲",listOf("味噌汁","みそ汁","みそしる")),
    MenuItem(8,"茶碗蒸し",230,"🥣",listOf("茶碗蒸し","ちゃわんむし"))
)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent { MaterialTheme { OrderScreen { speak(it) } } }
    }
    override fun onInit(status:Int) { if(status == TextToSpeech.SUCCESS) tts.language = Locale.JAPAN }
    private fun speak(text:String) { if(::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "guide") }
    override fun onDestroy(){ if(::tts.isInitialized){ tts.stop(); tts.shutdown() }; super.onDestroy() }
}

@Composable
fun OrderScreen(speak:(String)->Unit) {
    var cart by remember { mutableStateOf(listOf<CartLine>()) }
    var guide by remember { mutableStateOf("いらっしゃいませ。商品をタッチするか、マイクでご注文ください。") }
    var lastHeard by remember { mutableStateOf("音声入力待ち") }
    var autoScroll by remember { mutableStateOf(true) }
    val rowState = rememberLazyListState()

    fun add(item:MenuItem, count:Int=1) {
        val old = cart.find { it.item.id == item.id }
        cart = if(old == null) cart + CartLine(item,count) else cart.map { if(it.item.id==item.id) it.copy(count=it.count+count) else it }
        guide = "${item.name}を${count}皿、承りました。"
        speak(guide)
    }
    fun parse(text:String) {
        lastHeard = text
        when {
            listOf("会計","お会計","チェック").any{text.contains(it)} -> { guide="お会計ですね。注文内容をご確認ください。"; speak(guide) }
            listOf("取り消し","キャンセル","全部消して").any{text.contains(it)} -> { cart=emptyList(); guide="注文内容を取り消しました。"; speak(guide) }
            else -> {
                val found = menu.firstOrNull { m -> m.keywords.any { text.contains(it) } }
                if(found != null) {
                    val count = when {
                        Regex("(3|３|三)(皿|つ|個)").containsMatchIn(text) -> 3
                        Regex("(2|２|二)(皿|つ|個)").containsMatchIn(text) -> 2
                        else -> 1
                    }
                    add(found,count)
                } else { guide="申し訳ありません。商品名をもう一度お話しください。"; speak(guide) }
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            val words = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            words?.firstOrNull()?.let { parse(it) }
        }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) launchSpeech(speechLauncher) else { guide="マイクの使用を許可してください。"; speak(guide) }
    }

    LaunchedEffect(Unit) { delay(500); speak(guide) }
    LaunchedEffect(autoScroll, rowState.firstVisibleItemIndex) {
        if(autoScroll) while(true) { delay(2600); rowState.animateScrollToItem((rowState.firstVisibleItemIndex+1)%menu.size) }
    }

    Row(Modifier.fillMaxSize().background(Color(0xFFF4FBFF))) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth().background(Color(0xFFD9231E)).padding(horizontal=20.dp,vertical=10.dp), verticalAlignment=Alignment.CenterVertically) {
                Text("VOICE SUSHI", color=Color.White, fontSize=28.sp, fontWeight=FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("🎤「まぐろを2皿」", color=Color.White, fontSize=20.sp)
            }
            Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF8AD8F8))) {
                LazyRow(state=rowState, contentPadding=PaddingValues(22.dp), horizontalArrangement=Arrangement.spacedBy(18.dp), modifier=Modifier.fillMaxSize()) {
                    items(menu, key={it.id}) { item ->
                        Card(Modifier.width(245.dp).fillParentMaxHeight(.86f).clickable { add(item); autoScroll=false }, shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White)) {
                            Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.SpaceEvenly) {
                                Text(item.emoji, fontSize=72.sp)
                                Text(item.name, fontSize=27.sp, fontWeight=FontWeight.Bold)
                                Text("${item.price}円", fontSize=24.sp, color=Color(0xFFD9231E), fontWeight=FontWeight.Bold)
                                Button(onClick={ add(item); autoScroll=false }, colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFFA000))) { Text("注文する", fontSize=20.sp) }
                            }
                        }
                    }
                }
                Text("← 画面を横にスライド →", Modifier.align(Alignment.BottomCenter).padding(8.dp), fontWeight=FontWeight.Bold, color=Color(0xFF0B4778))
            }
            Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
                Button(onClick={
                    val activity = speechLauncher as? Any
                    val contextActivity = (speechLauncher)
                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }, colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF087E8B)), modifier=Modifier.height(64.dp)) { Text("🎤 音声で注文", fontSize=23.sp) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("音声ガイド: $guide", fontSize=20.sp, fontWeight=FontWeight.Bold)
                    Text("認識結果: $lastHeard", fontSize=16.sp, color=Color.DarkGray)
                }
                TextButton(onClick={ autoScroll=!autoScroll }) { Text(if(autoScroll) "自動スクロール停止" else "自動スクロール開始") }
            }
        }
        Column(Modifier.width(300.dp).fillMaxHeight().background(Color(0xFF102A43)).padding(16.dp)) {
            Text("ご注文", color=Color.White, fontSize=28.sp, fontWeight=FontWeight.Bold)
            HorizontalDivider(color=Color.Gray, modifier=Modifier.padding(vertical=10.dp))
            if(cart.isEmpty()) Text("商品はまだありません", color=Color.LightGray, fontSize=18.sp)
            cart.forEach { line ->
                Row(Modifier.fillMaxWidth().padding(vertical=7.dp)) {
                    Text("${line.item.name} ×${line.count}", color=Color.White, fontSize=18.sp, modifier=Modifier.weight(1f))
                    Text("${line.item.price*line.count}円", color=Color.White, fontSize=18.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            val total = cart.sumOf { it.item.price*it.count }
            Text("合計 ${total}円", color=Color(0xFFFFD166), fontSize=27.sp, fontWeight=FontWeight.Black)
            Button(onClick={ guide="ご注文を送信しました。商品が届くまでお待ちください。"; speak(guide) }, enabled=cart.isNotEmpty(), modifier=Modifier.fillMaxWidth().padding(top=12.dp).height(58.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFD9231E))) { Text("注文を確定", fontSize=22.sp) }
            OutlinedButton(onClick={ cart=emptyList(); guide="注文内容を取り消しました。"; speak(guide) }, modifier=Modifier.fillMaxWidth().padding(top=8.dp)) { Text("すべて取消", color=Color.White) }
        }
    }
}

private fun launchSpeech(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "商品名と数量を話してください")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }
    launcher.launch(intent)
}
