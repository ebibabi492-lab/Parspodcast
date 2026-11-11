package com.parspodcast.studio.domain.tts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersianTTSPreprocessorTest {

    @Test
    fun testPreprocessAbbreviations_expandsToFullPersianWords() {
        val input = "در ص. ۲۴ کتاب، سال ۲۰۲۴ م. به عنوان نقطه عطف ذکر شد."
        val output = PersianTTSPreprocessor.preprocessForSynthesis(input)

        assertTrue("Should expand ص.", output.contains("صفحه"))
        assertTrue("Should expand م.", output.contains("میلادی"))
        assertFalse("Should not contain raw abbreviated ص.", output.contains("ص."))
    }

    @Test
    fun testPreprocessNumbers_convertsToPersianWords() {
        val input = "نرخ رشد اقتصادی به ۴ درصد رسید."
        val output = PersianTTSPreprocessor.preprocessForSynthesis(input)

        assertTrue("Should contain word for 4 (چهار)", output.contains("چهار"))
        assertTrue("Should contain word for percent (درصد)", output.contains("درصد"))
        assertFalse("Should not contain raw digit 4", output.contains("۴"))
    }

    @Test
    fun testPreprocessEmojisAndMarkdown_cleansFormatting() {
        val input = "🎉 خبر داغ: [لینک گزارش](https://isna.ir) پیروزی بزرگ محققان!"
        val output = PersianTTSPreprocessor.preprocessForSynthesis(input)

        assertFalse("Should remove emoji", output.contains("🎉"))
        assertFalse("Should remove URL", output.contains("https://isna.ir"))
        assertTrue("Should preserve link title text", output.contains("لینک گزارش"))
        assertTrue("Should preserve content", output.contains("پیروزی بزرگ محققان"))
    }

    @Test
    fun testPronunciationCorrections_appliesCorrectDiacritics() {
        val input = "این اپیزود از پارس‌پادکست به کاربردهای هوش مصنوعی می‌پردازد."
        val output = PersianTTSPreprocessor.preprocessForSynthesis(input)

        assertTrue("Should adjust pronunciation for podcast", output.contains("پادکَست"))
    }
}
