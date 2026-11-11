package com.parspodcast.studio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parspodcast.studio.data.local.entities.NewsArticleEntity
import com.parspodcast.studio.data.local.entities.NewsSourceEntity
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.screens.news.NewsCenterUiState
import com.parspodcast.studio.ui.screens.news.NewsCenterViewModel
import com.parspodcast.studio.ui.screens.news.NewsDetailDialog
import com.parspodcast.studio.ui.screens.news.NewsImportPreviewDialog
import com.parspodcast.studio.ui.screens.news.NewsSourceManageDialog
import com.parspodcast.studio.ui.theme.*

@Composable
fun NewsCenterScreen(
    onNavigateToScriptStudio: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewsCenterViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // SAF File Picker for TXT/Markdown news import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleFileSelected(it) }
    }

    val categories = listOf("همه", "فناوری", "اقتصاد", "سیاسی", "علمی", "جامعه", "فرهنگی", "ورزشی", "بین‌الملل", "سلامت", "عمومی")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Overview & Status Card
        NewsTopOverviewCard(
            uiState = uiState,
            onRefreshRss = { viewModel.refreshRssFeeds() },
            onImportFile = { filePickerLauncher.launch(arrayOf("text/*", "*/*")) },
            onManageSources = { viewModel.openSourceManager(true) }
        )

        // Status Toast / Message
        uiState.statusMessage?.let { msg ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Turquoise, modifier = Modifier.size(16.dp))
                    Text(text = msg, style = MaterialTheme.typography.bodySmall, color = PrimaryText)
                }
            }
        }

        // Search Bar
        PersianTextField(
            value = uiState.filter.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = "جستجوی خبرها",
            placeholder = "عنوان، کلیدواژه، نام منبع یا بخشی از متن..."
        )

        // Filter Controls Row 1: Source Input Type & Quick Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.filter.inputTypeFilter == "همه",
                onClick = { viewModel.setInputTypeFilter("همه") },
                label = { Text("همه منابع (${uiState.totalCount})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Turquoise, selectedLabelColor = DeepNavy)
            )

            FilterChip(
                selected = uiState.filter.inputTypeFilter == "ONLINE_FEED",
                onClick = { viewModel.setInputTypeFilter("ONLINE_FEED") },
                label = { Text("فید برخط (${uiState.onlineCount})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Cyan, selectedLabelColor = DeepNavy)
            )

            FilterChip(
                selected = uiState.filter.inputTypeFilter == "OFFLINE_FILE",
                onClick = { viewModel.setInputTypeFilter("OFFLINE_FILE") },
                label = { Text("فایل آفلاین (${uiState.offlineCount})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SuccessGreen, selectedLabelColor = DeepNavy)
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { viewModel.toggleFavoritesOnly() },
                modifier = Modifier.size(34.dp).background(if (uiState.filter.onlyFavorites) GoldAccent.copy(alpha = 0.2f) else DarkNavyCard, RoundedCornerShape(6.dp))
            ) {
                Icon(
                    imageVector = if (uiState.filter.onlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "نشان‌شده‌ها",
                    tint = if (uiState.filter.onlyFavorites) GoldAccent else SecondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Filter Controls Row 2: Categories
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = uiState.filter.category == category
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Turquoise,
                        selectedLabelColor = DeepNavy,
                        containerColor = SecondaryNavy,
                        labelColor = PrimaryText
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }

        // Selection Action Bar for Script Studio
        if (uiState.selectedArticleIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SecondaryNavy, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${JalaliCalendar.toPersianDigits(uiState.selectedArticleIds.size.toString())} خبر برای سناریو انتخاب شده",
                        style = MaterialTheme.typography.bodySmall,
                        color = Turquoise,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.clearSelection() }) {
                        Text("لغو", color = SecondaryText, fontSize = 11.sp)
                    }
                }

                PrimaryActionButton(
                    text = "ارسال به استودیو فیلم‌نامه",
                    onClick = onNavigateToScriptStudio,
                    modifier = Modifier.height(36.dp)
                )
            }
        }

        // News List
        val filtered = uiState.filteredArticles
        if (filtered.isEmpty()) {
            EmptyState(
                title = "خبری یافت نشد",
                description = "هیچ رکوردی منطبق با فیلترهای انتخابی شما وجود ندارد. می‌توانید از بخش «بروزرسانی RSS» یا «ورود خبر از فایل» داده اضافه کنید."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.id }) { article ->
                    NewsItemCard(
                        article = article,
                        isSelected = uiState.selectedArticleIds.contains(article.id),
                        onToggleSelect = { viewModel.toggleArticleSelection(article.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(article.id, article.isFavorite) },
                        onClick = { viewModel.openArticleDetail(article) }
                    )
                }
            }
        }
    }

    // Modal Dialog: Import Preview
    uiState.draftsForPreview?.let { drafts ->
        NewsImportPreviewDialog(
            drafts = drafts,
            onToggleSelect = { viewModel.toggleDraftSelection(it) },
            onSelectAll = { viewModel.selectAllDrafts(it) },
            onRemoveItem = { viewModel.removeDraftItem(it) },
            onUpdateDraft = { id, t, s, c, content -> viewModel.updateDraftItem(id, t, s, c, content) },
            onConfirmImport = { viewModel.saveDraftsToDatabase() },
            onDismiss = { viewModel.cancelDraftImport() }
        )
    }

    // Modal Dialog: Article Detail Viewer & Editor
    uiState.viewingArticle?.let { article ->
        NewsDetailDialog(
            article = article,
            isSelectedForScript = uiState.selectedArticleIds.contains(article.id),
            onToggleSelect = { viewModel.toggleArticleSelection(article.id) },
            onToggleFavorite = { viewModel.toggleFavorite(article.id, article.isFavorite) },
            onDelete = { viewModel.deleteArticle(article) },
            onSaveEdit = { t, c, cat -> viewModel.updateArticleContent(article, t, c, cat) },
            onDismiss = { viewModel.closeArticleDetail() }
        )
    }

    // Modal Dialog: Manage RSS Sources
    if (uiState.isManageSourcesOpen) {
        NewsSourceManageDialog(
            sources = uiState.sources,
            onAddSource = { name, url, cat -> viewModel.addSource(name, url, cat) },
            onToggleActive = { viewModel.toggleSourceActive(it) },
            onDeleteSource = { viewModel.deleteSource(it) },
            onTestFeed = { viewModel.testFeedUrl(it) },
            testResult = uiState.testFeedResult,
            testError = uiState.testFeedError,
            onDismiss = { viewModel.openSourceManager(false) }
        )
    }
}

@Composable
private fun NewsTopOverviewCard(
    uiState: NewsCenterUiState,
    onRefreshRss: () -> Unit,
    onImportFile: () -> Unit,
    onManageSources: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مرکز دریافت و هوشمندی اخبار (هیبریدی)",
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "مجموع: ${uiState.totalCount} خبر | برخط: ${uiState.onlineCount} | آفلاین: ${uiState.offlineCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                StatusIndicator(
                    label = if (uiState.isSyncingRss) "در حال دریافت..." else "آماده (آفلاین + آنلاین)",
                    color = if (uiState.isSyncingRss) WarningAmber else SuccessGreen
                )
            }

            // Primary Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRefreshRss,
                    enabled = !uiState.isSyncingRss,
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isSyncingRss) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DeepNavy, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text("بروزرسانی RSS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onImportFile,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard, contentColor = Turquoise),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ورود خبر از فایل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onManageSources,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "مدیریت منابع", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun NewsItemCard(
    article: NewsArticleEntity,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) Turquoise else DarkNavyBorder
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(checkedColor = Turquoise, checkmarkColor = DeepNavy)
                    )

                    Column {
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimaryText,
                            fontWeight = if (!article.isRead) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 2
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(text = article.sourceName, style = MaterialTheme.typography.labelSmall, color = Turquoise)
                            Text(text = "•", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            Text(text = article.category, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                            Text(text = "•", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            Text(text = JalaliCalendar.formatShort(article.publishedAtUtc), style = MaterialTheme.typography.labelSmall, color = MutedText)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIndicator(
                        label = if (article.inputType == "RSS" || article.inputType == "ATOM") article.inputType else "فایل",
                        color = if (article.inputType == "RSS" || article.inputType == "ATOM") Cyan else SuccessGreen
                    )

                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (article.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (article.isFavorite) GoldAccent else SecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = article.rawSummary.ifBlank { article.content.take(160) },
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
