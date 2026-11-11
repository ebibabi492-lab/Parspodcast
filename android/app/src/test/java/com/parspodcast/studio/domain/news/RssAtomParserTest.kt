package com.parspodcast.studio.domain.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class RssAtomParserTest {

    @Test
    fun testParseValidRss2() {
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>خبرگزاری فناوری پارس</title>
                    <link>https://techparspodcast.ir</link>
                    <description>تازه‌ترین اخبار تکنولوژی</description>
                    <item>
                        <title><![CDATA[هوش مصنوعی مولد بر روی گوشی‌های همراه]]></title>
                        <link>https://techparspodcast.ir/news/101</link>
                        <description><![CDATA[<p>مدل‌های سبک محلی قادر به اجرای سریع و <b>بدون اینترنت</b> هستند.</p>]]></description>
                        <category>فناوری</category>
                        <pubDate>Mon, 10 Nov 2026 12:00:00 +0330</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val stream = ByteArrayInputStream(rssXml.toByteArray(Charsets.UTF_8))
        val items = RssAtomParser.parse(stream, fallbackSourceName = "پارس", sourceUrl = "https://techparspodcast.ir/feed")

        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("هوش مصنوعی مولد بر روی گوشی‌های همراه", item.title)
        assertEquals("https://techparspodcast.ir/news/101", item.articleUrl)
        assertEquals("فناوری", item.category)
        // Verify HTML tags were stripped from description
        assertFalse("Should strip <p> and <b> tags", item.summary.contains("<p>") || item.summary.contains("<b>"))
        assertTrue("Should keep text content", item.summary.contains("بدون اینترنت"))
        assertEquals(NewsInputType.RSS, item.inputType)
    }

    @Test
    fun testParseValidAtom1() {
        val atomXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <title>فید وبلاگ تخصصی هوش مصنوعی</title>
                <link href="https://ai-blog.ir"/>
                <entry>
                    <title>انتشار نسخه نوین مدل زبانی سبک</title>
                    <link rel="alternate" href="https://ai-blog.ir/posts/gemma4"/>
                    <summary>جزئیات تازه از پارامترهای مدل ۴ بیتی</summary>
                    <author><name>دکتر اکبری</name></author>
                    <category term="علمی"/>
                    <published>2026-11-10T14:30:00Z</published>
                </entry>
            </feed>
        """.trimIndent()

        val stream = ByteArrayInputStream(atomXml.toByteArray(Charsets.UTF_8))
        val items = RssAtomParser.parse(stream, fallbackSourceName = "وبلاگ هوش مصنوعی")

        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("انتشار نسخه نوین مدل زبانی سبک", item.title)
        assertEquals("https://ai-blog.ir/posts/gemma4", item.articleUrl)
        assertEquals("دکتر اکبری", item.author)
        assertEquals("علمی", item.category)
        assertEquals(NewsInputType.ATOM, item.inputType)
    }

    @Test
    fun testXxeProtection_doesNotResolveExternalEntities() {
        // Attempt an XXE attack injection with DOCTYPE entity
        val maliciousXml = """
            <?xml version="1.0"?>
            <!DOCTYPE test [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
            <rss version="2.0">
                <channel>
                    <title>تست امنیت</title>
                    <item>
                        <title>تست نفوذ &xxe;</title>
                        <description>توضیحات</description>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val stream = ByteArrayInputStream(maliciousXml.toByteArray(Charsets.UTF_8))
        // Should parse safely without throwing or resolving the external file
        val items = try {
            RssAtomParser.parse(stream, fallbackSourceName = "امنیت")
        } catch (e: Exception) {
            // Expected if parser is configured to disallow doctype entirely
            emptyList()
        }

        // If items parsed, verify that /etc/passwd was not leaked into title
        for (item in items) {
            assertFalse(item.title.contains("root:x:0:0"))
        }
    }
}
