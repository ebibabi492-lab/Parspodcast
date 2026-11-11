package com.parspodcast.studio.domain.podcast

import com.parspodcast.studio.data.local.entities.PodcastEpisodeEntity
import com.parspodcast.studio.data.local.entities.PodcastShowEntity
import org.junit.Assert.*
import org.junit.Test

class PodcastRssGeneratorTest {

    @Test
    fun testGenerateFeedXml_structureAndNamespaces() {
        val show = PodcastShowEntity(
            id = "show_1",
            title = "پارس‌پادکست & اخبار فناوری",
            subtitle = "هوش مصنوعی و جامعه <ویژه>",
            description = "تحلیل رویدادهای فناوری \"بدون اینترنت\"",
            author = "استودیوی پارس‌پادکست",
            ownerName = "پارس‌پادکست",
            ownerEmail = "studio@parspodcast.ir",
            websiteUrl = "https://parspodcast.ir",
            language = "fa",
            category = "Technology",
            subcategory = "Podcasting",
            copyright = "تمامی حقوق محفوظ است © ۱۴۰۵",
            explicitContent = false,
            artworkRemoteUrl = "https://parspodcast.ir/cover.jpg",
            publicFeedUrl = "https://parspodcast.ir/feed.xml"
        )

        val episode = PodcastEpisodeEntity(
            id = "ep_guid_12345",
            projectId = "proj_1",
            showId = "show_1",
            title = "اپیزود ۱: گما ۴ و پردازش محلی",
            descriptionFa = "توضیحات اپیزود با کاراکترهای خاص & < > \" '",
            summary = "خلاصه اپیزود",
            episodeNumber = 1,
            seasonNumber = 1,
            episodeType = "full",
            author = "علی محمدی",
            keywords = "هوش مصنوعی, گما",
            explicitContent = false,
            durationSeconds = 360,
            publicationDateUtc = 1731369600000L, // Fixed timestamp
            pubDateRfc822 = "Tue, 12 Nov 2024 00:00:00 GMT",
            enclosureUrl = "https://media.parspodcast.ir/ep1.mp4",
            audioMimeType = "audio/mp4",
            audioFileSizeBytes = 12500000L,
            publishingStatus = "PUBLISHED"
        )

        val xml = PodcastRssGenerator.generateFeedXml(
            show = show,
            episodes = listOf(episode),
            overrideFeedUrl = "https://parspodcast.ir/feed.xml"
        )

        // 1. Verify XML declaration & namespaces
        assertTrue("XML declaration missing", xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue("Root RSS tag missing", xml.contains("<rss version=\"2.0\""))
        assertTrue("iTunes namespace missing", xml.contains("xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\""))
        assertTrue("Atom namespace missing", xml.contains("xmlns:atom=\"http://www.w3.org/2005/Atom\""))

        // 2. Verify XML escaping
        assertTrue("Title should be escaped", xml.contains("<title>پارس‌پادکست &amp; اخبار فناوری</title>"))
        assertTrue("Subtitle should be escaped", xml.contains("<itunes:summary>هوش مصنوعی و جامعه &lt;ویژه&gt;</itunes:summary>"))

        // 3. Verify stable GUID
        assertTrue("Stable GUID missing", xml.contains("<guid isPermaLink=\"false\">ep_guid_12345</guid>"))

        // 4. Verify enclosure tag
        assertTrue("Enclosure tag missing or malformed", xml.contains("<enclosure url=\"https://media.parspodcast.ir/ep1.mp4\" length=\"12500000\" type=\"audio/mp4\"/>"))

        // 5. Verify duration formatting
        assertTrue("Duration formatted incorrectly", xml.contains("<itunes:duration>06:00</itunes:duration>"))

        // 6. Verify atom:link
        assertTrue("atom:link missing", xml.contains("<atom:link href=\"https://parspodcast.ir/feed.xml\" rel=\"self\" type=\"application/rss+xml\"/>"))

        // 7. Verify category & subcategory
        assertTrue("Category missing", xml.contains("<itunes:category text=\"Technology\">"))
        assertTrue("Subcategory missing", xml.contains("<itunes:category text=\"Podcasting\"/>"))
    }

    @Test
    fun testEscapeXml_handlesNullAndSpecialChars() {
        assertEquals("", PodcastRssGenerator.escapeXml(null))
        assertEquals("Hello &amp; World", PodcastRssGenerator.escapeXml("Hello & World"))
        assertEquals("&lt;test&gt; &quot;quote&quot; &apos;single&apos;", PodcastRssGenerator.escapeXml("<test> \"quote\" 'single'"))
        assertEquals("سلام بر پارس‌پادکست", PodcastRssGenerator.escapeXml("سلام بر پارس‌پادکست"))
    }

    @Test
    fun testFormatDuration() {
        assertEquals("00:45", PodcastRssGenerator.formatDuration(45))
        assertEquals("05:30", PodcastRssGenerator.formatDuration(330))
        assertEquals("01:05:00", PodcastRssGenerator.formatDuration(3900))
    }
}
