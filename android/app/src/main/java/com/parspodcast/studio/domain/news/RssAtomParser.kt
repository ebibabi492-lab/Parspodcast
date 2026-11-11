package com.parspodcast.studio.domain.news

import com.parspodcast.studio.domain.calendar.JalaliCalendar
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.XMLConstants

object RssAtomParser {

    private val RFC822_FORMATS = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
    )

    fun parse(
        inputStream: InputStream,
        fallbackSourceName: String,
        sourceUrl: String? = null,
        defaultCategory: String = "عمومی"
    ): List<ParsedNewsDraft> {
        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
            try {
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            } catch (e: Exception) {
                // Ignore unsupported parser features on older Android versions
            }
        }

        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var detectedFeedTitle = fallbackSourceName
        val items = mutableListOf<ParsedNewsDraft>()

        var isAtom = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tagName = parser.name?.lowercase() ?: ""
                when (tagName) {
                    "feed" -> {
                        isAtom = true
                    }
                    "channel" -> {
                        isAtom = false
                    }
                    "item" -> {
                        val item = parseRssItem(parser, detectedFeedTitle, sourceUrl, defaultCategory)
                        if (item.title.isNotBlank() || item.content.isNotBlank()) {
                            items.add(item)
                        }
                    }
                    "entry" -> {
                        val item = parseAtomEntry(parser, detectedFeedTitle, sourceUrl, defaultCategory)
                        if (item.title.isNotBlank() || item.content.isNotBlank()) {
                            items.add(item)
                        }
                    }
                    "title" -> {
                        // Channel or Feed title if outside item/entry
                        if (items.isEmpty()) {
                            val t = parser.nextText()?.trim() ?: ""
                            if (t.isNotBlank()) detectedFeedTitle = NewsNormalizer.cleanHtml(t)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return items
    }

    private fun parseRssItem(
        parser: XmlPullParser,
        feedTitle: String,
        sourceUrl: String?,
        defaultCategory: String
    ): ParsedNewsDraft {
        var title = ""
        var link = ""
        var description = ""
        var content = ""
        var author = ""
        var category = defaultCategory
        var pubDateUtc = System.currentTimeMillis()

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name?.lowercase() == "item")) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name?.lowercase() ?: ""
                when (tag) {
                    "title" -> title = parser.nextText()?.trim() ?: ""
                    "link" -> link = parser.nextText()?.trim() ?: ""
                    "description" -> description = parser.nextText()?.trim() ?: ""
                    "encoded" -> content = parser.nextText()?.trim() ?: "" // content:encoded
                    "author", "creator" -> author = parser.nextText()?.trim() ?: "" // dc:creator
                    "category" -> {
                        val cat = parser.nextText()?.trim() ?: ""
                        if (cat.isNotBlank()) category = cat
                    }
                    "pubdate" -> {
                        val rawDate = parser.nextText()?.trim() ?: ""
                        pubDateUtc = parseDateToUtc(rawDate)
                    }
                }
            }
            eventType = parser.next()
        }

        val cleanTitle = NewsNormalizer.cleanHtml(title)
        val cleanDesc = NewsNormalizer.cleanHtml(description)
        val cleanContent = if (content.isNotBlank()) NewsNormalizer.cleanHtml(content) else cleanDesc
        val hash = NewsNormalizer.computeContentHash(cleanTitle, cleanContent)
        val jalali = JalaliCalendar.formatShort(pubDateUtc)

        return ParsedNewsDraft(
            tempId = "rss_${UUID.randomUUID()}",
            title = cleanTitle,
            summary = cleanDesc.take(250).trim(),
            content = cleanContent,
            sourceName = feedTitle,
            sourceUrl = sourceUrl,
            articleUrl = link.ifBlank { null },
            author = author.ifBlank { null },
            category = category,
            publishedAtUtc = pubDateUtc,
            jalaliDateStr = jalali,
            inputType = NewsInputType.RSS,
            contentHash = hash
        )
    }

    private fun parseAtomEntry(
        parser: XmlPullParser,
        feedTitle: String,
        sourceUrl: String?,
        defaultCategory: String
    ): ParsedNewsDraft {
        var title = ""
        var link = ""
        var summary = ""
        var content = ""
        var author = ""
        var category = defaultCategory
        var publishedUtc = System.currentTimeMillis()

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name?.lowercase() == "entry")) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name?.lowercase() ?: ""
                when (tag) {
                    "title" -> title = parser.nextText()?.trim() ?: ""
                    "link" -> {
                        val href = parser.getAttributeValue(null, "href")
                        val rel = parser.getAttributeValue(null, "rel")
                        if (href != null && (rel == null || rel == "alternate")) {
                            link = href
                        }
                    }
                    "summary" -> summary = parser.nextText()?.trim() ?: ""
                    "content" -> content = parser.nextText()?.trim() ?: ""
                    "author" -> {
                        // Nested author -> name
                        author = parseAtomAuthor(parser)
                    }
                    "category" -> {
                        val term = parser.getAttributeValue(null, "term") ?: parser.getAttributeValue(null, "label")
                        if (!term.isNullOrBlank()) category = term
                    }
                    "published", "updated" -> {
                        val rawDate = parser.nextText()?.trim() ?: ""
                        publishedUtc = parseDateToUtc(rawDate)
                    }
                }
            }
            eventType = parser.next()
        }

        val cleanTitle = NewsNormalizer.cleanHtml(title)
        val cleanSummary = NewsNormalizer.cleanHtml(summary)
        val cleanContent = if (content.isNotBlank()) NewsNormalizer.cleanHtml(content) else cleanSummary
        val hash = NewsNormalizer.computeContentHash(cleanTitle, cleanContent)
        val jalali = JalaliCalendar.formatShort(publishedUtc)

        return ParsedNewsDraft(
            tempId = "atom_${UUID.randomUUID()}",
            title = cleanTitle,
            summary = cleanSummary.take(250).trim(),
            content = cleanContent,
            sourceName = feedTitle,
            sourceUrl = sourceUrl,
            articleUrl = link.ifBlank { null },
            author = author.ifBlank { null },
            category = category,
            publishedAtUtc = publishedUtc,
            jalaliDateStr = jalali,
            inputType = NewsInputType.ATOM,
            contentHash = hash
        )
    }

    private fun parseAtomAuthor(parser: XmlPullParser): String {
        var authorName = ""
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name?.lowercase() == "author")) {
            if (eventType == XmlPullParser.START_TAG && parser.name?.lowercase() == "name") {
                authorName = parser.nextText()?.trim() ?: ""
            }
            eventType = parser.next()
        }
        return authorName
    }

    fun parseDateToUtc(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        val trimmed = dateStr.trim()
        for (format in RFC822_FORMATS) {
            try {
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(trimmed)
                if (date != null) return date.time
            } catch (e: Exception) {
                // Try next pattern
            }
        }
        return System.currentTimeMillis()
    }
}
