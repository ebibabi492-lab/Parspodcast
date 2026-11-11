package com.parspodcast.studio.ui.screens.mixer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parspodcast.studio.data.audio.*
import com.parspodcast.studio.data.local.entities.AudioClipEntity
import com.parspodcast.studio.data.local.entities.AudioTrackEntity
import com.parspodcast.studio.data.local.entities.ProductionProjectEntity
import com.parspodcast.studio.data.repository.ProductionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

data class AudioMixerUiState(
    val currentProjectId: String = "",
    val projectTitle: String = "پادکست بررسی تحولات هوش مصنوعی",
    val tracks: List<AudioTrackEntity> = emptyList(),
    val clips: List<AudioClipEntity> = emptyList(),
    val selectedClipId: String? = null,
    val playbackState: MultitrackPlaybackState = MultitrackPlaybackState(),
    val zoomLevel: Float = 1.0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isRenderingExport: Boolean = false,
    val renderProgress: Float = 0f,
    val exportedFilePath: String? = null,
    val statusMessage: String? = null,
    val waveformCache: Map<String, FloatArray> = emptyMap()
) {
    val totalDurationMs: Long
        get() = MultitrackAudioMixer.calculateTimelineDurationMs(clips).coerceAtLeast(10000L)

    val selectedClip: AudioClipEntity?
        get() = clips.firstOrNull { it.id == selectedClipId }
}

@HiltViewModel
class AudioMixerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productionRepository: ProductionRepository,
    val playbackPlayer: SynchronizedMultitrackPlayer
) : ViewModel() {

    private val historyManager = TimelineHistoryManager()
    private val _uiState = MutableStateFlow(AudioMixerUiState())
    val uiState: StateFlow<AudioMixerUiState> = _uiState.asStateFlow()

    init {
        // Collect playback state
        viewModelScope.launch {
            playbackPlayer.playbackState.collect { playState ->
                _uiState.update { it.copy(playbackState = playState) }
            }
        }

        // Load or initialize default project
        loadOrCreateInitialProject()
    }

    private fun loadOrCreateInitialProject() {
        viewModelScope.launch {
            productionRepository.getAllProjects().collect { projects ->
                val activeProj = projects.firstOrNull() ?: run {
                    val defaultProj = ProductionProjectEntity(
                        id = "proj_${System.currentTimeMillis()}",
                        title = "پادکست اختصاصی پارس‌پادکست",
                        programType = "TWO_PRESENTERS",
                        jalaliDateStr = "۱۴۰۵/۰۸/۲۲",
                        createdAtUtc = System.currentTimeMillis(),
                        updatedAtUtc = System.currentTimeMillis(),
                        status = "MIXED"
                    )
                    productionRepository.insertProject(defaultProj)
                    defaultProj
                }

                _uiState.update {
                    it.copy(
                        currentProjectId = activeProj.id,
                        projectTitle = activeProj.title
                    )
                }

                loadTracksAndClips(activeProj.id)
            }
        }
    }

    private fun loadTracksAndClips(projectId: String) {
        viewModelScope.launch {
            // Collect tracks
            productionRepository.getTracksForProject(projectId).collect { trackList ->
                if (trackList.isEmpty()) {
                    // Create default tracks: Intro, Presenter A, Presenter B, BGM, SFX, Outro
                    createDefaultTracks(projectId)
                } else {
                    _uiState.update { it.copy(tracks = trackList) }
                }
            }
        }

        viewModelScope.launch {
            // Collect clips
            productionRepository.getAllClipsForProject(projectId).collect { clipList ->
                _uiState.update { it.copy(clips = clipList) }
                loadWaveformsForClips(clipList)
                updatePreviewIfReady()
            }
        }
    }

    private suspend fun createDefaultTracks(projectId: String) {
        val defaultTracks = listOf(
            AudioTrackEntity("tr_${projectId}_intro", projectId, "INTRO", "آرم و اینترو", 0.9f, trackOrder = 0),
            AudioTrackEntity("tr_${projectId}_host_a", projectId, "PRESENTER_A", "مجری الف (گوینده خبر)", 1.0f, trackOrder = 1),
            AudioTrackEntity("tr_${projectId}_host_b", projectId, "PRESENTER_B", "مجری ب (کارشناس تحلیل)", 1.0f, trackOrder = 2),
            AudioTrackEntity("tr_${projectId}_bgm", projectId, "BGM", "موسیقی پس‌زمینه (داکینگ)", 0.35f, duckingEnabled = true, trackOrder = 3),
            AudioTrackEntity("tr_${projectId}_sfx", projectId, "SFX", "افکت‌ها و ترنزیشن‌ها", 0.8f, trackOrder = 4),
            AudioTrackEntity("tr_${projectId}_outro", projectId, "OUTRO", "اوترو و تیتراژ", 0.9f, trackOrder = 5)
        )
        productionRepository.insertTracks(defaultTracks)
        autoPopulateVoiceSegmentsFromPhase6(projectId)
    }

    /**
     * Automatically discovers voice segments generated in Phase 6 and places them on timeline tracks.
     */
    fun autoPopulateVoiceSegmentsFromPhase6(projectId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val script = productionRepository.getLatestScriptForProject(projectId) ?: return@launch
            val segments = productionRepository.getSegmentsForScript(script.id).firstOrNull() ?: return@launch

            val tracks = productionRepository.getTracksForProjectSync(projectId)
            val hostATrack = tracks.firstOrNull { it.trackType == "PRESENTER_A" } ?: return@launch
            val hostBTrack = tracks.firstOrNull { it.trackType == "PRESENTER_B" } ?: hostATrack

            val newClips = mutableListOf<AudioClipEntity>()
            var currentTimelineMs = 3000L // 3s after intro

            for (seg in segments) {
                val audioPath = seg.generatedAudioPath ?: continue
                val file = File(audioPath)
                if (!file.exists()) continue

                val decoded = WavFileReader.readWavFile(file)
                val durationMs = decoded?.durationMs ?: (seg.estimatedDurationSec * 1000L)
                val targetTrackId = if (seg.speakerRole == "PRESENTER_B") hostBTrack.id else hostATrack.id

                newClips.add(
                    AudioClipEntity(
                        id = "clip_voice_${seg.id}",
                        trackId = targetTrackId,
                        filePath = file.absolutePath,
                        displayName = "بخش ${seg.segmentIndex}: ${seg.speakerRole}",
                        startTimeMs = currentTimelineMs,
                        durationMs = durationMs,
                        sourceStartMs = 0L,
                        sourceEndMs = durationMs,
                        fadeInMs = 50L,
                        fadeOutMs = 50L,
                        gainDb = 0f,
                        isMuted = false
                    )
                )

                currentTimelineMs += durationMs + 400L // 400ms inter-segment natural pause
            }

            if (newClips.isNotEmpty()) {
                productionRepository.insertClips(newClips)
                _uiState.update { it.copy(statusMessage = "${newClips.size} قطعه صوتی از فاز ۶ در تایم‌لاین قرار گرفت.") }
            }
        }
    }

    private fun loadWaveformsForClips(clips: List<AudioClipEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val cache = _uiState.value.waveformCache.toMutableMap()
            for (clip in clips) {
                if (!cache.containsKey(clip.id)) {
                    val peaks = WaveformExtractor.getWaveformPeaks(File(clip.filePath))
                    cache[clip.id] = peaks
                }
            }
            _uiState.update { it.copy(waveformCache = cache) }
        }
    }

    private fun saveHistorySnapshot(description: String) {
        historyManager.pushSnapshot(
            currentTracks = _uiState.value.tracks,
            currentClips = _uiState.value.clips,
            description = description
        )
        _uiState.update { it.copy(canUndo = historyManager.canUndo, canRedo = historyManager.canRedo) }
    }

    // --- Timeline Clip Operations ---

    fun selectClip(clipId: String?) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    fun moveClip(clipId: String, newStartTimeMs: Long, targetTrackId: String? = null) {
        val clip = _uiState.value.clips.firstOrNull { it.id == clipId } ?: return
        saveHistorySnapshot("انتقال کلیپ")

        val updated = clip.copy(
            startTimeMs = newStartTimeMs.coerceAtLeast(0L),
            trackId = targetTrackId ?: clip.trackId
        )
        viewModelScope.launch {
            productionRepository.updateClip(updated)
            updatePreviewIfReady()
        }
    }

    fun trimClip(clipId: String, newSourceStartMs: Long, newSourceEndMs: Long) {
        val clip = _uiState.value.clips.firstOrNull { it.id == clipId } ?: return
        saveHistorySnapshot("برش و اصلاح کلیپ")

        val newDuration = (newSourceEndMs - newSourceStartMs).coerceAtLeast(200L)
        val updated = clip.copy(
            sourceStartMs = newSourceStartMs.coerceAtLeast(0L),
            sourceEndMs = newSourceEndMs,
            durationMs = newDuration
        )
        viewModelScope.launch {
            productionRepository.updateClip(updated)
            updatePreviewIfReady()
        }
    }

    fun splitClipAtPlayhead(clipId: String, playheadMs: Long) {
        val clip = _uiState.value.clips.firstOrNull { it.id == clipId } ?: return
        if (playheadMs <= clip.startTimeMs || playheadMs >= clip.startTimeMs + clip.durationMs) return
        saveHistorySnapshot("تقسیم کلیپ در موقعیت نشانگر")

        val splitOffset = playheadMs - clip.startTimeMs
        val firstClip = clip.copy(
            sourceEndMs = clip.sourceStartMs + splitOffset,
            durationMs = splitOffset
        )
        val secondClip = clip.copy(
            id = "clip_${UUID.randomUUID()}",
            startTimeMs = playheadMs,
            sourceStartMs = clip.sourceStartMs + splitOffset,
            durationMs = clip.durationMs - splitOffset
        )

        viewModelScope.launch {
            productionRepository.updateClip(firstClip)
            productionRepository.insertClip(secondClip)
            updatePreviewIfReady()
        }
    }

    fun duplicateClip(clipId: String) {
        val clip = _uiState.value.clips.firstOrNull { it.id == clipId } ?: return
        saveHistorySnapshot("تکثیر کلیپ")

        val newClip = clip.copy(
            id = "clip_${UUID.randomUUID()}",
            startTimeMs = clip.startTimeMs + clip.durationMs + 200L,
            displayName = "${clip.displayName} (کپی)"
        )
        viewModelScope.launch {
            productionRepository.insertClip(newClip)
            updatePreviewIfReady()
        }
    }

    fun deleteClip(clipId: String) {
        saveHistorySnapshot("حذف کلیپ")
        viewModelScope.launch {
            productionRepository.deleteClipById(clipId)
            _uiState.update { it.copy(selectedClipId = null) }
            updatePreviewIfReady()
        }
    }

    fun updateClipGainAndFades(clipId: String, gainDb: Float, fadeInMs: Long, fadeOutMs: Long) {
        val clip = _uiState.value.clips.firstOrNull { it.id == clipId } ?: return
        saveHistorySnapshot("تنظیم ولوم و فید")

        val updated = clip.copy(
            gainDb = gainDb,
            fadeInMs = fadeInMs,
            fadeOutMs = fadeOutMs
        )
        viewModelScope.launch {
            productionRepository.updateClip(updated)
            updatePreviewIfReady()
        }
    }

    // --- Track Controls ---

    fun updateTrackVolume(trackId: String, volume: Float) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        val updated = track.copy(volume = volume.coerceIn(0f, 1.5f))
        viewModelScope.launch {
            productionRepository.updateTrack(updated)
            updatePreviewIfReady()
        }
    }

    fun updateTrackPan(trackId: String, pan: Float) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        val updated = track.copy(pan = pan.coerceIn(-1.0f, 1.0f))
        viewModelScope.launch {
            productionRepository.updateTrack(updated)
            updatePreviewIfReady()
        }
    }

    fun toggleTrackMute(trackId: String) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        val updated = track.copy(isMuted = !track.isMuted)
        viewModelScope.launch {
            productionRepository.updateTrack(updated)
            updatePreviewIfReady()
        }
    }

    fun toggleTrackSolo(trackId: String) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        val updated = track.copy(isSolo = !track.isSolo)
        viewModelScope.launch {
            productionRepository.updateTrack(updated)
            updatePreviewIfReady()
        }
    }

    fun toggleTrackDucking(trackId: String) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        val updated = track.copy(duckingEnabled = !track.duckingEnabled)
        viewModelScope.launch {
            productionRepository.updateTrack(updated)
            updatePreviewIfReady()
        }
    }

    // --- SAF Music & SFX Import ---

    fun importLocalAudio(uri: Uri, targetTrackType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "imported_audio.wav"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx != -1) {
                        fileName = cursor.getString(nameIdx) ?: fileName
                    }
                }

                val targetDir = File(context.filesDir, "audio/imported").apply { mkdirs() }
                val destFile = File(targetDir, "${System.currentTimeMillis()}_$fileName")

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val decoded = WavFileReader.readWavFile(destFile)
                val durationMs = decoded?.durationMs ?: 15000L

                val targetTrack = _uiState.value.tracks.firstOrNull { it.trackType == targetTrackType }
                    ?: _uiState.value.tracks.firstOrNull() ?: return@launch

                val newClip = AudioClipEntity(
                    id = "clip_${UUID.randomUUID()}",
                    trackId = targetTrack.id,
                    filePath = destFile.absolutePath,
                    displayName = fileName.substringBeforeLast("."),
                    startTimeMs = 0L,
                    durationMs = durationMs,
                    sourceStartMs = 0L,
                    sourceEndMs = durationMs
                )

                productionRepository.insertClip(newClip)
                _uiState.update { it.copy(statusMessage = "فایل «$fileName» به تایم‌لاین اضافه شد.") }
                updatePreviewIfReady()
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "خطا در واردسازی فایل صوتی: ${e.message}") }
            }
        }
    }

    // --- Undo / Redo ---

    fun undo() {
        val snapshot = historyManager.undo(_uiState.value.tracks, _uiState.value.clips) ?: return
        viewModelScope.launch {
            productionRepository.insertTracks(snapshot.tracks)
            productionRepository.insertClips(snapshot.clips)
            _uiState.update {
                it.copy(
                    tracks = snapshot.tracks,
                    clips = snapshot.clips,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo,
                    statusMessage = "بازگردانی: ${snapshot.description}"
                )
            }
            updatePreviewIfReady()
        }
    }

    fun redo() {
        val snapshot = historyManager.redo(_uiState.value.tracks, _uiState.value.clips) ?: return
        viewModelScope.launch {
            productionRepository.insertTracks(snapshot.tracks)
            productionRepository.insertClips(snapshot.clips)
            _uiState.update {
                it.copy(
                    tracks = snapshot.tracks,
                    clips = snapshot.clips,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo,
                    statusMessage = "تکرار: ${snapshot.description}"
                )
            }
            updatePreviewIfReady()
        }
    }

    // --- Playback ---

    fun togglePlayback() {
        if (_uiState.value.playbackState.isPlaying) {
            playbackPlayer.pause()
        } else {
            playbackPlayer.play()
        }
    }

    fun seekPlayback(positionMs: Long) {
        playbackPlayer.seekTo(positionMs)
    }

    fun stopPlayback() {
        playbackPlayer.stop()
    }

    fun setZoom(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom.coerceIn(0.5f, 3.0f)) }
    }

    private fun updatePreviewIfReady() {
        if (_uiState.value.clips.isEmpty()) return
        playbackPlayer.preparePreview(
            tracks = _uiState.value.tracks,
            clips = _uiState.value.clips,
            targetDurationMs = _uiState.value.totalDurationMs
        )
    }

    // --- Final Podcast Render & Export ---

    fun renderAndExportProject(outputFileName: String = "podcast_episode_final.wav") {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRenderingExport = true, renderProgress = 0f, statusMessage = "در حال میکس و رندر ۴۸ کیلوهرتز...") }

            val exportDir = File(context.filesDir, "audio/exports").apply { mkdirs() }
            val outputFile = File(exportDir, outputFileName)

            val result = MultitrackAudioMixer.renderToWavFile(
                tracks = _uiState.value.tracks,
                clips = _uiState.value.clips,
                outputFile = outputFile,
                targetSampleRate = MultitrackAudioMixer.MIX_SAMPLE_RATE,
                masterVolume = _uiState.value.playbackState.masterVolume
            ) { progress ->
                _uiState.update { it.copy(renderProgress = progress) }
            }

            if (result.isSuccess) {
                val file = result.getOrNull()
                // Update project with final audio path in Room
                val currentProj = productionRepository.getProjectById(_uiState.value.currentProjectId)
                currentProj?.let {
                    productionRepository.insertProject(it.copy(finalAudioPath = file?.absolutePath, status = "EXPORTED"))
                }

                _uiState.update {
                    it.copy(
                        isRenderingExport = false,
                        exportedFilePath = file?.absolutePath,
                        statusMessage = "پادکست با موفقیت رندر شد: ${file?.name} (${file?.length()?.div(1024)} کیلوبایت)"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isRenderingExport = false,
                        statusMessage = "خطا در رندر خروجی: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun dismissExportResult() {
        _uiState.update { it.copy(exportedFilePath = null) }
    }

    override fun onCleared() {
        super.onCleared()
        playbackPlayer.release()
    }
}
