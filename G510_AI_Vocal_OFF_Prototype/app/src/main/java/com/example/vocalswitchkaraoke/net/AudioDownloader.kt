package com.example.vocalswitchkaraoke.net
import android.content.Context
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
object AudioDownloader {
 data class Result(val uri:Uri,val name:String,val bytes:Long)
 fun download(ctx:Context,text:String,progress:(Int)->Unit):Result {
  require(text.startsWith("https://")){"HTTPS直接URLを指定してください"}; var u=URL(text); var redirects=0; lateinit var c:HttpURLConnection
  while(true){c=(u.openConnection() as HttpURLConnection).apply{instanceFollowRedirects=false;connectTimeout=15000;readTimeout=30000;setRequestProperty("User-Agent","VocalSwitchG510/1.2")};c.connect();if(c.responseCode in 300..399){require(redirects++<5){"redirect過多"};val n=URL(u,c.getHeaderField("Location")?:error("Locationなし"));require(n.protocol=="https"){"HTTPS以外へのredirect拒否"};c.disconnect();u=n}else break}
  require(c.responseCode in 200..299){"HTTP ${c.responseCode}"};val type=c.contentType?.substringBefore(';')?.lowercase().orEmpty();require(type.startsWith("audio/")||type=="application/octet-stream"){"音声Content-Typeではありません: $type"}
  val ext=when(type){"audio/mpeg"->"mp3";"audio/mp4","audio/x-m4a","audio/m4a"->"m4a";"audio/wav","audio/x-wav"->"wav";"audio/aac"->"aac";else->u.path.substringAfterLast('.',"bin").lowercase()};require(ext in setOf("mp3","m4a","wav","aac","bin")){"未対応: $ext"}
  val f=File(ctx.filesDir,"downloads/audio_${System.currentTimeMillis()}.$ext");f.parentFile!!.mkdirs();val total=c.contentLengthLong;var done=0L;c.inputStream.use{i->f.outputStream().use{o->val b=ByteArray(65536);while(true){val n=i.read(b);if(n<0)break;o.write(b,0,n);done+=n;if(total>0)progress((done*100/total).toInt().coerceAtMost(99))}}};c.disconnect();progress(100);return Result(Uri.fromFile(f),f.name,done)
 }
}
