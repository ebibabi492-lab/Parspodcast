package com.parspodcast.studio.data.manager

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.parspodcast.studio.domain.ai.DeviceCapabilityReport
import com.parspodcast.studio.domain.ai.ModelExecutionBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelFileManager @Inject constructor(
    private val context: Context
) {
    companion object {
        const val EXPECTED_MODEL_FILENAME = "gemma-4-E4B-it.litertlm"
        const val MODELS_DIR_NAME = "models"
        const val MIN_RECOMMENDED_RAM_MB = 4096L
        const val MIN_FILE_SIZE_BYTES = 500_000_000L // At least 500MB
    }

    private val modelsDirectory: File by lazy {
        File(context.filesDir, MODELS_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    fun getTargetModelFile(filename: String = EXPECTED_MODEL_FILENAME): File {
        return File(modelsDirectory, filename)
    }

    fun isModelInstalled(filename: String = EXPECTED_MODEL_FILENAME): Boolean {
        val file = getTargetModelFile(filename)
        return file.exists() && file.isFile && file.length() >= MIN_FILE_SIZE_BYTES
    }

    fun getInstalledModelSizeBytes(filename: String = EXPECTED_MODEL_FILENAME): Long {
        val file = getTargetModelFile(filename)
        return if (file.exists()) file.length() else 0L
    }

    suspend fun importModelFromUri(
        sourceUri: Uri,
        targetFilename: String = EXPECTED_MODEL_FILENAME,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // Determine total file size for progress reporting
            var totalBytes: Long = -1L
            contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex != -1) {
                    totalBytes = cursor.getLong(sizeIndex)
                }
            }

            // Temp file during copy to prevent incomplete/corrupted models
            val tempFile = File(modelsDirectory, "$targetFilename.tmp")
            val targetFile = File(modelsDirectory, targetFilename)

            if (tempFile.exists()) tempFile.delete()

            val inputStream: InputStream = contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalStateException("امکان باز کردن فایل ورودی وجود ندارد."))

            val outputStream = FileOutputStream(tempFile)

            // Streaming in 1MB chunks to ensure low RAM consumption on Daria Band
            val buffer = ByteArray(1024 * 1024)
            var bytesCopied: Long = 0L
            var read: Int

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (totalBytes > 0) {
                            val progress = (bytesCopied.toFloat() / totalBytes).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            // Validate copied file
            if (tempFile.length() < MIN_FILE_SIZE_BYTES) {
                tempFile.delete()
                return@withContext Result.failure(
                    IllegalArgumentException("حجم فایل مدل کمتر از حد استاندارد است (${tempFile.length()} بایت). لطفاً فایل اصلی gemma-4-E4B-it.litertlm را انتخاب کنید.")
                )
            }

            if (targetFile.exists()) targetFile.delete()
            val renameSuccess = tempFile.renameTo(targetFile)

            if (renameSuccess) {
                onProgress(1.0f)
                Result.success(targetFile)
            } else {
                Result.failure(IllegalStateException("خطا در نهایی‌سازی فایل مدل در حافظه اختصاصی."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteModel(filename: String = EXPECTED_MODEL_FILENAME): Boolean {
        val file = getTargetModelFile(filename)
        return if (file.exists()) file.delete() else true
    }

    fun probeDeviceCapabilities(): DeviceCapabilityReport {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)

        val statFs = StatFs(context.filesDir.absolutePath)
        val totalStorageMb = (statFs.blockCountLong * statFs.blockSizeLong) / (1024 * 1024)
        val availStorageMb = (statFs.availableBlocksLong * statFs.blockSizeLong) / (1024 * 1024)

        val isRamSufficient = totalRamMb >= MIN_RECOMMENDED_RAM_MB
        val isStorageSufficient = availStorageMb >= 5000L // 5GB free for 4.6GB model

        // Recommend GPU if RAM is ample, otherwise CPU fallback
        val recommendedBackend = if (isRamSufficient && !memInfo.lowMemory) {
            ModelExecutionBackend.GPU
        } else {
            ModelExecutionBackend.CPU
        }

        return DeviceCapabilityReport(
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            totalStorageMb = totalStorageMb,
            availableStorageMb = availStorageMb,
            isRamSufficientForE4B = isRamSufficient,
            isStorageSufficientForE4B = isStorageSufficient,
            recommendedBackend = recommendedBackend
        )
    }
}
