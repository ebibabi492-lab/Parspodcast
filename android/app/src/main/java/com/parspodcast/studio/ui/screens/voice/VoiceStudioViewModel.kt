package com.parspodcast.studio.ui.screens.voice

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parspodcast.studio.data.audio.AudioPlaybackManager
import com.parspodcast.studio.data.audio.PlaybackState
import com.parspodcast.studio.data.local.entities.ScriptSegmentEntity
import com.parspodcast.studio.data.manager.TTSModelFileManager
import com.parspodcast.studio.data.repository.ProductionRepository
import com.parspodcast.studio.domain.tts.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VoiceStudioUiState(
    val modelStatus: TTSModelStatus = TTSModelStatus(),
    val engineState: TTSEngineState = TTSEngineState.Uninitialized,
    val playbackState: PlaybackState = PlaybackState(),
    val segments: List<ScriptSegmentEntity> = emptyList(),
    val selectedSpeaker: String = "PRESENTER_A",
    val speechSpeed: Float = 1.0f,
    val noiseScale: Float = 0.667f,
    val testSentence: String = "سلام، به استودیوی تولید پادکست هوشمند پارس‌پادکست خوش آمدید.",
    val isImporting: Boolean = false,
    val importProgress: Float = 0f,
    val statusMessage: String? = null,
    val currentActiveProjectId: String? = null
)

@HiltViewModel
class VoiceStudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsProvider: TTSProvider,
    private val modelFileManager: TTSModelFileManager,
    val playbackManager: AudioPlaybackManager,
    private val productionRepository: ProductionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceStudioUiState())
    val uiState: StateFlow<VoiceStudioUiState> = _uiState.asStateFlow()

    init {
        // Collect engine state
        viewModelScope.launch {
            ttsProvider.engineState.collect { state ->
                _uiState.update { it.copy(engineState = state) }
            }
        }

        // Collect playback state
        viewModelScope.launch {
            playbackManager.playbackState.collect { state ->
                _uiState.update { it.copy(playbackState = state) }
            }
        }

        // Initial model status & load latest project segments
        refreshModelStatus()
        loadLatestProject()
    }

    fun refreshModelStatus() {
        viewModelScope.launch {
            val status = ttsProvider.getModelStatus()
            _uiState.update { it.copy(modelStatus = status) }
        }
    }

    private fun loadLatestProject() {
        viewModelScope.launch {
            productionRepository.getAllProjects().collect { projects ->
                val activeProj = projects.firstOrNull() ?: return@collect
                _uiState.update { it.copy(currentActiveProjectId = activeProj.id) }

                val script = productionRepository.getLatestScriptForProject(activeProj.id)
                if (script != null) {
                    productionRepository.getSegmentsForScript(script.id).collect { segs ->
                        _uiState.update { it.copy(segments = segs) }
                    }
                }
            }
        }
    }

    fun setSelectedSpeaker(speaker: String) {
        _uiState.update { it.copy(selectedSpeaker = speaker) }
    }

    fun setSpeechSpeed(speed: Float) {
        _uiState.update { it.copy(speechSpeed = speed) }
    }

    fun setTestSentence(text: String) {
        _uiState.update { it.copy(testSentence = text) }
    }

    fun importOnnxModel(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importProgress = 0f, statusMessage = "در حال انتقال مدل ONNX...") }
            val result = modelFileManager.importOnnxModel(uri) { copied, total ->
                val progress = if (total > 0) (copied.toFloat() / total).coerceIn(0f, 1f) else 0f
                _uiState.update { it.copy(importProgress = progress) }
            }
            if (result.isSuccess) {
                _uiState.update { it.copy(isImporting = false, statusMessage = "مدل fa_IR-mana-medium.onnx با موفقیت ذخیره شد") }
                refreshModelStatus()
            } else {
                _uiState.update { it.copy(isImporting = false, statusMessage = "خطا در انتقال فایل مدل: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun importJsonConfig(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, statusMessage = "در حال پردازش کانفیگ و استخراج واج‌ها...") }
            val result = modelFileManager.importJsonConfig(uri)
            if (result.isSuccess) {
                _uiState.update { it.copy(isImporting = false, statusMessage = "فایل کانفیگ و tokens.txt با موفقیت استخراج شدند") }
                refreshModelStatus()
            } else {
                _uiState.update { it.copy(isImporting = false, statusMessage = "خطا در پردازش کانفیگ: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun loadEngine() {
        viewModelScope.launch {
            ttsProvider.loadModel()
            refreshModelStatus()
        }
    }

    fun unloadEngine() {
        viewModelScope.launch {
            ttsProvider.unloadModel()
            refreshModelStatus()
        }
    }

    fun synthesizeTestSentence() {
        val text = _uiState.value.testSentence
        if (text.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val audioDir = File(context.filesDir, "audio/test").apply { mkdirs() }
            val testFile = File(audioDir, "test_synthesis_${System.currentTimeMillis()}.wav")

            val settings = TTSSettings(
                speechSpeed = _uiState.value.speechSpeed,
                noiseScale = _uiState.value.noiseScale,
                speakerId = 0,
                speakerRole = _uiState.value.selectedSpeaker
            )

            val result = ttsProvider.synthesizeToFile(text, testFile, settings)
            if (result.isSuccess) {
                _uiState.update { it.copy(statusMessage = "نمونه صدا با موفقیت سنتز شد (${result.getOrNull()?.durationSec} ثانیه)") }
                playbackManager.playFile(testFile, "test")
            } else {
                _uiState.update { it.copy(statusMessage = "خطا در سنتز صدا: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun synthesizeSegment(segment: ScriptSegmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val audioDir = File(context.filesDir, "audio/segments/${segment.scriptId}").apply { mkdirs() }
            val outputFile = File(audioDir, "${segment.speakerRole}_seg_${segment.segmentIndex}.wav")

            val settings = TTSSettings(
                speechSpeed = _uiState.value.speechSpeed,
                noiseScale = _uiState.value.noiseScale,
                speakerId = 0,
                speakerRole = segment.speakerRole
            )

            val result = ttsProvider.synthesizeToFile(segment.textFa, outputFile, settings)
            if (result.isSuccess) {
                val metadata = result.getOrNull()
                // Update Room segment with generated audio path and duration
                val updated = segment.copy(
                    generatedAudioPath = metadata?.filePath,
                    estimatedDurationSec = metadata?.durationSec?.toInt() ?: segment.estimatedDurationSec
                )
                productionRepository.updateSegment(updated)
                _uiState.update { it.copy(statusMessage = "صوت بخش ${segment.segmentIndex} تولید شد") }
            } else {
                _uiState.update { it.copy(statusMessage = "خطا در سنتز بخش ${segment.segmentIndex}: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun synthesizeAllSegments() {
        val segs = _uiState.value.segments
        if (segs.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            for (segment in segs) {
                synthesizeSegment(segment)
            }
            _uiState.update { it.copy(statusMessage = "تمام بخش‌های سناریو با موفقیت سنتز شدند") }
        }
    }

    fun cancelSynthesis() {
        ttsProvider.cancel()
    }

    fun playSegmentAudio(segment: ScriptSegmentEntity) {
        val path = segment.generatedAudioPath ?: return
        val file = File(path)
        if (file.exists()) {
            playbackManager.playFile(file, segment.id)
        }
    }

    fun togglePlayback(file: File, segmentId: String? = null) {
        val current = _uiState.value.playbackState
        if (current.isPlaying && current.currentAudioPath == file.absolutePath) {
            playbackManager.pause()
        } else {
            playbackManager.playFile(file, segmentId)
        }
    }

    fun seekAudio(progress: Float) {
        val total = _uiState.value.playbackState.totalDurationMs
        if (total > 0) {
            val targetMs = (progress * total).toLong()
            playbackManager.seekTo(targetMs)
        }
    }

    fun deleteModel() {
        viewModelScope.launch {
            unloadEngine()
            modelFileManager.deleteModel()
            refreshModelStatus()
            _uiState.update { it.copy(statusMessage = "فایل‌های مدل مانا پاکسازی شدند") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
