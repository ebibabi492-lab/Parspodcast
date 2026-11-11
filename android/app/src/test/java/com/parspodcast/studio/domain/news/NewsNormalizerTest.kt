package com.parspodcast.studio.domain.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsNormalizerTest {

    @Test
    fun testNormalizeText_unifiesArabicYehAndKaf() {
        val arabicText = "توسعه يک سيستم هوشمند براي بانک مرکزي"
        val normalized = NewsNormalizer.normalizeText(arabicText)

        assertFalse("Should not contain Arabic Yeh (ي)", normalized.contains("ي"))
        assertFalse("Should not contain Arabic Kaf (ك)", normalized.contains("ك"))
        assertTrue("Should contain Persian Yeh (ی)", normalized.contains("ی"))
        assertTrue("Should contain Persian Kaf (ک)", normalized.contains("ک"))
    }

    @Test
    fun testCleanHtml_stripsTagsAndUnescapesEntities() {
        val html = "<p>گزارش روزانه: &quot;رشد بازار سهام&quot; &amp; افزایش تقاضا</p>"
        val cleaned = NewsNormalizer.cleanHtml(html)

        assertFalse(cleaned.contains("<p>"))
        assertFalse(cleaned.contains("&quot;"))
        assertFalse(cleaned.contains("&amp;"))
        assertTrue(cleaned.contains("\"رشد بازار سهام\""))
        assertTrue(cleaned.contains("&"))
    }

    @Test
    fun testComputeContentHash_stableAndDeterministic() {
        val title1 = "رشد چشمگیر فناوری در سال ۱۴۰۵"
        val body1 = "پژوهشگران به نتایج امیدوارکننده‌ای دست یافتند."

        val title2 = " رشد چشمگیر فناوری در سال 1405 "
        val body2 = "پژوهشگران به نتایج امیدوارکننده‌ای دست یافتند.   "

        val hash1 = NewsNormalizer.computeContentHash(title1, body1)
        val hash2 = NewsNormalizer.computeContentHash(title2, body2)

        assertEquals("Hashes must match across digit representation and leading/trailing whitespace", hash1, hash2)
    }

    @Test
    fun testCalculateTitleSimilarity_detectsNearDuplicates() {
        val titleA = "افتتاح پروژه جدید فضایی ایران در پایگاه امام خمینی"
        val titleB = "افتتاح پروژه فضایی ایران در پایگاه امام خمینی"
        val titleC = "نتایج مسابقات فوتبال لیگ برتر خلیج فارس"

        val similarityAB = NewsNormalizer.calculateTitleSimilarity(titleA, titleB)
        val similarityAC = NewsNormalizer.calculateTitleSimilarity(titleA, titleC)

        assertTrue("Near-duplicate titles should have high similarity", similarityAB > 0.7f)
        assertTrue("Unrelated titles should have low similarity", similarityAC < 0.15f)
    }
}
