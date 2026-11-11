package com.parspodcast.studio.data.hosting

import com.parspodcast.studio.domain.podcast.PodcastFeedValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

data class EnclosureVerificationDetail(
    val url: String,
    val title: String,
    val isAccessible: Boolean,
    val httpCode: Int,
    val remoteContentType: String?,
    val expectedContentType: String,
    val remoteSizeBytes: Long?,
    val expectedSizeBytes: Long,
    val isHttps: Boolean,
    val error: String? = null
)

data class PublicFeedVerificationSummary(
    val isFeedAccessible: Boolean,
    val feedHttpCode: Int,
    val feedContentType: String?,
    val isXmlValid: Boolean,
    val totalEpisodesFound: Int,
    val artworkUrl: String?,
    val isArtworkAccessible: Boolean,
    val enclosureDetails: List<EnclosureVerificationDetail>,
    val fatalErrors: List<String>,
    val warnings: List<String>
) {
    val isFullyVerified: Boolean
        get() = isFeedAccessible && isXmlValid && (totalEpisodesFound == 0 || enclosureDetails.all { it.isAccessible })
}

@Singleton
class PublicFeedVerifier @Inject constructor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    suspend fun verifyPublicFeed(feedUrl: String): PublicFeedVerificationSummary = withContext(Dispatchers.IO) {
        val fatalErrors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val enclosureDetails = mutableListOf<EnclosureVerificationDetail>()

        if (!feedUrl.startsWith("https://")) {
            warnings.add("نشانی فید از پروتکل HTTPS استفاده نمی‌کند.")
        }

        var isFeedAccessible = false
        var feedHttpCode = 0
        var feedContentType: String? = null
        var feedXmlContent = ""

        try {
            val request = Request.Builder().url(feedUrl).get().build()
            client.newCall(request).execute().use { response ->
                feedHttpCode = response.code
                feedContentType = response.header("Content-Type")
                if (response.isSuccessful) {
                    isFeedAccessible = true
                    feedXmlContent = response.body?.string() ?: ""
                } else {
                    fatalErrors.add("عدم دسترسی به فید: سرور با کد وضعیت ${response.code} پاسخ داد.")
                }
            }
        } catch (e: Exception) {
            fatalErrors.add("خطا در برقراری ارتباط با نشانی فید: ${e.message}")
        }

        if (!isFeedAccessible || feedXmlContent.isBlank()) {
            return@withContext PublicFeedVerificationSummary(
                isFeedAccessible = isFeedAccessible,
                feedHttpCode = feedHttpCode,
                feedContentType = feedContentType,
                isXmlValid = false,
                totalEpisodesFound = 0,
                artworkUrl = null,
                isArtworkAccessible = false,
                enclosureDetails = emptyList(),
                fatalErrors = fatalErrors,
                warnings = warnings
            )
        }

        // Validate local XML rules
        val valReport = PodcastFeedValidator.validateFeedXml(feedXmlContent)
        val isXmlValid = !valReport.hasFatalErrors
        valReport.errors.forEach { fatalErrors.add("خطای ساختار فید: ${it.messageFa}") }
        valReport.warnings.forEach { warnings.add("هشدار فید: ${it.messageFa}") }

        // Parse DOM to extract enclosures & artwork
        var artworkUrl: String? = null
        var isArtworkAccessible = false

        try {
            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(feedXmlContent)))

            // Find artwork
            val imgNodes = doc.getElementsByTagName("image")
            if (imgNodes.length > 0) {
                val urlNodes = (imgNodes.item(0) as Element).getElementsByTagName("url")
                if (urlNodes.length > 0) {
                    artworkUrl = urlNodes.item(0).textContent.trim()
                }
            }

            // Verify Artwork
            if (!artworkUrl.isNullOrBlank()) {
                isArtworkAccessible = checkAssetAccessible(artworkUrl)
                if (!isArtworkAccessible) {
                    warnings.add("تصویر کاور پادکست ($artworkUrl) از طریق اینترنت در دسترس نیست.")
                }
            }

            // Inspect Items
            val items = doc.getElementsByTagName("item")
            for (i in 0 until items.length) {
                val item = items.item(i) as Element
                val titleNodes = item.getElementsByTagName("title")
                val itemTitle = if (titleNodes.length > 0) titleNodes.item(0).textContent.trim() else "اپیزود ${i + 1}"

                val enclosures = item.getElementsByTagName("enclosure")
                if (enclosures.length > 0) {
                    val enc = enclosures.item(0) as Element
                    val encUrl = enc.getAttribute("url")
                    val encType = enc.getAttribute("type")
                    val encLength = enc.getAttribute("length").toLongOrNull() ?: 0L

                    if (encUrl.isNotBlank()) {
                        val encDetail = verifyEnclosureUrl(encUrl, itemTitle, encType, encLength)
                        enclosureDetails.add(encDetail)
                        if (!encDetail.isAccessible) {
                            fatalErrors.add("فایل صوتی اپیزود «$itemTitle» در سرور یافت نشد یا در دسترس نیست.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            fatalErrors.add("خطا در تحلیل اقلام فید: ${e.message}")
        }

        PublicFeedVerificationSummary(
            isFeedAccessible = isFeedAccessible,
            feedHttpCode = feedHttpCode,
            feedContentType = feedContentType,
            isXmlValid = isXmlValid,
            totalEpisodesFound = enclosureDetails.size,
            artworkUrl = artworkUrl,
            isArtworkAccessible = isArtworkAccessible,
            enclosureDetails = enclosureDetails,
            fatalErrors = fatalErrors,
            warnings = warnings
        )
    }

    private fun checkAssetAccessible(url: String): Boolean {
        return try {
            val req = Request.Builder().url(url).head().build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    private fun verifyEnclosureUrl(
        url: String,
        title: String,
        expectedType: String,
        expectedSize: Long
    ): EnclosureVerificationDetail {
        val isHttps = url.startsWith("https://")
        return try {
            // Send HEAD or range GET
            var req = Request.Builder().url(url).head().build()
            var resp = client.newCall(req).execute()
            if (resp.code == 405) {
                resp.close()
                req = Request.Builder().url(url).addHeader("Range", "bytes=0-0").get().build()
                resp = client.newCall(req).execute()
            }

            resp.use { r ->
                val accessible = r.isSuccessful || r.code == 206
                val cType = r.header("Content-Type")
                val cLen = r.header("Content-Length")?.toLongOrNull()

                EnclosureVerificationDetail(
                    url = url,
                    title = title,
                    isAccessible = accessible,
                    httpCode = r.code,
                    remoteContentType = cType,
                    expectedContentType = expectedType,
                    remoteSizeBytes = cLen,
                    expectedSizeBytes = expectedSize,
                    isHttps = isHttps,
                    error = if (!accessible) "کد وضعیت HTTP ${r.code}" else null
                )
            }
        } catch (e: Exception) {
            EnclosureVerificationDetail(
                url = url,
                title = title,
                isAccessible = false,
                httpCode = 0,
                remoteContentType = null,
                expectedContentType = expectedType,
                remoteSizeBytes = null,
                expectedSizeBytes = expectedSize,
                isHttps = isHttps,
                error = e.message
            )
        }
    }
}
