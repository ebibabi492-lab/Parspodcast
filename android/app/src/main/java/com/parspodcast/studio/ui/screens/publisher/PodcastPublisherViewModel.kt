package com.parspodcast.studio.ui.screens.publisher

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.parspodcast.studio.data.audio.AudioPreparationResult
import com.parspodcast.studio.data.audio.AudioPublishingPreparer
import com.parspodcast.studio.data.audio.PublishingAudioFormat
import com.parspodcast.studio.data.hosting.*
import com.parspodcast.studio.data.local.entities.HostingConfigEntity
import com.parspodcast.studio.data.local.entities.PodcastEpisodeEntity
import com.parspodcast.studio.data.local.entities.PodcastShowEntity
import com.parspodcast.studio.data.local.entities.ProductionProjectEntity
import com.parspodcast.studio.data.manager.PodcastArtworkManager
import com.parspodcast.studio.data.repository.ProductionRepository
import com.parspodcast.studio.data.repository.PublishingRepository
import com.parspodcast.studio.data.security.SecureCredentialManager
import com.parspodcast.studio.data.sync.PodcastUploadWorker
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.domain.podcast.FeedValidationReport
import com.parspodcast.studio.domain.podcast.PodcastFeedValidator
import com.parspodcast.studio.domain.podcast.PodcastRssGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject

sealed class PublishingUiState {
    object Idle : PublishingUiState()
    data class InProgress(val stepTitle: String, val percent: Int) : PublishingUiState()
    data class Success(val feedUrl: String, val audioUrl: String) : PublishingUiState()
    data class Error(val message: String) : PublishingUiState()
}

@HiltViewModel
class PodcastPublisherViewModel @Inject constructor(
    private val publishingRepository: PublishingRepository,
    private val productionRepository: ProductionRepository,
    private val artworkManager: PodcastArtworkManager,
    private val audioPreparer: AudioPublishingPreparer,
    private val localExportProvider: LocalExportProvider,
    private val credentialManager: SecureCredentialManager,
    private val feedVerifier: PublicFeedVerifier,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val shows: StateFlow<List<PodcastShowEntity>> = publishingRepository.getAllShows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedShow = MutableStateFlow<PodcastShowEntity?>(null)
    val selectedShow: StateFlow<PodcastShowEntity?> = _selectedShow.asStateFlow()

    val episodes: StateFlow<List<PodcastEpisodeEntity>> = _selectedShow
        .flatMapLatest { show ->
            if (show == null) publishingRepository.getAllEpisodes()
            else publishingRepository.getEpisodesForShow(show.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedProjects: StateFlow<List<ProductionProjectEntity>> = productionRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hostingConfigs: StateFlow<List<HostingConfigEntity>> = publishingRepository.getAllHostingConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedConfig = MutableStateFlow<HostingConfigEntity?>(null)
    val selectedConfig: StateFlow<HostingConfigEntity?> = _selectedConfig.asStateFlow()

    private val _generatedRssXml = MutableStateFlow("")
    val generatedRssXml: StateFlow<String> = _generatedRssXml.asStateFlow()

    private val _validationReport = MutableStateFlow<FeedValidationReport?>(null)
    val validationReport: StateFlow<FeedValidationReport?> = _validationReport.asStateFlow()

    private val _publishingState = MutableStateFlow<PublishingUiState>(PublishingUiState.Idle)
    val publishingState: StateFlow<PublishingUiState> = _publishingState.asStateFlow()

    private val _verificationSummary = MutableStateFlow<PublicFeedVerificationSummary?>(null)
    val verificationSummary: StateFlow<PublicFeedVerificationSummary?> = _verificationSummary.asStateFlow()

    private val _isVerifyingFeed = MutableStateFlow(false)
    val isVerifyingFeed: StateFlow<Boolean> = _isVerifyingFeed.asStateFlow()

    private val _exportPackageSummary = MutableStateFlow<LocalExportPackageSummary?>(null)
    val exportPackageSummary: StateFlow<LocalExportPackageSummary?> = _exportPackageSummary.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    init {
        // Initialize default show if empty
        viewModelScope.launch {
            shows.collect { showList ->
                if (showList.isEmpty()) {
                    val defaultShow = PodcastShowEntity(
                        id = "default_show",
                        title = "پارس‌پادکست",
                        subtitle = "فناوری، هوش مصنوعی و جامعه",
                        description = "پادکست تخصصی تحلیل اخبار و رویدادهای فناوری با پردازش صوت روی دستگاه و مدل محلی گما",
                        author = "استودیوی پارس‌پادکست",
                        ownerName = "پارس‌پادکست",
                        ownerEmail = "studio@parspodcast.ir",
                        websiteUrl = "https://parspodcast.ir",
                        language = "fa",
                        category = "News",
                        subcategory = "Tech News",
                        copyright = "تمامی حقوق محفوظ است © ۱۴۰۵",
                        explicitContent = false,
                        showType = "episodic",
                        publicFeedUrl = "https://parspodcast.ir/feed.xml"
                    )
                    publishingRepository.insertShow(defaultShow)
                    _selectedShow.value = defaultShow
                } else if (_selectedShow.value == null) {
                    _selectedShow.value = showList.first()
                }
            }
        }
    }

    fun selectShow(show: PodcastShowEntity) {
        _selectedShow.value = show
        generateRssPreview(show)
    }

    fun selectHostingConfig(config: HostingConfigEntity) {
        _selectedConfig.value = config
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun saveShow(show: PodcastShowEntity) {
        viewModelScope.launch {
            if (shows.value.any { it.id == show.id }) {
                publishingRepository.updateShow(show.copy(updatedAt = System.currentTimeMillis()))
            } else {
                publishingRepository.insertShow(show)
            }
            _selectedShow.value = show
            _uiMessage.value = "مشخصات پادکست ذخیره شد."
            generateRssPreview(show)
        }
    }

    fun deleteShow(show: PodcastShowEntity) {
        viewModelScope.launch {
            publishingRepository.deleteShow(show)
            _selectedShow.value = shows.value.firstOrNull { it.id != show.id }
            _uiMessage.value = "پادکست با موفقیت حذف شد."
        }
    }

    fun createEpisodeFromProject(project: ProductionProjectEntity, showId: String) {
        viewModelScope.launch {
            val nowUtc = System.currentTimeMillis()
            val jDate = JalaliCalendar.fromDate(Date(nowUtc)).toFormattedString()
            val rfc822 = PodcastRssGenerator.formatRfc822Date(nowUtc)

            val currentEpCount = episodes.value.size
            val nextEpNumber = currentEpCount + 1

            // Look for existing rendered mix wav in project directory or standard output
            val defaultProjectAudioPath = "${context.filesDir}/audio/projects/${project.id}/master_mix.wav"
            val existingAudioFile = File(defaultProjectAudioPath)

            val newEpisode = PodcastEpisodeEntity(
                id = "ep_${UUID.randomUUID()}",
                projectId = project.id,
                showId = showId,
                title = project.title,
                subtitle = "اپیزود تولید شده از پروژه استودیوی میکس",
                descriptionFa = project.description.ifBlank { "اپیزود تولید شده در استودیوی پارس‌پادکست" },
                summary = project.description,
                episodeNumber = nextEpNumber,
                seasonNumber = 1,
                episodeType = "full",
                author = "استودیوی پارس‌پادکست",
                keywords = "پادکست, اخبار, هوش مصنوعی, فناوری",
                explicitContent = false,
                durationSeconds = 0,
                publicationDateUtc = nowUtc,
                createdAt = nowUtc,
                updatedAt = nowUtc,
                localAudioPath = if (existingAudioFile.exists()) defaultProjectAudioPath else "",
                audioMimeType = "audio/wav",
                audioFileSizeBytes = if (existingAudioFile.exists()) existingAudioFile.length() else 0L,
                jalaliPubDate = jDate,
                pubDateRfc822 = rfc822,
                publishingStatus = "DRAFT"
            )

            publishingRepository.insertEpisode(newEpisode)
            _uiMessage.value = "پیش‌نویس اپیزود جدید با موفقیت ایجاد شد."
            selectedShow.value?.let { generateRssPreview(it) }
        }
    }

    fun updateEpisode(episode: PodcastEpisodeEntity) {
        viewModelScope.launch {
            publishingRepository.updateEpisode(episode.copy(updatedAt = System.currentTimeMillis()))
            _uiMessage.value = "متادیتای اپیزود به‌روزرسانی شد."
            selectedShow.value?.let { generateRssPreview(it) }
        }
    }

    fun deleteEpisode(episode: PodcastEpisodeEntity) {
        viewModelScope.launch {
            publishingRepository.deleteEpisode(episode)
            _uiMessage.value = "اپیزود حذف شد."
            selectedShow.value?.let { generateRssPreview(it) }
        }
    }

    fun processShowArtwork(uri: Uri, show: PodcastShowEntity) {
        viewModelScope.launch {
            val result = artworkManager.processAndSaveArtwork(uri)
            if (result.isValid && result.savedPath != null) {
                val updatedShow = show.copy(
                    artworkLocalPath = result.savedPath,
                    updatedAt = System.currentTimeMillis()
                )
                publishingRepository.updateShow(updatedShow)
                _selectedShow.value = updatedShow
                val warningMsg = if (result.warnings.isNotEmpty()) " (${result.warnings.first()})" else ""
                _uiMessage.value = "تصویر کاور پادکست استاندارد ذخیره شد$warningMsg"
                generateRssPreview(updatedShow)
            } else {
                _uiMessage.value = "خطا در پردازش کاور: ${result.error ?: "تصویر نامعتبر است"}"
            }
        }
    }

    fun prepareAudioForEpisode(
        episode: PodcastEpisodeEntity,
        format: PublishingAudioFormat,
        sourceFile: File
    ) {
        viewModelScope.launch {
            _publishingState.value = PublishingUiState.InProgress("در حال فشرده‌سازی و آماده‌سازی فایل صوتی...", 15)
            val prepResult = audioPreparer.prepareAudioForPublishing(
                sourceWavFile = sourceFile,
                targetFormat = format,
                outputFileName = "ep_${episode.episodeNumber}_${episode.id.take(8)}"
            )

            if (prepResult.isSuccess && prepResult.exportedFile != null) {
                val updated = episode.copy(
                    localAudioPath = prepResult.exportedFile.absolutePath,
                    audioMimeType = prepResult.mimeType,
                    audioFileSizeBytes = prepResult.fileSizeBytes,
                    durationSeconds = prepResult.durationSeconds,
                    audioChecksum = prepResult.checksumSha256,
                    publishingStatus = "READY",
                    updatedAt = System.currentTimeMillis()
                )
                publishingRepository.updateEpisode(updated)
                _publishingState.value = PublishingUiState.Idle
                _uiMessage.value = "فایل صوتی برای انتشار آماده شد (${format.displayName}، ${prepResult.fileSizeBytes / 1024} کیلوبایت)."
                selectedShow.value?.let { generateRssPreview(it) }
            } else {
                _publishingState.value = PublishingUiState.Error(prepResult.error ?: "خطا در پردازش فایل صوتی")
            }
        }
    }

    fun generateRssPreview(show: PodcastShowEntity) {
        viewModelScope.launch {
            val currentEps = episodes.value
            val xml = PodcastRssGenerator.generateFeedXml(
                show = show,
                episodes = currentEps,
                includeOnlyPublishedOrReady = false
            )
            _generatedRssXml.value = xml
            _validationReport.value = PodcastFeedValidator.validateFeedXml(xml)
        }
    }

    fun exportLocalPublicationPackage(
        show: PodcastShowEntity,
        targetDir: File,
        baseUrlPlaceholder: String
    ) {
        viewModelScope.launch {
            _publishingState.value = PublishingUiState.InProgress("تولید بسته صادرات آفلاین...", 20)
            val currentEps = episodes.value
            val res = localExportProvider.createPublicationPackage(
                targetDirectory = targetDir,
                show = show,
                episodes = currentEps,
                publicBaseUrlPlaceholder = baseUrlPlaceholder
            )

            if (res.isSuccess) {
                val summary = res.getOrThrow()
                _exportPackageSummary.value = summary
                _publishingState.value = PublishingUiState.Idle
                _uiMessage.value = "بسته انتشار در ${summary.exportDirectoryPath} ذخیره شد (${summary.totalAudioFilesCopied} فایل صوتی)."
            } else {
                val err = res.exceptionOrNull()?.message ?: "خطا در صادرات"
                _publishingState.value = PublishingUiState.Error(err)
            }
        }
    }

    fun saveHostingConfiguration(
        name: String,
        serverUrl: String,
        remoteBasePath: String,
        publicBaseUrl: String,
        username: String,
        passwordPlain: String
    ) {
        viewModelScope.launch {
            val encryptedPassword = credentialManager.encrypt(passwordPlain)
            val config = HostingConfigEntity(
                id = "cfg_${UUID.randomUUID()}",
                name = name,
                providerType = "WEBDAV",
                serverUrl = serverUrl,
                remoteBasePath = remoteBasePath,
                publicBaseUrl = publicBaseUrl,
                username = username,
                encryptedPassword = encryptedPassword,
                isDefault = true
            )
            publishingRepository.insertHostingConfig(config)
            _selectedConfig.value = config
            _uiMessage.value = "تنظیمات سرور میزبانی WebDAV با موفقیت ذخیره شد."
        }
    }

    suspend fun testWebDavConnection(
        serverUrl: String,
        remoteBasePath: String,
        publicBaseUrl: String,
        username: String,
        passwordPlain: String
    ): Result<Boolean> {
        return try {
            val provider = WebDavHostingProvider(
                serverUrl = serverUrl,
                remoteBasePath = remoteBasePath,
                publicBaseUrl = publicBaseUrl,
                username = username,
                passwordPlain = passwordPlain
            )
            provider.testConnection()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun publishEpisodeViaWorkManager(
        episode: PodcastEpisodeEntity,
        show: PodcastShowEntity,
        config: HostingConfigEntity
    ) {
        viewModelScope.launch {
            _publishingState.value = PublishingUiState.InProgress("در حال ثبت درخواست انتشار...", 5)

            val inputData = Data.Builder()
                .putString(PodcastUploadWorker.KEY_EPISODE_ID, episode.id)
                .putString(PodcastUploadWorker.KEY_SHOW_ID, show.id)
                .putString(PodcastUploadWorker.KEY_CONFIG_ID, config.id)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<PodcastUploadWorker>()
                .setInputData(inputData)
                .build()

            val workManager = WorkManager.getInstance(context)
            workManager.enqueue(workRequest)

            // Observe work info
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { info ->
                if (info != null) {
                    when (info.state) {
                        WorkInfo.State.RUNNING -> {
                            val step = info.progress.getString(PodcastUploadWorker.KEY_PROGRESS_STEP) ?: "در حال بارگذاری..."
                            val p = info.progress.getInt(PodcastUploadWorker.KEY_PROGRESS_PERCENT, 30)
                            _publishingState.value = PublishingUiState.InProgress(step, p)
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val feedUrl = info.outputData.getString("public_feed_url") ?: show.publicFeedUrl.orEmpty()
                            val audioUrl = info.outputData.getString("public_audio_url").orEmpty()
                            _publishingState.value = PublishingUiState.Success(feedUrl, audioUrl)
                            _uiMessage.value = "اپیزود با موفقیت منتشر شد و فید RSS به‌روز گردید!"
                        }
                        WorkInfo.State.FAILED -> {
                            val err = info.outputData.getString(PodcastUploadWorker.KEY_ERROR_MESSAGE) ?: "خطا در عملیات انتشار"
                            _publishingState.value = PublishingUiState.Error(err)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun verifyPublicFeed(feedUrl: String) {
        viewModelScope.launch {
            _isVerifyingFeed.value = true
            val summary = feedVerifier.verifyPublicFeed(feedUrl)
            _verificationSummary.value = summary
            _isVerifyingFeed.value = false
            if (summary.isFullyVerified) {
                _uiMessage.value = "فید عمومی و کلیه فایل‌های صوتی با موفقیت تأیید شدند!"
            } else {
                _uiMessage.value = "برخی خطاها یا هشدارها در بررسی فید عمومی گزارش شده است."
            }
        }
    }
}
