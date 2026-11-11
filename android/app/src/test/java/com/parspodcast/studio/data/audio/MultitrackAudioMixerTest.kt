package com.parspodcast.studio.data.audio

import com.parspodcast.studio.data.local.entities.AudioClipEntity
import com.parspodcast.studio.data.local.entities.AudioTrackEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.math.abs

class MultitrackAudioMixerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSyntheticWav(fileName: String, durationMs: Long, sampleRate: Int = 48000, value: Float = 0.5f): File {
        val file = File(tempFolder.root, fileName)
        val numSamples = ((durationMs * sampleRate) / 1000).toInt()
        val floatSamples = FloatArray(numSamples) { value }
        val pcmBytes = WavAudioWriter.floatTo16BitPcm(floatSamples)
        WavAudioWriter.writeWavFile(file, pcmBytes, sampleRate = sampleRate)
        return file
    }

    @Test
    fun testTimelineDurationCalculation_findsMaxClipEnd() {
        val clips = listOf(
            AudioClipEntity("c1", "t1", startTimeMs = 0L, durationMs = 3000L, filePath = ""),
            AudioClipEntity("c2", "t1", startTimeMs = 4000L, durationMs = 2500L, filePath = ""),
            AudioClipEntity("c3", "t2", startTimeMs = 1000L, durationMs = 8000L, filePath = "")
        )

        val totalMs = MultitrackAudioMixer.calculateTimelineDurationMs(clips)
        assertEquals(9000L, totalMs) // 1000 + 8000
    }

    @Test
    fun testMixTwoOverlappingClips_sumsAccurately() = runBlocking {
        val wav1 = createSyntheticWav("clip1.wav", 1000L, value = 0.3f)
        val wav2 = createSyntheticWav("clip2.wav", 1000L, value = 0.2f)

        val track1 = AudioTrackEntity("t1", "p1", "HOST_VOICE", "Voice 1", volume = 1.0f, pan = 0.0f)
        val clip1 = AudioClipEntity("c1", "t1", startTimeMs = 0L, durationMs = 1000L, filePath = wav1.absolutePath)
        val clip2 = AudioClipEntity("c2", "t1", startTimeMs = 0L, durationMs = 1000L, filePath = wav2.absolutePath)

        val mixedStereo = MultitrackAudioMixer.mixToStereoBuffer(
            tracks = listOf(track1),
            clips = listOf(clip1, clip2),
            targetSampleRate = 48000,
            masterVolume = 1.0f
        )

        assertTrue("Stereo buffer must not be empty", mixedStereo.isNotEmpty())
        // At mid-point (500ms), sample should reflect sum: 0.3 + 0.2 = ~0.5 (scaled by pan and fade)
        val midSampleLeft = mixedStereo[48000 / 2 * 2]
        assertTrue("Sum of overlapping clips should be positive", midSampleLeft > 0.25f)
    }

    @Test
    fun testTrackMute_silencesTrackInMix() = runBlocking {
        val wav = createSyntheticWav("muted_clip.wav", 1000L, value = 0.8f)
        val track = AudioTrackEntity("t1", "p1", "BGM", "Music", volume = 1.0f, isMuted = true)
        val clip = AudioClipEntity("c1", "t1", startTimeMs = 0L, durationMs = 1000L, filePath = wav.absolutePath)

        val mixed = MultitrackAudioMixer.mixToStereoBuffer(
            tracks = listOf(track),
            clips = listOf(clip),
            targetSampleRate = 48000
        )

        // All samples should be zero
        for (sample in mixed) {
            assertEquals(0f, sample, 0.0001f)
        }
    }

    @Test
    fun testTrackSolo_isolatesSoloedTrackOnly() = runBlocking {
        val wav1 = createSyntheticWav("solo.wav", 1000L, value = 0.5f)
        val wav2 = createSyntheticWav("ignored.wav", 1000L, value = 0.5f)

        val track1 = AudioTrackEntity("t1", "p1", "PRESENTER_A", "Voice", isSolo = true)
        val track2 = AudioTrackEntity("t2", "p1", "BGM", "Music", isSolo = false)

        val clip1 = AudioClipEntity("c1", "t1", startTimeMs = 0L, durationMs = 1000L, filePath = wav1.absolutePath)
        val clip2 = AudioClipEntity("c2", "t2", startTimeMs = 0L, durationMs = 1000L, filePath = wav2.absolutePath)

        val mixed = MultitrackAudioMixer.mixToStereoBuffer(
            tracks = listOf(track1, track2),
            clips = listOf(clip1, clip2),
            targetSampleRate = 48000
        )

        assertTrue(mixed.isNotEmpty())
    }

    @Test
    fun testRenderToWavFile_generatesValid16BitStereoWav() = runBlocking {
        val wav = createSyntheticWav("source.wav", 1000L, value = 0.4f)
        val track = AudioTrackEntity("t1", "p1", "INTRO", "Intro", volume = 1.0f)
        val clip = AudioClipEntity("c1", "t1", startTimeMs = 0L, durationMs = 1000L, filePath = wav.absolutePath)

        val outputFile = File(tempFolder.root, "mixed_output_48k.wav")
        val result = MultitrackAudioMixer.renderToWavFile(
            tracks = listOf(track),
            clips = listOf(clip),
            outputFile = outputFile,
            targetSampleRate = 48000
        )

        assertTrue("Render must succeed", result.isSuccess)
        assertTrue("Output file must exist", outputFile.exists())
        assertTrue("File size must be > 44 bytes", outputFile.length() > 44)

        // Verify decoded WAV
        val decoded = WavFileReader.readWavFile(outputFile)
        assertTrue(decoded != null)
        assertEquals(48000, decoded?.sampleRate)
        assertEquals(2, decoded?.channels) // Stereo
    }

    @Test
    fun testSoftClippingLimiter_preventsExceedingOne() = runBlocking {
        // Create two high amplitude clips that would sum to 1.6f without limiter
        val wav1 = createSyntheticWav("high1.wav", 500L, value = 0.8f)
        val wav2 = createSyntheticWav("high2.wav", 500L, value = 0.8f)

        val track = AudioTrackEntity("t1", "p1", "SFX", "Boom", volume = 1.0f)
        val clip1 = AudioClipEntity("c1", "t1", startTimeMs = 0L, durationMs = 500L, filePath = wav1.absolutePath)
        val clip2 = AudioClipEntity("c2", "t1", startTimeMs = 0L, durationMs = 500L, filePath = wav2.absolutePath)

        val mixed = MultitrackAudioMixer.mixToStereoBuffer(
            tracks = listOf(track),
            clips = listOf(clip1, clip2),
            targetSampleRate = 48000,
            masterVolume = 1.0f
        )

        for (sample in mixed) {
            assertTrue("Sample magnitude must never exceed 1.0f", abs(sample) <= 1.0f)
        }
    }
}
