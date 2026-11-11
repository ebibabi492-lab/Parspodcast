package com.parspodcast.studio.data.audio

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DecodedWavAudio(
    val samples: FloatArray, // Interleaved or mono normalized [-1.0f, 1.0f]
    val sampleRate: Int,
    val channels: Int,
    val durationMs: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DecodedWavAudio
        return samples.contentEquals(other.samples) && sampleRate == other.sampleRate && channels == other.channels
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channels
        return result
    }
}

object WavFileReader {

    fun readWavFile(file: File): DecodedWavAudio? {
        if (!file.exists() || file.length() < 44) return null

        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                if (fis.read(header) < 44) return null

                val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

                val riff = ByteArray(4).also { buffer.get(it) }.toString(Charsets.US_ASCII)
                if (riff != "RIFF") return null

                buffer.int // chunkSize
                val wave = ByteArray(4).also { buffer.get(it) }.toString(Charsets.US_ASCII)
                if (wave != "WAVE") return null

                val fmt = ByteArray(4).also { buffer.get(it) }.toString(Charsets.US_ASCII)
                if (fmt != "fmt ") return null

                buffer.int // subchunk1Size
                val audioFormat = buffer.short.toInt() // 1 = PCM
                val channels = buffer.short.toInt()
                val sampleRate = buffer.int
                buffer.int // byteRate
                buffer.short // blockAlign
                val bitsPerSample = buffer.short.toInt()

                val dataTag = ByteArray(4).also { buffer.get(it) }.toString(Charsets.US_ASCII)
                if (dataTag != "data") return null
                val dataSize = buffer.int

                val rawPcm = ByteArray(dataSize)
                var totalRead = 0
                while (totalRead < dataSize) {
                    val read = fis.read(rawPcm, totalRead, dataSize - totalRead)
                    if (read == -1) break
                    totalRead += read
                }

                val pcmBuffer = ByteBuffer.wrap(rawPcm, 0, totalRead).order(ByteOrder.LITTLE_ENDIAN)
                val numSamples = totalRead / (bitsPerSample / 8)
                val floatSamples = FloatArray(numSamples)

                if (bitsPerSample == 16) {
                    for (i in 0 until numSamples) {
                        floatSamples[i] = pcmBuffer.short / 32768.0f
                    }
                } else if (bitsPerSample == 8) {
                    for (i in 0 until numSamples) {
                        floatSamples[i] = ((pcmBuffer.get().toInt() and 0xFF) - 128) / 128.0f
                    }
                } else {
                    return null
                }

                val durationMs = if (sampleRate > 0 && channels > 0) {
                    ((numSamples / channels).toLong() * 1000) / sampleRate
                } else 0L

                DecodedWavAudio(
                    samples = floatSamples,
                    sampleRate = sampleRate,
                    channels = channels,
                    durationMs = durationMs
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resamples float audio to target sample rate using linear interpolation.
     */
    fun resample(
        sourceSamples: FloatArray,
        sourceSampleRate: Int,
        targetSampleRate: Int,
        channels: Int
    ): FloatArray {
        if (sourceSampleRate == targetSampleRate) return sourceSamples
        if (sourceSamples.isEmpty()) return FloatArray(0)

        val ratio = targetSampleRate.toDouble() / sourceSampleRate.toDouble()
        val sourceFrames = sourceSamples.size / channels
        val targetFrames = (sourceFrames * ratio).toInt()
        val targetSamples = FloatArray(targetFrames * channels)

        for (ch in 0 until channels) {
            for (outFrame in 0 until targetFrames) {
                val inFrameExact = outFrame / ratio
                val inFrameLow = inFrameExact.toInt()
                val inFrameHigh = (inFrameLow + 1).coerceAtMost(sourceFrames - 1)
                val frac = (inFrameExact - inFrameLow).toFloat()

                val sampleLow = sourceSamples[inFrameLow * channels + ch]
                val sampleHigh = sourceSamples[inFrameHigh * channels + ch]
                val interpolated = sampleLow + frac * (sampleHigh - sampleLow)

                targetSamples[outFrame * channels + ch] = interpolated
            }
        }

        return targetSamples
    }
}
