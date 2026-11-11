package com.parspodcast.studio.domain.tts

import kotlinx.coroutines.flow.StateFlow
import java.io.File

sealed interface TTSEngineState {
    data object Uninitialized : TTSEngineState
    data class Loading(val progress: Float = 0f, val message: String = "") : TTSEngineState
    data class Ready(val modelName: String, val sampleRate: Int, val numSpeakers: Int) : TTSEngineState
    data class Synthesizing(val currentSegment: Int, val totalSegments: Int, val textSnippet: String) : TTSEngineState
    data class Error(val message: String, val isFatal: Boolean = false) : TTSEngineState
}

data class TTSSettings(
    val speechSpeed: Float = 1.0f,
    val noiseScale: Float = 0.667f,
    val noiseScaleW: Float = 0.8f,
    val speakerId: Int = 0,
    val speakerRole: String = "PRESENTER_A",
    val addSilencePrefixMs: Long = 100L,
    val addSilenceSuffixMs: Long = 400L
)

data class AudioSynthesisResult(
    val samples: FloatArray,
    val sampleRate: Int,
    val durationSec: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioSynthesisResult
        return samples.contentEquals(other.samples) && sampleRate == other.sampleRate && durationSec == other.durationSec
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + durationSec.hashCode()
        return result
    }
}

data class AudioFileMetadata(
    val filePath: String,
    val fileSizeBytes: Long,
    val durationSec: Float,
    val sampleRate: Int,
    val channels: Int = 1,
    val bitDepth: Int = 16,
    val speakerRole: String,
    val segmentId: String,
    val createdAtUtc: Long
)

data class TTSModelStatus(
    val modelName: String = "Mana Persian Piper (fa_IR-mana-medium)",
    val isOnnxInstalled: Boolean = false,
    val isJsonInstalled: Boolean = false,
    val isTokensGenerated: Boolean = false,
    val onnxSizeBytes: Long = 0L,
    val onnxFilePath: String? = null,
    val tokensFilePath: String? = null,
    val sampleRate: Int = 22050,
    val isLoadedInRam: Boolean = false
) {
    val isComplete: Boolean get() = isOnnxInstalled && (isJsonInstalled || isTokensGenerated)
}

interface TTSProvider {
    val engineState: StateFlow<TTSEngineState>

    suspend fun initialize(onnxPath: String, tokensPath: String, dataDir: String? = null): Result<Unit>
    suspend fun loadModel(): Result<Unit>
    suspend fun unloadModel()
    suspend fun synthesize(text: String, settings: TTSSettings = TTSSettings()): Result<AudioSynthesisResult>
    suspend fun synthesizeToFile(text: String, outputFile: File, settings: TTSSettings = TTSSettings()): Result<AudioFileMetadata>
    fun cancel()
    fun release()
    fun getModelStatus(): TTSModelStatus
}
