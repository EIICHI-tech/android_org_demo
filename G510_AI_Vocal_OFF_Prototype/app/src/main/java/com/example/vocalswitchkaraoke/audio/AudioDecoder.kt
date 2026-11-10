package com.example.vocalswitchkaraoke.audio
import android.content.Context
import android.media.*
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder
object AudioDecoder {
 fun decode(context:Context, uri:Uri):PcmData {
  val ex=MediaExtractor(); ex.setDataSource(context,uri,null); var ti=-1
  for(i in 0 until ex.trackCount) if(ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true){ti=i;break}
  require(ti>=0){"音声トラックなし"}; ex.selectTrack(ti); val fmt=ex.getTrackFormat(ti); val mime=fmt.getString(MediaFormat.KEY_MIME)!!
  val sr=fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE); val ch=fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT); require(ch==2){"Stereo音源が必要です"}
  val c=MediaCodec.createDecoderByType(mime); c.configure(fmt,null,null,0); c.start(); val out=ByteArrayOutputStream(); val info=MediaCodec.BufferInfo(); var id=false; var od=false
  while(!od){ if(!id){val i=c.dequeueInputBuffer(10000); if(i>=0){val b=c.getInputBuffer(i)!!; val n=ex.readSampleData(b,0); if(n<0){c.queueInputBuffer(i,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);id=true}else{c.queueInputBuffer(i,0,n,ex.sampleTime,0);ex.advance()}}}; val o=c.dequeueOutputBuffer(info,10000); if(o>=0){c.getOutputBuffer(o)?.let{b->val a=ByteArray(info.size);b.position(info.offset);b.limit(info.offset+info.size);b.get(a);out.write(a)};od=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0;c.releaseOutputBuffer(o,false)}}
  c.stop();c.release();ex.release(); val sb=java.nio.ByteBuffer.wrap(out.toByteArray()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer(); return PcmData(FloatArray(sb.remaining()){sb.get()/32768f},sr,ch)
 }
}
