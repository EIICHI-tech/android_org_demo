package jp.ashida.g510vpnanalyzer

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import kotlin.concurrent.thread

class CaptureVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    @Volatile private var live = false

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (i?.action == "STOP") {
            stopNow()
            return START_NOT_STICKY
        }
        if (!live) start()
        return START_STICKY
    }

    private fun start() {
        val ch = "capture"
        val m = getSystemService(NotificationManager::class.java)
        m.createNotificationChannel(
            NotificationChannel(
                ch,
                "Packet capture",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        val n = Notification.Builder(this, ch)
        startForeground(
            7,
            n.setContentTitle("G510 Packet Analyzer")
                .setContentText("端末送信パケットを取得中")
                .setSmallIcon(R.drawable.stat_sys_download_done)
                .build()
        )
        tun = Builder()
            .setSession("G510 Packet Analyzer")
            .setMtu(1500)
            .addAddress("10.77.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .establish()
        live = tun != null
        CaptureStore.running.value = live
        thread(name = "tun-reader") {
            val b = ByteArray(32767)
            val input = tun?.let { FileInputStream(it.fileDescriptor) }
            try {
                while (live) {
                    val nread = input?.read(b) ?: -1
                    if (nread > 0) {
                        Parser.parse(b, nread)?.let(CaptureStore::add)
                    } else if (nread < 0) {
                        break
                    }
                }
            } catch (_: Exception) {
            } finally {
                stopNow()
            }
        }
    }

    private fun stopNow() {
        live = false
        CaptureStore.running.value = false
        try {
            tun?.close()
        } catch (_: Exception) {
        }
        tun = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopNow()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopNow()
        super.onDestroy()
    }
}

object Parser {
    private var no = 0L

    fun parse(a: ByteArray, n: Int): Packet? {
        if (n < 20) return null
        val v = (a[0].toInt() ushr 4) and 15
        return if (v == 4) v4(a, n) else if (v == 6) v6(a, n) else null
    }

    private fun u(b: Byte) = b.toInt() and 255
    private fun p16(a: ByteArray, o: Int) = u(a[o]) * 256 + u(a[o + 1])
    private fun ip4(a: ByteArray, o: Int) =
        "${u(a[o])}.${u(a[o + 1])}.${u(a[o + 2])}.${u(a[o + 3])}"

    private fun v4(a: ByteArray, n: Int): Packet? {
        val ih = (u(a[0]) and 15) * 4
        if (n < ih) return null
        val pr = u(a[9])
        val src = ip4(a, 12)
        val dst = ip4(a, 16)
        val ports = if ((pr == 6 || pr == 17) && n >= ih + 4) p16(a, ih) to p16(a, ih + 2) else null
        val name = when (pr) {
            6 -> "TCP"
            17 -> "UDP"
            1 -> "ICMP"
            else -> "IP-$pr"
        }
        val info = if (pr == 6 && n >= ih + 14) {
            val fl = u(a[ih + 13])
            listOf(
                "FIN" to 1,
                "SYN" to 2,
                "RST" to 4,
                "PSH" to 8,
                "ACK" to 16,
                "URG" to 32
            ).filter { fl and it.second != 0 }.joinToString(",") { it.first }
        } else if (pr == 17) "Datagram" else "IPv4"

        return Packet(++no, 4, name, src, dst, ports?.first, ports?.second, n, info)
    }

    private fun v6(a: ByteArray, n: Int): Packet? {
        if (n < 40) return null
        val pr = u(a[6])
        fun ip(o: Int) = (0..7).joinToString(":") {
            "%02x%02x".format(u(a[o + it * 2]), u(a[o + it * 2 + 1]))
        }
        val ports = if ((pr == 6 || pr == 17) && n >= 44) p16(a, 40) to p16(a, 42) else null
        return Packet(
            ++no,
            6,
            when (pr) {
                6 -> "TCP"
                17 -> "UDP"
                58 -> "ICMPv6"
                else -> "IPv6-$pr"
            },
            ip(8),
            ip(24),
            ports?.first,
            ports?.second,
            n,
            "IPv6"
        )
    }
}
