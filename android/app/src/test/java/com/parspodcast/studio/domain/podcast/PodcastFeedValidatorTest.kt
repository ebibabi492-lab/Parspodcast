package com.parspodcast.studio.domain.podcast

import org.junit.Assert.*
import org.junit.Test

class PodcastFeedValidatorTest {

    @Test
    fun testValidateFeedXml_validFeedReturnsSuccess() {
        val validXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://itunes.com/dtds/podcast-1.0.dtd" xmlns:atom="http://www.w3.org/2005/Atom">
              <channel>
                <title>پارس‌پادکست</title>
                <link>https://parspodcast.ir</link>
                <description>پادکست فناوری</description>
                <language>fa</language>
                <itunes:image href="https://parspodcast.ir/cover.jpg"/>
                <item>
                  <title>اپیزود اول</title>
                  <description>توضیحات</description>
                  <guid isPermaLink="false">guid-1111</guid>
                  <pubDate>Tue, 12 Nov 2024 00:00:00 GMT</pubDate>
                  <enclosure url="https://parspodcast.ir/ep1.wav" length="1048576" type="audio/wav"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val report = PodcastFeedValidator.validateFeedXml(validXml)
        assertTrue("Valid XML should pass", report.isValid)
        assertFalse("Should not have fatal errors", report.hasFatalErrors)
        assertEquals(1, report.totalEpisodes)
        assertEquals(0, report.errors.size)
    }

    @Test
    fun testValidateFeedXml_detectsMissingTitleAndDuplicateGuid() {
        val invalidXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title></title>
                <description>توضیحات</description>
                <item>
                  <title>اپیزود ۱</title>
                  <guid isPermaLink="false">same-guid</guid>
                  <enclosure url="https://parspodcast.ir/ep1.mp4" length="5000" type="audio/mp4"/>
                </item>
                <item>
                  <title>اپیزود ۲</title>
                  <guid isPermaLink="false">same-guid</guid>
                  <enclosure url="https://parspodcast.ir/ep2.mp4" length="5000" type="audio/mp4"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val report = PodcastFeedValidator.validateFeedXml(invalidXml)
        assertFalse("Should be marked invalid", report.isValid)
        assertTrue("Should have fatal errors", report.hasFatalErrors)

        val titleError = report.errors.any { it.field == "title" }
        val duplicateGuidError = report.errors.any { it.field.contains("guid") && it.messageFa.contains("تکراری") }

        assertTrue("Should detect empty title", titleError)
        assertTrue("Should detect duplicate GUID", duplicateGuidError)
    }

    @Test
    fun testValidateFeedXml_detectsMalformedXml() {
        val badXml = "<rss><channel><title>خراب"
        val report = PodcastFeedValidator.validateFeedXml(badXml)
        assertFalse(report.isValid)
        assertTrue(report.hasFatalErrors)
        assertTrue(report.errors.any { it.field == "xml_parse" })
    }
}
