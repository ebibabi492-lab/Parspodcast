package com.parspodcast.studio.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "news_sources")
data class NewsSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val feedUrl: String,
    val parserType: String, // RSS, ATOM, OFFLINE_FILE
    val category: String, // فناوری، اقتصاد، سیاست، فرهنگ، بین‌الملل، علمی، اجتماعی، ورزشی، سلامت، عمومی
    val updateIntervalMins: Int = 60,
    val isActive: Boolean = true,
    val lastFetchedUtc: Long = 0L,
    val fetchErrorCount: Int = 0,
    val lastErrorMessage: String? = null,
    val articleCount: Int = 0
)

@Entity(
    tableName = "news_articles",
    indices = [
        Index("source_id"),
        Index(value = ["deduplication_hash"], unique = true),
        Index("category"),
        Index("inputType")
    ]
)
data class NewsArticleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val sourceName: String = "منبع ناشناس",
    val title: String,
    val content: String,
    val originalUrl: String? = null,
    val author: String? = null,
    val category: String = "عمومی",
    val publishedAtUtc: Long,
    val importedAtUtc: Long = System.currentTimeMillis(),
    val rawSummary: String,
    @ColumnInfo(name = "deduplication_hash") val deduplicationHash: String,
    val inputType: String = "RSS", // RSS, ATOM, TXT, MARKDOWN, CSV
    val isFavorite: Boolean = false,
    val isRead: Boolean = false,
    val originalContent: String = "",
    val keywordsJson: String = "[]",
    val isProcessedForIntelligence: Boolean = false
)

@Entity(tableName = "news_events")
data class NewsEventEntity(
    @PrimaryKey val id: String,
    val canonicalTitle: String,
    val eventSummary: String,
    val category: String,
    val firstReportedUtc: Long,
    val lastReportedUtc: Long,
    val reportCount: Int = 1,
    val primaryFactsJson: String = "[]",
    val verifiedDifferentialsJson: String = "[]" // اختلافات روایت منابع بدون تحریف
)

@Entity(
    tableName = "news_event_article_cross_ref",
    primaryKeys = ["event_id", "article_id"],
    foreignKeys = [
        ForeignKey(
            entity = NewsEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NewsArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("event_id"), Index("article_id")]
)
data class NewsEventArticleCrossRef(
    @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "article_id") val articleId: String
)
