package com.parspodcast.studio.domain.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class OfflineTextNewsParserTest {

    @Test
    fun testParseStructuredPersianTxt_singleItem() {
        val txt = """
            عنوان: پیشرفت در پردازش زبان طبیعی فارسی
            منبع: خبرگزاری ایرنا
            تاریخ: ۱۴۰۵/۰۸/۲۲
            دسته‌بندی: فناوری

            متن:
            محققان دانشگاهی از مدل جدید زبانی رونمایی کردند که قادر است به درستی اعراب و لحن زبان فارسی را درک کند.
        """.trimIndent()

        val stream = ByteArrayInputStream(txt.toByteArray(Charsets.UTF_8))
        val drafts = OfflineTextNewsParser.parseStream(stream, defaultSourceName = "فایل تست")

        assertEquals(1, drafts.size)
        val item = drafts[0]
        assertEquals("پیشرفت در پردازش زبان طبیعی فارسی", item.title)
        assertEquals("خبرگزاری ایرنا", item.sourceName)
        assertEquals("فناوری", item.category)
        assertTrue(item.content.contains("محققان دانشگاهی از مدل جدید"))
        assertTrue(item.contentHash.isNotEmpty())
    }

    @Test
    fun testParseMultipleItemsWithDelimiters() {
        val txt = """
            عنوان: خبر اول فناوری
            منبع: دیجیاتو
            متن:
            متن خبر اول در این قسمت است.
            ---
            عنوان: خبر دوم اقتصادی
            منبع: تجارت‌نیوز
            دسته‌بندی: اقتصاد
            متن:
            شاخص کل بورس رشد قابل توجهی داشت.
            ===
            عنوان: خبر سوم علمی
            منبع: ایسنا
            دسته‌بندی: علمی
            متن:
            کشف سیاره فراخورشیدی جدید توسط منجمان.
        """.trimIndent()

        val stream = ByteArrayInputStream(txt.toByteArray(Charsets.UTF_8))
        val drafts = OfflineTextNewsParser.parseStream(stream)

        assertEquals(3, drafts.size)
        assertEquals("خبر اول فناوری", drafts[0].title)
        assertEquals("دیجیاتو", drafts[0].sourceName)
        assertEquals("خبر دوم اقتصادی", drafts[1].title)
        assertEquals("تجارت‌نیوز", drafts[1].sourceName)
        assertEquals("خبر سوم علمی", drafts[2].title)
    }

    @Test
    fun testParseWithUtf8Bom() {
        val txtWithBom = "\uFEFFعنوان: خبر با استاندارد بوم\nمنبع: منبع تست\nمتن: متن این خبر دارای هدر بایت اوردر مارک است."
        val stream = ByteArrayInputStream(txtWithBom.toByteArray(Charsets.UTF_8))
        val drafts = OfflineTextNewsParser.parseStream(stream)

        assertEquals(1, drafts.size)
        assertEquals("خبر با استاندارد بوم", drafts[0].title)
        assertFalse("Should strip BOM from title", drafts[0].title.startsWith("\uFEFF"))
    }

    @Test
    fun testParseMarkdownHeadings() {
        val md = """
            # رونمایی از دستاوردهای نوین حوزه فضا
            منبع: مهرنیوز
            دسته‌بندی: علمی
            متن:
            ایران به زودی ماهواره‌های مخابراتی جدیدی به مدار تزریق خواهد کرد.
            این پروژه گامی بلند در استقلال فناورانه است.
        """.trimIndent()

        val stream = ByteArrayInputStream(md.toByteArray(Charsets.UTF_8))
        val drafts = OfflineTextNewsParser.parseStream(stream, inputType = NewsInputType.MARKDOWN)

        assertEquals(1, drafts.size)
        assertEquals("رونمایی از دستاوردهای نوین حوزه فضا", drafts[0].title)
        assertEquals(NewsInputType.MARKDOWN, drafts[0].inputType)
    }

    @Test
    fun testParseUnstructuredBlockFallback() {
        val rawText = """
            امروز بارندگی شدید در استان‌های شمالی کشور گزارش شده است.
            سازمان هواشناسی نسبت به طغیان رودخانه‌ها هشدار صادر کرد.
            ---
            تیم ملی والیبال در مرحله نهایی مسابقات قهرمانی به پیروزی رسید.
        """.trimIndent()

        val stream = ByteArrayInputStream(rawText.toByteArray(Charsets.UTF_8))
        val drafts = OfflineTextNewsParser.parseStream(stream, defaultSourceName = "فایل خام")

        assertEquals(2, drafts.size)
        assertTrue(drafts[0].title.contains("امروز بارندگی شدید"))
        assertEquals("فایل خام", drafts[0].sourceName)
        assertTrue(drafts[1].title.contains("تیم ملی والیبال"))
    }

    @Test
    fun testEmptyFile_returnsEmptyList() {
        val stream = ByteArrayInputStream("   \n\n  ".toByteArray(Charsets.UTF_8))
        val drafts = OfflineTextNewsParser.parseStream(stream)
        assertTrue(drafts.isEmpty())
    }
}
