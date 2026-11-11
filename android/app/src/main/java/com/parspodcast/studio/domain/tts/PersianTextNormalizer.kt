package com.parspodcast.studio.domain.tts

object PersianTextNormalizer {

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

    private val ONES = arrayOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
    private val TEENS = arrayOf("ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
    private val TENS = arrayOf("", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val HUNDREDS = arrayOf("", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
    private val SCALES = arrayOf("", "هزار", "میلیون", "میلیارد", "تریلیون")

    fun normalize(rawText: String): String {
        var text = rawText

        // 1. Unify Arabic Yeh / Kaf to Persian
        val sb = StringBuilder()
        for (char in text) {
            sb.append(ARABIC_TO_PERSIAN_MAP[char] ?: char)
        }
        text = sb.toString()

        // 2. Remove redundant diacritics / tatweel
        text = text.replace("ـ", "") // remove kashida
        text = text.replace(Regex("[\u064B-\u065F]"), "") // remove harakat

        // 3. Normalize Persian and English digits into pronounced words
        text = expandNumbersToPersianWords(text)

        // 4. Clean extra spaces and preserve pause punctuation
        text = text.replace(Regex("\\s+"), " ")
        text = text.replace("،", " ، ")
        text = text.replace("؛", " ؛ ")

        return text.trim()
    }

    fun numberToPersianWords(number: Long): String {
        if (number == 0L) return "صفر"
        if (number < 0) return "منفی " + numberToPersianWords(-number)

        var n = number
        val parts = mutableListOf<String>()
        var scaleIdx = 0

        while (n > 0) {
            val chunk = (n % 1000).toInt()
            if (chunk > 0) {
                val chunkWords = threeDigitToWords(chunk)
                val scale = SCALES[scaleIdx]
                val part = if (scale.isNotEmpty()) "$chunkWords $scale" else chunkWords
                parts.add(0, part)
            }
            n /= 1000
            scaleIdx++
        }

        return parts.joinToString(" و ")
    }

    private fun threeDigitToWords(num: Int): String {
        val parts = mutableListOf<String>()
        val h = num / 100
        val remainder = num % 100

        if (h > 0) {
            parts.add(HUNDREDS[h])
        }

        if (remainder in 10..19) {
            parts.add(TEENS[remainder - 10])
        } else {
            val t = remainder / 10
            val o = remainder % 10
            if (t > 0) parts.add(TENS[t])
            if (o > 0) parts.add(ONES[o])
        }

        return parts.joinToString(" و ")
    }

    private fun expandNumbersToPersianWords(input: String): String {
        // Convert any Persian/Arabic digits to ASCII first for regex matching
        val asciiDigits = input.map {
            when (it) {
                in '۰'..'۹' -> '0' + (it - '۰')
                in '٠'..'٩' -> '0' + (it - '٠')
                else -> it
            }
        }.joinToString("")

        return Regex("\\b\\d+\\b").replace(asciiDigits) { matchResult ->
            val num = matchResult.value.toLongOrNull()
            if (num != null) numberToPersianWords(num) else matchResult.value
        }
    }
}
