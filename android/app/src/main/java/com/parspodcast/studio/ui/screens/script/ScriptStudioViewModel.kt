package com.parspodcast.studio.ui.screens.script

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parspodcast.studio.data.local.entities.NewsArticleEntity
import com.parspodcast.studio.data.local.entities.ScriptEntity
import com.parspodcast.studio.data.local.entities.ScriptSegmentEntity
import com.parspodcast.studio.data.manager.ModelFileManager
import com.parspodcast.studio.data.repository.ProductionRepository
import com.parspodcast.studio.domain.ai.*
import com.parspodcast.studio.domain.parser.ParsedPodcastScript
import com.parspodcast.studio.domain.parser.ScriptJsonParser
import com.parspodcast.studio.domain.prompt.PersianPromptBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface ScriptGenerationUiState {
    object Idle : ScriptGenerationUiState
    data class Generating(
        val partialText: String,
        val tokensGenerated: Int,
        val elapsedMs: Long
    ) : ScriptGenerationUiState
    data class Completed(
        val parsedScript: ParsedPodcastScript,
        val totalTokens: Int,
        val tokensPerSec: Float
    ) : ScriptGenerationUiState
    data class Error(val messageFa: String) : ScriptGenerationUiState
}

@HiltViewModel
class ScriptStudioViewModel @Inject constructor(
    val localLLMProvider: LocalLLMProvider,
    private val modelFileManager: ModelFileManager,
    private val productionRepository: ProductionRepository
) : ViewModel() {

    val engineState: StateFlow<EngineLoadingState> = localLLMProvider.loadingState

    private val _uiState = MutableStateFlow<ScriptGenerationUiState>(ScriptGenerationUiState.Idle)
    val uiState: StateFlow<ScriptGenerationUiState> = _uiState.asStateFlow()

    private val _generationSettings = MutableStateFlow(GenerationSettings())
    val generationSettings: StateFlow<GenerationSettings> = _generationSettings.asStateFlow()

    private val _segments = MutableStateFlow<List<ScriptSegmentEntity>>(emptyList())
    val segments: StateFlow<List<ScriptSegmentEntity>> = _segments.asStateFlow()

    private var currentJob: Job? = null

    fun isModelFileInstalled(): Boolean {
        return modelFileManager.isModelInstalled()
    }

    fun loadLocalModel(preferredBackend: ModelExecutionBackend = ModelExecutionBackend.GPU) {
        viewModelScope.launch {
            val modelFile = modelFileManager.getTargetModelFile()
            localLLMProvider.initializeAndLoad(modelFile, preferredBackend)
        }
    }

    fun unloadLocalModel() {
        viewModelScope.launch {
            localLLMProvider.unload()
        }
    }

    fun updateSettings(settings: GenerationSettings) {
        _generationSettings.value = settings
    }

    fun setInitialSegments(initial: List<ScriptSegmentEntity>) {
        if (_segments.value.isEmpty()) {
            _segments.value = initial
        }
    }

    fun updateSegmentText(segmentId: String, newText: String) {
        val words = newText.split("\\s+".toRegex()).size
        _segments.value = _segments.value.map { seg ->
            if (seg.id == segmentId) {
                seg.copy(
                    textFa = newText,
                    estimatedDurationSec = maxOf(4, (words * 0.4).toInt())
                )
            } else seg
        }
    }

    fun toggleSpeaker(segmentId: String) {
        _segments.value = _segments.value.map { seg ->
            if (seg.id == segmentId) {
                val newSpeaker = if (seg.speakerRole == "PRESENTER_A") "PRESENTER_B" else "PRESENTER_A"
                seg.copy(speakerRole = newSpeaker)
            } else seg
        }
    }

    fun generateFullPodcastScript(
        title: String,
        targetDurationMinutes: Int,
        isDualHost: Boolean,
        tone: String,
        articles: List<NewsArticleEntity>
    ) {
        if (!localLLMProvider.isReady) {
            _uiState.value = ScriptGenerationUiState.Error("مدل هوش مصنوعی محلی بارگذاری نشده است.")
            return
        }

        val (system, prompt) = if (isDualHost) {
            PersianPromptBuilder.buildDualHostScriptPrompt(title, targetDurationMinutes, tone, articles)
        } else {
            PersianPromptBuilder.buildSingleHostScriptPrompt(title, targetDurationMinutes, tone, articles)
        }

        val request = GenerationRequest(
            prompt = prompt,
            systemInstruction = system,
            settings = _generationSettings.value
        )

        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val streamedSb = StringBuilder()
            var tokenCount = 0

            _uiState.value = ScriptGenerationUiState.Generating("", 0, 0L)

            try {
                localLLMProvider.generateStream(request).collect { tokenChunk ->
                    streamedSb.append(tokenChunk)
                    tokenCount++
                    _uiState.value = ScriptGenerationUiState.Generating(
                        partialText = streamedSb.toString(),
                        tokensGenerated = tokenCount,
                        elapsedMs = System.currentTimeMillis() - startTime
                    )
                }

                val duration = System.currentTimeMillis() - startTime
                val tps = if (duration > 0) (tokenCount.toFloat() / (duration / 1000f)) else 0f
                val parsed = ScriptJsonParser.parse(streamedSb.toString(), title, isDualHost)

                _uiState.value = ScriptGenerationUiState.Completed(
                    parsedScript = parsed,
                    totalTokens = tokenCount,
                    tokensPerSec = tps
                )

                // Update segments automatically
                val convertedSegments = ScriptJsonParser.toScriptSegmentEntities(parsed)
                _segments.value = convertedSegments

            } catch (e: Exception) {
                _uiState.value = ScriptGenerationUiState.Error("خطا در استنتاج محلی مدل: ${e.message}")
            }
        }
    }

    fun rewriteSegment(segmentId: String, instruction: String) {
        val segment = _segments.value.find { it.id == segmentId } ?: return
        if (!localLLMProvider.isReady) {
            _uiState.value = ScriptGenerationUiState.Error("مدل محلی بارگذاری نشده است.")
            return
        }

        val (system, prompt) = PersianPromptBuilder.buildRewritePrompt(segment.textFa, instruction)
        runSegmentOperation(segmentId, system, prompt)
    }

    fun summarizeSegment(segmentId: String) {
        val segment = _segments.value.find { it.id == segmentId } ?: return
        if (!localLLMProvider.isReady) {
            _uiState.value = ScriptGenerationUiState.Error("مدل محلی بارگذاری نشده است.")
            return
        }

        val (system, prompt) = PersianPromptBuilder.buildSummarizePrompt(segment.textFa)
        runSegmentOperation(segmentId, system, prompt)
    }

    fun expandSegment(segmentId: String, topic: String) {
        val segment = _segments.value.find { it.id == segmentId } ?: return
        if (!localLLMProvider.isReady) {
            _uiState.value = ScriptGenerationUiState.Error("مدل محلی بارگذاری نشده است.")
            return
        }

        val (system, prompt) = PersianPromptBuilder.buildExpandPrompt(segment.textFa, topic)
        runSegmentOperation(segmentId, system, prompt)
    }

    fun changeSegmentTone(segmentId: String, targetTone: String) {
        val segment = _segments.value.find { it.id == segmentId } ?: return
        if (!localLLMProvider.isReady) {
            _uiState.value = ScriptGenerationUiState.Error("مدل محلی بارگذاری نشده است.")
            return
        }

        val (system, prompt) = PersianPromptBuilder.buildToneChangePrompt(segment.textFa, targetTone)
        runSegmentOperation(segmentId, system, prompt)
    }

    private fun runSegmentOperation(segmentId: String, system: String, prompt: String) {
        val request = GenerationRequest(
            prompt = prompt,
            systemInstruction = system,
            settings = _generationSettings.value
        )

        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            val sb = StringBuilder()
            _uiState.value = ScriptGenerationUiState.Generating("", 0, 0L)
            val startTime = System.currentTimeMillis()
            var tokenCount = 0

            try {
                localLLMProvider.generateStream(request).collect { chunk ->
                    sb.append(chunk)
                    tokenCount++
                    _uiState.value = ScriptGenerationUiState.Generating(
                        partialText = sb.toString(),
                        tokensGenerated = tokenCount,
                        elapsedMs = System.currentTimeMillis() - startTime
                    )
                }

                val newText = sb.toString().trim()
                updateSegmentText(segmentId, newText)
                _uiState.value = ScriptGenerationUiState.Idle
            } catch (e: Exception) {
                _uiState.value = ScriptGenerationUiState.Error("خطا در پردازش سگمنت: ${e.message}")
            }
        }
    }

    fun cancelGeneration() {
        localLLMProvider.cancelInference()
        currentJob?.cancel()
        currentJob = null
        _uiState.value = ScriptGenerationUiState.Idle
    }

    fun saveDraft(projectId: String, modelIdUsed: String) {
        viewModelScope.launch {
            val scriptId = UUID.randomUUID().toString()
            val script = ScriptEntity(
                id = scriptId,
                projectId = projectId,
                modelIdUsed = modelIdUsed,
                totalSegments = _segments.value.size,
                rawJson = "",
                version = 1,
                createdAtUtc = System.currentTimeMillis()
            )
            productionRepository.insertScript(script)
            val segmentsWithScriptId = _segments.value.map { it.copy(scriptId = scriptId) }
            productionRepository.insertSegments(segmentsWithScriptId)
        }
    }
}
