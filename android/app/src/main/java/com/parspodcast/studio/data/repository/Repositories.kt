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
    suspend fun deleteSource(source: NewsSourceEntity)
    suspend fun insertArticles(articles: List<NewsArticleEntity>)
    suspend fun insertEvent(event: NewsEventEntity)
}

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val newsDao: NewsDao
) : NewsRepository {
    override fun getAllSources(): Flow<List<NewsSourceEntity>> = newsDao.getAllSources()
    override fun getAllArticles(): Flow<List<NewsArticleEntity>> = newsDao.getAllArticles()
    override fun getAllEvents(): Flow<List<NewsEventEntity>> = newsDao.getAllEvents()
    override suspend fun insertSource(source: NewsSourceEntity) = newsDao.insertSource(source)
    override suspend fun deleteSource(source: NewsSourceEntity) = newsDao.deleteSource(source)
    override suspend fun insertArticles(articles: List<NewsArticleEntity>) { newsDao.insertArticles(articles) }
    override suspend fun insertEvent(event: NewsEventEntity) = newsDao.insertEvent(event)
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
    suspend fun insertTracks(tracks: List<AudioTrackEntity>)
    fun getClipsForTrack(trackId: String): Flow<List<AudioClipEntity>>
    suspend fun insertClip(clip: AudioClipEntity)
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
    override suspend fun insertTracks(tracks: List<AudioTrackEntity>) = productionDao.insertTracks(tracks)
    override fun getClipsForTrack(trackId: String): Flow<List<AudioClipEntity>> = productionDao.getClipsForTrack(trackId)
    override suspend fun insertClip(clip: AudioClipEntity) = productionDao.insertClip(clip)
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
