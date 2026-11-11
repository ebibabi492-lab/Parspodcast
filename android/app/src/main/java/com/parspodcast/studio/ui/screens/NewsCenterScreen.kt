package com.parspodcast.studio.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parspodcast.studio.data.local.entities.NewsArticleEntity
import com.parspodcast.studio.data.local.entities.NewsSourceEntity
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.theme.*

@Composable
fun NewsCenterScreen(
    articles: List<NewsArticleEntity>,
    sources: List<NewsSourceEntity>,
    selectedArticleIds: Set<String>,
    onToggleSelectArticle: (String) -> Unit,
    onNavigateToScriptStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("همه") }
    var viewingArticle by remember { mutableStateOf<NewsArticleEntity?>(null) }

    val categories = listOf("همه", "فناوری", "اقتصاد", "علم و دانش", "جامعه")

    val filteredArticles = articles.filter { article ->
        val matchesSearch = article.title.contains(searchQuery, ignoreCase = true) ||
                article.rawSummary.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "همه" || article.title.contains(selectedCategory)
        matchesSearch && matchesCategory
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp)
    ) {
        // Search & Filter Header
        PersianTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "جستجوی اخبار و رویدادها",
            placeholder = "عنوان، کلیدواژه یا فکت‌های خبری..."
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Pills/Buttons
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(text = category, color = if (isSelected) DeepNavy else PrimaryText) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Turquoise,
                        containerColor = SecondaryNavy
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) Turquoise else DarkNavyBorder
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selection Action Bar
        if (selectedArticleIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SecondaryNavy, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${JalaliCalendar.toPersianDigits(selectedArticleIds.size.toString())} خبر برای سناریو انتخاب شده",
                    style = MaterialTheme.typography.bodySmall,
                    color = Turquoise,
                    fontWeight = FontWeight.Bold
                )

                PrimaryActionButton(
                    text = "ارسال به فیلم‌نامه",
                    onClick = onNavigateToScriptStudio,
                    modifier = Modifier.height(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Articles List or Empty State
        if (filteredArticles.isEmpty()) {
            EmptyState(
                title = "خبری یافت نشد",
                description = "در حال حاضر داده‌ای منطبق بر جستجوی شما وجود ندارد یا فیدها هنوز به‌روزرسانی نشده‌اند."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredArticles) { article ->
                    NewsCard(
                        article = article,
                        isSelected = selectedArticleIds.contains(article.id),
                        onToggleSelect = { onToggleSelectArticle(article.id) },
                        onClick = { viewingArticle = article }
                    )
                }
            }
        }
    }

    // Article Detail Dialog
    viewingArticle?.let { article ->
        AlertDialog(
            onDismissRequest = { viewingArticle = null },
            title = {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "منبع: ${article.sourceId} | آدرس اصلی: ${article.originalUrl}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan
                    )
                    Text(
                        text = article.content.ifEmpty { article.rawSummary },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleSelectArticle(article.id)
                        viewingArticle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                ) {
                    Text(
                        text = if (selectedArticleIds.contains(article.id)) "حذف از انتخاب" else "انتخاب برای نگارش فیلم‌نامه",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingArticle = null }) {
                    Text(text = "بستن", color = SecondaryText)
                }
            },
            containerColor = SecondaryNavy,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
