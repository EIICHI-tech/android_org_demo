package com.kagafei.sonicprism

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.media.*
import android.os.Debug
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.*

data class DashboardState(
    val running:Boolean=false, val melHistory:List<FloatArray> = emptyList(), val waveform:FloatArray=floatArrayOf(),
    val levelDb:Float=-80f, val energyPercent:Float=0f, val pitchHz:Float=0f, val zcr:Float=0f,
    val centroidHz:Float=0f, val rolloffHz:Float=0f, val flux:Float=0f, val flatness:Float=0f,
    val crestFactor:Float=0f, val voiceActive:Boolean=false, val cpuPercent:Float=0f, val appRamMb:Float=0f,
    val fps:Float=0f, val zramEnabled:Boolean=false, val zramUsedMb:Float=0f, val zramTotalMb:Float=0f,
    val compressionRatio:Float=0f, val memoryPressure:Float=0f, val swapInPerSec:Float=0f, val swapOutPerSec:Float=0f,
    val zramSource:String="Unavailable", val error:String?=null)

class AudioFeatureViewModel(app:Application):AndroidViewModel(app){
    private val _state=MutableStateFlow(DashboardState()); val state:StateFlow<DashboardState> = _state
    private val alive=AtomicBoolean(false); private var recorder:AudioRecord?=null; private val performance=ZramPerformanceMonitor()
    fun start(){if(alive.getAndSet(true))return;_state.value=_state.value.copy(running=true,error=null);thread(name="sonic-prism-audio"){try{capture()}catch(t:Throwable){_state.value=_state.value.copy(running=false,error=t.message?:"Audio error")}finally{release();alive.set(false)}}}
    fun stop(){alive.set(false);try{recorder?.stop()}catch(_:Throwable){};_state.value=_state.value.copy(running=false)}
    fun clear(){_state.value=_state.value.copy(melHistory=emptyList())}
    override fun onCleared(){stop();super.onCleared()}

    @SuppressLint("MissingPermission")
    private fun createRecorder(sr:Int,bytes:Int):AudioRecord{
        fun build(source:Int)=AudioRecord.Builder().setAudioSource(source).setAudioFormat(AudioFormat.Builder().setSampleRate(sr).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()).setBufferSizeInBytes(bytes).build()
        var r=build(MediaRecorder.AudioSource.UNPROCESSED)
        if(r.state!=AudioRecord.STATE_INITIALIZED){r.release();r=build(MediaRecorder.AudioSource.MIC)}
        return r
    }
    private fun capture(){
        val sr=16000;val n=1024;val hop=256;val min=AudioRecord.getMinBufferSize(sr,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);require(min>0){"16 kHz mono PCM not supported"}
        recorder=createRecorder(sr,max(min,n*4));val ar=recorder!!;require(ar.state==AudioRecord.STATE_INITIALIZED){"AudioRecord initialization failed"}
        val pcm=ShortArray(hop);val frame=DoubleArray(n);val hann=DoubleArray(n){i->.5-.5*cos(2*PI*i/(n-1))};val mel=MelBank(sr,n,64,50.0,8000.0);val history=ArrayDeque<FloatArray>();var previous=DoubleArray(n/2+1);var lastNs=System.nanoTime();var frames=0;var fps=0f;var prevCpu=Process.getElapsedCpuTime();var prevWall=System.currentTimeMillis()
        ar.startRecording()
        while(alive.get()){
            val got=ar.read(pcm,0,hop,AudioRecord.READ_BLOCKING);if(got<=0)continue
            System.arraycopy(frame,hop,frame,0,n-hop);for(i in 0 until got)frame[n-hop+i]=pcm[i]/32768.0
            val power=Fft.power(DoubleArray(n){frame[it]*hann[it]});val melCol=mel.apply(power);history.addLast(melCol);while(history.size>190)history.removeFirst()
            val rms=sqrt(frame.sumOf{it*it}/n);val peak=frame.maxOf{abs(it)};val level=(20*log10(rms.coerceAtLeast(1e-9))).toFloat().coerceIn(-80f,0f);val energy=((level+80f)/80f*100f).coerceIn(0f,100f)
            var crossings=0;for(i in 1 until n)if((frame[i]>=0)!=(frame[i-1]>=0))crossings++;val zcr=crossings.toFloat()/(n-1)
            var sumP=0.0;var weighted=0.0;for(k in power.indices){sumP+=power[k];weighted+=power[k]*(k.toDouble()*sr/n)};val centroid=(weighted/sumP.coerceAtLeast(1e-12)).toFloat()
            val target=sumP*.85;var accum=0.0;var rb=0;for(k in power.indices){accum+=power[k];if(accum>=target){rb=k;break}};val rolloff=rb.toFloat()*sr/n
            var fluxSum=0.0;for(k in power.indices){val d=sqrt(power[k])-sqrt(previous[k]);if(d>0)fluxSum+=d*d};val flux=sqrt(fluxSum).toFloat().coerceAtMost(1f);previous=power
            var logSum=0.0;var arith=0.0;for(v in power){val x=v.coerceAtLeast(1e-15);logSum+=ln(x);arith+=x};val flat=(exp(logSum/power.size)/(arith/power.size).coerceAtLeast(1e-15)).toFloat().coerceIn(0f,1f)
            val pitch=estimatePitch(frame,sr);val crest=(peak/rms.coerceAtLeast(1e-9)).toFloat().coerceAtMost(20f);val voice=level>-48f && pitch in 65f..420f && zcr<.22f
            frames++;val nowNs=System.nanoTime();if(nowNs-lastNs>=1_000_000_000L){fps=frames*1_000_000_000f/(nowNs-lastNs);frames=0;lastNs=nowNs}
            val nowWall=System.currentTimeMillis();val nowCpu=Process.getElapsedCpuTime();val elapsed=(nowWall-prevWall).coerceAtLeast(1);val cpu=((nowCpu-prevCpu)*100f/elapsed/Runtime.getRuntime().availableProcessors()).coerceIn(0f,100f);if(elapsed>500){prevWall=nowWall;prevCpu=nowCpu}
            val ram=Debug.getPss().toFloat()/1024f
            val perf=performance.sample()
            _state.value=DashboardState(running=true,melHistory=history.map{it.copyOf()},waveform=FloatArray(256){i->frame[n-256+i].toFloat()},levelDb=level,energyPercent=energy,pitchHz=pitch,zcr=zcr,centroidHz=centroid,rolloffHz=rolloff,flux=flux,flatness=flat,crestFactor=crest,voiceActive=voice,cpuPercent=cpu,appRamMb=ram,fps=fps,zramEnabled=perf.enabled,zramUsedMb=perf.usedMb,zramTotalMb=perf.totalMb,compressionRatio=perf.compressionRatio,memoryPressure=perf.memoryPressure,swapInPerSec=perf.swapInPerSec,swapOutPerSec=perf.swapOutPerSec,zramSource=perf.source,error=null)
        }
    }
    private fun estimatePitch(x:DoubleArray,sr:Int):Float{val minLag=sr/420;val maxLag=sr/65;var bestLag=0;var best=0.0;var e=0.0;for(v in x)e+=v*v;if(e<1e-5)return 0f;for(lag in minLag..maxLag){var c=0.0;for(i in 0 until x.size-lag)c+=x[i]*x[i+lag];if(c>best){best=c;bestLag=lag}};return if(bestLag>0&&best/e>.18)(sr.toFloat()/bestLag)else 0f}
    private fun release(){try{recorder?.release()}catch(_:Throwable){};recorder=null}
}

class MelBank(private val sr:Int,private val n:Int,bands:Int,minHz:Double,maxHz:Double){
    private val w:Array<DoubleArray>
    init{
        fun h2m(h:Double)=2595*log10(1+h/700)
        fun m2h(m:Double)=700*(10.0.pow(m/2595)-1)
        val lo=h2m(minHz)
        val hi=h2m(maxHz)
        val hz=DoubleArray(bands+2){m2h(lo+(hi-lo)*it/(bands+1))}
        val bins=IntArray(bands+2){floor((n+1)*hz[it]/sr).toInt().coerceIn(0,n/2)}
        w=Array(bands){b->DoubleArray(n/2+1).also{a->val l=bins[b];val c=bins[b+1];val r=bins[b+2];for(k in l until c)a[k]=(k-l).toDouble()/(c-l).coerceAtLeast(1);for(k in c..r)a[k]=(r-k).toDouble()/(r-c).coerceAtLeast(1)}}
    }
    fun apply(p:DoubleArray)=FloatArray(w.size){b->var e=0.0;for(k in w[b].indices)e+=p[k]*w[b][k];(10*log10(e.coerceAtLeast(1e-12))).toFloat().coerceIn(-80f,0f)}
}

object Fft{fun power(samples:DoubleArray):DoubleArray{val n=samples.size;val re=samples.copyOf();val im=DoubleArray(n);var j=0;for(i in 1 until n){var bit=n shr 1;while(j and bit!=0){j=j xor bit;bit=bit shr 1};j=j xor bit;if(i<j){val q=re[i];re[i]=re[j];re[j]=q}};var len=2;while(len<=n){val a=-2*PI/len;val lr=cos(a);val li=sin(a);var base=0;while(base<n){var wr=1.0;var wi=0.0;for(k in 0 until len/2){val ur=re[base+k];val ui=im[base+k];val p=base+k+len/2;val vr=re[p]*wr-im[p]*wi;val vi=re[p]*wi+im[p]*wr;re[base+k]=ur+vr;im[base+k]=ui+vi;re[p]=ur-vr;im[p]=ui-vi;val nr=wr*lr-wi*li;wi=wr*li+wi*lr;wr=nr};base+=len};len=len shl 1};return DoubleArray(n/2+1){k->(re[k]*re[k]+im[k]*im[k])/(n*n)}}}
