package com.example.vocalswitchkaraoke
import android.content.Intent
import android.media.*
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vocalswitchkaraoke.audio.*
import com.example.vocalswitchkaraoke.databinding.ActivityMainBinding
import com.example.vocalswitchkaraoke.net.AudioDownloader
import kotlinx.coroutines.*
import kotlin.math.*
class MainActivity:AppCompatActivity(){lateinit var b:ActivityMainBinding;var uri:Uri?=null;var raw:PcmData?=null;var karaoke:PcmData?=null;var track:AudioTrack?=null
 val picker=registerForActivityResult(ActivityResultContracts.OpenDocument()){u->u?:return@registerForActivityResult;contentResolver.takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);uri=u;b.process.isEnabled=true;b.play.isEnabled=false;b.status.text="選択済み"}
 override fun onCreate(s:Bundle?){super.onCreate(s);b=ActivityMainBinding.inflate(layoutInflater);setContentView(b.root);b.select.setOnClickListener{picker.launch(arrayOf("audio/*"))};b.download.setOnClickListener{download()};b.process.setOnClickListener{process()};b.play.setOnClickListener{play()};b.stop.setOnClickListener{stop()};b.vocalSwitch.setOnCheckedChangeListener{_,on->b.vocalSwitch.text=if(on)"歌手 ON" else "歌手 OFF"}}
 fun download()=lifecycleScope.launch{val x=b.url.text.toString().trim();if(x.isEmpty()){b.status.text="URLを入力してください";return@launch};b.download.isEnabled=false;try{val r=withContext(Dispatchers.IO){AudioDownloader.download(this@MainActivity,x){p->runOnUiThread{b.progress.progress=p}}};uri=r.uri;b.status.text="DL完了 ${r.name} ${r.bytes/1024}KB";b.process.isEnabled=true}catch(e:Exception){b.status.text="DL失敗: ${e.message}"}finally{b.download.isEnabled=true}}
 fun process()=lifecycleScope.launch{val u=uri?:return@launch;b.status.text="デコード中";try{val d=withContext(Dispatchers.IO){AudioDecoder.decode(this@MainActivity,u)};raw=d;b.status.text="ボーカルセンター解析中";karaoke=withContext(Dispatchers.Default){separate(d)};b.progress.progress=100;b.play.isEnabled=true;b.status.text="処理完了"}catch(e:Exception){b.status.text="処理失敗: ${e.message}"}}
 fun separate(p:PcmData):PcmData{val o=FloatArray(p.samples.size);var ll=1e-6f;var rr=1e-6f;var lr=0f;val a=.9992f;for(i in 0 until p.samples.size/2){val l=p.samples[2*i];val r=p.samples[2*i+1];ll=a*ll+(1-a)*l*l;rr=a*rr+(1-a)*r*r;lr=a*lr+(1-a)*l*r;val corr=(lr/sqrt(max(1e-9f,ll*rr))).coerceIn(-1f,1f);val conf=((corr-.15f)/.85f).coerceIn(0f,1f);val center=(l+r)*.5f;val side=(l-r)*.5f;val kept=center*(1-.82f*conf);o[2*i]=(kept+side*1.05f).coerceIn(-1f,1f);o[2*i+1]=(kept-side*1.05f).coerceIn(-1f,1f)};return PcmData(o,p.sampleRate,2)}
 fun play(){stop();val r=raw?:return;val k=karaoke;val amount=b.strength.progress/100f;val s=if(b.vocalSwitch.isChecked||k==null)r.samples else FloatArray(r.samples.size){i->(r.samples[i]*(1-amount)+k.samples[i]*amount).coerceIn(-1f,1f)};val pcm=ShortArray(s.size){(s[it]*32767).toInt().coerceIn(-32768,32767).toShort()};val f=AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(r.sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build();track=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(f).setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.size*2).build().also{it.write(pcm,0,pcm.size);it.play()}}
 fun stop(){track?.let{try{it.stop()}catch(_:Exception){};it.release()};track=null}
 override fun onDestroy(){stop();super.onDestroy()}
}
