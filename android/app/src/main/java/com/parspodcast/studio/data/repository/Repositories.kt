package com.parspodcast.studio.data.repository

import com.parspodcast.studio.data.local.dao.*
import com.parspodcast.studio.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface NewsRepository {
    fun getAllSources(): Flow<List<NewsSourceEntity>>
    fun getAllArticles(): Flow<List<NewsArticleEntity>>
    fun getAllEvents(): Flow<List<NewsEventEntity>>
    suspend fun insertSource(source: NewsSourceEntity)
    suspend fun updateSource(source: NewsSourceEntity)
    suspend fun deleteSource(source: NewsSourceEntity)
    suspend fun insertArticles(articles: List<NewsArticleEntity>): List<Long>
    suspend fun updateArticle(article: NewsArticleEntity)
    suspend fun deleteArticle(article: NewsArticleEntity)
    suspend fun deleteArticles(articles: List<NewsArticleEntity>)
    suspend fun toggleFavorite(articleId: String, isFavorite: Boolean)
    suspend fun markAsRead(articleId: String, isRead: Boolean)
    suspend fun insertEvent(event: NewsEventEntity)
    suspend fun syncAllFeeds(): Result<com.parspodcast.studio.domain.news.SyncSummary>
    suspend fun testFeed(url: String): Result<Pair<String, Int>>
    suspend fun saveDrafts(drafts: List<com.parspodcast.studio.domain.news.ParsedNewsDraft>): Result<Int>
}

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val newsDao: NewsDao,
    private val rssFetcher: com.parspodcast.studio.data.news.RssFeedFetcher
) : NewsRepository {
    override fun getAllSources(): Flow<List<NewsSourceEntity>> = newsDao.getAllSources()
    override fun getAllArticles(): Flow<List<NewsArticleEntity>> = newsDao.getAllArticles()
    override fun getAllEvents(): Flow<List<NewsEventEntity>> = newsDao.getAllEvents()

    override suspend fun insertSource(source: NewsSourceEntity) = newsDao.insertSource(source)
    override suspend fun updateSource(source: NewsSourceEntity) = newsDao.updateSource(source)
    override suspend fun deleteSource(source: NewsSourceEntity) = newsDao.deleteSource(source)

    override suspend fun insertArticles(articles: List<NewsArticleEntity>): List<Long> = newsDao.insertArticles(articles)
    override suspend fun updateArticle(article: NewsArticleEntity) = newsDao.updateArticle(article)
    override suspend fun deleteArticle(article: NewsArticleEntity) = newsDao.deleteArticle(article)
    override suspend fun deleteArticles(articles: List<NewsArticleEntity>) = newsDao.deleteArticles(articles)

    override suspend fun toggleFavorite(articleId: String, isFavorite: Boolean) {
        newsDao.setArticleFavorite(articleId, isFavorite)
    }

    override suspend fun markAsRead(articleId: String, isRead: Boolean) {
        newsDao.setArticleRead(articleId, isRead)
    }

    override suspend fun insertEvent(event: NewsEventEntity) = newsDao.insertEvent(event)

    override suspend fun testFeed(url: String): Result<Pair<String, Int>> {
        return rssFetcher.testFeed(url)
    }

    override suspend fun syncAllFeeds(): Result<com.parspodcast.studio.domain.news.SyncSummary> {
        val sources = kotlinx.coroutines.flow.first(newsDao.getAllSources()).filter { it.isActive }
        var totalSynced = 0
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()

        for (source in sources) {
            val fetchResult = rssFetcher.fetchFeed(source.feedUrl, source.name, source.category)
            if (fetchResult.isSuccess) {
                val drafts = fetchResult.getOrDefault(emptyList())
                val entities = drafts.map { draft ->
                    NewsArticleEntity(
                        id = "art_${java.util.UUID.randomUUID()}",
                        sourceId = source.id,
                        sourceName = source.name,
                        title = draft.title,
                        content = draft.content,
                        originalUrl = draft.articleUrl,
                        author = draft.author,
                        category = draft.category,
                        publishedAtUtc = draft.publishedAtUtc,
                        importedAtUtc = System.currentTimeMillis(),
                        rawSummary = draft.summary,
                        deduplicationHash = draft.contentHash,
                        inputType = draft.inputType.name,
                        originalContent = draft.content
                    )
                }
                val insertedIds = newsDao.insertArticles(entities)
                val newCount = insertedIds.count { it != -1L }
                totalSynced += newCount
                successCount++
                newsDao.updateSourceSyncStatus(source.id, System.currentTimeMillis(), 0, null, source.articleCount + newCount)
            } else {
                failCount++
                val err = fetchResult.exceptionOrNull()?.message ?: "خطای ناشناخته در دریافت فید"
                errors.add("${source.name}: $err")
                newsDao.updateSourceSyncStatus(source.id, source.lastFetchedUtc, source.fetchErrorCount + 1, err, source.articleCount)
            }
        }

        return Result.success(
            com.parspodcast.studio.domain.news.SyncSummary(
                totalSynced = totalSynced,
                sourcesSuccess = successCount,
                sourcesFailed = failCount,
                errors = errors
            )
        )
    }

    override suspend fun saveDrafts(drafts: List<com.parspodcast.studio.domain.news.ParsedNewsDraft>): Result<Int> {
        val selected = drafts.filter { it.isSelected }
        if (selected.isEmpty()) return Result.success(0)

        // Ensure default offline source exists in database
        val offlineSource = NewsSourceEntity(
            id = "src_offline_file",
            name = "ورود آفلاین از فایل",
            feedUrl = "file://local_storage",
            parserType = "OFFLINE_FILE",
            category = "عمومی",
            isActive = true,
            lastFetchedUtc = System.currentTimeMillis()
        )
        newsDao.insertSource(offlineSource)

        val entities = selected.map { draft ->
            NewsArticleEntity(
                id = "art_off_${java.util.UUID.randomUUID()}",
                sourceId = "src_offline_file",
                sourceName = draft.sourceName,
                title = draft.title,
                content = draft.content,
                originalUrl = draft.articleUrl,
                author = draft.author,
                category = draft.category,
                publishedAtUtc = draft.publishedAtUtc,
                importedAtUtc = System.currentTimeMillis(),
                rawSummary = draft.summary,
                deduplicationHash = draft.contentHash.ifBlank {
                    com.parspodcast.studio.domain.news.NewsNormalizer.computeContentHash(draft.title, draft.content)
                },
                inputType = draft.inputType.name,
                originalContent = draft.content
            )
        }

        val insertedIds = newsDao.insertArticles(entities)
        val successCount = insertedIds.count { it != -1L }
        return Result.success(successCount)
    }
}

interface ProductionRepository {
    fun getAllProjects(): Flow<List<ProductionProjectEntity>>
    suspend fun getProjectById(projectId: String): ProductionProjectEntity?
    suspend fun insertProject(project: ProductionProjectEntity)
    suspend fun deleteProject(project: ProductionProjectEntity)
    suspend fun getLatestScriptForProject(projectId: String): ScriptEntity?
    suspend fun insertScript(script: ScriptEntity)
    fun getSegmentsForScript(scriptId: String): Flow<List<ScriptSegmentEntity>>
    suspend fun insertSegments(segments: List<ScriptSegmentEntity>)
    suspend fun updateSegment(segment: ScriptSegmentEntity)
    fun getTracksForProject(projectId: String): Flow<List<AudioTrackEntity>>
    suspend fun getTracksForProjectSync(projectId: String): List<AudioTrackEntity>
    suspend fun insertTracks(tracks: List<AudioTrackEntity>)
    suspend fun updateTrack(track: AudioTrackEntity)
    suspend fun deleteTrack(track: AudioTrackEntity)
    fun getAllClipsForProject(projectId: String): Flow<List<AudioClipEntity>>
    suspend fun getAllClipsForProjectSync(projectId: String): List<AudioClipEntity>
    fun getClipsForTrack(trackId: String): Flow<List<AudioClipEntity>>
    suspend fun getClipsForTrackSync(trackId: String): List<AudioClipEntity>
    suspend fun insertClip(clip: AudioClipEntity)
    suspend fun insertClips(clips: List<AudioClipEntity>)
    suspend fun updateClip(clip: AudioClipEntity)
    suspend fun deleteClip(clip: AudioClipEntity)
    suspend fun deleteClipById(clipId: String)
}

@Singleton
class ProductionRepositoryImpl @Inject constructor(
    private val productionDao: ProductionDao
) : ProductionRepository {
    override fun getAllProjects(): Flow<List<ProductionProjectEntity>> = productionDao.getAllProjects()
    override suspend fun getProjectById(projectId: String): ProductionProjectEntity? = productionDao.getProjectById(projectId)
    override suspend fun insertProject(project: ProductionProjectEntity) = productionDao.insertProject(project)
    override suspend fun deleteProject(project: ProductionProjectEntity) = productionDao.deleteProject(project)
    override suspend fun getLatestScriptForProject(projectId: String): ScriptEntity? = productionDao.getLatestScriptForProject(projectId)
    override suspend fun insertScript(script: ScriptEntity) = productionDao.insertScript(script)
    override fun getSegmentsForScript(scriptId: String): Flow<List<ScriptSegmentEntity>> = productionDao.getSegmentsForScript(scriptId)
    override suspend fun insertSegments(segments: List<ScriptSegmentEntity>) = productionDao.insertSegments(segments)
    override suspend fun updateSegment(segment: ScriptSegmentEntity) = productionDao.updateSegment(segment)

    override fun getTracksForProject(projectId: String): Flow<List<AudioTrackEntity>> = productionDao.getTracksForProject(projectId)
    override suspend fun getTracksForProjectSync(projectId: String): List<AudioTrackEntity> = productionDao.getTracksForProjectSync(projectId)
    override suspend fun insertTracks(tracks: List<AudioTrackEntity>) = productionDao.insertTracks(tracks)
    override suspend fun updateTrack(track: AudioTrackEntity) = productionDao.updateTrack(track)
    override suspend fun deleteTrack(track: AudioTrackEntity) = productionDao.deleteTrack(track)

    override fun getAllClipsForProject(projectId: String): Flow<List<AudioClipEntity>> = productionDao.getAllClipsForProject(projectId)
    override suspend fun getAllClipsForProjectSync(projectId: String): List<AudioClipEntity> = productionDao.getAllClipsForProjectSync(projectId)
    override fun getClipsForTrack(trackId: String): Flow<List<AudioClipEntity>> = productionDao.getClipsForTrack(trackId)
    override suspend fun getClipsForTrackSync(trackId: String): List<AudioClipEntity> = productionDao.getClipsForTrackSync(trackId)
    override suspend fun insertClip(clip: AudioClipEntity) = productionDao.insertClip(clip)
    override suspend fun insertClips(clips: List<AudioClipEntity>) = productionDao.insertClips(clips)
    override suspend fun updateClip(clip: AudioClipEntity) = productionDao.updateClip(clip)
    override suspend fun deleteClip(clip: AudioClipEntity) = productionDao.deleteClip(clip)
    override suspend fun deleteClipById(clipId: String) = productionDao.deleteClipById(clipId)
}

interface VoiceRepository {
    fun getAllProfiles(): Flow<List<VoiceProfileEntity>>
    suspend fun getProfileForRole(role: String): VoiceProfileEntity?
    suspend fun insertProfile(profile: VoiceProfileEntity)
}

@Singleton
class VoiceRepositoryImpl @Inject constructor(
    private val voiceDao: VoiceDao
) : VoiceRepository {
    override fun getAllProfiles(): Flow<List<VoiceProfileEntity>> = voiceDao.getAllProfiles()
    override suspend fun getProfileForRole(role: String): VoiceProfileEntity? = voiceDao.getProfileForRole(role)
    override suspend fun insertProfile(profile: VoiceProfileEntity) = voiceDao.insertProfile(profile)
}

interface PublishingRepository {
    fun getAllEpisodes(): Flow<List<PodcastEpisodeEntity>>
    suspend fun insertEpisode(episode: PodcastEpisodeEntity)
    suspend fun updateEpisode(episode: PodcastEpisodeEntity)
}

@Singleton
class PublishingRepositoryImpl @Inject constructor(
    private val publishingDao: PublishingDao
) : PublishingRepository {
    override fun getAllEpisodes(): Flow<List<PodcastEpisodeEntity>> = publishingDao.getAllEpisodes()
    override suspend fun insertEpisode(episode: PodcastEpisodeEntity) = publishingDao.insertEpisode(episode)
    override suspend fun updateEpisode(episode: PodcastEpisodeEntity) = publishingDao.updateEpisode(episode)
}

data class ModelStatus(
    val id: String,
    val name: String,
    val version: String,
    val modelType: String, // LLM or TTS
    val sizeBytes: Long,
    val minRamRequiredMb: Int,
    val isInstalled: Boolean,
    val filePath: String?,
    val isLoaded: Boolean = false,
    val runtimeAvailability: String = "LiteRT-LM"
)

interface ModelRepository {
    fun getModelStatuses(): Flow<List<ModelStatus>>
    suspend fun checkModelInstalled(modelId: String): Boolean
    suspend fun setModelLoaded(modelId: String, loaded: Boolean)
}

@Singleton
class ModelRepositoryImpl @Inject constructor() : ModelRepository {
    private val _models = MutableStateFlow(
        listOf(
            ModelStatus(
                id = "gemma-4-e4b-instruct",
                name = "Gemma 4 E4B Instruct",
                version = "4.0-E4B",
                modelType = "LLM (متنی)",
                sizeBytes = 4600000000L, // 4.6 GB
                minRamRequiredMb = 4096,
                isInstalled = false, // Honest state: not installed by default
                filePath = null,
                isLoaded = false,
                runtimeAvailability = "LiteRT-LM v2.4 (نیازمند استقرار وزن‌ها)"
            ),
            ModelStatus(
                id = "gemma-4-e2b-instruct",
                name = "Gemma 4 E2B Instruct",
                version = "4.0-E2B",
                modelType = "LLM (متنی سبک)",
                sizeBytes = 2400000000L, // 2.4 GB
                minRamRequiredMb = 2500,
                isInstalled = false, // Honest state
                filePath = null,
                isLoaded = false,
                runtimeAvailability = "LiteRT-LM v2.4 (نیازمند استقرار وزن‌ها)"
            ),
            ModelStatus(
                id = "fa-ir-mana-piper",
                name = "Mana Persian Piper (Sherpa-ONNX)",
                version = "fa_IR-mana-medium",
                modelType = "TTS (صوت فارسی)",
                sizeBytes = 125000000L, // 125 MB
                minRamRequiredMb = 512,
                isInstalled = false, // Honest state: Piper voice model candidate
                filePath = null,
                isLoaded = false,
                runtimeAvailability = "Sherpa-ONNX Runtime (آماده دریافت)"
            )
        )
    )

    override fun getModelStatuses(): Flow<List<ModelStatus>> = _models.asStateFlow()

    override suspend fun checkModelInstalled(modelId: String): Boolean {
        return _models.value.find { it.id == modelId }?.isInstalled ?: false
    }

    override suspend fun setModelLoaded(modelId: String, loaded: Boolean) {
        _models.value = _models.value.map {
            if (it.id == modelId) it.copy(isLoaded = loaded) else it
        }
    }
}

data class UserSettings(
    val language: String = "فارسی",
    val darkTheme: Boolean = true,
    val usePersianNumerals: Boolean = true,
    val defaultProgramFormat: String = "TWO_PRESENTERS",
    val defaultSpeechRate: Float = 1.0f,
    val targetAudioQuality: String = "MP3_320KBPS",
    val podcastAuthor: String = "استودیوی پارس‌پادکست",
    val podcastCategory: String = "Technology"
)

interface SettingsRepository {
    fun getSettings(): Flow<UserSettings>
    suspend fun updateSettings(settings: UserSettings)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor() : SettingsRepository {
    private val _settings = MutableStateFlow(UserSettings())
    override fun getSettings(): Flow<UserSettings> = _settings.asStateFlow()
    override suspend fun updateSettings(settings: UserSettings) {
        _settings.value = settings
    }
}
