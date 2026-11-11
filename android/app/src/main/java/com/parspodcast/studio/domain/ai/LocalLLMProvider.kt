package com.parspodcast.studio.domain.ai

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

sealed interface EngineLoadingState {
    object Uninitialized : EngineLoadingState
    object CheckingResources : EngineLoadingState
    data class Loading(val progress: Float, val stageFa: String) : EngineLoadingState
    data class Ready(
        val backend: String,
        val modelPath: String,
        val ramUsageMb: Long,
        val modelName: String = "Gemma 4 E4B Instruct"
    ) : EngineLoadingState
    data class Error(val messageFa: String, val cause: Throwable? = null) : EngineLoadingState
}

enum class ModelExecutionBackend {
    CPU,
    GPU,
    NPU
}

data class GenerationSettings(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val maxTokens: Int = 2048,
    val streaming: Boolean = true
)

data class GenerationRequest(
    val prompt: String,
    val systemInstruction: String,
    val settings: GenerationSettings = GenerationSettings()
)

data class GenerationResult(
    val fullText: String,
    val tokenCount: Int,
    val durationMs: Long,
    val tokensPerSecond: Float
)

data class DeviceCapabilityReport(
    val androidVersion: String,
    val deviceModel: String,
    val cpuAbi: String,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val totalStorageMb: Long,
    val availableStorageMb: Long,
    val isRamSufficientForE4B: Boolean,
    val isStorageSufficientForE4B: Boolean,
    val recommendedBackend: ModelExecutionBackend
)

interface LocalLLMProvider {
    val loadingState: StateFlow<EngineLoadingState>
    val isReady: Boolean

    suspend fun initializeAndLoad(
        modelFile: File,
        preferredBackend: ModelExecutionBackend = ModelExecutionBackend.GPU
    ): Result<Unit>

    suspend fun unload(): Result<Unit>

    fun generateStream(request: GenerationRequest): Flow<String>

    suspend fun generate(request: GenerationRequest): Result<GenerationResult>

    fun cancelInference()

    fun getDeviceCapabilityReport(context: Context): DeviceCapabilityReport
}
