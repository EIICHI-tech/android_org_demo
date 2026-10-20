package jp.co.demo.g510fft

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    private lateinit var spectrum: SpectrumView
    private lateinit var status: TextView
    private lateinit var nrSwitch: Switch
    private val running=AtomicBoolean(false)
    private var worker:Thread?=null
    private val fs=48000; private val fftSize=1024

    override fun onCreate(b:Bundle?){ super.onCreate(b)
        spectrum=SpectrumView(this)
        status=TextView(this).apply{ textSize=18f; setPadding(16,8,16,8) }
        nrSwitch=Switch(this).apply{ text="Noise Reduction"; isChecked=true; setOnCheckedChangeListener{_,v->spectrum.nrEnabled=v} }
        val start=Button(this).apply{ text="START"; setOnClickListener{ensurePermissionAndStart()} }
        val stop=Button(this).apply{ text="STOP"; setOnClickListener{stopAudio()} }
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; addView(start);addView(stop);addView(nrSwitch);addView(status,LinearLayout.LayoutParams(0,-2,1f))}
        setContentView(LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;addView(bar);addView(spectrum,LinearLayout.LayoutParams(-1,0,1f))})
    }
    private fun ensurePermissionAndStart(){
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),10) else startAudio()
    }
    override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==10&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)startAudio()}
    private fun startAudio(){
        if(running.getAndSet(true))return
        worker=Thread{
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val min=AudioRecord.getMinBufferSize(fs,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
            if(min<=0){runOnUiThread{status.text="48 kHz input is not available"};running.set(false);return@Thread}
            if(ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){running.set(false);return@Thread}
            val rec=AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(fs).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
                .setBufferSizeInBytes(max(min,fftSize*8)).build()
            val reducer=SpectralNoiseReducer(fftSize);val buf=ShortArray(fftSize);var smoothRaw=FloatArray(fftSize/2){-120f};var smoothNr=smoothRaw.copyOf()
            rec.startRecording();runOnUiThread{status.text="48 kHz / PCM16 / mono"}
            while(running.get()){
                val n=rec.read(buf,0,buf.size,AudioRecord.READ_BLOCKING);if(n<=0)continue
                val input=if(n==fftSize)buf.copyOf() else ShortArray(fftSize){if(it<n)buf[it] else 0}
                val out=reducer.process(input);val a=Fft.spectrumDb(input,fftSize);val z=Fft.spectrumDb(out,fftSize)
                for(i in a.indices){smoothRaw[i]=0.72f*smoothRaw[i]+0.28f*a[i];smoothNr[i]=0.72f*smoothNr[i]+0.28f*z[i]}
                spectrum.raw=smoothRaw.copyOf();spectrum.nr=smoothNr.copyOf();spectrum.postInvalidateOnAnimation()
                val rawRms=rmsDb(input);val nrRms=rmsDb(out);runOnUiThread{status.text="RAW %.1f dBFS   NR %.1f dBFS   Delta %.1f dB".format(rawRms,nrRms,rawRms-nrRms)}
            };runCatching{rec.stop()};rec.release()
        }.also{it.start()}
    }
    private fun rmsDb(x:ShortArray):Double{var s=0.0;for(v in x){val q=v/32768.0;s+=q*q};return 20*log10(max(sqrt(s/x.size),1e-7))}
    private fun stopAudio(){running.set(false);worker?.interrupt();worker=null;status.text="Stopped"}
    override fun onStop(){super.onStop();stopAudio()}
}
