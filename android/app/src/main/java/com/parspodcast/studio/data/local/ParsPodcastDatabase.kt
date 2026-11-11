package com.parspodcast.studio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        PodcastShowEntity::class,
        PodcastEpisodeEntity::class,
        HostingConfigEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class ParsPodcastDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun productionDao(): ProductionDao
    abstract fun voiceDao(): VoiceDao
    abstract fun publishingDao(): PublishingDao

    companion object {
        const val DATABASE_NAME = "parspodcast_studio.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create podcast_shows table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `podcast_shows` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `author` TEXT NOT NULL,
                        `ownerName` TEXT NOT NULL,
                        `ownerEmail` TEXT,
                        `websiteUrl` TEXT,
                        `language` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `subcategory` TEXT,
                        `copyright` TEXT NOT NULL,
                        `explicitContent` INTEGER NOT NULL,
                        `showType` TEXT NOT NULL,
                        `artworkLocalPath` TEXT,
                        `artworkRemoteUrl` TEXT,
                        `feedLocalPath` TEXT,
                        `publicFeedUrl` TEXT,
                        `hostingDestinationId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                """.trimIndent())

                // 2. Create hosting_configs table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `hosting_configs` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `providerType` TEXT NOT NULL,
                        `serverUrl` TEXT NOT NULL,
                        `remoteBasePath` TEXT NOT NULL,
                        `publicBaseUrl` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `encryptedPassword` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL
                    )
                """.trimIndent())

                // 3. Insert default show
                db.execSQL("""
                    INSERT OR IGNORE INTO `podcast_shows` (
                        `id`, `title`, `subtitle`, `description`, `author`, `ownerName`,
                        `language`, `category`, `subcategory`, `copyright`, `explicitContent`,
                        `showType`, `createdAt`, `updatedAt`, `isActive`
                    ) VALUES (
                        'default_show', 'پارس‌پادکست', 'فناوری، هوش مصنوعی و جامعه',
                        'پادکست تخصصی فناوری و هوش مصنوعی با تحلیل‌های صوتی تولید شده در استودیوی پارس‌پادکست',
                        'استودیوی پارس‌پادکست', 'پارس‌پادکست', 'fa', 'News', 'Tech News',
                        'تمامی حقوق محفوظ است © ۱۴۰۵', 0, 'episodic', 1700000000000, 1700000000000, 1
                    )
                """.trimIndent())

                // 4. Safely migrate podcast_episodes to v2 schema
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `podcast_episodes_v2` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `project_id` TEXT,
                        `show_id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `descriptionFa` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `episodeNumber` INTEGER NOT NULL,
                        `seasonNumber` INTEGER NOT NULL,
                        `episodeType` TEXT NOT NULL,
                        `author` TEXT NOT NULL,
                        `keywords` TEXT NOT NULL,
                        `explicitContent` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `publicationDateUtc` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `localAudioPath` TEXT NOT NULL,
                        `localArtworkPath` TEXT,
                        `audioMimeType` TEXT NOT NULL,
                        `audioFileSizeBytes` INTEGER NOT NULL,
                        `audioChecksum` TEXT NOT NULL,
                        `publicAudioUrl` TEXT,
                        `guid` TEXT NOT NULL,
                        `jalaliPubDate` TEXT NOT NULL,
                        `pubDateRfc822` TEXT NOT NULL,
                        `enclosureUrl` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `localRssStatus` TEXT NOT NULL,
                        `publishingStatus` TEXT NOT NULL,
                        `publishedAt` INTEGER,
                        `lastPublicationError` TEXT,
                        FOREIGN KEY(`project_id`) REFERENCES `production_projects`(`id`) ON UPDATE NO ACTION ON DELETE SET_NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `podcast_episodes_v2` (
                        `id`, `project_id`, `show_id`, `title`, `subtitle`, `descriptionFa`, `summary`,
                        `episodeNumber`, `seasonNumber`, `episodeType`, `author`, `keywords`,
                        `explicitContent`, `durationSeconds`, `publicationDateUtc`, `createdAt`, `updatedAt`,
                        `localAudioPath`, `localArtworkPath`, `audioMimeType`, `audioFileSizeBytes`,
                        `audioChecksum`, `publicAudioUrl`, `guid`, `jalaliPubDate`, `pubDateRfc822`,
                        `enclosureUrl`, `category`, `localRssStatus`, `publishingStatus`, `publishedAt`, `lastPublicationError`
                    )
                    SELECT
                        `id`, `project_id`, 'default_show', `title`, '', `descriptionFa`, '',
                        1, 1, 'full', 'استودیوی پارس‌پادکست', '',
                        0, `durationSeconds`, 0, 0, 0,
                        '', `artworkPath`, `mimeType`, `fileSizeBytes`,
                        '', `enclosureUrl`, `id`, `jalaliPubDate`, `pubDateRfc822`,
                        `enclosureUrl`, `category`, 'READY', `publishingStatus`, NULL, NULL
                    FROM `podcast_episodes`
                """.trimIndent())

                db.execSQL("DROP TABLE `podcast_episodes`")
                db.execSQL("ALTER TABLE `podcast_episodes_v2` RENAME TO `podcast_episodes`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_podcast_episodes_project_id` ON `podcast_episodes` (`project_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_podcast_episodes_show_id` ON `podcast_episodes` (`show_id`)")
            }
        }
    }
}
