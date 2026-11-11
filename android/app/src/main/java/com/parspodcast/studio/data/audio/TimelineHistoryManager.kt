package com.parspodcast.studio.data.audio

import com.parspodcast.studio.data.local.entities.AudioClipEntity
import com.parspodcast.studio.data.local.entities.AudioTrackEntity

data class MultitrackSnapshot(
    val tracks: List<AudioTrackEntity>,
    val clips: List<AudioClipEntity>,
    val description: String = ""
)

class TimelineHistoryManager(private val maxHistoryDepth: Int = 25) {

    private val undoStack = ArrayDeque<MultitrackSnapshot>()
    private val redoStack = ArrayDeque<MultitrackSnapshot>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun pushSnapshot(
        currentTracks: List<AudioTrackEntity>,
        currentClips: List<AudioClipEntity>,
        description: String = ""
    ) {
        if (undoStack.size >= maxHistoryDepth) {
            undoStack.removeFirst()
        }
        undoStack.addLast(
            MultitrackSnapshot(
                tracks = currentTracks.map { it.copy() },
                clips = currentClips.map { it.copy() },
                description = description
            )
        )
        redoStack.clear()
    }

    fun undo(
        currentTracks: List<AudioTrackEntity>,
        currentClips: List<AudioClipEntity>
    ): MultitrackSnapshot? {
        if (undoStack.isEmpty()) return null

        val previous = undoStack.removeLast()
        redoStack.addLast(
            MultitrackSnapshot(
                tracks = currentTracks.map { it.copy() },
                clips = currentClips.map { it.copy() },
                description = "Before Undo"
            )
        )
        return previous
    }

    fun redo(
        currentTracks: List<AudioTrackEntity>,
        currentClips: List<AudioClipEntity>
    ): MultitrackSnapshot? {
        if (redoStack.isEmpty()) return null

        val next = redoStack.removeLast()
        undoStack.addLast(
            MultitrackSnapshot(
                tracks = currentTracks.map { it.copy() },
                clips = currentClips.map { it.copy() },
                description = "Before Redo"
            )
        )
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
