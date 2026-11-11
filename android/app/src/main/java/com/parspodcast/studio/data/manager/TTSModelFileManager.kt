package com.parspodcast.studio.data.manager

import android.content.Context
import android.net.Uri
import com.parspodcast.studio.domain.tts.TTSModelStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TTSModelFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MODEL_SUBDIR = "models/tts/mana"
        const val ONNX_MODEL_FILENAME = "fa_IR-mana-medium.onnx"
        const val ONNX_CONFIG_FILENAME = "fa_IR-mana-medium.onnx.json"
        const val TOKENS_FILENAME = "tokens.txt"
        const val DEFAULT_SAMPLE_RATE = 22050
        private const val MIN_ONNX_SIZE_BYTES = 10_000_000L // ~10MB sanity check (actual is ~125MB)
    }

    private val modelDir: File
        get() = File(context.filesDir, MODEL_SUBDIR).apply { mkdirs() }

    val onnxModelFile: File
        get() = File(modelDir, ONNX_MODEL_FILENAME)

    val onnxConfigFile: File
        get() = File(modelDir, ONNX_CONFIG_FILENAME)

    val tokensFile: File
        get() = File(modelDir, TOKENS_FILENAME)

    suspend fun getModelStatus(): TTSModelStatus = withContext(Dispatchers.IO) {
        val onnxExists = onnxModelFile.exists() && onnxModelFile.length() > MIN_ONNX_SIZE_BYTES
        val jsonExists = onnxConfigFile.exists() && onnxConfigFile.length() > 0
        val tokensExist = tokensFile.exists() && tokensFile.length() > 0

        var detectedSampleRate = DEFAULT_SAMPLE_RATE
        if (jsonExists) {
            try {
                val jsonStr = onnxConfigFile.readText(Charsets.UTF_8)
                val root = JSONObject(jsonStr)
                if (root.has("audio")) {
                    val audio = root.getJSONObject("audio")
                    if (audio.has("sample_rate")) {
                        detectedSampleRate = audio.getInt("sample_rate")
                    }
                }
            } catch (e: Exception) {
                // Keep default
            }
        }

        TTSModelStatus(
            modelName = "Mana Persian Piper (fa_IR-mana-medium)",
            isOnnxInstalled = onnxExists,
            isJsonInstalled = jsonExists,
            isTokensGenerated = tokensExist,
            onnxSizeBytes = if (onnxExists) onnxModelFile.length() else 0L,
            onnxFilePath = if (onnxExists) onnxModelFile.absolutePath else null,
            tokensFilePath = if (tokensExist) tokensFile.absolutePath else null,
            sampleRate = detectedSampleRate,
            isLoadedInRam = false
        )
    }

    /**
     * Imports the ONNX binary model file via chunked 1MB streaming.
     */
    suspend fun importOnnxModel(
        sourceUri: Uri,
        onProgress: (bytesCopied: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val fileSize = contentResolver.openFileDescriptor(sourceUri, "r")?.use {
                it.statSize
            } ?: -1L

            val tempFile = File(modelDir, "$ONNX_MODEL_FILENAME.tmp")
            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(1024 * 1024) // 1MB buffer to prevent heap pressure
                    var bytesCopied = 0L
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesCopied += read
                        onProgress(bytesCopied, fileSize)
                    }
                    outputStream.flush()
                }
            } ?: return@withContext Result.failure(IllegalStateException("امکان باز کردن فایل ورودی وجود ندارد"))

            if (tempFile.length() < MIN_ONNX_SIZE_BYTES) {
                tempFile.delete()
                return@withContext Result.failure(IllegalArgumentException("حجم فایل مدل کمتر از حد استاندارد است"))
            }

            if (onnxModelFile.exists()) {
                onnxModelFile.delete()
            }
            tempFile.renameTo(onnxModelFile)

            Result.success(onnxModelFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports the JSON configuration file and generates tokens.txt for Sherpa-ONNX.
     */
    suspend fun importJsonConfig(sourceUri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val content = contentResolver.openInputStream(sourceUri)?.use {
                it.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext Result.failure(IllegalStateException("امکان خواندن فایل کانفیگ JSON وجود ندارد"))

            // Validate that this is a valid Piper JSON config
            val json = JSONObject(content)
            if (!json.has("phoneme_id_map")) {
                return@withContext Result.failure(IllegalArgumentException("فایل انتخاب شده فاقد بخش phoneme_id_map استاندارد مدل پایپر است"))
            }

            // Save JSON file
            onnxConfigFile.writeText(content, Charsets.UTF_8)

            // Extract tokens.txt from phoneme_id_map for Sherpa-ONNX
            extractTokensFromJson(json, tokensFile)

            Result.success(onnxConfigFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses the Piper JSON phoneme_id_map and writes tokens.txt
     * format: "<token> <id>" per line.
     */
    fun extractTokensFromJson(configJson: JSONObject, targetTokensFile: File) {
        val phonemeMap = configJson.getJSONObject("phoneme_id_map")
        val lines = mutableListOf<String>()

        val keys = phonemeMap.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val ids = phonemeMap.getJSONArray(key)
            if (ids.length() > 0) {
                val id = ids.getInt(0)
                lines.add("$key $id")
            }
        }

        targetTokensFile.writeText(lines.joinToString("\n"), Charsets.UTF_8)
    }

    suspend fun deleteModel(): Boolean = withContext(Dispatchers.IO) {
        var success = true
        if (onnxModelFile.exists()) success = onnxModelFile.delete() && success
        if (onnxConfigFile.exists()) success = onnxConfigFile.delete() && success
        if (tokensFile.exists()) success = tokensFile.delete() && success
        success
    }
}
