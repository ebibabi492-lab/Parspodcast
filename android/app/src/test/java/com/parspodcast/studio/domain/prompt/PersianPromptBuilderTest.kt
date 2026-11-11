package com.parspodcast.studio.domain.prompt

import com.parspodcast.studio.domain.model.NewsItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersianPromptBuilderTest {

    @Test
    fun testBuildSingleHostPrompt_containsPersianSystemInstructions() {
        val news = listOf(
            NewsItem(
                id = 1L,
                title = "افتتاح پروژه جدید فضایی ایران",
                lead = "جزئیات تازه از پرتاب ماهواره تحقیقاتی",
                body = "ماهواره جدید با موفقیت در مدار قرار گرفت.",
                category = "علمی و فناوری",
                source = "ایرنا",
                jalaliDate = "۱۴۰۳/۱۱/۰۵"
            )
        )

        val prompt = PersianPromptBuilder.buildFullScriptPrompt(
            newsItems = news,
            format = "SINGLE_PRESENTER",
            targetDurationSeconds = 180,
            tone = "JOURNALISTIC",
            customInstructions = "روی سرعت انتقال خبر تاکید شود"
        )

        // Verify Persian instructions
        assertTrue(prompt.contains("پارس‌پادکست"))
        assertTrue(prompt.contains("تک‌گوینده"))
        assertTrue(prompt.contains("HOST_A"))
        assertFalse(prompt.contains("HOST_B"))
        assertTrue(prompt.contains("افتتاح پروژه جدید فضایی ایران"))
        assertTrue(prompt.contains("روی سرعت انتقال خبر تاکید شود"))
    }

    @Test
    fun testBuildDualHostPrompt_containsBothPresentersAndConversationalTone() {
        val news = listOf(
            NewsItem(
                id = 2L,
                title = "گسترش کاربردهای هوش مصنوعی در سلامت",
                lead = "تشخیص زودهنگام بیماری‌ها با مدل‌های نوین",
                body = "پزشکان از نتایج آزمایش بالینی ابراز خرسندی کردند.",
                category = "پزشکی",
                source = "ایسنا",
                jalaliDate = "۱۴۰۳/۱۱/۰۶"
            )
        )

        val prompt = PersianPromptBuilder.buildFullScriptPrompt(
            newsItems = news,
            format = "TWO_PRESENTERS_CONVERSATIONAL",
            targetDurationSeconds = 300,
            tone = "CONVERSATIONAL",
            customInstructions = null
        )

        assertTrue(prompt.contains("پادکست گفتگومحور دو نفره"))
        assertTrue(prompt.contains("HOST_A"))
        assertTrue(prompt.contains("HOST_B"))
        assertTrue(prompt.contains("گفتگوی پویا و تعامل طبیعی"))
    }

    @Test
    fun testRewriteSegmentPrompt_containsSelectedToneAndConstraint() {
        val originalText = "امروز بازار سهام رشد چشمگیری داشت."
        val prompt = PersianPromptBuilder.buildRewriteSegmentPrompt(
            segmentText = originalText,
            targetTone = "STORYTELLING",
            customInstructions = "کمی خودمانی‌تر و با هیجان"
        )

        assertTrue(prompt.contains(originalText))
        assertTrue(prompt.contains("روایی/داستانی"))
        assertTrue(prompt.contains("کمی خودمانی‌تر و با هیجان"))
    }

    @Test
    fun testTitleSuggestionsPrompt_containsPersianKeywords() {
        val content = "امروز در پارس‌پادکست به بررسی آخرین تحولات اقتصاد جهانی و فناوری پرداختیم."
        val prompt = PersianPromptBuilder.buildTitleSuggestionsPrompt(content, count = 5)

        assertTrue(prompt.contains(content))
        assertTrue(prompt.contains("۵ عنوان جذاب، حرفه‌ای و گیرا"))
        assertTrue(prompt.contains("بدون مقدمه‌چینی"))
    }
}
