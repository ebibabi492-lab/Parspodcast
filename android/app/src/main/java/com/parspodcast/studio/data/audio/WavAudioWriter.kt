package com.parspodcast.studio.data.audio

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavAudioWriter {

    const val DEFAULT_SAMPLE_RATE = 22050
    const val NUM_CHANNELS = 1 // Mono
    const val BITS_PER_SAMPLE = 16

    /**
     * Converts float PCM samples (-1.0 to 1.0) into 16-bit PCM bytes (Little-Endian).
     */
    fun floatTo16BitPcm(floatSamples: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(floatSamples.size * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        for (sample in floatSamples) {
            // Clamp to [-1.0, 1.0]
            val clamped = sample.coerceIn(-1.0f, 1.0f)
            val shortVal = (clamped * 32767.0f).toInt().toShort()
            byteBuffer.putShort(shortVal)
        }

        return byteBuffer.array()
    }

    /**
     * Generates a byte array of silence PCM samples.
     */
    fun generateSilenceBytes(durationMs: Long, sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        if (durationMs <= 0) return ByteArray(0)
        val numSamples = ((durationMs * sampleRate) / 1000).toInt()
        return ByteArray(numSamples * 2) // 2 bytes per sample for 16-bit
    }

    /**
     * Writes 16-bit PCM bytes into a standard RIFF/WAVE file.
     */
    fun writeWavFile(
        outputFile: File,
        pcmBytes: ByteArray,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        numChannels: Int = NUM_CHANNELS
    ): Long {
        outputFile.parentFile?.mkdirs()

        val byteRate = sampleRate * numChannels * (BITS_PER_SAMPLE / 8)
        val blockAlign = numChannels * (BITS_PER_SAMPLE / 8)
        val dataSize = pcmBytes.size
        val chunkSize = 36 + dataSize

        FileOutputStream(outputFile).use { fos ->
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)

                // RIFF header
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt(chunkSize)
                put("WAVE".toByteArray(Charsets.US_ASCII))

                // fmt subchunk
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16) // Subchunk1Size for PCM
                putShort(1.toShort()) // AudioFormat 1 = PCM
                putShort(numChannels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort(blockAlign.toShort())
                putShort(BITS_PER_SAMPLE.toShort())

                // data subchunk
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(dataSize)
            }

            fos.write(header.array())
            fos.write(pcmBytes)
            fos.flush()
        }

        return outputFile.length()
    }

    /**
     * Computes the audio duration in seconds from file size and sample rate.
     */
    fun calculateDurationSec(fileSizeBytes: Long, sampleRate: Int = DEFAULT_SAMPLE_RATE): Float {
        val pcmBytes = (fileSizeBytes - 44).coerceAtLeast(0)
        val numSamples = pcmBytes / 2
        return if (sampleRate > 0) numSamples.toFloat() / sampleRate else 0f
    }
}
