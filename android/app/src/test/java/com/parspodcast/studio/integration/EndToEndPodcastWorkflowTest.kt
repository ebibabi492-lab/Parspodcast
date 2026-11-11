package com.parspodcast.studio.integration

import com.parspodcast.studio.data.audio.MultitrackAudioMixer
import com.parspodcast.studio.data.audio.WavAudioWriter
import com.parspodcast.studio.data.local.entities.PodcastEpisodeEntity
import com.parspodcast.studio.data.local.entities.PodcastShowEntity
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.domain.news.NewsNormalizer
import com.parspodcast.studio.domain.news.OfflineTextNewsParser
import com.parspodcast.studio.domain.parser.ScriptJsonParser
import com.parspodcast.studio.domain.podcast.PodcastFeedValidator
import com.parspodcast.studio.domain.podcast.PodcastRssGenerator
import com.parspodcast.studio.domain.prompt.PersianPromptBuilder
import com.parspodcast.studio.domain.tts.PersianSentenceSplitter
import com.parspodcast.studio.domain.tts.PersianTTSPreprocessor
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Date

class EndToEndPodcastWorkflowTest {

    @Test
    fun testCompletePodcastPipeline_NewsToScriptToAudioToRss() {
        // =========================================================================
        // WORKFLOW A: NEWS INGESTION TO SCRIPT GENERATION
        // =========================================================================
        val sampleNewsRaw = """
            # رویداد اول: توسعه مدل‌های زبانی بومی
            گروهی از پژوهشگران ایرانی موفق به بهینه‌سازی مدل‌های خانواده Gemma 4 برای دستگاه‌های همراه شدند. این سیستم بدون نیاز به اینترنت عمل می‌کند.
            ---
            # رویداد دوم: گسترش پادکست‌های تحلیلی
            استودیوهای تولید محتوا به سمت استفاده از معماری پردازش لبه‌ای برای کاهش هزینه‌ها حرکت می‌کنند.
        """.trimIndent()

        val parsedDrafts = OfflineTextNewsParser.parseText(sampleNewsRaw, "فایل آفلاین تست", "اخبار فناوری")
        assertEquals(2, parsedDrafts.size)

        // Normalization
        val normalizedTitle = NewsNormalizer.normalizePersian(parsedDrafts[0].title)
        assertFalse(normalizedTitle.contains("ي")) // Arabic Yeh normalized to Persian
        assertFalse(normalizedTitle.contains("ك")) // Arabic Keh normalized to Persian

        // Prompt construction
        val (systemPrompt, userPrompt) = PersianPromptBuilder.buildDualHostScriptPrompt(
            topicTitle = "فناوری آفلاین هوش مصنوعی",
            targetDurationMinutes = 5,
            tone = "نیمه‌رسمی تحلیلی",
            articles = emptyList()
        )
        assertTrue(systemPrompt.contains("JSON"))
        assertTrue(userPrompt.contains("PRESENTER_A") && userPrompt.contains("PRESENTER_B"))

        // Script JSON Parsing
        val validJson = """
        {
          "title": "فناوری آفلاین هوش مصنوعی",
          "summary": "بررسی دستاوردهای جدید پردازش لبه‌ای در تولید پادکست",
          "estimated_duration_minutes": 5,
          "segments": [
            {
              "type": "INTRO",
              "speaker": "PRESENTER_A",
              "text": "سلام و درود به شنوندگان گرامی پارس‌پادکست، امروز به بررسی استقلال پردازش هوش مصنوعی می‌پردازیم.",
              "estimated_seconds": 15
            },
            {
              "type": "DISCUSSION",
              "speaker": "PRESENTER_B",
              "text": "بله دقیقاً، مدل‌های سبک مانند Gemma 4 اکنون روی گوشی‌های همراه بدون سرور ابری اجرا می‌شوند.",
              "estimated_seconds": 20
            }
          ]
        }
        """.trimIndent()

        val parsedScript = ScriptJsonParser.parseScript(validJson)
        assertEquals(2, parsedScript.segments.size)
        assertEquals("PRESENTER_A", parsedScript.segments[0].speakerRole)
        assertEquals("PRESENTER_B", parsedScript.segments[1].speakerRole)

        // =========================================================================
        // WORKFLOW B: SCRIPT TO VOICE TTS
        // =========================================================================
        val segment1Text = parsedScript.segments[0].persianText
        val preprocessedText = PersianTTSPreprocessor.preprocessForSynthesis(segment1Text)
        assertTrue(preprocessedText.isNotBlank())

        val chunks = PersianSentenceSplitter.splitIntoChunks(preprocessedText)
        assertTrue(chunks.isNotEmpty())

        // Simulate PCM synthesis & WAV header generation
        val sampleRate = 22050
        val dummyPcmFloats = FloatArray(sampleRate * 2) { 0.05f } // 2 seconds audio
        val pcmBytes = WavAudioWriter.floatTo16BitPcm(dummyPcmFloats)
        val tempWavFile = File.createTempFile("test_tts_seg_", ".wav").apply { deleteOnExit() }

        val bytesWritten = WavAudioWriter.writeWavFile(tempWavFile, pcmBytes, sampleRate)
        assertTrue(bytesWritten > 44)
        assertTrue(tempWavFile.exists())
        assertEquals(pcmBytes.size + 44L, tempWavFile.length())

        // =========================================================================
        // WORKFLOW C: AUDIO STUDIO MULTITRACK MIXING
        // =========================================================================
        val mixer = MultitrackAudioMixer(targetSampleRate = 48000)
        val dummyAudioData = MultitrackAudioMixer.AudioFileData(
            sampleRate = 48000,
            channels = 2,
            samplesLeft = FloatArray(48000 * 3) { 0.1f },
            samplesRight = FloatArray(48000 * 3) { 0.1f }
        )

        val trackVoice = MultitrackAudioMixer.MixTrack(
            id = "t_voice",
            displayNameFa = "صدای مجری الف",
            volume = 1.0f,
            pan = -0.1f,
            isMuted = false,
            isSolo = false,
            duckingEnabled = false,
            clips = listOf(
                MultitrackAudioMixer.MixClip(
                    id = "c1",
                    trackId = "t_voice",
                    timelinePositionMs = 0L,
                    durationMs = 3000L,
                    sourceStartMs = 0L,
                    sourceEndMs = 3000L,
                    fadeInMs = 100L,
                    fadeOutMs = 100L,
                    gainDb = 0f,
                    isMuted = false,
                    audioData = dummyAudioData
                )
            )
        )

        val mixResult = mixer.mixTracks(listOf(trackVoice))
        assertNotNull(mixResult)
        assertEquals(48000, mixResult.sampleRate)
        assertTrue(mixResult.samplesLeft.isNotEmpty())
        assertEquals(mixResult.samplesLeft.size, mixResult.samplesRight.size)

        // Verify WAV rendering output
        val renderedWavBytes = mixer.renderWavFile(mixResult)
        assertTrue(renderedWavBytes.size > 44)
        // Check RIFF header magic bytes
        assertEquals('R'.code.toByte(), renderedWavBytes[0])
        assertEquals('I'.code.toByte(), renderedWavBytes[1])
        assertEquals('F'.code.toByte(), renderedWavBytes[2])
        assertEquals('F'.code.toByte(), renderedWavBytes[3])

        // =========================================================================
        // WORKFLOW D: PODCAST PUBLISHING & RSS 2.0 VALIDATION
        // =========================================================================
        val show = PodcastShowEntity(
            id = "show_main",
            title = "پارس‌پادکست",
            subtitle = "فناوری، هوش مصنوعی و جامعه",
            description = "تحلیل رویدادهای فناوری با پردازش صوت روی دستگاه",
            author = "استودیوی پارس‌پادکست",
            ownerName = "پارس‌پادکست",
            ownerEmail = "studio@parspodcast.ir",
            websiteUrl = "https://parspodcast.ir",
            language = "fa",
            category = "News",
            subcategory = "Tech News",
            copyright = "تمامی حقوق محفوظ است © ۱۴۰۵",
            explicitContent = false,
            artworkRemoteUrl = "https://parspodcast.ir/cover.jpg",
            publicFeedUrl = "https://parspodcast.ir/feed.xml"
        )

        val stableGuid = "urn:uuid:e2e-stable-guid-98765"
        val episode = PodcastEpisodeEntity(
            id = stableGuid,
            projectId = "proj_test_1",
            showId = "show_main",
            title = "اپیزود ۱: فناوری آفلاین هوش مصنوعی",
            descriptionFa = "بررسی دستاوردهای جدید پردازش لبه‌ای در تولید پادکست فارسی",
            episodeNumber = 1,
            seasonNumber = 1,
            episodeType = "full",
            durationSeconds = 180,
            publicationDateUtc = 1731369600000L,
            pubDateRfc822 = "Tue, 12 Nov 2024 00:00:00 GMT",
            enclosureUrl = "https://media.parspodcast.ir/podcasts/ep1.m4a",
            audioMimeType = "audio/mp4",
            audioFileSizeBytes = 15000000L,
            publishingStatus = "PUBLISHED"
        )

        val rssXml = PodcastRssGenerator.generateFeedXml(
            show = show,
            episodes = listOf(episode),
            overrideFeedUrl = "https://parspodcast.ir/feed.xml"
        )

        val validationReport = PodcastFeedValidator.validateFeedXml(rssXml)
        assertTrue("RSS XML should be fully valid", validationReport.isValid)
        assertFalse("Should have no fatal errors", validationReport.hasFatalErrors)
        assertEquals(1, validationReport.totalEpisodes)
        assertTrue(rssXml.contains("<guid isPermaLink=\"false\">$stableGuid</guid>"))

        // =========================================================================
        // WORKFLOW E: JALALI CALENDAR & LOCALIZATION
        // =========================================================================
        val jDate = JalaliCalendar.fromDate(Date(1731369600000L))
        val jFormatted = jDate.toFormattedString(usePersianDigits = true)
        assertTrue(jFormatted.contains("آبان") || jFormatted.contains("۱۴۰۳") || jFormatted.contains("۱۴۰۵"))
    }
}
