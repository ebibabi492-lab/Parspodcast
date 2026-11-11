package com.parspodcast.studio.domain.news

enum class NewsInputType(val displayNameFa: String) {
    RSS("فید RSS"),
    ATOM("فید Atom"),
    TXT("فایل متنی (TXT)"),
    MARKDOWN("مارک‌داون (MD)"),
    CSV("جدول (CSV)")
}

data class ParsedNewsDraft(
    val tempId: String,
    var title: String,
    var summary: String,
    var content: String,
    var sourceName: String,
    var sourceUrl: String? = null,
    var articleUrl: String? = null,
    var author: String? = null,
    var category: String = "عمومی",
    var publishedAtUtc: Long = System.currentTimeMillis(),
    var jalaliDateStr: String = "",
    val inputType: NewsInputType = NewsInputType.TXT,
    val contentHash: String = "",
    var isSelected: Boolean = true,
    var isDuplicate: Boolean = false,
    var duplicateReason: String? = null
)

data class NewsFilter(
    val category: String = "همه",
    val inputTypeFilter: String = "همه", // "همه", "ONLINE_FEED", "OFFLINE_FILE"
    val searchQuery: String = "",
    val onlyFavorites: Boolean = false,
    val onlyUnread: Boolean = false
)

data class SyncSummary(
    val totalSynced: Int = 0,
    val sourcesSuccess: Int = 0,
    val sourcesFailed: Int = 0,
    val errors: List<String> = emptyList()
)
