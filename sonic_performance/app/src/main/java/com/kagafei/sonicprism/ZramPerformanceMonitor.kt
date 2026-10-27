package com.kagafei.sonicprism

import java.io.File
import kotlin.math.max

data class ZramPerformance(
    val enabled:Boolean=false,
    val usedMb:Float=0f,
    val totalMb:Float=0f,
    val compressionRatio:Float=0f,
    val memoryPressure:Float=0f,
    val swapInPerSec:Float=0f,
    val swapOutPerSec:Float=0f,
    val source:String="Unavailable"
)

/** Read-only collector. It never enables, disables, or reconfigures ZRAM. */
class ZramPerformanceMonitor {
    private var previousPsIn=0L
    private var previousPsOut=0L
    private var previousTime=0L

    fun sample():ZramPerformance {
        val mem=readKeyValues("/proc/meminfo")
        val totalKb=mem["MemTotal"] ?: 0L
        val availableKb=mem["MemAvailable"] ?: (mem["MemFree"] ?: 0L)
        val pressure=if(totalKb>0) ((totalKb-availableKb).toFloat()/totalKb).coerceIn(0f,1f) else 0f

        val disksize=readLong("/sys/block/zram0/disksize")
        val mm=readNumbers("/sys/block/zram0/mm_stat")
        val swapTotalKb=mem["SwapTotal"] ?: 0L
        val swapFreeKb=mem["SwapFree"] ?: 0L

        // Linux zram mm_stat: orig_data_size, compr_data_size, mem_used_total, ...
        val orig=mm.getOrNull(0) ?: 0L
        val compressed=mm.getOrNull(1) ?: 0L
        val used=mm.getOrNull(2) ?: ((swapTotalKb-swapFreeKb).coerceAtLeast(0L)*1024L)
        val total=if(disksize>0) disksize else swapTotalKb*1024L
        val ratio=if(compressed>0) orig.toFloat()/compressed else 0f

        val vm=readKeyValues("/proc/vmstat")
        val psIn=vm["pswpin"] ?: 0L
        val psOut=vm["pswpout"] ?: 0L
        val now=System.nanoTime()
        val seconds=if(previousTime>0)(now-previousTime)/1_000_000_000.0 else 0.0
        val inRate=if(seconds>0)((psIn-previousPsIn).coerceAtLeast(0)/seconds).toFloat() else 0f
        val outRate=if(seconds>0)((psOut-previousPsOut).coerceAtLeast(0)/seconds).toFloat() else 0f
        previousPsIn=psIn; previousPsOut=psOut; previousTime=now

        val direct=disksize>0 || mm.isNotEmpty()
        val enabled=total>0 || swapTotalKb>0
        val source=when { direct -> "zram0 sysfs"; swapTotalKb>0 -> "proc fallback"; else -> "not readable / disabled" }
        return ZramPerformance(enabled,used/1048576f,total/1048576f,ratio,pressure,inRate,outRate,source)
    }

    private fun readLong(path:String)=try { File(path).readText().trim().toLong() } catch(_:Throwable){0L}
    private fun readNumbers(path:String)=try { File(path).readText().trim().split(Regex("\\s+")).mapNotNull{it.toLongOrNull()} } catch(_:Throwable){emptyList()}
    private fun readKeyValues(path:String):Map<String,Long> = try {
        File(path).useLines { lines -> lines.mapNotNull { line ->
            val parts=line.trim().split(Regex("\\s+")); if(parts.size>=2) parts[1].toLongOrNull()?.let{parts[0].removeSuffix(":") to it} else null
        }.toMap() }
    } catch(_:Throwable){emptyMap()}
}
