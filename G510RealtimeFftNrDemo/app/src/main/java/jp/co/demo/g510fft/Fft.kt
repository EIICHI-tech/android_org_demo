package jp.co.demo.g510fft

import kotlin.math.*

object Fft {
    fun transform(re: DoubleArray, im: DoubleArray, inverse: Boolean = false) {
        val n = re.size
        require(n > 0 && n and (n - 1) == 0)
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var len = 2
        while (len <= n) {
            val angle = 2.0 * Math.PI / len * if (inverse) 1.0 else -1.0
            val wLenRe = cos(angle); val wLenIm = sin(angle)
            var i = 0
            while (i < n) {
                var wr = 1.0; var wi = 0.0
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]; val uIm = im[i + k]
                    val p = i + k + len / 2
                    val vRe = re[p] * wr - im[p] * wi
                    val vIm = re[p] * wi + im[p] * wr
                    re[i + k] = uRe + vRe; im[i + k] = uIm + vIm
                    re[p] = uRe - vRe; im[p] = uIm - vIm
                    val nextWr = wr * wLenRe - wi * wLenIm
                    wi = wr * wLenIm + wi * wLenRe; wr = nextWr
                }
                i += len
            }
            len = len shl 1
        }
        if (inverse) for (i in 0 until n) { re[i] /= n; im[i] /= n }
    }

    fun spectrumDb(samples: ShortArray, size: Int): FloatArray {
        val re = DoubleArray(size); val im = DoubleArray(size)
        for (i in 0 until size) {
            val x = if (i < samples.size) samples[i] / 32768.0 else 0.0
            val w = 0.5 - 0.5 * cos(2.0 * Math.PI * i / (size - 1))
            re[i] = x * w
        }
        transform(re, im)
        return FloatArray(size / 2) { i ->
            val mag = sqrt(re[i] * re[i] + im[i] * im[i]) / (size / 2.0)
            (20.0 * log10(max(mag, 1e-7))).toFloat().coerceIn(-120f, 0f)
        }
    }
}
