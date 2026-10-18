package jp.co.kagafei.g510pickplace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class EArmClient(private val base: String = "http://192.168.4.1/") {
    suspend fun send(command: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            (URL("$base?$command").openConnection() as HttpURLConnection).run {
                connectTimeout=800; readTimeout=800; requestMethod="GET"; useCaches=false
                responseCode in 200..299
            }
        }.getOrDefault(false)
    }
    suspend fun pulse(name:String, millis:Long):Boolean { if(!send("$name=1")) return false; delay(millis); send("$name=0"); return true }
    suspend fun stopAll(){ listOf("a_minus","a_plus","b_minus","b_plus","c_minus","c_plus","d_minus","d_plus").forEach { send("$it=0") } }
    suspend fun ping()=send("status_check=1")
}
