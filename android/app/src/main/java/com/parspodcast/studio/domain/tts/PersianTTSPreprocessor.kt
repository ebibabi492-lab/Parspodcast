package com.parspodcast.studio.domain.tts

object PersianTTSPreprocessor {

    // Common Iranian news broadcasting abbreviations
    private val ABBREVIATIONS_MAP = listOf(
        Regex("\\bص\\.") to "صفحه ",
        Regex("\\bم\\.") to "میلادی ",
        Regex("\\bهـ?\\.ش\\.") to "هجری شمسی ",
        Regex("\\bهـ?\\.ق\\.") to "هجری قمری ",
        Regex("\\bق\\.?م\\.") to "قبل از میلاد ",
        Regex("\\bش\\.") to "شماره ",
        Regex("\\bد\\.") to "دقیقه ",
        Regex("\\bس\\.") to "ساعت ",
        Regex("٪|%") to " درصد ",
        Regex("\\bkm\\b", RegexOption.IGNORE_CASE) to " کیلومتر ",
        Regex("\\bkm/h\\b", RegexOption.IGNORE_CASE) to " کیلومتر بر ساعت ",
        Regex("\\bkg\\b", RegexOption.IGNORE_CASE) to " کیلوگرم ",
        Regex("\\bcm\\b", RegexOption.IGNORE_CASE) to " سانتی‌متر ",
        Regex("\\bmm\\b", RegexOption.IGNORE_CASE) to " میلی‌متر "
    )

    // Pronunciation clarifications for common Persian broadcasting words
    private val PRONUNCIATION_REPLACEMENTS = listOf(
        Regex("\\bپادکست\\b") to "پادکَست",
        Regex("\\bآفلاین\\b") to "آف‌لاین",
        Regex("\\bآنلاین\\b") to "آن‌لاین",
        Regex("\\bهوش مصنوعی\\b") to "هوشِ مصنوعی",
        Regex("\\bپارس‌پادکست\\b") to "پارس پادکَست"
    )

    fun preprocessForSynthesis(rawText: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText

        // 1. Strip URLs and markdown links
        text = text.replace(Regex("https?://\\S+"), "")
        text = text.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")

        // 2. Strip code fences and formatting characters
        text = text.replace(Regex("```[\\s\\S]*?```"), "")
        text = text.replace(Regex("[`*_~#>]"), "")

        // 3. Remove emojis and non-alphanumeric pictographs that confuse acoustic phonemizers
        text = text.replace(Regex("[\\p{So}\\p{Cn}]"), "")

        // 4. Expand broadcasting abbreviations
        for ((pattern, replacement) in ABBREVIATIONS_MAP) {
            text = text.replace(pattern, replacement)
        }

        // 5. Expand numerals to Persian words and normalize Persian/Arabic characters
        text = PersianTextNormalizer.normalize(text)

        // 6. Apply pronunciation adjustments
        for ((pattern, replacement) in PRONUNCIATION_REPLACEMENTS) {
            text = text.replace(pattern, replacement)
        }

        // 7. Normalize pause punctuation (keep only valid speech separators)
        text = text.replace("،", " ، ")
        text = text.replace("؛", " ؛ ")
        text = text.replace("؟", " ؟ ")
        text = text.replace("!", " ! ")
        text = text.replace(Regex("[()«»\"\\[\\]]"), " ")

        // 8. Condense multiple spaces into single space
        text = text.replace(Regex("\\s+"), " ").trim()

        return text
    }
}
