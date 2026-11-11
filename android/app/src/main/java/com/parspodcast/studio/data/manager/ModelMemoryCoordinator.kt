package com.parspodcast.studio.data.manager

import android.app.ActivityManager
import android.content.Context
import com.parspodcast.studio.domain.ai.LocalLLMProvider
import com.parspodcast.studio.domain.tts.TTSProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ExecutionSlot {
    IDLE,
    LLM_INFERENCE,
    TTS_SYNTHESIS,
    AUDIO_RENDERING
}

enum class MemoryPressureLevel {
    NORMAL,
    MODERATE,
    CRITICAL
}

data class SystemMemoryStatus(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val thresholdMb: Long,
    val isLowMemory: Boolean,
    val pressureLevel: MemoryPressureLevel
)

@Singleton
class ModelMemoryCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localLlmProvider: LocalLLMProvider,
    private val ttsProvider: TTSProvider
) {
    private val slotMutex = Mutex()

    private val _currentSlot = MutableStateFlow(ExecutionSlot.IDLE)
    val currentSlot: StateFlow<ExecutionSlot> = _currentSlot.asStateFlow()

    fun getMemoryStatus(): SystemMemoryStatus {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)
        val threshMb = memInfo.threshold / (1024 * 1024)

        val pressure = when {
            memInfo.lowMemory || availMb < 1500 -> MemoryPressureLevel.CRITICAL
            availMb < 3000 -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.NORMAL
        }

        return SystemMemoryStatus(
            totalRamMb = totalMb,
            availableRamMb = availMb,
            thresholdMb = threshMb,
            isLowMemory = memInfo.lowMemory,
            pressureLevel = pressure
        )
    }

    suspend fun prepareForLlmInference(): Result<Unit> = withContext(Dispatchers.IO) {
        slotMutex.withLock {
            try {
                val mem = getMemoryStatus()
                // If memory is moderate/critical, unload TTS to give Gemma headroom
                if (mem.pressureLevel != MemoryPressureLevel.NORMAL) {
                    ttsProvider.unloadModel()
                }

                _currentSlot.value = ExecutionSlot.LLM_INFERENCE
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun prepareForTtsSynthesis(): Result<Unit> = withContext(Dispatchers.IO) {
        slotMutex.withLock {
            try {
                val mem = getMemoryStatus()
                // If available RAM is tight (< 2.5 GB), unload LLM session to avoid OOM
                if (mem.availableRamMb < 2500) {
                    localLlmProvider.unload()
                }

                _currentSlot.value = ExecutionSlot.TTS_SYNTHESIS
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun prepareForAudioRendering(): Result<Unit> = withContext(Dispatchers.IO) {
        slotMutex.withLock {
            _currentSlot.value = ExecutionSlot.AUDIO_RENDERING
            Result.success(Unit)
        }
    }

    suspend fun releaseSlot() = withContext(Dispatchers.IO) {
        slotMutex.withLock {
            _currentSlot.value = ExecutionSlot.IDLE
            System.gc()
        }
    }
}
