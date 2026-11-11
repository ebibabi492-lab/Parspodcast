package com.parspodcast.studio.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "production_projects")
data class ProductionProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val programType: String, // SINGLE_PRESENTER, TWO_PRESENTERS
    val jalaliDateStr: String, // e.g. "۱۴۰۵/۰۸/۲۱"
    val createdAtUtc: Long,
    val updatedAtUtc: Long,
    val status: String, // DRAFT, SCRIPTED, VOICED, MIXED, PUBLISHED
    val targetDurationSec: Int = 300,
    val selectedEventIdsJson: String = "[]",
    val finalAudioPath: String? = null
)

@Entity(
    tableName = "scripts",
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
data class ScriptEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String,
    val modelIdUsed: String, // gemma-4-e2b-instruct, etc.
    val totalSegments: Int,
    val rawJson: String,
    val version: Int = 1,
    val createdAtUtc: Long
)

@Entity(
    tableName = "script_segments",
    foreignKeys = [
        ForeignKey(
            entity = ScriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["script_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("script_id")]
)
data class ScriptSegmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "script_id") val scriptId: String,
    val segmentIndex: Int,
    val segmentType: String, // INTRO, NEWS, DISCUSSION, TRANSITION, OUTRO
    val speakerRole: String, // PRESENTER_A, PRESENTER_B
    val textFa: String,
    val estimatedDurationSec: Int,
    val generatedAudioPath: String? = null,
    val isApproved: Boolean = false,
    val sourceIdsJson: String = "[]"
)

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey val id: String,
    val profileRole: String, // PRESENTER_A, PRESENTER_B
    val displayNameFa: String,
    val modelName: String, // fa_IR-mana-medium, etc.
    val engineType: String = "SHERPA_ONNX_PIPER",
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val isVerified: Boolean = true,
    val sampleAudioPath: String? = null
)
