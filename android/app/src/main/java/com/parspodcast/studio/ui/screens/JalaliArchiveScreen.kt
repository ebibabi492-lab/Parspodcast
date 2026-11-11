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
import com.parspodcast.studio.data.local.entities.PodcastEpisodeEntity
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.theme.*

@Composable
fun JalaliArchiveScreen(
    episodes: List<PodcastEpisodeEntity>,
    onDeleteEpisode: (PodcastEpisodeEntity) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("همه") }
    var episodeToDelete by remember { mutableStateOf<PodcastEpisodeEntity?>(null) }

    val months = listOf("همه", "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")

    val filteredEpisodes = episodes.filter { episode ->
        val matchesSearch = episode.title.contains(searchQuery, ignoreCase = true) ||
                episode.descriptionFa.contains(searchQuery, ignoreCase = true)
        val matchesMonth = selectedMonth == "همه" || episode.jalaliPubDate.contains(selectedMonth)
        matchesSearch && matchesMonth
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp)
    ) {
        // Search Input
        PersianTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "جستجو در آرشیو جلالی",
            placeholder = "عنوان اپیزود، موضوع یا کلیدواژه..."
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Month Filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(months) { month ->
                val isSelected = selectedMonth == month
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedMonth = month },
                    label = { Text(text = month, color = if (isSelected) DeepNavy else PrimaryText) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Turquoise,
                        containerColor = SecondaryNavy
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Episodes List
        if (filteredEpisodes.isEmpty()) {
            EmptyState(
                title = "هیچ اپیزودی در این بازه زمانی یافت نشد",
                description = "اپیزودهای تولید و ذخیره شده در پایگاه داده محلی Room در این بخش نمایش داده می‌شوند."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredEpisodes) { episode ->
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
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = episode.jalaliPubDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Turquoise
                                )
                                StatusIndicator(
                                    label = when (episode.publishingStatus) {
                                        "PUBLISHED" -> "منتشر شده"
                                        "READY" -> "آماده انتشار"
                                        else -> "پیش‌نویس"
                                    },
                                    color = when (episode.publishingStatus) {
                                        "PUBLISHED" -> SuccessGreen
                                        "READY" -> Cyan
                                        else -> SecondaryText
                                    }
                                )
                            }

                            Text(
                                text = episode.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            Text(
                                text = episode.descriptionFa,
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText,
                                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مدت زمان: ${JalaliCalendar.toPersianDigits((episode.durationSeconds / 60).toString())} دقیقه",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedText
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { episode.projectId?.let { onNavigateToEdit(it) } },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "ویرایش پروژه",
                                            tint = Cyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { episodeToDelete = episode },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف اپیزود",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    episodeToDelete?.let { ep ->
        ConfirmationDialog(
            title = "حذف اپیزود از آرشیو",
            message = "آیا از حذف اپیزود «${ep.title}» از پایگاه داده و آرشیو جلالی اطمینان دارید؟ این عملیات غیرقابل بازگشت است.",
            confirmText = "حذف قطعی",
            dismissText = "انصراف",
            onConfirm = {
                onDeleteEpisode(ep)
                episodeToDelete = null
            },
            onDismiss = { episodeToDelete = null }
        )
    }
}
