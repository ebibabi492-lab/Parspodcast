package com.parspodcast.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parspodcast.studio.data.local.entities.NewsArticleEntity
import com.parspodcast.studio.data.local.entities.PodcastEpisodeEntity
import com.parspodcast.studio.data.repository.ModelStatus
import com.parspodcast.studio.ui.theme.*

@Composable
fun EpisodeCard(
    episode: PodcastEpisodeEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPlay: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
        )
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
                        "UPLOADING" -> "در حال ارسال"
                        else -> "پیش‌نویس"
                    },
                    color = when (episode.publishingStatus) {
                        "PUBLISHED" -> SuccessGreen
                        "READY" -> Cyan
                        "UPLOADING" -> WarningAmber
                        else -> SecondaryText
                    }
                )
            }

            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = episode.descriptionFa,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مدت زمان: ${episode.durationSeconds / 60} دقیقه",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )

                if (onPlay != null) {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(32.dp)
                            .background(DarkNavyCard, RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "پخش",
                            tint = Turquoise,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(
    article: NewsArticleEntity,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
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
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "منبع: ${article.sourceId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan
                )
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Turquoise,
                        checkmarkColor = DeepNavy,
                        uncheckedColor = MutedText
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )

            Text(
                text = article.rawSummary,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ModelStatusCard(
    model: ModelStatus,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
        )
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
                Column {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = model.modelType,
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                StatusIndicator(
                    label = if (model.isInstalled) {
                        if (model.isLoaded) "بارگذاری شده" else "نصب شده"
                    } else "نصب نشده",
                    color = if (model.isInstalled) SuccessGreen else WarningAmber
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "حجم: ${(model.sizeBytes / (1024 * 1024)).toInt()} مگابایت",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Text(
                    text = "حداقل رم: ${model.minRamRequiredMb} مگابایت",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }

            Text(
                text = "محیط اجرا: ${model.runtimeAvailability}",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            PrimaryActionButton(
                text = if (model.isInstalled) {
                    if (model.isLoaded) "خروج از حافظه" else "بارگذاری در حافظه"
                } else "بررسی فایل مدل",
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AudioPlayerControls(
    isPlaying: Boolean,
    currentTimeMs: Long,
    totalDurationMs: Long,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SecondaryNavy, RoundedCornerShape(12.dp))
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            value = if (totalDurationMs > 0) currentTimeMs.toFloat() / totalDurationMs else 0f,
            onValueChange = { fraction ->
                onSeekTo((fraction * totalDurationMs).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = Turquoise,
                activeTrackColor = Turquoise,
                inactiveTrackColor = DarkNavyCard
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(currentTimeMs / 60000)}:${((currentTimeMs % 60000) / 1000).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
            Text(
                text = "${(totalDurationMs / 60000)}:${((totalDurationMs % 60000) / 1000).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            IconButton(
                onClick = { onSeekTo(maxOf(0L, currentTimeMs - 5000L)) }
            ) {
                Icon(
                    imageVector = Icons.Default.Replay5,
                    contentDescription = "۵ ثانیه عقب",
                    tint = SecondaryText
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(48.dp)
                    .background(Turquoise, RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "توقف" else "پخش",
                    tint = DeepNavy,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = { onSeekTo(minOf(totalDurationMs, currentTimeMs + 5000L)) }
            ) {
                Icon(
                    imageVector = Icons.Default.Forward5,
                    contentDescription = "۵ ثانیه جلو",
                    tint = SecondaryText
                )
            }
        }
    }
}

@Composable
fun AudioTimeline(
    tracks: List<Pair<String, Color>>,
    currentPositionMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SecondaryNavy, RoundedCornerShape(12.dp))
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tracks.forEach { (trackName, trackColor) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = trackName,
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText,
                    modifier = Modifier.width(90.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(DarkNavyCard, RoundedCornerShape(6.dp))
                        .border(1.dp, DarkNavyBorder, RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.85f)
                            .background(trackColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .border(1.dp, trackColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    )
                }
            }
        }
    }
}
