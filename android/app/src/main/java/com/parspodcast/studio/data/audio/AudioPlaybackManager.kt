package com.parspodcast.studio.data.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val currentAudioPath: String? = null,
    val activeSegmentId: String? = null
) {
    val progress: Float
        get() = if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
}

@Singleton
class AudioPlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressPollingJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also { player ->
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = isPlaying,
                        totalDurationMs = player.duration.coerceAtLeast(0L)
                    )
                    if (isPlaying) {
                        startProgressPolling()
                    } else {
                        stopProgressPolling()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            currentPositionMs = 0L
                        )
                        stopProgressPolling()
                    }
                }
            })
            exoPlayer = player
        }
    }

    fun playFile(file: File, segmentId: String? = null) {
        if (!file.exists()) return

        val player = getOrCreatePlayer()
        val mediaItem = MediaItem.fromUri(Uri.fromFile(file))

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        _playbackState.value = _playbackState.value.copy(
            currentAudioPath = file.absolutePath,
            activeSegmentId = segmentId,
            isPlaying = true
        )
    }

    fun pause() {
        exoPlayer?.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        stopProgressPolling()
    }

    fun resume() {
        exoPlayer?.play()
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
        startProgressPolling()
    }

    fun stop() {
        exoPlayer?.stop()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            currentPositionMs = 0L
        )
        stopProgressPolling()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = scope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                exoPlayer?.let { player ->
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                        totalDurationMs = player.duration.coerceAtLeast(0L)
                    )
                }
                delay(200)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = null
    }

    fun release() {
        stopProgressPolling()
        exoPlayer?.release()
        exoPlayer = null
        _playbackState.value = PlaybackState()
    }
}
