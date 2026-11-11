package com.parspodcast.studio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.screens.mixer.*
import com.parspodcast.studio.ui.theme.*

@Composable
fun AudioMixerScreen(
    onNavigateToPublisher: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AudioMixerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var targetTrackForImport by remember { mutableStateOf("BGM") }
    var showExportDialog by remember { mutableStateOf(false) }

    // SAF Audio File Picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importLocalAudio(it, targetTrackForImport) }
    }

    val trackColors = mapOf(
        "INTRO" to Turquoise,
        "PRESENTER_A" to Cyan,
        "PRESENTER_B" to PurpleAccent,
        "BGM" to SuccessGreen,
        "SFX" to GoldAccent,
        "OUTRO" to Turquoise
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Project Header & Action Bar
        ProjectHeaderBar(
            title = uiState.projectTitle,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            zoomLevel = uiState.zoomLevel,
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onZoomIn = { viewModel.setZoom(uiState.zoomLevel + 0.3f) },
            onZoomOut = { viewModel.setZoom(uiState.zoomLevel - 0.3f) },
            onRenderExport = { showExportDialog = true },
            onAutoPopulateVoice = { viewModel.autoPopulateVoiceSegmentsFromPhase6(uiState.currentProjectId) }
        )

        // Status Toast
        uiState.statusMessage?.let { msg ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Turquoise, modifier = Modifier.size(16.dp))
                    Text(text = msg, style = MaterialTheme.typography.bodySmall, color = PrimaryText)
                }
            }
        }

        // Synchronized Master Transport Controls
        MasterTransportBar(
            playbackState = uiState.playbackState,
            totalDurationMs = uiState.totalDurationMs,
            onPlayPause = { viewModel.togglePlayback() },
            onStop = { viewModel.stopPlayback() },
            onSeekTo = { viewModel.seekPlayback(it) },
            onVolumeChange = { viewModel.playbackPlayer.setMasterVolume(it) }
        )

        // Multitrack Timeline Area (Scrollable horizontal & vertical)
        val horizontalScrollState = rememberScrollState()

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Timeline Time Ruler
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.width(180.dp).height(28.dp).background(DarkNavyCard)) {
                        Text(
                            text = "ترک‌های صوتی",
                            fontSize = 11.sp,
                            color = SecondaryText,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Box(modifier = Modifier.weight(1f).horizontalScroll(horizontalScrollState)) {
                        TimelineRuler(
                            totalDurationMs = uiState.totalDurationMs,
                            zoomLevel = uiState.zoomLevel,
                            modifier = Modifier.width((uiState.totalDurationMs / 1000 * 20 * uiState.zoomLevel).coerceAtLeast(600f).dp)
                        )
                    }
                }

                // Tracks and Clips
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.tracks, key = { it.id }) { track ->
                        val trackClips = uiState.clips.filter { it.trackId == track.id }
                        val color = trackColors[track.trackType] ?: Turquoise

                        Row(
                            modifier = Modifier.fillMaxWidth().height(84.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Track Controls
                            TrackControlPanel(
                                track = track,
                                onVolumeChange = { viewModel.updateTrackVolume(track.id, it) },
                                onPanChange = { viewModel.updateTrackPan(track.id, it) },
                                onToggleMute = { viewModel.toggleTrackMute(track.id) },
                                onToggleSolo = { viewModel.toggleTrackSolo(track.id) },
                                onToggleDucking = { viewModel.toggleTrackDucking(track.id) },
                                onImportAudio = {
                                    targetTrackForImport = track.trackType
                                    audioPickerLauncher.launch(arrayOf("audio/*", "*/*"))
                                }
                            )

                            // Right Track Timeline Lane
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(DeepNavy.copy(alpha = 0.5f))
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                val laneWidth = (uiState.totalDurationMs / 1000 * 20 * uiState.zoomLevel).coerceAtLeast(600f).dp

                                Box(modifier = Modifier.width(laneWidth).fillMaxHeight()) {
                                    for (clip in trackClips) {
                                        TimelineClipBlock(
                                            clip = clip,
                                            totalDurationMs = uiState.totalDurationMs,
                                            isSelected = uiState.selectedClipId == clip.id,
                                            waveformPeaks = uiState.waveformCache[clip.id],
                                            trackColor = color,
                                            onSelect = { viewModel.selectClip(clip.id) },
                                            onMove = { newTime -> viewModel.moveClip(clip.id, newTime) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Clip Properties Sheet
        uiState.selectedClip?.let { clip ->
            ClipPropertiesPanel(
                clip = clip,
                onTrim = { s, e -> viewModel.trimClip(clip.id, s, e) },
                onSplit = { viewModel.splitClipAtPlayhead(clip.id, uiState.playbackState.currentPositionMs) },
                onDuplicate = { viewModel.duplicateClip(clip.id) },
                onDelete = { viewModel.deleteClip(clip.id) },
                onGainAndFadesChange = { g, fi, fo -> viewModel.updateClipGainAndFades(clip.id, g, fi, fo) },
                onClose = { viewModel.selectClip(null) }
            )
        }
    }

    // Render & Export Podcast Dialog
    if (showExportDialog) {
        RenderExportDialog(
            isRendering = uiState.isRenderingExport,
            renderProgress = uiState.renderProgress,
            exportedPath = uiState.exportedFilePath,
            onStartRender = { fileName, sampleRate ->
                viewModel.renderAndExportProject(fileName, sampleRate)
            },
            onDismiss = {
                showExportDialog = false
                viewModel.dismissExportResult()
            },
            onNavigateToPublish = onNavigateToPublisher
        )
    }
}

@Composable
private fun ProjectHeaderBar(
    title: String,
    canUndo: Boolean,
    canRedo: Boolean,
    zoomLevel: Float,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRenderExport: () -> Unit,
    onAutoPopulateVoice: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "استودیو میکس چندترکه (Multitrack Studio)",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$title | پردازش محلی ۴۸ کیلوهرتز",
                style = MaterialTheme.typography.labelSmall,
                color = Cyan
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (canUndo) Turquoise else MutedText)
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (canRedo) Turquoise else MutedText)
            }

            IconButton(onClick = onZoomOut, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = SecondaryText)
            }
            IconButton(onClick = onZoomIn, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = SecondaryText)
            }

            OutlinedButton(
                onClick = onAutoPopulateVoice,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("دریافت صوت فاز ۶", fontSize = 10.sp)
            }

            Button(
                onClick = onRenderExport,
                colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("خروجی پادکست", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MasterTransportBar(
    playbackState: com.parspodcast.studio.data.audio.MultitrackPlaybackState,
    totalDurationMs: Long,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Play / Pause / Stop
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(36.dp)
                    .background(Turquoise, RoundedCornerShape(18.dp))
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = DeepNavy,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(32.dp)
                    .background(DarkNavyCard, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, tint = PrimaryText, modifier = Modifier.size(16.dp))
            }

            // Time Scrubber
            Column(modifier = Modifier.weight(1f)) {
                Slider(
                    value = playbackState.progress,
                    onValueChange = { frac ->
                        val target = (frac * totalDurationMs).toLong()
                        onSeekTo(target)
                    },
                    colors = SliderDefaults.colors(thumbColor = Turquoise, activeTrackColor = Turquoise),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Timestamp display
            Text(
                text = "${formatMs(playbackState.currentPositionMs)} / ${formatMs(totalDurationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = Cyan,
                fontWeight = FontWeight.Bold
            )

            // Master volume
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = SecondaryText, modifier = Modifier.size(16.dp))
                Slider(
                    value = playbackState.masterVolume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = Turquoise, activeTrackColor = Turquoise),
                    modifier = Modifier.width(80.dp)
                )
            }
        }
    }
}

@Composable
private fun RenderExportDialog(
    isRendering: Boolean,
    renderProgress: Float,
    exportedPath: String?,
    onStartRender: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToPublish: () -> Unit
) {
    var outputFileName by remember { mutableStateOf("ParsPodcast_Episode_1.wav") }
    var selectedSampleRate by remember { mutableIntStateOf(48000) }

    AlertDialog(
        onDismissRequest = { if (!isRendering) onDismiss() },
        title = {
            Text(
                text = "رندر نهایی و خروجی پادکست",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (exportedPath == null) {
                    Text(
                        text = "تمامی ترک‌ها (مجریان، موسیقی پس‌زمینه با داکینگ و افکت‌ها) به صورت همگام در قالب فایل صوتی استریو ۱۶ بیتی ترکیب می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )

                    OutlinedTextField(
                        value = outputFileName,
                        onValueChange = { outputFileName = it },
                        label = { Text("نام فایل خروجی") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSampleRate == 48000,
                            onClick = { selectedSampleRate = 48000 },
                            label = { Text("۴۸ کیلوهرتز (استاندارد برادکست)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedSampleRate == 44100,
                            onClick = { selectedSampleRate = 44100 },
                            label = { Text("۴۴.۱ کیلوهرتز (کیفیت CD)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isRendering) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("در حال میکس ترک‌ها: ${(renderProgress * 100).toInt()}%", fontSize = 11.sp, color = Turquoise)
                            LinearProgressIndicator(
                                progress = { renderProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = Turquoise,
                                trackColor = DarkNavyCard
                            )
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                Text("فایل خروجی با موفقیت تولید شد", fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                            Text("مسیر فایل: $exportedPath", fontSize = 10.sp, color = PrimaryText)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (exportedPath == null) {
                Button(
                    onClick = { onStartRender(outputFileName, selectedSampleRate) },
                    enabled = !isRendering,
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                ) {
                    Text("آغاز رندر آفلاین", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToPublish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                ) {
                    Text("ورود به بخش انتشار پادکست", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isRendering) {
                TextButton(onClick = onDismiss) {
                    Text("بستن", color = SecondaryText)
                }
            }
        },
        containerColor = SecondaryNavy,
        shape = RoundedCornerShape(16.dp)
    )
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
