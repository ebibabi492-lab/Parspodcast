package com.parspodcast.studio.data.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ArtworkValidationResult(
    val isValid: Boolean,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val savedPath: String?,
    val warnings: List<String> = emptyList(),
    val error: String? = null
)

@Singleton
class PodcastArtworkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val artworkDir: File by lazy {
        File(context.filesDir, "podcasts/artwork").apply {
            if (!exists()) mkdirs()
        }
    }

    fun processAndSaveArtwork(uri: Uri): ArtworkValidationResult {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return ArtworkValidationResult(
                    isValid = false,
                    width = 0,
                    height = 0,
                    fileSizeBytes = 0,
                    savedPath = null,
                    error = "عدم امکان خواندن فایل تصویر انتخابی"
                )
            }

            // Decode bounds first
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val rawWidth = options.outWidth
            val rawHeight = options.outHeight

            if (rawWidth <= 0 || rawHeight <= 0) {
                return ArtworkValidationResult(
                    isValid = false,
                    width = 0,
                    height = 0,
                    fileSizeBytes = 0,
                    savedPath = null,
                    error = "فایل انتخاب شده تصویر معتبر نیست"
                )
            }

            val warnings = mutableListOf<String>()
            val isSquare = Math.abs(rawWidth - rawHeight) <= (Math.max(rawWidth, rawHeight) * 0.03) // 3% tolerance
            if (!isSquare) {
                warnings.add("تصویر اصلی مربعی نبوده و به صورت خودکار به نسبت ۱:۱ برش داده شد.")
            }

            if (rawWidth < 1400 || rawHeight < 1400) {
                warnings.add("ابعاد تصویر کمتر از استاندارد دایرکتوری‌های پادکست (۱۴۰۰×۱۴۰۰) است و به ابعاد استاندارد افزایش یافت.")
            } else if (rawWidth > 3000 || rawHeight > 3000) {
                warnings.add("ابعاد تصویر بیشتر از ۳۰۰۰×۳۰۰۰ بوده و برای بهینه‌سازی بارگذاری کاهش داده شد.")
            }

            // Decode actual bitmap with subsampling if huge
            val decodeStream = contentResolver.openInputStream(uri)
            val fullBitmap = BitmapFactory.decodeStream(decodeStream)
            decodeStream?.close()

            if (fullBitmap == null) {
                return ArtworkValidationResult(
                    isValid = false,
                    width = rawWidth,
                    height = rawHeight,
                    fileSizeBytes = 0,
                    savedPath = null,
                    error = "خطا در رمزگشایی تصویر"
                )
            }

            // Crop to center square
            val minEdge = Math.min(fullBitmap.width, fullBitmap.height)
            val startX = (fullBitmap.width - minEdge) / 2
            val startY = (fullBitmap.height - minEdge) / 2
            val cropped = Bitmap.createBitmap(fullBitmap, startX, startY, minEdge, minEdge)

            // Scale to target dimension (between 1400 and 3000, target 1400 if small, or keep up to 3000)
            val targetSize = when {
                minEdge < 1400 -> 1400
                minEdge > 3000 -> 3000
                else -> minEdge
            }

            val finalBitmap = if (minEdge != targetSize) {
                Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
            } else {
                cropped
            }

            // Save to private storage
            val outputFileName = "artwork_${UUID.randomUUID()}.jpg"
            val outputFile = File(artworkDir, outputFileName)
            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Cleanup bitmaps
            if (finalBitmap != cropped && !cropped.isRecycled) cropped.recycle()
            if (fullBitmap != cropped && !fullBitmap.isRecycled) fullBitmap.recycle()
            if (!finalBitmap.isRecycled) finalBitmap.recycle()

            ArtworkValidationResult(
                isValid = true,
                width = targetSize,
                height = targetSize,
                fileSizeBytes = outputFile.length(),
                savedPath = outputFile.absolutePath,
                warnings = warnings
            )
        } catch (e: Exception) {
            ArtworkValidationResult(
                isValid = false,
                width = 0,
                height = 0,
                fileSizeBytes = 0,
                savedPath = null,
                error = "خطا در پردازش تصویر: ${e.message}"
            )
        }
    }

    fun getArtworkFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val f = File(path)
        return if (f.exists()) f else null
    }
}
