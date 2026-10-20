package jp.co.demo.g510fft

import kotlin.math.*

/** Demonstration spectral-subtraction NR. Not a substitute for a production trained VPNR model. */
class SpectralNoiseReducer(private val n: Int = 1024) {
    private val noisePower = DoubleArray(n / 2 + 1) { 1e-5 }
    private var frames = 0

    fun process(input: ShortArray): ShortArray {
        val re = DoubleArray(n); val im = DoubleArray(n)
        var rms = 0.0
        for (i in 0 until n) {
            val x = input.getOrElse(i) { 0 } / 32768.0
            rms += x * x
            re[i] = x
        }
        rms = sqrt(rms / n)
        Fft.transform(re, im)
        val learnNoise = frames < 80 || rms < 0.018
        for (k in 0..n / 2) {
            val p = re[k] * re[k] + im[k] * im[k]
            if (learnNoise) noisePower[k] = 0.96 * noisePower[k] + 0.04 * p
            val clean = max(p - 1.6 * noisePower[k], 0.0)
            val gain = sqrt(clean / max(p, 1e-12)).coerceIn(0.10, 1.0)
            re[k] *= gain; im[k] *= gain
            if (k > 0 && k < n / 2) {
                re[n-k] *= gain; im[n-k] *= gain
            }
        }
        Fft.transform(re, im, true)
        frames++
        return ShortArray(n) { i -> (re[i].coerceIn(-1.0, 1.0) * 32767.0).roundToInt().toShort() }
    }
}
