package com.parspodcast.studio.data.hosting

import android.content.Context
import com.parspodcast.studio.data.local.entities.PodcastEpisodeEntity
import com.parspodcast.studio.data.local.entities.PodcastShowEntity
import com.parspodcast.studio.domain.podcast.PodcastRssGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

data class LocalExportPackageSummary(
    val exportDirectoryPath: String,
    val feedFilePath: String,
    val totalAudioFilesCopied: Int,
    val totalBytesExported: Long,
    val hasArtwork: Boolean,
    val readmePath: String
)

@Singleton
class LocalExportProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun createPublicationPackage(
        targetDirectory: File,
        show: PodcastShowEntity,
        episodes: List<PodcastEpisodeEntity>,
        publicBaseUrlPlaceholder: String = "https://your-domain.com/podcast/"
    ): Result<LocalExportPackageSummary> = withContext(Dispatchers.IO) {
        try {
            if (!targetDirectory.exists()) {
                targetDirectory.mkdirs()
            }

            var totalBytes = 0L
            var audioFilesCount = 0

            // 1. Export Artwork
            var hasArtwork = false
            var relativeArtworkPath: String? = null
            if (!show.artworkLocalPath.isNullOrBlank()) {
                val artworkSrc = File(show.artworkLocalPath)
                if (artworkSrc.exists()) {
                    val ext = artworkSrc.extension.ifBlank { "jpg" }
                    val artworkDest = File(targetDirectory, "cover.$ext")
                    artworkSrc.copyTo(artworkDest, overwrite = true)
                    totalBytes += artworkDest.length()
                    hasArtwork = true
                    relativeArtworkPath = "cover.$ext"
                }
            }

            // 2. Export Episode Audio files & prepare enclosures
            val episodesWithLocalEnclosures = episodes.map { ep ->
                val audioSrc = File(ep.localAudioPath)
                if (audioSrc.exists() && audioSrc.length() > 0) {
                    val safeAudioName = "ep_${ep.episodeNumber}_${ep.id.take(8)}.${audioSrc.extension}"
                    val audioDest = File(targetDirectory, safeAudioName)
                    audioSrc.copyTo(audioDest, overwrite = true)
                    totalBytes += audioDest.length()
                    audioFilesCount++

                    val publicAudioUrl = "${publicBaseUrlPlaceholder.trimEnd('/')}/$safeAudioName"
                    ep.copy(
                        enclosureUrl = publicAudioUrl,
                        publicAudioUrl = publicAudioUrl,
                        audioFileSizeBytes = audioDest.length()
                    )
                } else {
                    ep
                }
            }

            // 3. Generate podcast.xml
            val publicFeedUrl = "${publicBaseUrlPlaceholder.trimEnd('/')}/podcast.xml"
            val showWithUrls = show.copy(
                publicFeedUrl = publicFeedUrl,
                artworkRemoteUrl = relativeArtworkPath?.let { "${publicBaseUrlPlaceholder.trimEnd('/')}/$it" }
            )

            val xmlContent = PodcastRssGenerator.generateFeedXml(
                show = showWithUrls,
                episodes = episodesWithLocalEnclosures,
                overrideFeedUrl = publicFeedUrl
            )

            val feedFile = File(targetDirectory, "podcast.xml")
            FileOutputStream(feedFile).use { fos ->
                fos.write(xmlContent.toByteArray(StandardCharsets.UTF_8))
            }
            totalBytes += feedFile.length()

            // 4. Generate Persian Publication Guide (README_PUBLISHING_FA.txt)
            val readmeContent = generateReadmeGuide(show, audioFilesCount, publicBaseUrlPlaceholder)
            val readmeFile = File(targetDirectory, "راهنمای_انتشار_پادکست.txt")
            FileOutputStream(readmeFile).use { fos ->
                fos.write(readmeContent.toByteArray(StandardCharsets.UTF_8))
            }
            totalBytes += readmeFile.length()

            Result.success(
                LocalExportPackageSummary(
                    exportDirectoryPath = targetDirectory.absolutePath,
                    feedFilePath = feedFile.absolutePath,
                    totalAudioFilesCopied = audioFilesCount,
                    totalBytesExported = totalBytes,
                    hasArtwork = hasArtwork,
                    readmePath = readmeFile.absolutePath
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("خطا در ایجاد بسته صادرات محلی پادکست: ${e.message}"))
        }
    }

    private fun generateReadmeGuide(
        show: PodcastShowEntity,
        audioCount: Int,
        baseUrlPlaceholder: String
    ): String {
        return """
========================================================================
بسته صادرات آفلاین استودیوی پارس‌پادکست (ParsPodcast Studio)
عنوان پادکست: ${show.title}
تعداد فایل‌های صوتی اپیزودها: $audioCount
تاریخ تولید بسته: ${java.util.Date()}
========================================================================

این بسته شامل تمامی فایل‌های لازم جهت میزبانی مستقل پادکست شما بر روی هر سرویس میزبانی دلخواه است:
۱. فایل فید استاندارد: podcast.xml (مطابق استانداردهای RSS 2.0 و iTunes)
۲. فایل‌های صوتی اپیزودها با فرمت استاندارد WAV / M4A
۳. تصویر کاور استاندارد: cover.jpg (نسبت ۱:۱ مربع)

------------------------------------------------------------------------
مراحل انتشار در فضای ابری و وب‌سرور خودمیزبان:
------------------------------------------------------------------------
۱. تمامی فایل‌های این پوشه را در شاخه عمومی وب‌سرور، مخزن GitHub Pages، باکت S3 یا سرور WebDAV خود آپلود نمایید.
۲. در صورتی که دامنه یا نشانی میزبانی شما با $baseUrlPlaceholder تفاوت دارد:
   - فایل podcast.xml را با یک ویرایشگر متن باز کنید.
   - نشانی $baseUrlPlaceholder را با نشانی دامنه واقعی خود جایگزین فرمایید.
   - تگ <atom:link> و تگ‌های <enclosure url="..."> به صورت خودکار به نشانی جدید اشاره خواهند کرد.
۳. نشانی مستقیم فایل podcast.xml در اینترنت (مثلاً https://yourdomain.ir/podcast/podcast.xml) را در دایرکتوری‌های بزرگ پادکست ثبت کنید:
   - Apple Podcasts Connect: https://podcastsconnect.apple.com
   - Spotify for Podcasters: https://podcasters.spotify.com
   - Amazon Music / Castbox / Pocket Casts

نکته مهم امنیتی و اجرایی:
استودیوی پارس‌پادکست به صورت ۱۰۰٪ آفلاین و محلی روی دستگاه شما اجرا شده و هیچ وابستگی به سرویس‌های متمرکز خارجی ندارد. مالکیت فید، فایل‌های صوتی و مخاطبان شما کاملاً در اختیار خود شماست.
========================================================================
""".trimIndent()
    }
}
