package com.parspodcast.studio.ui.screens.mixer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parspodcast.studio.data.local.entities.AudioClipEntity
import com.parspodcast.studio.data.local.entities.AudioTrackEntity
import com.parspodcast.studio.ui.theme.*

@Composable
fun TimelineRuler(
    totalDurationMs: Long,
    zoomLevel: Float,
    modifier: Modifier = Modifier
) {
    val durationSeconds = (totalDurationMs / 1000).toInt().coerceAtLeast(10)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(SecondaryNavy)
    ) {
        val width = size.width
        val secStep = when {
            zoomLevel > 1.8f -> 1
            zoomLevel > 1.0f -> 2
            else -> 5
        }

        drawLine(
            color = DarkNavyBorder,
            start = Offset(0f, size.height),
            end = Offset(width, size.height),
            strokeWidth = 2f
        )

        for (sec in 0..durationSeconds step secStep) {
            val x = (sec.toFloat() / durationSeconds) * width
            val isMajor = sec % 10 == 0
            val tickHeight = if (isMajor) 14f else 8f

            drawLine(
                color = if (isMajor) Turquoise else MutedText,
                start = Offset(x, size.height - tickHeight),
                end = Offset(x, size.height),
                strokeWidth = if (isMajor) 2f else 1f
            )
        }
    }
}

@Composable
fun TrackControlPanel(
    track: AudioTrackEntity,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onToggleDucking: () -> Unit,
    onImportAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
        ),
        modifier = modifier
            .width(180.dp)
            .height(84.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Track Name & Import Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = track.displayNameFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                IconButton(
                    onClick = onImportAudio,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "افزودن صوت",
                        tint = Turquoise,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Mute / Solo / Ducking buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onToggleMute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (track.isMuted) ErrorRed else SecondaryNavy,
                        contentColor = if (track.isMuted) PrimaryText else SecondaryText
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text("M", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onToggleSolo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (track.isSolo) GoldAccent else SecondaryNavy,
                        contentColor = if (track.isSolo) DeepNavy else SecondaryText
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text("S", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                if (track.trackType == "BGM") {
                    Button(
                        onClick = onToggleDucking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (track.duckingEnabled) Turquoise else SecondaryNavy,
                            contentColor = if (track.duckingEnabled) DeepNavy else SecondaryText
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text("DUCK", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Volume Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Vol", fontSize = 8.sp, color = MutedText)
                Slider(
                    value = track.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = Turquoise, activeTrackColor = Turquoise),
                    modifier = Modifier.height(16.dp).weight(1f)
                )
                Text(
                    text = "${(track.volume * 100).toInt()}%",
                    fontSize = 8.sp,
                    color = Cyan
                )
            }
        }
    }
}

@Composable
fun TimelineClipBlock(
    clip: AudioClipEntity,
    totalDurationMs: Long,
    isSelected: Boolean,
    waveformPeaks: FloatArray?,
    trackColor: Color,
    onSelect: () -> Unit,
    onMove: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val durationSafe = totalDurationMs.coerceAtLeast(1000L)
    val startRatio = (clip.startTimeMs.toFloat() / durationSafe).coerceIn(0f, 1f)
    val widthRatio = (clip.durationMs.toFloat() / durationSafe).coerceIn(0.02f, 1f)

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(84.dp)) {
        val totalWidth = maxWidth
        val startOffset = totalWidth * startRatio
        val clipWidth = (totalWidth * widthRatio).coerceAtLeast(40.dp)

        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) trackColor.copy(alpha = 0.25f) else SecondaryNavy
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (isSelected) Turquoise else trackColor.copy(alpha = 0.6f)
                )
            ),
            modifier = Modifier
                .offset(x = startOffset)
                .width(clipWidth)
                .fillMaxHeight()
                .clickable(onClick = onSelect)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = clip.displayName.ifBlank { "قطعه صوتی" },
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                // Render Waveform Canvas
                WaveformCanvas(
                    peaks = waveformPeaks ?: FloatArray(64) { 0.3f },
                    waveColor = trackColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${clip.durationMs / 1000}s",
                        fontSize = 8.sp,
                        color = MutedText
                    )
                    if (clip.gainDb != 0f) {
                        Text(
                            text = String.format("%+.1fdB", clip.gainDb),
                            fontSize = 8.sp,
                            color = Cyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WaveformCanvas(
    peaks: FloatArray,
    waveColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        if (peaks.isEmpty()) return@Canvas

        val barWidth = (width / peaks.size).coerceAtLeast(1f)

        for (i in peaks.indices) {
            val peak = peaks[i].coerceIn(0.05f, 1.0f)
            val barHeight = peak * midY
            val x = i * barWidth

            drawLine(
                color = waveColor,
                start = Offset(x, midY - barHeight),
                end = Offset(x, midY + barHeight),
                strokeWidth = (barWidth * 0.75f).coerceAtLeast(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipPropertiesPanel(
    clip: AudioClipEntity,
    onTrim: (Long, Long) -> Unit,
    onSplit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onGainAndFadesChange: (Float, Long, Long) -> Unit,
    onClose: () -> Unit
) {
    var gainDb by remember(clip.id, clip.gainDb) { mutableFloatStateOf(clip.gainDb) }
    var fadeInMs by remember(clip.id, clip.fadeInMs) { mutableLongStateOf(clip.fadeInMs) }
    var fadeOutMs by remember(clip.id, clip.fadeOutMs) { mutableLongStateOf(clip.fadeOutMs) }

    Card(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
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
                        text = "تنظیمات کلیپ: ${clip.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "موقعیت: ${clip.startTimeMs / 1000}s | طول: ${clip.durationMs / 1000}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "بستن", tint = SecondaryText)
                }
            }

            // Gain Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("گین صوتی (Gain):", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                Text(String.format("%+.1f dB", gainDb), style = MaterialTheme.typography.labelSmall, color = Turquoise, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = gainDb,
                onValueChange = {
                    gainDb = it
                    onGainAndFadesChange(gainDb, fadeInMs, fadeOutMs)
                },
                valueRange = -12f..6f,
                colors = SliderDefaults.colors(thumbColor = Turquoise, activeTrackColor = Turquoise)
            )

            // Fade-in / Fade-out Sliders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("فید آغازین (Fade In): ${fadeInMs}ms", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    Slider(
                        value = fadeInMs.toFloat(),
                        onValueChange = {
                            fadeInMs = it.toLong()
                            onGainAndFadesChange(gainDb, fadeInMs, fadeOutMs)
                        },
                        valueRange = 0f..2000f,
                        colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Cyan)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("فید پایانی (Fade Out): ${fadeOutMs}ms", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    Slider(
                        value = fadeOutMs.toFloat(),
                        onValueChange = {
                            fadeOutMs = it.toLong()
                            onGainAndFadesChange(gainDb, fadeInMs, fadeOutMs)
                        },
                        valueRange = 0f..3000f,
                        colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent)
                    )
                }
            }

            // Actions: Split, Duplicate, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSplit,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard, contentColor = Turquoise),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تقسیم در نشانگر", fontSize = 11.sp)
                }

                Button(
                    onClick = onDuplicate,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard, contentColor = Cyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CopyAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تکثیر", fontSize = 11.sp)
                }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.2f), contentColor = ErrorRed),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("حذف", fontSize = 11.sp)
                }
            }
        }
    }
}
