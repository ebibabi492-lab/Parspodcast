package com.parspodcast.studio.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavAudioWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFloatTo16BitPcm_convertsCorrectly() {
        val floatSamples = floatArrayOf(0.0f, 1.0f, -1.0f, 0.5f, -0.5f)
        val pcmBytes = WavAudioWriter.floatTo16BitPcm(floatSamples)

        assertEquals("Each float becomes 2 bytes in 16-bit PCM", floatSamples.size * 2, pcmBytes.size)

        val buffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0.toShort(), buffer.short)
        assertEquals(32767.toShort(), buffer.short)
        assertEquals((-32767).toShort(), buffer.short)
    }

    @Test
    fun testWriteWavFile_generatesValidRiffHeader() {
        val testFile = File(tempFolder.root, "test_output.wav")
        val sampleRate = 22050
        val numSamples = 22050 // exactly 1 second of audio
        val floatSamples = FloatArray(numSamples) { 0.1f }
        val pcmBytes = WavAudioWriter.floatTo16BitPcm(floatSamples)

        val writtenBytes = WavAudioWriter.writeWavFile(testFile, pcmBytes, sampleRate = sampleRate)

        assertTrue(testFile.exists())
        assertEquals(44 + pcmBytes.size.toLong(), writtenBytes)

        // Read and verify 44-byte WAV header
        val headerBytes = ByteArray(44)
        testFile.inputStream().use { it.read(headerBytes) }
        val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk descriptor
        val riff = ByteArray(4).also { buffer.get(it) }.toString(Charsets.US_ASCII)
        assertEquals("RIFF", riff)

        val chunkSize = buffer.int
        assertEquals(36 + pcmBytes.size, chunkSize)

        val wave = ByteArray(4).also { buffer.get(it) }.toString(Charsets.US_ASCII)
        assertEquals("WAVE", wave)

        // fmt subchunk
        val fmt = ByteArray(4).also { buffer.get(it) }.toString(Charsets.US_ASCII)
        assertEquals("fmt ", fmt)
        assertEquals(16, buffer.int) // subchunk1 size
        assertEquals(1.toShort(), buffer.short) // PCM format = 1
        assertEquals(1.toShort(), buffer.short) // Mono = 1
        assertEquals(sampleRate, buffer.int) // Sample rate

        // Verify calculated duration
        val duration = WavAudioWriter.calculateDurationSec(writtenBytes, sampleRate)
        assertEquals(1.0f, duration, 0.01f)
    }

    @Test
    fun testGenerateSilenceBytes_createsAccurateLength() {
        val durationMs = 500L
        val sampleRate = 22050
        val silenceBytes = WavAudioWriter.generateSilenceBytes(durationMs, sampleRate)

        val expectedSamples = (durationMs * sampleRate / 1000).toInt()
        assertEquals(expectedSamples * 2, silenceBytes.size)

        for (b in silenceBytes) {
            assertEquals(0.toByte(), b)
        }
    }
}
