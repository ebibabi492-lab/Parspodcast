package com.parspodcast.studio.data.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.parspodcast.studio.data.local.entities.AudioClipEntity
import com.parspodcast.studio.data.local.entities.AudioTrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class MultitrackPlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val masterVolume: Float = 1.0f
) {
    val progress: Float
        get() = if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
}

@Singleton
class SynchronizedMultitrackPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressPollingJob: Job? = null
    private var prepareJob: Job? = null

    private val previewCacheFile: File
        get() = File(context.cacheDir, "preview_multitrack.wav")

    private val _playbackState = MutableStateFlow(MultitrackPlaybackState())
    val playbackState: StateFlow<MultitrackPlaybackState> = _playbackState.asStateFlow()

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also { player ->
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.update {
                        it.copy(
                            isPlaying = isPlaying,
                            totalDurationMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                    if (isPlaying) startProgressPolling() else stopProgressPolling()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        _playbackState.update {
                            it.copy(isPlaying = false, currentPositionMs = 0L)
                        }
                        stopProgressPolling()
                    }
                }
            })
            exoPlayer = player
        }
    }

    /**
     * Prepares the synchronized multitrack preview buffer in the background.
     */
    fun preparePreview(
        tracks: List<AudioTrackEntity>,
        clips: List<AudioClipEntity>,
        targetDurationMs: Long,
        onReady: (() -> Unit)? = null
    ) {
        prepareJob?.cancel()
        _playbackState.update {
            it.copy(
                isBuffering = true,
                totalDurationMs = targetDurationMs
            )
        }

        prepareJob = scope.launch(Dispatchers.IO) {
            val renderResult = MultitrackAudioMixer.renderToWavFile(
                tracks = tracks,
                clips = clips,
                outputFile = previewCacheFile,
                masterVolume = _playbackState.value.masterVolume
            )

            withContext(Dispatchers.Main) {
                _playbackState.update { it.copy(isBuffering = false) }
                if (renderResult.isSuccess) {
                    val player = getOrCreatePlayer()
                    val wasPlaying = player.isPlaying
                    val currentPos = player.currentPosition

                    player.setMediaItem(MediaItem.fromUri(Uri.fromFile(previewCacheFile)))
                    player.prepare()
                    if (currentPos > 0) player.seekTo(currentPos)
                    if (wasPlaying) player.play()

                    onReady?.invoke()
                }
            }
        }
    }

    fun play() {
        val player = getOrCreatePlayer()
        player.play()
        _playbackState.update { it.copy(isPlaying = true) }
        startProgressPolling()
    }

    fun pause() {
        exoPlayer?.pause()
        _playbackState.update { it.copy(isPlaying = false) }
        stopProgressPolling()
    }

    fun stop() {
        exoPlayer?.stop()
        _playbackState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
        stopProgressPolling()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun setMasterVolume(volume: Float) {
        _playbackState.update { it.copy(masterVolume = volume.coerceIn(0f, 1.5f)) }
        exoPlayer?.volume = volume.coerceIn(0f, 1.0f)
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = scope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                exoPlayer?.let { player ->
                    _playbackState.update {
                        it.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            totalDurationMs = player.duration.coerceAtLeast(it.totalDurationMs)
                        )
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = null
    }

    fun release() {
        prepareJob?.cancel()
        stopProgressPolling()
        exoPlayer?.release()
        exoPlayer = null
        if (previewCacheFile.exists()) {
            previewCacheFile.delete()
        }
        _playbackState.update { MultitrackPlaybackState() }
    }
}
