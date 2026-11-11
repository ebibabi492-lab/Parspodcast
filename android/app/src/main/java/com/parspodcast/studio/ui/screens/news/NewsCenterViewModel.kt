package com.parspodcast.studio.ui.screens.news

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parspodcast.studio.data.local.entities.NewsArticleEntity
import com.parspodcast.studio.data.local.entities.NewsSourceEntity
import com.parspodcast.studio.data.manager.OfflineNewsImportManager
import com.parspodcast.studio.data.repository.NewsRepository
import com.parspodcast.studio.domain.news.NewsFilter
import com.parspodcast.studio.domain.news.ParsedNewsDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NewsCenterUiState(
    val articles: List<NewsArticleEntity> = emptyList(),
    val sources: List<NewsSourceEntity> = emptyList(),
    val filter: NewsFilter = NewsFilter(),
    val selectedArticleIds: Set<String> = emptySet(),
    val isSyncingRss: Boolean = false,
    val isAnalyzingFile: Boolean = false,
    val draftsForPreview: List<ParsedNewsDraft>? = null,
    val viewingArticle: NewsArticleEntity? = null,
    val isManageSourcesOpen: Boolean = false,
    val statusMessage: String? = null,
    val lastSyncTimestamp: Long = 0L,
    val testFeedResult: Pair<String, Int>? = null,
    val testFeedError: String? = null
) {
    val totalCount: Int get() = articles.size
    val unreadCount: Int get() = articles.count { !it.isRead }
    val onlineCount: Int get() = articles.count { it.inputType == "RSS" || it.inputType == "ATOM" }
    val offlineCount: Int get() = articles.count { it.inputType != "RSS" && it.inputType != "ATOM" }

    val filteredArticles: List<NewsArticleEntity>
        get() = articles.filter { article ->
            val matchesCategory = filter.category == "همه" || article.category == filter.category
            val matchesType = when (filter.inputTypeFilter) {
                "ONLINE_FEED" -> article.inputType == "RSS" || article.inputType == "ATOM"
                "OFFLINE_FILE" -> article.inputType != "RSS" && article.inputType != "ATOM"
                else -> true
            }
            val matchesSearch = filter.searchQuery.isBlank() ||
                    article.title.contains(filter.searchQuery, ignoreCase = true) ||
                    article.rawSummary.contains(filter.searchQuery, ignoreCase = true) ||
                    article.sourceName.contains(filter.searchQuery, ignoreCase = true)

            val matchesFavorite = !filter.onlyFavorites || article.isFavorite
            val matchesUnread = !filter.onlyUnread || !article.isRead

            matchesCategory && matchesType && matchesSearch && matchesFavorite && matchesUnread
        }
}

@HiltViewModel
class NewsCenterViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val offlineImportManager: OfflineNewsImportManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsCenterUiState())
    val uiState: StateFlow<NewsCenterUiState> = _uiState.asStateFlow()

    init {
        // Collect articles from Room
        viewModelScope.launch {
            newsRepository.getAllArticles().collect { list ->
                _uiState.update { it.copy(articles = list) }
            }
        }

        // Collect sources from Room
        viewModelScope.launch {
            newsRepository.getAllSources().collect { list ->
                _uiState.update { it.copy(sources = list) }
            }
        }
    }

    fun updateFilter(filter: NewsFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(filter = it.filter.copy(searchQuery = query)) }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(filter = it.filter.copy(category = category)) }
    }

    fun setInputTypeFilter(typeFilter: String) {
        _uiState.update { it.copy(filter = it.filter.copy(inputTypeFilter = typeFilter)) }
    }

    fun toggleFavoritesOnly() {
        _uiState.update { it.copy(filter = it.filter.copy(onlyFavorites = !it.filter.onlyFavorites)) }
    }

    fun toggleUnreadOnly() {
        _uiState.update { it.copy(filter = it.filter.copy(onlyUnread = !it.filter.onlyUnread)) }
    }

    fun toggleArticleSelection(articleId: String) {
        _uiState.update { current ->
            val set = current.selectedArticleIds.toMutableSet()
            if (set.contains(articleId)) set.remove(articleId) else set.add(articleId)
            current.copy(selectedArticleIds = set)
        }
    }

    fun selectAllFiltered() {
        val ids = _uiState.value.filteredArticles.map { it.id }.toSet()
        _uiState.update { it.copy(selectedArticleIds = ids) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedArticleIds = emptySet()) }
    }

    fun openArticleDetail(article: NewsArticleEntity) {
        viewModelScope.launch {
            if (!article.isRead) {
                newsRepository.markAsRead(article.id, true)
            }
            _uiState.update { it.copy(viewingArticle = article) }
        }
    }

    fun closeArticleDetail() {
        _uiState.update { it.copy(viewingArticle = null) }
    }

    fun toggleFavorite(articleId: String, currentVal: Boolean) {
        viewModelScope.launch {
            newsRepository.toggleFavorite(articleId, !currentVal)
        }
    }

    fun deleteArticle(article: NewsArticleEntity) {
        viewModelScope.launch {
            newsRepository.deleteArticle(article)
            _uiState.update { it.copy(viewingArticle = null, statusMessage = "خبر حذف شد") }
        }
    }

    fun updateArticleContent(article: NewsArticleEntity, newTitle: String, newContent: String, newCategory: String) {
        viewModelScope.launch {
            val updated = article.copy(
                title = newTitle,
                content = newContent,
                category = newCategory
            )
            newsRepository.updateArticle(updated)
            _uiState.update { it.copy(viewingArticle = updated, statusMessage = "تغییرات ذخیره شد") }
        }
    }

    // --- RSS Synchronization ---

    fun refreshRssFeeds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingRss = true, statusMessage = "در حال بروزرسانی فیدهای خبری...") }
            val result = newsRepository.syncAllFeeds()
            if (result.isSuccess) {
                val summary = result.getOrNull()
                _uiState.update {
                    it.copy(
                        isSyncingRss = false,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        statusMessage = "همگام‌سازی پایان یافت: ${summary?.totalSynced} خبر جدید اضافه شد."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSyncingRss = false,
                        statusMessage = "خطا در همگام‌سازی فیدها: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    // --- Offline File Import ---

    fun handleFileSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingFile = true, statusMessage = "در حال خواندن و بررسی محتوای فایل...") }
            val result = offlineImportManager.parseAndAnalyzeFile(uri)
            if (result.isSuccess) {
                val drafts = result.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isAnalyzingFile = false,
                        draftsForPreview = drafts,
                        statusMessage = "${drafts.size} خبر شناسایی شد. لطفاً پیش‌نمایش را بررسی کنید."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAnalyzingFile = false,
                        statusMessage = "خطا در واردسازی فایل: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun toggleDraftSelection(tempId: String) {
        _uiState.update { current ->
            val updated = current.draftsForPreview?.map {
                if (it.tempId == tempId) it.copy(isSelected = !it.isSelected) else it
            }
            current.copy(draftsForPreview = updated)
        }
    }

    fun selectAllDrafts(select: Boolean) {
        _uiState.update { current ->
            val updated = current.draftsForPreview?.map { it.copy(isSelected = select) }
            current.copy(draftsForPreview = updated)
        }
    }

    fun removeDraftItem(tempId: String) {
        _uiState.update { current ->
            val updated = current.draftsForPreview?.filter { it.tempId != tempId }
            current.copy(draftsForPreview = updated)
        }
    }

    fun updateDraftItem(tempId: String, title: String, source: String, category: String, content: String) {
        _uiState.update { current ->
            val updated = current.draftsForPreview?.map {
                if (it.tempId == tempId) {
                    it.copy(
                        title = title,
                        sourceName = source,
                        category = category,
                        content = content,
                        summary = content.take(220)
                    )
                } else it
            }
            current.copy(draftsForPreview = updated)
        }
    }

    fun saveDraftsToDatabase() {
        val drafts = _uiState.value.draftsForPreview ?: return
        viewModelScope.launch {
            val result = newsRepository.saveDrafts(drafts)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _uiState.update {
                    it.copy(
                        draftsForPreview = null,
                        statusMessage = "$count خبر با موفقیت در دیتابیس ثبت شد."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(statusMessage = "خطا در ثبت خبرها: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun cancelDraftImport() {
        _uiState.update { it.copy(draftsForPreview = null) }
    }

    // --- RSS Source Management ---

    fun openSourceManager(open: Boolean) {
        _uiState.update { it.copy(isManageSourcesOpen = open, testFeedResult = null, testFeedError = null) }
    }

    fun addSource(name: String, url: String, category: String) {
        viewModelScope.launch {
            val newSrc = NewsSourceEntity(
                id = "src_${UUID.randomUUID()}",
                name = name.trim(),
                feedUrl = url.trim(),
                parserType = "RSS",
                category = category,
                isActive = true,
                lastFetchedUtc = 0L
            )
            newsRepository.insertSource(newSrc)
            _uiState.update { it.copy(statusMessage = "منبع «$name» افزوده شد.") }
        }
    }

    fun toggleSourceActive(source: NewsSourceEntity) {
        viewModelScope.launch {
            newsRepository.updateSource(source.copy(isActive = !source.isActive))
        }
    }

    fun deleteSource(source: NewsSourceEntity) {
        viewModelScope.launch {
            newsRepository.deleteSource(source)
            _uiState.update { it.copy(statusMessage = "منبع حذف شد.") }
        }
    }

    fun testFeedUrl(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(testFeedResult = null, testFeedError = null) }
            val res = newsRepository.testFeed(url)
            if (res.isSuccess) {
                _uiState.update { it.copy(testFeedResult = res.getOrNull()) }
            } else {
                _uiState.update { it.copy(testFeedError = res.exceptionOrNull()?.message) }
            }
        }
    }
}
