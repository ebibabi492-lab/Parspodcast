package com.parspodcast.studio.data.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object WaveformExtractor {

    private const val DEFAULT_PEAK_COUNT = 96
    private val memoryCache = ConcurrentHashMap<String, FloatArray>()

    suspend fun getWaveformPeaks(
        audioFile: File,
        peakCount: Int = DEFAULT_PEAK_COUNT
    ): FloatArray = withContext(Dispatchers.IO) {
        val cacheKey = "${audioFile.absolutePath}_${audioFile.lastModified()}_$peakCount"
        memoryCache[cacheKey]?.let { return@withContext it }

        val decoded = WavFileReader.readWavFile(audioFile) ?: return@withContext FloatArray(peakCount) { 0.2f }
        val samples = decoded.samples
        if (samples.isEmpty()) return@withContext FloatArray(peakCount)

        val peaks = FloatArray(peakCount)
        val step = (samples.size / peakCount).coerceAtLeast(1)

        for (i in 0 until peakCount) {
            val start = i * step
            val end = ((i + 1) * step).coerceAtMost(samples.size)
            var maxPeak = 0.05f // minimum visual height

            for (s in start until end) {
                val absVal = abs(samples[s])
                if (absVal > maxPeak) maxPeak = absVal
            }
            peaks[i] = maxPeak.coerceIn(0.05f, 1.0f)
        }

        memoryCache[cacheKey] = peaks
        return@withContext peaks
    }

    fun clearCache() {
        memoryCache.clear()
    }
}
