package com.parspodcast.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.parspodcast.studio.data.local.entities.PodcastEpisodeEntity
import com.parspodcast.studio.data.repository.ModelStatus
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.theme.*
import java.util.Date

@Composable
fun DashboardScreen(
    onNavigateTo: (String) -> Unit,
    episodes: List<PodcastEpisodeEntity> = emptyList(),
    modelStatuses: List<ModelStatus> = emptyList(),
    newsCount: Int = 0,
    activeProjectTitle: String? = null,
    modifier: Modifier = Modifier
) {
    val jalaliToday = remember { JalaliCalendar.fromDate(Date()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Welcome and Jalali Date Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "استودیوی پارس‌پادکست",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        StatusIndicator(
                            label = "حالت استودیو آماده",
                            color = Turquoise
                        )
                    }

                    Text(
                        text = jalaliToday.toFormattedString(usePersianDigits = true),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Cyan,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = "تولید مستقل پادکست و خبر صوتی فارسی با پردازش ۱۰۰٪ محلی و هوش مصنوعی بدون نیاز به اینترنت",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryActionButton(
                            text = "تولید اپیزود جدید",
                            onClick = { onNavigateTo("script_studio") },
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.Add, contentDescription = null, tint = DeepNavy) }
                        )

                        SecondaryActionButton(
                            text = "مرکز خبر",
                            onClick = { onNavigateTo("news_center") },
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.Article, contentDescription = null, tint = PrimaryText) }
                        )
                    }
                }
            }
        }

        // Quick Statistics Overview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "اخبار جمع‌آوری شده", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        Text(
                            text = JalaliCalendar.toPersianDigits(newsCount.toString()),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Turquoise,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(text = "آماده انتخاب و سناریو", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "اپیزودهای آرشیو", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        Text(
                            text = JalaliCalendar.toPersianDigits(episodes.size.toString()),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Cyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(text = "ثبت در آرشیو جلالی", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    }
                }
            }
        }

        // Local AI & TTS Model Readiness
        item {
            SectionHeader(
                title = "وضعیت مدل‌های هوش مصنوعی و صوت محلی",
                subtitle = "Gemma 4 و Sherpa-ONNX Mana"
            )

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
                        Text(text = "مدل نگارش: Gemma 4 E4B", style = MaterialTheme.typography.bodySmall, color = PrimaryText)
                        val gemmaStatus = modelStatuses.find { it.id == "gemma-4-e4b-instruct" }
                        StatusIndicator(
                            label = if (gemmaStatus?.isInstalled == true) "آماده استنتاج" else "نصب نشده (نیازمند دریافت)",
                            color = if (gemmaStatus?.isInstalled == true) SuccessGreen else WarningAmber
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "مدل گویندگی: Mana Persian Piper", style = MaterialTheme.typography.bodySmall, color = PrimaryText)
                        val manaStatus = modelStatuses.find { it.id == "fa-ir-mana-piper" }
                        StatusIndicator(
                            label = if (manaStatus?.isInstalled == true) "آماده تولید صدا" else "نصب نشده (نیازمند دریافت)",
                            color = if (manaStatus?.isInstalled == true) SuccessGreen else WarningAmber
                        )
                    }

                    TextButton(
                        onClick = { onNavigateTo("model_manager") },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = "مدیریت فایل‌های مدل ←", color = Turquoise, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Recent Production or Episodes
        item {
            SectionHeader(
                title = "آخرین برنامه‌ها و پروژه‌ها",
                subtitle = "آرشیو اپیزودهای اخیر",
                action = {
                    TextButton(onClick = { onNavigateTo("jalali_archive") }) {
                        Text(text = "مشاهده همه", color = Turquoise, style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        }

        if (episodes.isEmpty()) {
            item {
                EmptyState(
                    title = "هنوز اپیزودی تولید نشده است",
                    description = "برای شروع، اخبار روز را از مرکز خبر انتخاب کرده و وارد استودیو فیلم‌نامه شوید.",
                    actionButton = {
                        PrimaryActionButton(
                            text = "شروع تولید اولین پادکست",
                            onClick = { onNavigateTo("news_center") }
                        )
                    }
                )
            }
        } else {
            items(episodes.take(3)) { episode ->
                EpisodeCard(
                    episode = episode,
                    onClick = { onNavigateTo("jalali_archive") },
                    onPlay = { /* preview */ }
                )
            }
        }
    }
}
