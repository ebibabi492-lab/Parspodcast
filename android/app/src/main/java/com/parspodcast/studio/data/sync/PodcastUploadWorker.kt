package com.parspodcast.studio.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.parspodcast.studio.data.hosting.PublicFeedVerifier
import com.parspodcast.studio.data.hosting.WebDavHostingProvider
import com.parspodcast.studio.data.local.ParsPodcastDatabase
import com.parspodcast.studio.data.security.SecureCredentialManager
import com.parspodcast.studio.domain.podcast.PodcastRssGenerator
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

class PodcastUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_EPISODE_ID = "key_episode_id"
        const val KEY_SHOW_ID = "key_show_id"
        const val KEY_CONFIG_ID = "key_config_id"
        const val KEY_PROGRESS_STEP = "key_progress_step"
        const val KEY_PROGRESS_PERCENT = "key_progress_percent"
        const val KEY_ERROR_MESSAGE = "key_error_message"
    }

    override suspend fun doWork(): Result {
        val episodeId = inputData.getString(KEY_EPISODE_ID) ?: return Result.failure()
        val showId = inputData.getString(KEY_SHOW_ID) ?: return Result.failure()
        val configId = inputData.getString(KEY_CONFIG_ID) ?: return Result.failure()

        // Get DB instance safely
        val db = androidx.room.Room.databaseBuilder(
            applicationContext,
            ParsPodcastDatabase::class.java,
            ParsPodcastDatabase.DATABASE_NAME
        ).addMigrations(ParsPodcastDatabase.MIGRATION_1_2).build()

        val publishingDao = db.publishingDao()
        val credentialManager = SecureCredentialManager(applicationContext)
        val feedVerifier = PublicFeedVerifier()

        val episode = publishingDao.getEpisodeById(episodeId)
        val show = publishingDao.getShowById(showId)
        val config = publishingDao.getHostingConfigById(configId)

        if (episode == null || show == null || config == null) {
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "اطلاعات اپیزود، پادکست یا پیکربندی سرور یافت نشد."))
        }

        try {
            // Mark as UPLOADING
            publishingDao.updateEpisode(episode.copy(publishingStatus = "UPLOADING", lastPublicationError = null))
            setProgress(workDataOf(KEY_PROGRESS_STEP to "شروع اتصال به سرور میزبانی...", KEY_PROGRESS_PERCENT to 5))

            val decryptedPassword = credentialManager.decrypt(config.encryptedPassword)
            val hostingProvider = WebDavHostingProvider(
                serverUrl = config.serverUrl,
                remoteBasePath = config.remoteBasePath,
                publicBaseUrl = config.publicBaseUrl,
                username = config.username,
                passwordPlain = decryptedPassword
            )

            // Step 1: Test connection
            val connResult = hostingProvider.testConnection()
            if (connResult.isFailure) {
                val err = connResult.exceptionOrNull()?.message ?: "خطای اتصال به سرور"
                publishingDao.updateEpisode(episode.copy(publishingStatus = "FAILED", lastPublicationError = err))
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to err))
            }

            // Step 2: Upload Artwork if present and not yet remote
            var remoteArtworkUrl = show.artworkRemoteUrl
            if (!show.artworkLocalPath.isNullOrBlank()) {
                val artworkFile = File(show.artworkLocalPath)
                if (artworkFile.exists()) {
                    setProgress(workDataOf(KEY_PROGRESS_STEP to "بارگذاری تصویر کاور پادکست...", KEY_PROGRESS_PERCENT to 20))
                    val artExt = artworkFile.extension.ifBlank { "jpg" }
                    val artUploadResult = hostingProvider.uploadFile(
                        localFile = artworkFile,
                        remoteRelativePath = "cover.$artExt",
                        mimeType = if (artExt == "png") "image/png" else "image/jpeg"
                    )
                    if (artUploadResult.isSuccess) {
                        remoteArtworkUrl = artUploadResult.getOrNull()
                        publishingDao.updateShow(show.copy(artworkRemoteUrl = remoteArtworkUrl))
                    }
                }
            }

            // Step 3: Upload Episode Audio file
            val audioFile = File(episode.localAudioPath)
            if (!audioFile.exists() || audioFile.length() == 0L) {
                val err = "فایل صوتی اپیزود در حافظه دستگاه یافت نشد."
                publishingDao.updateEpisode(episode.copy(publishingStatus = "FAILED", lastPublicationError = err))
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to err))
            }

            setProgress(workDataOf(KEY_PROGRESS_STEP to "بارگذاری فایل صوتی پادکست در سرور...", KEY_PROGRESS_PERCENT to 35))
            val audioRelativeName = "ep_${episode.episodeNumber}_${episode.id.take(8)}.${audioFile.extension}"
            val audioUploadResult = hostingProvider.uploadFile(
                localFile = audioFile,
                remoteRelativePath = audioRelativeName,
                mimeType = episode.audioMimeType,
                onProgress = { p ->
                    val totalP = (35 + (p * 45)).toInt()
                    // Update progress periodically
                }
            )

            if (audioUploadResult.isFailure) {
                val err = audioUploadResult.exceptionOrNull()?.message ?: "خطا در آپلود فایل صوتی"
                publishingDao.updateEpisode(episode.copy(publishingStatus = "FAILED", lastPublicationError = err))
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to err))
            }

            val publicAudioUrl = audioUploadResult.getOrThrow()

            // Step 4: Verify uploaded audio file publicly
            setProgress(workDataOf(KEY_PROGRESS_STEP to "اعتبارسنجی دسترسی عمومی به فایل صوتی...", KEY_PROGRESS_PERCENT to 82))
            val audioVerifyResult = hostingProvider.verifyPublicFile(publicAudioUrl)
            if (audioVerifyResult.isSuccess && !audioVerifyResult.getOrThrow().isAccessible) {
                val err = "فایل بارگذاری شد ولی نشانی عمومی آن قابل دسترس نیست: $publicAudioUrl"
                publishingDao.updateEpisode(episode.copy(publishingStatus = "FAILED", lastPublicationError = err))
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to err))
            }

            // Step 5: Update episode with public URL
            val updatedEpisode = episode.copy(
                enclosureUrl = publicAudioUrl,
                publicAudioUrl = publicAudioUrl,
                audioFileSizeBytes = audioFile.length(),
                publishingStatus = "READY"
            )
            publishingDao.updateEpisode(updatedEpisode)

            // Step 6: Generate updated RSS XML and upload it
            setProgress(workDataOf(KEY_PROGRESS_STEP to "تولید و انتشار فید استاندارد RSS 2.0...", KEY_PROGRESS_PERCENT to 88))
            val allEpisodes = publishingDao.getEpisodesForShowSync(showId).map {
                if (it.id == updatedEpisode.id) updatedEpisode else it
            }

            val publicFeedUrl = hostingProvider.getPublicUrl("podcast.xml")
            val updatedShow = show.copy(
                publicFeedUrl = publicFeedUrl,
                artworkRemoteUrl = remoteArtworkUrl,
                updatedAt = System.currentTimeMillis()
            )

            val xmlContent = PodcastRssGenerator.generateFeedXml(
                show = updatedShow,
                episodes = allEpisodes,
                overrideFeedUrl = publicFeedUrl,
                includeOnlyPublishedOrReady = true
            )

            val tempRssFile = File(applicationContext.cacheDir, "temp_podcast.xml")
            FileOutputStream(tempRssFile).use { fos ->
                fos.write(xmlContent.toByteArray(StandardCharsets.UTF_8))
            }

            val rssUploadResult = hostingProvider.uploadFile(
                localFile = tempRssFile,
                remoteRelativePath = "podcast.xml",
                mimeType = "application/rss+xml"
            )

            if (rssUploadResult.isFailure) {
                val err = "فایل صوتی بارگذاری شد اما به‌روزرسانی فید با خطا مواجه شد: ${rssUploadResult.exceptionOrNull()?.message}"
                publishingDao.updateEpisode(updatedEpisode.copy(publishingStatus = "UPDATE_PENDING", lastPublicationError = err))
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to err))
            }

            // Step 7: Verify public feed URL
            setProgress(workDataOf(KEY_PROGRESS_STEP to "اعتبارسنجی نهایی فید عمومی...", KEY_PROGRESS_PERCENT to 95))
            val feedVerify = feedVerifier.verifyPublicFeed(publicFeedUrl)

            // Step 8: Mark episode as PUBLISHED
            val finalEpisode = updatedEpisode.copy(
                publishingStatus = "PUBLISHED",
                publishedAt = System.currentTimeMillis(),
                lastPublicationError = null
            )
            publishingDao.updateEpisode(finalEpisode)
            publishingDao.updateShow(updatedShow)

            setProgress(workDataOf(KEY_PROGRESS_STEP to "انتشار رسمی با موفقیت پایان یافت.", KEY_PROGRESS_PERCENT to 100))
            Result.success(workDataOf(
                "public_feed_url" to publicFeedUrl,
                "public_audio_url" to publicAudioUrl
            ))
        } catch (e: Exception) {
            val err = "خطای غیرمنتظره در فرآیند انتشار: ${e.message}"
            publishingDao.updateEpisode(episode.copy(publishingStatus = "FAILED", lastPublicationError = err))
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to err))
        }
    }
}
