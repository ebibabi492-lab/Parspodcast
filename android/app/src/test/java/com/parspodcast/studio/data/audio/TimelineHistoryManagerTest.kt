package com.parspodcast.studio.data.audio

import com.parspodcast.studio.data.local.entities.AudioClipEntity
import com.parspodcast.studio.data.local.entities.AudioTrackEntity
import org.junit.Assert.*
import org.junit.Test

class TimelineHistoryManagerTest {

    @Test
    fun testUndoAndRedoLifecycle() {
        val history = TimelineHistoryManager(maxHistoryDepth = 10)
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)

        val track1 = AudioTrackEntity("t1", "p1", "INTRO", "Intro", 1.0f)
        val clip1 = AudioClipEntity("c1", "t1", startTimeMs = 0L, durationMs = 1000L, filePath = "")

        // Initial state saved before an edit
        history.pushSnapshot(listOf(track1), listOf(clip1), "Initial State")
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)

        // Simulate state after an edit (clip moved to 2000ms)
        val clipMoved = clip1.copy(startTimeMs = 2000L)

        // Perform undo
        val reverted = history.undo(listOf(track1), listOf(clipMoved))
        assertNotNull(reverted)
        assertEquals(0L, reverted?.clips?.first()?.startTimeMs)
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)

        // Perform redo
        val redone = history.redo(listOf(track1), listOf(clip1))
        assertNotNull(redone)
        assertEquals(2000L, redone?.clips?.first()?.startTimeMs)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun testHistoryDepthLimit() {
        val history = TimelineHistoryManager(maxHistoryDepth = 3)
        val track = AudioTrackEntity("t1", "p1", "BGM", "Music", 1.0f)

        history.pushSnapshot(listOf(track), emptyList(), "Step 1")
        history.pushSnapshot(listOf(track), emptyList(), "Step 2")
        history.pushSnapshot(listOf(track), emptyList(), "Step 3")
        history.pushSnapshot(listOf(track), emptyList(), "Step 4") // should drop Step 1

        var undoCount = 0
        while (history.canUndo) {
            history.undo(listOf(track), emptyList())
            undoCount++
        }

        assertEquals(3, undoCount)
    }
}
