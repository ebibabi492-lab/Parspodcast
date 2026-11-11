package com.parspodcast.studio.data.manager

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.parspodcast.studio.data.local.dao.NewsDao
import com.parspodcast.studio.domain.news.NewsInputType
import com.parspodcast.studio.domain.news.OfflineTextNewsParser
import com.parspodcast.studio.domain.news.ParsedNewsDraft
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineNewsImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val newsDao: NewsDao
) {

    companion object {
        const val MAX_FILE_SIZE_BYTES = 15_000_000L // 15MB limit for text files
    }

    suspend fun parseAndAnalyzeFile(uri: Uri): Result<List<ParsedNewsDraft>> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Extract metadata and file size
            var fileName = "imported_news.txt"
            var fileSize = -1L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return@withContext Result.failure(IllegalArgumentException("حجم فایل بیش از حد مجاز (۱۵ مگابایت) است."))
            }

            val inputType = when {
                fileName.endsWith(".md", ignoreCase = true) || fileName.endsWith(".markdown", ignoreCase = true) -> NewsInputType.MARKDOWN
                else -> NewsInputType.TXT
            }

            val defaultSource = fileName.substringBeforeLast(".")

            // 2. Open stream and parse
            val drafts = contentResolver.openInputStream(uri)?.use { stream ->
                OfflineTextNewsParser.parseStream(
                    inputStream = stream,
                    defaultSourceName = defaultSource,
                    inputType = inputType
                )
            } ?: return@withContext Result.failure(IOException("امکان باز کردن فایل ورودی وجود ندارد."))

            if (drafts.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("هیچ خبری در فایل انتخاب‌شده شناسایی نشد."))
            }

            // 3. Deduplication check against Room database
            for (draft in drafts) {
                val existingByHash = newsDao.getArticleByHash(draft.contentHash)
                if (existingByHash != null) {
                    draft.isDuplicate = true
                    draft.duplicateReason = "محتوای یکسان در پایگاه داده وجود دارد"
                    draft.isSelected = false // uncheck duplicates by default to prevent clutter
                    continue
                }

                val existingByTitle = newsDao.getArticleByTitleAndSource(draft.title, draft.sourceName)
                if (existingByTitle != null) {
                    draft.isDuplicate = true
                    draft.duplicateReason = "عنوان و منبع مشابه قبلاً ثبت شده است"
                    draft.isSelected = false
                }
            }

            Result.success(drafts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
