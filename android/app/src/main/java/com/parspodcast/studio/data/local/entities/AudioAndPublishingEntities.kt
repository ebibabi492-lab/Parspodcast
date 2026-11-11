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
    val trackType: String, // INTRO, PRESENTER_A, PRESENTER_B, BGM, SFX, OUTRO
    val displayNameFa: String,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val duckingEnabled: Boolean = false,
    val pan: Float = 0.0f
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
    val startTimeMs: Long,
    val durationMs: Long,
    val filePath: String,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val gainDb: Float = 0.0f
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
    indices = [Index("project_id")]
)
data class PodcastEpisodeEntity(
    @PrimaryKey val id: String, // RFC GUID
    @ColumnInfo(name = "project_id") val projectId: String?,
    val title: String,
    val descriptionFa: String,
    val jalaliPubDate: String, // e.g. "۲۱ آبان ۱۴۰۵"
    val pubDateRfc822: String,
    val enclosureUrl: String,
    val fileSizeBytes: Long,
    val mimeType: String = "audio/mpeg",
    val durationSeconds: Int,
    val artworkPath: String? = null,
    val category: String = "News",
    val publishingStatus: String // DRAFT, READY, UPLOADING, PUBLISHED, FAILED, SCHEDULED
)
