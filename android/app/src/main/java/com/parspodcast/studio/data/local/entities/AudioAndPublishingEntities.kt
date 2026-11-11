package com.parspodcast.studio.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_tracks",
    foreignKeys = [
        ForeignKey(
            entity = ProductionProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("project_id")]
)
data class AudioTrackEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String,
    val trackType: String, // INTRO, PRESENTER_A, PRESENTER_B, BGM, SFX, OUTRO, HOST_VOICE, AMBIENCE
    val displayNameFa: String,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val isLocked: Boolean = false,
    val duckingEnabled: Boolean = false,
    val pan: Float = 0.0f, // -1.0 (Full Left) to +1.0 (Full Right)
    val trackOrder: Int = 0,
    val isVisible: Boolean = true
)

@Entity(
    tableName = "audio_clips",
    foreignKeys = [
        ForeignKey(
            entity = AudioTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("track_id")]
)
data class AudioClipEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "track_id") val trackId: String,
    val startTimeMs: Long, // timelinePositionMs
    val durationMs: Long,
    val filePath: String,
    val displayName: String = "",
    val sourceStartMs: Long = 0L,
    val sourceEndMs: Long = durationMs,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val gainDb: Float = 0.0f,
    val isMuted: Boolean = false,
    val clipOrder: Int = 0
)

@Entity(
    tableName = "podcast_shows"
)
data class PodcastShowEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String = "",
    val description: String,
    val author: String = "استودیوی پارس‌پادکست",
    val ownerName: String = "پارس‌پادکست",
    val ownerEmail: String? = null,
    val websiteUrl: String? = null,
    val language: String = "fa", // BCP 47
    val category: String = "News",
    val subcategory: String? = "Tech News",
    val copyright: String = "تمامی حقوق محفوظ است © ۱۴۰۵",
    val explicitContent: Boolean = false,
    val showType: String = "episodic", // episodic or serial
    val artworkLocalPath: String? = null,
    val artworkRemoteUrl: String? = null,
    val feedLocalPath: String? = null,
    val publicFeedUrl: String? = null,
    val hostingDestinationId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(
    tableName = "podcast_episodes",
    foreignKeys = [
        ForeignKey(
            entity = ProductionProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("project_id"),
        Index("show_id")
    ]
)
data class PodcastEpisodeEntity(
    @PrimaryKey val id: String, // Stable UUID GUID
    @ColumnInfo(name = "project_id") val projectId: String?,
    @ColumnInfo(name = "show_id") val showId: String = "default_show",
    val title: String,
    val subtitle: String = "",
    val descriptionFa: String,
    val summary: String = "",
    val episodeNumber: Int = 1,
    val seasonNumber: Int = 1,
    val episodeType: String = "full", // full, trailer, bonus
    val author: String = "استودیوی پارس‌پادکست",
    val keywords: String = "پادکست, هوش مصنوعی, فناوری, ایران",
    val explicitContent: Boolean = false,
    val durationSeconds: Int = 0,
    val publicationDateUtc: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val localAudioPath: String = "",
    val localArtworkPath: String? = null,
    val audioMimeType: String = "audio/wav", // audio/wav or audio/mp4
    val audioFileSizeBytes: Long = 0L,
    val audioChecksum: String = "",
    val publicAudioUrl: String? = null,
    val guid: String = id,
    val jalaliPubDate: String = "", // e.g. "۲۱ آبان ۱۴۰۵"
    val pubDateRfc822: String = "",
    val enclosureUrl: String = "",
    val category: String = "News",
    val localRssStatus: String = "DRAFT", // DRAFT, VALID, INVALID
    val publishingStatus: String = "DRAFT", // DRAFT, READY, UPLOADING, PUBLISHED, UPDATE_PENDING, FAILED, ARCHIVED
    val publishedAt: Long? = null,
    val lastPublicationError: String? = null
)

@Entity(
    tableName = "hosting_configs"
)
data class HostingConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val providerType: String = "WEBDAV", // WEBDAV, LOCAL
    val serverUrl: String, // e.g. "https://dav.parspodcast.ir/podcasts/"
    val remoteBasePath: String = "/podcasts/parspodcast/",
    val publicBaseUrl: String = "https://media.parspodcast.ir/podcasts/parspodcast/",
    val username: String = "",
    val encryptedPassword: String = "", // Keystore encrypted
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = true
)
