package jp.g510.zramcompare
import android.app.Service
import android.content.Intent
import android.os.*
import java.util.*
open class StressBase:Service(){private val data=Collections.synchronizedList(mutableListOf<ByteArray>());private val m=Messenger(object:Handler(Looper.getMainLooper()){override fun handleMessage(x:Message){when(x.what){1->Thread{alloc(x.arg1,x.arg2)}.start();2->{data.clear();System.gc()}}}});override fun onBind(i:Intent)=m.binder
 private fun alloc(n:Int,p:Int){data.clear();System.gc();try{repeat(n){i->val b=ByteArray(1048576);when(p){0->Arrays.fill(b,(i%8).toByte());2->Random(i.toLong()).nextBytes(b);else->for(k in b.indices step 4096)b[k]=(k%251).toByte()};data.add(b);if(i%16==0)Thread.sleep(8)}}catch(_:OutOfMemoryError){}}
 override fun onDestroy(){data.clear();super.onDestroy()}}
class Stress1:StressBase();class Stress2:StressBase();class Stress3:StressBase();class Stress4:StressBase()
