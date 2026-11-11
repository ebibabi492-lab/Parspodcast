package com.parspodcast.studio.data.local.dao

import androidx.room.*
import com.parspodcast.studio.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_sources ORDER BY name ASC")
    fun getAllSources(): Flow<List<NewsSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: NewsSourceEntity)

    @Delete
    suspend fun deleteSource(source: NewsSourceEntity)

    @Query("SELECT * FROM news_articles ORDER BY publishedAtUtc DESC")
    fun getAllArticles(): Flow<List<NewsArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<NewsArticleEntity>): List<Long>

    @Query("SELECT * FROM news_events ORDER BY lastReportedUtc DESC")
    fun getAllEvents(): Flow<List<NewsEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: NewsEventEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEventCrossRef(crossRef: NewsEventArticleCrossRef)
}

@Dao
interface ProductionDao {
    @Query("SELECT * FROM production_projects ORDER BY updatedAtUtc DESC")
    fun getAllProjects(): Flow<List<ProductionProjectEntity>>

    @Query("SELECT * FROM production_projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): ProductionProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProductionProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProductionProjectEntity)

    @Query("SELECT * FROM scripts WHERE project_id = :projectId ORDER BY version DESC LIMIT 1")
    suspend fun getLatestScriptForProject(projectId: String): ScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptEntity)

    @Query("SELECT * FROM script_segments WHERE script_id = :scriptId ORDER BY segmentIndex ASC")
    fun getSegmentsForScript(scriptId: String): Flow<List<ScriptSegmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<ScriptSegmentEntity>)

    @Update
    suspend fun updateSegment(segment: ScriptSegmentEntity)

    @Query("SELECT * FROM audio_tracks WHERE project_id = :projectId")
    fun getTracksForProject(projectId: String): Flow<List<AudioTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<AudioTrackEntity>)

    @Query("SELECT * FROM audio_clips WHERE track_id = :trackId ORDER BY startTimeMs ASC")
    fun getClipsForTrack(trackId: String): Flow<List<AudioClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: AudioClipEntity)
}

@Dao
interface VoiceDao {
    @Query("SELECT * FROM voice_profiles")
    fun getAllProfiles(): Flow<List<VoiceProfileEntity>>

    @Query("SELECT * FROM voice_profiles WHERE profileRole = :role LIMIT 1")
    suspend fun getProfileForRole(role: String): VoiceProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VoiceProfileEntity)
}

@Dao
interface PublishingDao {
    @Query("SELECT * FROM podcast_episodes ORDER BY pubDateRfc822 DESC")
    fun getAllEpisodes(): Flow<List<PodcastEpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: PodcastEpisodeEntity)

    @Update
    suspend fun updateEpisode(episode: PodcastEpisodeEntity)
}
