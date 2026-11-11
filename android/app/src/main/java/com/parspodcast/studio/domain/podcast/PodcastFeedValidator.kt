package com.parspodcast.studio.domain.podcast

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory

data class ValidationIssue(
    val field: String,
    val messageFa: String,
    val isFatal: Boolean
)

data class FeedValidationReport(
    val isValid: Boolean,
    val totalEpisodes: Int,
    val errors: List<ValidationIssue>,
    val warnings: List<ValidationIssue>
) {
    val hasFatalErrors: Boolean get() = errors.isNotEmpty()
}

object PodcastFeedValidator {

    private val VALID_MIME_TYPES = setOf(
        "audio/wav", "audio/x-wav",
        "audio/mp4", "audio/x-m4a", "audio/aac",
        "audio/mpeg", "audio/mp3"
    )

    fun validateFeedXml(xmlContent: String): FeedValidationReport {
        val errors = mutableListOf<ValidationIssue>()
        val warnings = mutableListOf<ValidationIssue>()

        if (xmlContent.isBlank()) {
            return FeedValidationReport(
                isValid = false,
                totalEpisodes = 0,
                errors = listOf(ValidationIssue("xml", "محتوای فید خالی است.", true)),
                warnings = emptyList()
            )
        }

        val doc = try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            val builder = factory.newDocumentBuilder()
            builder.parse(InputSource(StringReader(xmlContent)))
        } catch (e: Exception) {
            return FeedValidationReport(
                isValid = false,
                totalEpisodes = 0,
                errors = listOf(ValidationIssue("xml_parse", "ساختار XML نامعتبر است: ${e.message}", true)),
                warnings = emptyList()
            )
        }

        // 1. Root element check
        val root = doc.documentElement
        if (root == null || root.nodeName != "rss") {
            errors.add(ValidationIssue("root", "تگ ریشه باید <rss> باشد.", true))
            return FeedValidationReport(false, 0, errors, warnings)
        }
        val version = root.getAttribute("version")
        if (version != "2.0") {
            errors.add(ValidationIssue("version", "نسخه فید RSS باید ۲.۰ باشد (دریافت شد: $version).", true))
        }

        // 2. Channel check
        val channels = root.getElementsByTagName("channel")
        if (channels.length == 0) {
            errors.add(ValidationIssue("channel", "تگ <channel> در فید یافت نشد.", true))
            return FeedValidationReport(false, 0, errors, warnings)
        }
        val channel = channels.item(0) as Element

        val channelTitle = getDirectChildText(channel, "title")
        if (channelTitle.isBlank()) {
            errors.add(ValidationIssue("title", "عنوان پادکست (<title>) نمی‌تواند خالی باشد.", true))
        }

        val channelDesc = getDirectChildText(channel, "description")
        if (channelDesc.isBlank()) {
            errors.add(ValidationIssue("description", "توضیحات کانال پادکست (<description>) الزامی است.", true))
        }

        val channelLink = getDirectChildText(channel, "link")
        if (channelLink.isBlank()) {
            warnings.add(ValidationIssue("link", "پیوند وبسایت کانال پادکست (<link>) وارد نشده است.", false))
        }

        val channelLang = getDirectChildText(channel, "language")
        if (channelLang.isBlank()) {
            warnings.add(ValidationIssue("language", "زبان فید (<language>) مشخص نشده است؛ مقدار پیش‌فرض fa پیشنهاد می‌شود.", false))
        }

        // Check artwork
        val itunesImages = channel.getElementsByTagNameNS("http://itunes.com/dtds/podcast-1.0.dtd", "image")
        var hasItunesImage = false
        if (itunesImages.length > 0) {
            val imgEl = itunesImages.item(0) as Element
            val href = imgEl.getAttribute("href")
            if (href.isNotBlank()) {
                hasItunesImage = true
                if (!href.startsWith("https://")) {
                    warnings.add(ValidationIssue("itunes:image", "نشانی کاور پادکست باید از پروتکل امن HTTPS استفاده کند.", false))
                }
            }
        }
        if (!hasItunesImage) {
            warnings.add(ValidationIssue("itunes:image", "تصویر کاور استاندارد پادکست (itunes:image) در کانال تعریف نشده است.", false))
        }

        // 3. Episodes / Items
        val items = channel.getElementsByTagName("item")
        val seenGuids = mutableSetOf<String>()
        val totalItems = items.length

        for (i in 0 until totalItems) {
            val item = items.item(i) as Element
            val epNum = i + 1

            val itemTitle = getDirectChildText(item, "title")
            if (itemTitle.isBlank()) {
                errors.add(ValidationIssue("item[$epNum].title", "عنوان اپیزود شماره $epNum نمی‌تواند خالی باشد.", true))
            }

            val itemGuid = getDirectChildText(item, "guid")
            if (itemGuid.isBlank()) {
                errors.add(ValidationIssue("item[$epNum].guid", "شناسه یکتای GUID برای اپیزود شماره $epNum یافت نشد.", true))
            } else if (seenGuids.contains(itemGuid)) {
                errors.add(ValidationIssue("item[$epNum].guid", "شناسه یکتای GUID تکراری است: $itemGuid", true))
            } else {
                seenGuids.add(itemGuid)
            }

            // Enclosure validation
            val enclosures = item.getElementsByTagName("enclosure")
            if (enclosures.length == 0) {
                warnings.add(ValidationIssue("item[$epNum].enclosure", "اپیزود شماره $epNum فاقد تگ مدیا (<enclosure>) است.", false))
            } else {
                val enc = enclosures.item(0) as Element
                val url = enc.getAttribute("url")
                val length = enc.getAttribute("length")
                val type = enc.getAttribute("type")

                if (url.isBlank()) {
                    errors.add(ValidationIssue("item[$epNum].enclosure.url", "نشانی فایل صوتی در اینکلوژر خالی است.", true))
                } else {
                    try {
                        val uri = URI(url)
                        if (!uri.isAbsolute) {
                            warnings.add(ValidationIssue("item[$epNum].enclosure.url", "نشانی فایل صوتی باید یک URL مطلق باشد.", false))
                        }
                    } catch (e: Exception) {
                        errors.add(ValidationIssue("item[$epNum].enclosure.url", "نشانی فایل صوتی نامعتبر است: $url", true))
                    }
                }

                val lengthVal = length.toLongOrNull()
                if (lengthVal == null || lengthVal <= 0) {
                    warnings.add(ValidationIssue("item[$epNum].enclosure.length", "طول بایت فایل صوتی نامعتبر یا صفر است ($length).", false))
                }

                if (type.isBlank()) {
                    errors.add(ValidationIssue("item[$epNum].enclosure.type", "نوع MIME فایل صوتی تعیین نشده است.", true))
                } else if (!VALID_MIME_TYPES.contains(type.lowercase(Locale.ROOT))) {
                    warnings.add(ValidationIssue("item[$epNum].enclosure.type", "نوع MIME فایل صوتی استاندارد پادکست نیست ($type).", false))
                }
            }

            // Date validation
            val pubDate = getDirectChildText(item, "pubDate")
            if (pubDate.isNotBlank()) {
                try {
                    val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("GMT")
                    }
                    sdf.parse(pubDate)
                } catch (e: Exception) {
                    warnings.add(ValidationIssue("item[$epNum].pubDate", "فرمت تاریخ اپیزود مطابق استاندارد RFC-822 نیست: $pubDate", false))
                }
            }
        }

        return FeedValidationReport(
            isValid = errors.isEmpty(),
            totalEpisodes = totalItems,
            errors = errors,
            warnings = warnings
        )
    }

    private fun getDirectChildText(parent: Element, tagName: String): String {
        val list: NodeList = parent.getElementsByTagName(tagName)
        for (i in 0 until list.length) {
            val node = list.item(i)
            if (node.parentNode == parent) {
                return node.textContent.trim()
            }
        }
        return ""
    }
}
