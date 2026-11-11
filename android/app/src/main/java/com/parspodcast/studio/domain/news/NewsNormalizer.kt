package com.parspodcast.studio.domain.news

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.security.MessageDigest

object NewsNormalizer {

    private val ARABIC_TO_PERSIAN_MAP = mapOf(
        'ي' to 'ی',
        'ك' to 'ک',
        'ة' to 'ه',
        'ۀ' to 'ه',
        'ؤ' to 'و',
        'إ' to 'ا',
        'أ' to 'ا',
        'ء' to ''
    )

    fun normalizeText(rawText: String): String {
        if (rawText.isBlank()) return ""

        // 1. Map Arabic characters to standard Persian
        val sb = StringBuilder()
        for (char in rawText) {
            sb.append(ARABIC_TO_PERSIAN_MAP[char] ?: char)
        }
        var text = sb.toString()

        // 2. Remove kashida (tatweel) and harakat
        text = text.replace("ـ", "")
        text = text.replace(Regex("[\u064B-\u065F]"), "")

        // 3. Normalize digits to Persian standard for consistency
        text = toStandardPersianDigits(text)

        // 4. Normalize multiple whitespaces and trim lines
        val lines = text.lines().map { it.trim().replace(Regex("[ \\t]+"), " ") }
        text = lines.filter { it.isNotEmpty() }.joinToString("\n\n")

        return text.trim()
    }

    fun cleanHtml(rawHtml: String): String {
        if (rawHtml.isBlank()) return ""
        return try {
            // Safe HTML cleaning using Jsoup: strip all scripts, styles, dangerous tags
            val cleaned = Jsoup.clean(rawHtml, Safelist.none())
            // Decode any remaining HTML entities
            Jsoup.parse(cleaned).text()
        } catch (e: Exception) {
            // Fallback manual entity replacement if Jsoup encounters malformed input
            rawHtml.replace(Regex("<[^>]*>"), " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }

    fun toStandardPersianDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> sb.append(persianDigits[ch - '0'])
                in arabicDigits -> {
                    val idx = arabicDigits.indexOf(ch)
                    if (idx != -1) sb.append(persianDigits[idx]) else sb.append(ch)
                }
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toAsciiDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> sb.append('0' + (ch - '۰'))
                in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Computes a stable SHA-256 hash from normalized title and content.
     */
    fun computeContentHash(title: String, content: String): String {
        val normTitle = normalizeText(title).lowercase().replace("\\s+".toRegex(), "")
        val normBody = normalizeText(content).lowercase().replace("\\s+".toRegex(), "").take(500)
        val combined = "$normTitle|$normBody"

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculates Jaccard character similarity between two titles for near-duplicate detection.
     */
    fun calculateTitleSimilarity(titleA: String, titleB: String): Float {
        val wordsA = normalizeText(titleA).split("\\s+".toRegex()).filter { it.length > 2 }.toSet()
        val wordsB = normalizeText(titleB).split("\\s+".toRegex()).filter { it.length > 2 }.toSet()

        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0f

        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size

        return if (union > 0) intersection.toFloat() / union else 0f
    }
}
