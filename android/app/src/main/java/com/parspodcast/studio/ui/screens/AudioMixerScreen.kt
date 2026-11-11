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
import com.parspodcast.studio.data.local.entities.AudioTrackEntity
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.theme.*

@Composable
fun AudioMixerScreen(
    onNavigateToPublisher: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackPositionMs by remember { mutableLongStateOf(0L) }
    val totalDurationMs = 75000L // 1:15 min
    var showExportPreparedDialog by remember { mutableStateOf(false) }

    var tracks by remember {
        mutableStateOf(
            listOf(
                AudioTrackEntity("track_1", "proj_1", "INTRO", "اینترو و آرم آغازین", 0.9f, false, false, false),
                AudioTrackEntity("track_2", "proj_1", "PRESENTER_A", "مجری الف (گوینده اصلی)", 1.0f, false, false, false),
                AudioTrackEntity("track_3", "proj_1", "PRESENTER_B", "مجری ب (کارشناس تحلیل)", 1.0f, false, false, false),
                AudioTrackEntity("track_4", "proj_1", "BGM", "موسیقی متن (داکینگ فعال)", 0.35f, false, false, true),
                AudioTrackEntity("track_5", "proj_1", "SFX", "افکت‌ها و ترنزیشن‌ها", 0.8f, false, false, false),
                AudioTrackEntity("track_6", "proj_1", "OUTRO", "اوترو و تیتراژ پایانی", 0.9f, false, false, false)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Transport Controls & Timeline Scrubber
        AudioPlayerControls(
            isPlaying = isPlaying,
            currentTimeMs = currentPlaybackPositionMs,
            totalDurationMs = totalDurationMs,
            onPlayPause = { isPlaying = !isPlaying },
            onSeekTo = { currentPlaybackPositionMs = it }
        )

        // Visual Multitrack Timeline Preview
        AudioTimeline(
            tracks = listOf(
                Pair("اینترو", Turquoise),
                Pair("مجری الف", Cyan),
                Pair("مجری ب", PurpleAccent),
                Pair("موسیقی متن", SuccessGreen),
                Pair("افکت‌ها", GoldAccent),
                Pair("اوترو", Turquoise)
            ),
            currentPositionMs = currentPlaybackPositionMs,
            totalDurationMs = totalDurationMs
        )

        // Studio Mixing Board Header & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "کانال‌های صوتی استودیو (۶ ترک)",
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryActionButton(
                    text = "رندر و خروجی",
                    onClick = { showExportPreparedDialog = true },
                    modifier = Modifier.height(34.dp),
                    icon = { Icon(Icons.Default.Download, contentDescription = null, tint = PrimaryText, modifier = Modifier.size(16.dp)) }
                )

                PrimaryActionButton(
                    text = "انتشار پادکست",
                    onClick = onNavigateToPublisher,
                    modifier = Modifier.height(34.dp),
                    icon = { Icon(Icons.Default.Publish, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        // Multitrack List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(tracks) { track ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = track.displayNameFa,
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        tracks = tracks.map {
                                            if (it.id == track.id) it.copy(isMuted = !it.isMuted) else it
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (track.isMuted) ErrorRed else DarkNavyCard,
                                        contentColor = if (track.isMuted) PrimaryText else SecondaryText
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("MUTE", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        tracks = tracks.map {
                                            if (it.id == track.id) it.copy(isSolo = !it.isSolo) else it
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (track.isSolo) GoldAccent else DarkNavyCard,
                                        contentColor = if (track.isSolo) DeepNavy else SecondaryText
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("SOLO", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Volume Fader
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = SecondaryText,
                                modifier = Modifier.size(16.dp)
                            )

                            Slider(
                                value = track.volume,
                                onValueChange = { newVol ->
                                    tracks = tracks.map {
                                        if (it.id == track.id) it.copy(volume = newVol) else it
                                    }
                                },
                                valueRange = 0f..1.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Turquoise,
                                    activeTrackColor = Turquoise,
                                    inactiveTrackColor = DarkNavyCard
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "${(track.volume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Export Dialog (Honest state: Prepared UI)
    if (showExportPreparedDialog) {
        AlertDialog(
            onDismissRequest = { showExportPreparedDialog = false },
            title = {
                Text(
                    text = "آماده‌سازی خط رندر صوتی",
                    style = MaterialTheme.typography.titleSmall,
                    color = Cyan,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "رابط کاربری و ساختار داده‌های میکس ۶ ترکه با موفقیت تنظیم شد. خط رندر چندترکه Media3 در فازهای بعدی پردازش PCM/WAV را به صورت بومی اجرا خواهد نمود.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = { showExportPreparedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                ) {
                    Text("متوجه شدم", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SecondaryNavy,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
