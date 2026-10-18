package jp.co.kagafei.earmg510

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EArmUsbClient(private val context: Context) {
    companion object {
        const val PERMISSION = "jp.co.kagafei.earmg510.USB_PERMISSION"
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    @Volatile
    private var port: UsbSerialPort? = null

    fun find(): UsbDevice? {
        return UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).firstOrNull()?.device
    }

    fun permitted(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    fun request(device: UsbDevice) {
        val intent = Intent(PERMISSION).setPackage(context.packageName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
        usbManager.requestPermission(device, pendingIntent)
    }

    suspend fun open(device: UsbDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            close()
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
                ?: throw IllegalStateException("CH340未検出")
            val connection = usbManager.openDevice(device)
                ?: throw IllegalStateException("USB権限なし")
            
            val q = driver.ports[0]
            q.open(connection)
            q.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            q.dtr = false
            q.rts = false
            port = q
            q.write("PING\n".toByteArray(), 500)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun send(command: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val activePort = port ?: throw IllegalStateException("USB未接続")
            activePort.write("$command\n".toByteArray(), 300)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        try {
            port?.close()
        } catch (e: Exception) {
            // Ignore
        }
        port = null
    }
}
