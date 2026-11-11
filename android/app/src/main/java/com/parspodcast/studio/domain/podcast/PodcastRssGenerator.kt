package com.parspodcast.studio.domain.podcast

import com.parspodcast.studio.data.local.entities.PodcastEpisodeEntity
import com.parspodcast.studio.data.local.entities.PodcastShowEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PodcastRssGenerator {

    private const val GENERATOR_NAME = "ParsPodcast Studio Android v1.0 (LiteRT & On-Device Persian AI)"

    fun formatRfc822Date(timestampUtc: Long): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.format(Date(timestampUtc))
    }

    fun escapeXml(text: String?): String {
        if (text == null) return ""
        val sb = StringBuilder()
        for (c in text) {
            when (c) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    // Filter XML 1.0 invalid control characters
                    if (c.code in 0x20..0xD7FF || c == '\t' || c == '\n' || c == '\r' || c.code in 0xE000..0xFFFD) {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    fun formatDuration(durationSeconds: Int): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun generateFeedXml(
        show: PodcastShowEntity,
        episodes: List<PodcastEpisodeEntity>,
        overrideFeedUrl: String? = null,
        includeOnlyPublishedOrReady: Boolean = false
    ): String {
        val feedUrl = overrideFeedUrl ?: show.publicFeedUrl ?: "https://parspodcast.ir/feed.xml"
        val showSiteUrl = show.websiteUrl ?: "https://parspodcast.ir"
        val nowRfc822 = formatRfc822Date(System.currentTimeMillis())

        val filteredEpisodes = if (includeOnlyPublishedOrReady) {
            episodes.filter { it.publishingStatus in listOf("PUBLISHED", "READY", "UPDATE_PENDING") }
        } else {
            episodes
        }.sortedByDescending { it.publicationDateUtc }

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<rss version="2.0"""")
        sb.append(""" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"""")
        sb.append(""" xmlns:atom="http://www.w3.org/2005/Atom"""")
        sb.append(""" xmlns:content="http://purl.org/rss/1.0/modules/content/"""")
        sb.append(">\n")
        sb.append("  <channel>\n")

        // Standard RSS 2.0 Channel Tags
        sb.append("    <title>${escapeXml(show.title)}</title>\n")
        sb.append("    <link>${escapeXml(showSiteUrl)}</link>\n")
        sb.append("    <description>${escapeXml(show.description)}</description>\n")
        sb.append("    <language>${escapeXml(show.language.ifBlank { "fa" })}</language>\n")
        sb.append("    <copyright>${escapeXml(show.copyright)}</copyright>\n")
        sb.append("    <generator>$GENERATOR_NAME</generator>\n")
        sb.append("    <pubDate>$nowRfc822</pubDate>\n")
        sb.append("    <lastBuildDate>$nowRfc822</lastBuildDate>\n")

        // atom:link rel="self"
        sb.append("""    <atom:link href="${escapeXml(feedUrl)}" rel="self" type="application/rss+xml"/>""").append("\n")

        // RSS image
        val artworkUrl = show.artworkRemoteUrl ?: "https://parspodcast.ir/cover.jpg"
        sb.append("    <image>\n")
        sb.append("      <url>${escapeXml(artworkUrl)}</url>\n")
        sb.append("      <title>${escapeXml(show.title)}</title>\n")
        sb.append("      <link>${escapeXml(showSiteUrl)}</link>\n")
        sb.append("    </image>\n")

        // iTunes Channel Tags
        sb.append("    <itunes:author>${escapeXml(show.author)}</itunes:author>\n")
        sb.append("    <itunes:summary>${escapeXml(show.subtitle.ifBlank { show.description })}</itunes:summary>\n")
        sb.append("    <itunes:type>${if (show.showType == "serial") "serial" else "episodic"}</itunes:type>\n")
        sb.append("    <itunes:explicit>${if (show.explicitContent) "true" else "false"}</itunes:explicit>\n")
        sb.append("""    <itunes:image href="${escapeXml(artworkUrl)}"/>""").append("\n")

        // Owner
        sb.append("    <itunes:owner>\n")
        sb.append("      <itunes:name>${escapeXml(show.ownerName)}</itunes:name>\n")
        if (!show.ownerEmail.isNullOrBlank()) {
            sb.append("      <itunes:email>${escapeXml(show.ownerEmail)}</itunes:email>\n")
        }
        sb.append("    </itunes:owner>\n")

        // Category & Subcategory
        val cat = show.category.ifBlank { "News" }
        if (!show.subcategory.isNullOrBlank()) {
            sb.append("""    <itunes:category text="${escapeXml(cat)}">""").append("\n")
            sb.append("""      <itunes:category text="${escapeXml(show.subcategory)}"/>""").append("\n")
            sb.append("    </itunes:category>\n")
        } else {
            sb.append("""    <itunes:category text="${escapeXml(cat)}"/>""").append("\n")
        }

        // Episodes
        for (ep in filteredEpisodes) {
            val epGuid = ep.guid.ifBlank { ep.id }
            val epPubDate = if (ep.pubDateRfc822.isNotBlank()) ep.pubDateRfc822 else formatRfc822Date(ep.publicationDateUtc)
            val epAudioUrl = ep.publicAudioUrl ?: ep.enclosureUrl
            val epMime = ep.audioMimeType.ifBlank { "audio/wav" }
            val epArtwork = ep.localArtworkPath?.let { show.artworkRemoteUrl } ?: artworkUrl

            sb.append("    <item>\n")
            sb.append("      <title>${escapeXml(ep.title)}</title>\n")
            sb.append("      <description>${escapeXml(ep.descriptionFa)}</description>\n")
            sb.append("      <link>${escapeXml(showSiteUrl)}</link>\n")
            sb.append("""      <guid isPermaLink="false">${escapeXml(epGuid)}</guid>""").append("\n")
            sb.append("      <pubDate>$epPubDate</pubDate>\n")

            // Enclosure: must have url, length, type
            if (epAudioUrl.isNotBlank()) {
                val sizeBytes = if (ep.audioFileSizeBytes > 0) ep.audioFileSizeBytes else 1024L
                sb.append("""      <enclosure url="${escapeXml(epAudioUrl)}" length="$sizeBytes" type="${escapeXml(epMime)}"/>""").append("\n")
            }

            // iTunes Episode Tags
            sb.append("      <itunes:title>${escapeXml(ep.title)}</itunes:title>\n")
            sb.append("      <itunes:author>${escapeXml(ep.author.ifBlank { show.author })}</itunes:author>\n")
            sb.append("      <itunes:summary>${escapeXml(ep.summary.ifBlank { ep.descriptionFa })}</itunes:summary>\n")
            sb.append("      <itunes:duration>${formatDuration(ep.durationSeconds)}</itunes:duration>\n")
            sb.append("      <itunes:episodeType>${escapeXml(ep.episodeType)}</itunes:episodeType>\n")
            if (ep.episodeNumber > 0) {
                sb.append("      <itunes:episode>${ep.episodeNumber}</itunes:episode>\n")
            }
            if (ep.seasonNumber > 0) {
                sb.append("      <itunes:season>${ep.seasonNumber}</itunes:season>\n")
            }
            sb.append("      <itunes:explicit>${if (ep.explicitContent) "true" else "false"}</itunes:explicit>\n")
            if (epArtwork.isNotBlank()) {
                sb.append("""      <itunes:image href="${escapeXml(epArtwork)}"/>""").append("\n")
            }

            sb.append("    </item>\n")
        }

        sb.append("  </channel>\n")
        sb.append("</rss>\n")

        return sb.toString()
    }
}
