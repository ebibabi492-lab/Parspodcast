package com.parspodcast.studio.data.ai

import android.content.Context
import com.google.ai.edge.litertlm.*
import com.parspodcast.studio.data.manager.ModelFileManager
import com.parspodcast.studio.domain.ai.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRTLMEngine @Inject constructor(
    private val context: Context,
    private val modelFileManager: ModelFileManager
) : LocalLLMProvider {

    private val _loadingState = MutableStateFlow<EngineLoadingState>(EngineLoadingState.Uninitialized)
    override val loadingState: StateFlow<EngineLoadingState> = _loadingState.asStateFlow()

    override val isReady: Boolean
        get() = _loadingState.value is EngineLoadingState.Ready

    private var activeEngine: Engine? = null
    private var activeConversation: Conversation? = null
    private val engineMutex = Mutex()
    private var currentInferenceJob: Job? = null

    override suspend fun initializeAndLoad(
        modelFile: File,
        preferredBackend: ModelExecutionBackend
    ): Result<Unit> = withContext(Dispatchers.IO) {
        engineMutex.withLock {
            try {
                _loadingState.value = EngineLoadingState.CheckingResources

                if (!modelFile.exists() || modelFile.length() < 100_000_000L) {
                    val errorMsg = "فایل مدل ${modelFile.name} معتبر نیست یا هنوز وارد حافظه نشده است."
                    _loadingState.value = EngineLoadingState.Error(errorMsg)
                    return@withContext Result.failure(IllegalArgumentException(errorMsg))
                }

                // Device Capability check
                val capabilities = modelFileManager.probeDeviceCapabilities()
                if (!capabilities.isRamSufficientForE4B) {
                    val warning = "حافظه رم دستگاه (${capabilities.totalRamMb} مگابایت) ممکن است برای مدل ۴.۶ گیگابایتی کافی نباشد، تلاش برای بارگذاری با بک‌اند بهینه..."
                    _loadingState.value = EngineLoadingState.Loading(0.1f, warning)
                } else {
                    _loadingState.value = EngineLoadingState.Loading(0.15f, "در حال تخصیص بافر حافظه...")
                }

                // Clean up any previously loaded model instance
                activeConversation?.close()
                activeConversation = null
                activeEngine?.close()
                activeEngine = null

                // Attempt GPU initialization first if preferred, with graceful CPU fallback
                var selectedBackendName = "CPU (XNNPACK)"
                var engine: Engine? = null

                if (preferredBackend == ModelExecutionBackend.GPU) {
                    try {
                        _loadingState.value = EngineLoadingState.Loading(0.4f, "تلاش برای بارگذاری با شتاب‌دهنده گرافیکی GPU...")
                        val gpuConfig = EngineConfig(
                            modelPath = modelFile.absolutePath,
                            backend = Backend.GPU(),
                            maxNumTokens = 2048
                        )
                        val candidateEngine = Engine(gpuConfig)
                        candidateEngine.initialize()
                        engine = candidateEngine
                        selectedBackendName = "GPU (OpenCL / Vulkan)"
                    } catch (gpuError: Throwable) {
                        // Fallback gracefully to CPU as required by Section 5
                        _loadingState.value = EngineLoadingState.Loading(0.6f, "پشتیبانی GPU ناموفق بود؛ در حال راه‌اندازی با پردازنده اصلی CPU...")
                        val cpuConfig = EngineConfig(
                            modelPath = modelFile.absolutePath,
                            backend = Backend.CPU(),
                            maxNumTokens = 2048
                        )
                        val cpuEngine = Engine(cpuConfig)
                        cpuEngine.initialize()
                        engine = cpuEngine
                        selectedBackendName = "CPU (XNNPACK Fallback)"
                    }
                } else {
                    _loadingState.value = EngineLoadingState.Loading(0.5f, "بارگذاری مدل بر روی پردازنده اصلی CPU...")
                    val cpuConfig = EngineConfig(
                        modelPath = modelFile.absolutePath,
                        backend = Backend.CPU(),
                        maxNumTokens = 2048
                    )
                    val cpuEngine = Engine(cpuConfig)
                    cpuEngine.initialize()
                    engine = cpuEngine
                    selectedBackendName = "CPU (XNNPACK)"
                }

                activeEngine = engine
                _loadingState.value = EngineLoadingState.Loading(0.9f, "ایجاد نشست گفتگو و کش KV...")

                // Estimate RAM usage
                val memAfter = modelFileManager.probeDeviceCapabilities()
                val estimatedRamUsedMb = maxOf(1800L, capabilities.availableRamMb - memAfter.availableRamMb)

                _loadingState.value = EngineLoadingState.Ready(
                    backend = selectedBackendName,
                    modelPath = modelFile.absolutePath,
                    ramUsageMb = estimatedRamUsedMb,
                    modelName = "Gemma 4 E4B Instruct"
                )

                Result.success(Unit)
            } catch (e: Throwable) {
                activeConversation?.close()
                activeConversation = null
                activeEngine?.close()
                activeEngine = null
                _loadingState.value = EngineLoadingState.Error("خطا در بارگذاری LiteRT-LM: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun unload(): Result<Unit> = withContext(Dispatchers.IO) {
        engineMutex.withLock {
            try {
                cancelInference()
                activeConversation?.close()
                activeConversation = null
                activeEngine?.close()
                activeEngine = null
                _loadingState.value = EngineLoadingState.Uninitialized
                System.gc() // Hint GC to reclaim memory
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }
    }

    override fun generateStream(request: GenerationRequest): Flow<String> = channelFlow {
        val engine = activeEngine
            ?: throw IllegalStateException("مدل هوش مصنوعی محلی هنوز بارگذاری نشده است.")

        val startTime = System.currentTimeMillis()

        // Create or reuse clean conversation session
        val conversationConfig = ConversationConfig(
            systemInstruction = Contents.of(request.systemInstruction)
        )

        // Close any prior conversation to prevent unbounded memory growth
        activeConversation?.close()
        val conversation = engine.createConversation(conversationConfig)
        activeConversation = conversation

        currentInferenceJob = launch(Dispatchers.IO) {
            try {
                conversation.sendMessageAsync(request.prompt).collect { tokenDelta ->
                    send(tokenDelta)
                }
            } catch (cancellation: CancellationException) {
                // Inference cancelled safely
            } catch (e: Throwable) {
                close(e)
            }
        }

        currentInferenceJob?.join()
    }

    override suspend fun generate(request: GenerationRequest): Result<GenerationResult> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            val startTime = System.currentTimeMillis()
            var tokenCount = 0

            generateStream(request).collect { token ->
                sb.append(token)
                tokenCount++
            }

            val durationMs = System.currentTimeMillis() - startTime
            val tokensPerSecond = if (durationMs > 0) (tokenCount.toFloat() / (durationMs / 1000f)) else 0f

            Result.success(
                GenerationResult(
                    fullText = sb.toString(),
                    tokenCount = tokenCount,
                    durationMs = durationMs,
                    tokensPerSecond = tokensPerSecond
                )
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override fun cancelInference() {
        currentInferenceJob?.cancel()
        currentInferenceJob = null
    }

    override fun getDeviceCapabilityReport(context: Context): DeviceCapabilityReport {
        return modelFileManager.probeDeviceCapabilities()
    }
}
