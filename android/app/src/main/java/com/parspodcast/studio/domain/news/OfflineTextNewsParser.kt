package com.parspodcast.studio.domain.news

import com.parspodcast.studio.domain.calendar.JalaliCalendar
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

object OfflineTextNewsParser {

    private val TITLE_TAGS = listOf("عنوان:", "تیتر:", "Title:", "TITLE:", "# ")
    private val SOURCE_TAGS = listOf("منبع:", "خبرگزاری:", "Source:", "SOURCE:")
    private val DATE_TAGS = listOf("تاریخ:", "زمان:", "Date:", "DATE:")
    private val CATEGORY_TAGS = listOf("دسته‌بندی:", "دسته:", "موضوع:", "Category:", "CATEGORY:")
    private val BODY_TAGS = listOf("متن:", "شرح:", "مشروح:", "Body:", "Content:", "BODY:")
    private val DELIMITER_REGEX = Regex("^(---|===|\\*\\*\\*|___|===+)$")

    /**
     * Parses an InputStream (TXT or Markdown) into a list of ParsedNewsDraft items.
     */
    fun parseStream(
        inputStream: InputStream,
        defaultSourceName: String = "فایل متنی محلی",
        inputType: NewsInputType = NewsInputType.TXT
    ): List<ParsedNewsDraft> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val blocks = splitIntoBlocks(reader)

        val drafts = mutableListOf<ParsedNewsDraft>()
        val defaultJalali = JalaliCalendar.formatShort(System.currentTimeMillis())

        for ((index, rawBlock) in blocks.withIndex()) {
            val trimmed = rawBlock.trim()
            if (trimmed.isBlank()) continue

            val draft = parseBlock(
                blockText = trimmed,
                index = index + 1,
                defaultSourceName = defaultSourceName,
                defaultJalali = defaultJalali,
                inputType = inputType
            )

            if (draft.title.isNotBlank() || draft.content.isNotBlank()) {
                // If title was missing, pick first line
                if (draft.title.isBlank() && draft.content.isNotBlank()) {
                    val firstLine = draft.content.lines().firstOrNull { it.isNotBlank() } ?: "خبر شماره ${index + 1}"
                    draft.title = firstLine.take(90).trim()
                }

                // If summary is empty, generate from first 200 chars of content
                if (draft.summary.isBlank()) {
                    draft.summary = draft.content.take(220).replace("\n", " ").trim()
                }

                // Compute stable content hash
                val hash = NewsNormalizer.computeContentHash(draft.title, draft.content)
                val finalized = draft.copy(contentHash = hash)

                drafts.add(finalized)
            }
        }

        return drafts
    }

    private fun splitIntoBlocks(reader: BufferedReader): List<String> {
        val blocks = mutableListOf<String>()
        var currentBlock = StringBuilder()
        var line: String?

        // Strip UTF-8 BOM if present on the first line
        var isFirstLine = true

        while (reader.readLine().also { line = it } != null) {
            var curLine = line ?: break
            if (isFirstLine) {
                curLine = curLine.removePrefix("\uFEFF")
                isFirstLine = false
            }

            val trimmedLine = curLine.trim()

            // Check if line is a major delimiter (---, ===, etc.)
            if (DELIMITER_REGEX.matches(trimmedLine)) {
                if (currentBlock.isNotBlank()) {
                    blocks.add(currentBlock.toString())
                    currentBlock = StringBuilder()
                }
                continue
            }

            // Check if a new structured item starts with "عنوان:" after content has already started
            val isTitleTag = TITLE_TAGS.any { trimmedLine.startsWith(it) }
            if (isTitleTag && currentBlock.contains("\n") && currentBlock.lines().any { BODY_TAGS.any { tag -> it.contains(tag) } }) {
                blocks.add(currentBlock.toString())
                currentBlock = StringBuilder()
            }

            currentBlock.append(curLine).append("\n")
        }

        if (currentBlock.isNotBlank()) {
            blocks.add(currentBlock.toString())
        }

        return blocks
    }

    private fun parseBlock(
        blockText: String,
        index: Int,
        defaultSourceName: String,
        defaultJalali: String,
        inputType: NewsInputType
    ): ParsedNewsDraft {
        var title = ""
        var source = defaultSourceName
        var dateStr = defaultJalali
        var category = "عمومی"
        var isReadingBody = false
        val bodyBuilder = StringBuilder()

        val lines = blockText.lines()

        for (rawLine in lines) {
            val line = rawLine.trim()

            if (isReadingBody) {
                bodyBuilder.append(rawLine).append("\n")
                continue
            }

            val matchedTitle = TITLE_TAGS.firstOrNull { line.startsWith(it) }
            val matchedSource = SOURCE_TAGS.firstOrNull { line.startsWith(it) }
            val matchedDate = DATE_TAGS.firstOrNull { line.startsWith(it) }
            val matchedCategory = CATEGORY_TAGS.firstOrNull { line.startsWith(it) }
            val matchedBody = BODY_TAGS.firstOrNull { line.startsWith(it) }

            when {
                matchedTitle != null -> {
                    title = line.removePrefix(matchedTitle).trim()
                }
                matchedSource != null -> {
                    source = line.removePrefix(matchedSource).trim()
                }
                matchedDate != null -> {
                    dateStr = line.removePrefix(matchedDate).trim()
                }
                matchedCategory != null -> {
                    category = normalizeCategory(line.removePrefix(matchedCategory).trim())
                }
                matchedBody != null -> {
                    isReadingBody = true
                    val remainder = line.removePrefix(matchedBody).trim()
                    if (remainder.isNotEmpty()) {
                        bodyBuilder.append(remainder).append("\n")
                    }
                }
                else -> {
                    // If no explicit tags exist, treat lines as free-flowing text content
                    bodyBuilder.append(rawLine).append("\n")
                }
            }
        }

        val content = NewsNormalizer.normalizeText(bodyBuilder.toString().trim())
        val normTitle = NewsNormalizer.normalizeText(title)

        return ParsedNewsDraft(
            tempId = "draft_${UUID.randomUUID()}",
            title = normTitle,
            summary = content.take(220).replace("\n", " ").trim(),
            content = content,
            sourceName = source.ifBlank { defaultSourceName },
            category = category,
            publishedAtUtc = System.currentTimeMillis(),
            jalaliDateStr = dateStr,
            inputType = inputType,
            contentHash = ""
        )
    }

    private fun normalizeCategory(rawCategory: String): String {
        val cat = rawCategory.trim()
        val standard = listOf("سیاسی", "اقتصادی", "فناوری", "علمی", "اجتماعی", "فرهنگی", "ورزشی", "بین‌الملل", "سلامت", "عمومی")
        return standard.firstOrNull { cat.contains(it) } ?: if (cat.isNotBlank()) cat else "عمومی"
    }
}
