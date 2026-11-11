package com.parspodcast.studio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.parspodcast.studio.data.local.dao.*
import com.parspodcast.studio.data.local.entities.*

@Database(
    entities = [
        NewsSourceEntity::class,
        NewsArticleEntity::class,
        NewsEventEntity::class,
        NewsEventArticleCrossRef::class,
        ProductionProjectEntity::class,
        ScriptEntity::class,
        ScriptSegmentEntity::class,
        VoiceProfileEntity::class,
        AudioTrackEntity::class,
        AudioClipEntity::class,
        PodcastEpisodeEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ParsPodcastDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun productionDao(): ProductionDao
    abstract fun voiceDao(): VoiceDao
    abstract fun publishingDao(): PublishingDao

    companion object {
        const val DATABASE_NAME = "parspodcast_studio.db"
    }
}
