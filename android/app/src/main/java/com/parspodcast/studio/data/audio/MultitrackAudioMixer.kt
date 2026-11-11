package com.parspodcast.studio.data.audio

import com.parspodcast.studio.data.local.entities.AudioClipEntity
import com.parspodcast.studio.data.local.entities.AudioTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.*

object MultitrackAudioMixer {

    const val MIX_SAMPLE_RATE = 48000
    const val MIX_CHANNELS = 2 // Stereo

    /**
     * Calculates the total timeline duration in milliseconds.
     */
    fun calculateTimelineDurationMs(clips: List<AudioClipEntity>): Long {
        if (clips.isEmpty()) return 0L
        return clips.maxOf { it.startTimeMs + it.durationMs }
    }

    /**
     * Mixes a multitrack project into an interleaved FloatArray of stereo samples (48kHz).
     */
    suspend fun mixToStereoBuffer(
        tracks: List<AudioTrackEntity>,
        clips: List<AudioClipEntity>,
        targetSampleRate: Int = MIX_SAMPLE_RATE,
        masterVolume: Float = 1.0f,
        onProgress: ((Float) -> Unit)? = null
    ): FloatArray = withContext(Dispatchers.Default) {
        val totalDurationMs = calculateTimelineDurationMs(clips)
        if (totalDurationMs <= 0L) return@withContext FloatArray(0)

        val totalFrames = ((totalDurationMs * targetSampleRate) / 1000).toInt() + (targetSampleRate / 2) // small buffer
        val mixLeft = FloatArray(totalFrames)
        val mixRight = FloatArray(totalFrames)

        // Determine Solo / Mute status
        val hasSolo = tracks.any { it.isSolo }
        val activeTracks = tracks.filter { track ->
            if (hasSolo) track.isSolo && !track.isMuted else !track.isMuted
        }.associateBy { it.id }

        val trackClipsMap = clips.filter { it.trackId in activeTracks.keys && !it.isMuted }
            .groupBy { it.trackId }

        // Track voice activity timeline for ducking BGM
        val voiceActivityMask = BooleanArray(totalFrames)
        val voiceTrackTypes = setOf("PRESENTER_A", "PRESENTER_B", "HOST_VOICE")

        // 1. First pass: Decode voice tracks and build voice activity mask
        for ((trackId, track) in activeTracks) {
            coroutineContext.ensureActive()
            if (track.trackType in voiceTrackTypes) {
                val trackClips = trackClipsMap[trackId] ?: continue
                for (clip in trackClips) {
                    val startFrame = ((clip.startTimeMs * targetSampleRate) / 1000).toInt()
                    val endFrame = (((clip.startTimeMs + clip.durationMs) * targetSampleRate) / 1000).toInt().coerceAtMost(totalFrames)
                    for (f in startFrame until endFrame) {
                        voiceActivityMask[f] = true
                    }
                }
            }
        }

        // 2. Mix all tracks into stereo accumulator
        var processedClips = 0
        val totalActiveClips = trackClipsMap.values.sumOf { it.size }.coerceAtLeast(1)

        for ((trackId, track) in activeTracks) {
            coroutineContext.ensureActive()
            val trackClips = trackClipsMap[trackId] ?: continue
            val trackVol = track.volume
            val trackPan = track.pan.coerceIn(-1.0f, 1.0f)

            // Constant power stereo panning
            val panAngle = (trackPan + 1.0f) * (PI.toFloat() / 4.0f)
            val panLeftGain = cos(panAngle)
            val panRightGain = sin(panAngle)

            val isBgmDucking = track.duckingEnabled

            for (clip in trackClips) {
                coroutineContext.ensureActive()

                val sourceFile = File(clip.filePath)
                val decoded = WavFileReader.readWavFile(sourceFile) ?: continue

                // Resample to 48kHz if required
                val resampled = if (decoded.sampleRate != targetSampleRate) {
                    WavFileReader.resample(decoded.samples, decoded.sampleRate, targetSampleRate, decoded.channels)
                } else {
                    decoded.samples
                }

                // Compute clip gain and fade curves
                val clipGainLinear = (10.0.pow(clip.gainDb / 20.0)).toFloat() * trackVol
                val clipStartFrame = ((clip.startTimeMs * targetSampleRate) / 1000).toInt()

                // Source trimming
                val sourceStartFrame = ((clip.sourceStartMs * targetSampleRate) / 1000).toInt().coerceAtLeast(0)
                val sourceEndFrame = if (clip.sourceEndMs > 0) {
                    ((clip.sourceEndMs * targetSampleRate) / 1000).toInt()
                } else {
                    resampled.size / decoded.channels
                }

                val framesToCopy = (sourceEndFrame - sourceStartFrame).coerceAtLeast(0)
                val fadeInFrames = ((clip.fadeInMs * targetSampleRate) / 1000).toInt()
                val fadeOutFrames = ((clip.fadeOutMs * targetSampleRate) / 1000).toInt()

                val channels = decoded.channels

                for (frameIdx in 0 until framesToCopy) {
                    val targetFrame = clipStartFrame + frameIdx
                    if (targetFrame >= totalFrames) break

                    val srcFrame = sourceStartFrame + frameIdx
                    if (srcFrame >= resampled.size / channels) break

                    // Extract source sample (mono or stereo average)
                    val sample = if (channels == 1) {
                        resampled[srcFrame]
                    } else {
                        (resampled[srcFrame * 2] + resampled[srcFrame * 2 + 1]) * 0.5f
                    }

                    // Calculate Fade Factor
                    var fadeFactor = 1.0f
                    if (fadeInFrames > 0 && frameIdx < fadeInFrames) {
                        fadeFactor = frameIdx.toFloat() / fadeInFrames
                    } else if (fadeOutFrames > 0 && (framesToCopy - frameIdx) < fadeOutFrames) {
                        fadeFactor = (framesToCopy - frameIdx).toFloat() / fadeOutFrames
                    }

                    // Ducking attenuation for BGM (-12dB when voice is active)
                    var duckingFactor = 1.0f
                    if (isBgmDucking && voiceActivityMask[targetFrame]) {
                        duckingFactor = 0.25f // -12 dB
                    }

                    val finalSample = sample * clipGainLinear * fadeFactor * duckingFactor

                    mixLeft[targetFrame] += finalSample * panLeftGain
                    mixRight[targetFrame] += finalSample * panRightGain
                }

                processedClips++
                onProgress?.invoke(processedClips.toFloat() / totalActiveClips)
            }
        }

        // 3. Apply Master Volume, Fade at project boundaries, and Soft Clipping Limiter
        val interleaved = FloatArray(totalFrames * 2)
        val projectFadeInFrames = (100 * targetSampleRate) / 1000 // 100ms anti-pop
        val projectFadeOutFrames = (300 * targetSampleRate) / 1000 // 300ms smooth tail

        for (f in 0 until totalFrames) {
            var boundaryFade = 1.0f
            if (f < projectFadeInFrames) {
                boundaryFade = f.toFloat() / projectFadeInFrames
            } else if ((totalFrames - f) < projectFadeOutFrames) {
                boundaryFade = (totalFrames - f).toFloat() / projectFadeOutFrames
            }

            var left = mixLeft[f] * masterVolume * boundaryFade
            var right = mixRight[f] * masterVolume * boundaryFade

            // Soft-knee limiter using hyperbolic tangent to avoid harsh digital clipping
            if (abs(left) > 0.95f) {
                left = tanh(left)
            }
            if (abs(right) > 0.95f) {
                right = tanh(right)
            }

            interleaved[f * 2] = left.coerceIn(-1.0f, 1.0f)
            interleaved[f * 2 + 1] = right.coerceIn(-1.0f, 1.0f)
        }

        return@withContext interleaved
    }

    /**
     * Renders the multitrack project and writes a broadcast-standard 16-bit Stereo PCM WAV file.
     */
    suspend fun renderToWavFile(
        tracks: List<AudioTrackEntity>,
        clips: List<AudioClipEntity>,
        outputFile: File,
        targetSampleRate: Int = MIX_SAMPLE_RATE,
        masterVolume: Float = 1.0f,
        onProgress: ((Float) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val interleavedSamples = mixToStereoBuffer(
                tracks = tracks,
                clips = clips,
                targetSampleRate = targetSampleRate,
                masterVolume = masterVolume,
                onProgress = onProgress
            )

            if (interleavedSamples.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("هیچ ترک صوتی برای میکس در تایم‌لاین وجود ندارد"))
            }

            // Convert FloatArray [-1.0f, 1.0f] to 16-bit PCM Byte array
            val byteBuffer = ByteBuffer.allocate(interleavedSamples.size * 2).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                for (s in interleavedSamples) {
                    val clamped = s.coerceIn(-1.0f, 1.0f)
                    val shortVal = (clamped * 32767.0f).toInt().toShort()
                    putShort(shortVal)
                }
            }

            val pcmBytes = byteBuffer.array()
            outputFile.parentFile?.mkdirs()

            val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
            WavAudioWriter.writeWavFile(
                outputFile = tempFile,
                pcmBytes = pcmBytes,
                sampleRate = targetSampleRate,
                numChannels = MIX_CHANNELS
            )

            if (outputFile.exists()) outputFile.delete()
            tempFile.renameTo(outputFile)

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
