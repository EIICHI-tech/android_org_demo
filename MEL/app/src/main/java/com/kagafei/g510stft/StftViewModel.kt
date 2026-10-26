package com.kagafei.g510stft

import android.app.Application
import android.media.*
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.*

data class StftUiState(
    val running:Boolean=false,
    val history:List<FloatArray> = emptyList(),
    val spectrum:FloatArray = floatArrayOf(),
    val levelDb:Float=-90f,
    val error:String?=null
)

class StftViewModel(app:Application):AndroidViewModel(app) {
    private val _state=MutableStateFlow(StftUiState())
    val state:StateFlow<StftUiState> = _state
    private val alive=AtomicBoolean(false)
    private var recorder:AudioRecord?=null

    fun start() {
        if (alive.getAndSet(true)) return
        _state.value=_state.value.copy(running=true,error=null)
        thread(name="stft-capture") {
            try { captureLoop() } catch(t:Throwable) {
                _state.value=_state.value.copy(running=false,error=t.message ?: "Audio error")
            } finally { releaseAudio(); alive.set(false) }
        }
    }

    fun stop(){ alive.set(false); try { recorder?.stop() } catch(_:Throwable){}; _state.value=_state.value.copy(running=false) }
    override fun onCleared(){ stop(); super.onCleared() }

    private fun captureLoop(){
        val sr=16000; val n=1024; val hop=256
        val min=AudioRecord.getMinBufferSize(sr,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
        require(min>0){"Unsupported audio format"}
        recorder=AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sr).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
            .setBufferSizeInBytes(max(min,n*4)).build()
        require(recorder?.state==AudioRecord.STATE_INITIALIZED){"AudioRecord initialization failed"}
        val r=recorder!!; val input=ShortArray(hop); val frame=DoubleArray(n)
        val window=DoubleArray(n){i->0.5-0.5*cos(2.0*PI*i/(n-1))}
        val history=ArrayDeque<FloatArray>()
        r.startRecording()
        while(alive.get()){
            val got=r.read(input,0,input.size,AudioRecord.READ_BLOCKING)
            if(got<=0) continue
            System.arraycopy(frame,hop,frame,0,n-hop)
            for(i in 0 until got) frame[n-hop+i]=input[i]/32768.0
            val win=DoubleArray(n){frame[it]*window[it]}
            val spec=Fft.magnitudeDb(win)
            val rms=sqrt(frame.sumOf{it*it}/n)
            val db=(20*log10(rms.coerceAtLeast(1e-9))).toFloat().coerceIn(-90f,0f)
            history.addLast(spec); while(history.size>180) history.removeFirst()
            _state.value=StftUiState(true,history.map{it.copyOf()},spec,db,null)
        }
    }

    private fun releaseAudio(){ try{recorder?.release()}catch(_:Throwable){}; recorder=null }
}

object Fft {
    fun magnitudeDb(samples:DoubleArray):FloatArray{
        val n=samples.size; val re=samples.copyOf(); val im=DoubleArray(n)
        var j=0
        for(i in 1 until n){
            var bit=n shr 1
            while(j and bit !=0){j=j xor bit; bit=bit shr 1}
            j=j xor bit
            if(i<j){ val tr=re[i]; re[i]=re[j]; re[j]=tr }
        }
        var len=2
        while(len<=n){
            val angle=-2.0*PI/len; val wLenR=cos(angle); val wLenI=sin(angle)
            var i=0
            while(i<n){
                var wr=1.0; var wi=0.0
                for(k in 0 until len/2){
                    val uR=re[i+k]; val uI=im[i+k]
                    val vR=re[i+k+len/2]*wr-im[i+k+len/2]*wi
                    val vI=re[i+k+len/2]*wi+im[i+k+len/2]*wr
                    re[i+k]=uR+vR; im[i+k]=uI+vI
                    re[i+k+len/2]=uR-vR; im[i+k+len/2]=uI-vI
                    val nextR=wr*wLenR-wi*wLenI; wi=wr*wLenI+wi*wLenR; wr=nextR
                }; i+=len
            }; len=len shl 1
        }
        return FloatArray(n/2){k->(20*log10((hypot(re[k],im[k])/(n/2)).coerceAtLeast(1e-9))).toFloat().coerceIn(-90f,0f)}
    }
}
