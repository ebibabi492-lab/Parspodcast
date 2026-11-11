package com.parspodcast.studio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.parspodcast.studio.data.local.entities.ScriptSegmentEntity
import com.parspodcast.studio.domain.tts.TTSEngineState
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.screens.voice.VoiceStudioUiState
import com.parspodcast.studio.ui.screens.voice.VoiceStudioViewModel
import com.parspodcast.studio.ui.theme.*
import java.io.File

@Composable
fun VoiceStudioScreen(
    onNavigateToMixer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceStudioViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // SAF File Pickers for ONNX and JSON
    val onnxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importOnnxModel(it) }
    }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importJsonConfig(it) }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status & Engine Bar
        EngineStatusBanner(
            uiState = uiState,
            onLoadEngine = { viewModel.loadEngine() },
            onUnloadEngine = { viewModel.unloadEngine() },
            onSelectOnnx = { onnxPickerLauncher.launch(arrayOf("*/*")) },
            onSelectJson = { jsonPickerLauncher.launch(arrayOf("*/*")) }
        )

        // Status or Toast Message
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
                    Icon(Icons.Default.Info, contentDescription = null, tint = Turquoise, modifier = Modifier.size(18.dp))
                    Text(text = msg, style = MaterialTheme.typography.bodySmall, color = PrimaryText)
                }
            }
        }

        // Active Player Bar (if an audio track is loaded or playing)
        if (uiState.playbackState.currentAudioPath != null) {
            AudioPlayerWidget(
                uiState = uiState,
                onTogglePlay = {
                    val path = uiState.playbackState.currentAudioPath ?: return@AudioPlayerWidget
                    viewModel.togglePlayback(File(path))
                },
                onStop = { viewModel.playbackManager.stop() },
                onSeek = { viewModel.seekAudio(it) }
            )
        }

        // Speaker Selection with honest architecture indication
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedSpeaker == "PRESENTER_A",
                    onClick = { viewModel.setSelectedSpeaker("PRESENTER_A") },
                    label = { Text("مجری الف (گوینده خبر)") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Turquoise,
                        selectedLabelColor = DeepNavy,
                        containerColor = SecondaryNavy,
                        labelColor = PrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                FilterChip(
                    selected = uiState.selectedSpeaker == "PRESENTER_B",
                    onClick = { viewModel.setSelectedSpeaker("PRESENTER_B") },
                    label = { Text("مجری ب (تحلیل‌گر)") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleAccent,
                        selectedLabelColor = DeepNavy,
                        containerColor = SecondaryNavy,
                        labelColor = PrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            Text(
                text = "نکته معماری: مدل مانا یکپارچه (تک‌گوینده زن) است؛ تمایز دو مجری فعلاً با پارامتر سرعت و لحن اعمال می‌شود.",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Speed & Acoustic Tuning Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("سرعت گفتار (Speech Speed):", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    Text(String.format("%.2fx", uiState.speechSpeed), style = MaterialTheme.typography.labelSmall, color = Turquoise, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = uiState.speechSpeed,
                    onValueChange = { viewModel.setSpeechSpeed(it) },
                    valueRange = 0.8f..1.3f,
                    colors = SliderDefaults.colors(thumbColor = Turquoise, activeTrackColor = Turquoise)
                )

                // Quick Sentence Test
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.testSentence,
                        onValueChange = { viewModel.setTestSentence(it) },
                        label = { Text("متن آزمایشی") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Turquoise,
                            unfocusedBorderColor = DarkNavyBorder,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        )
                    )

                    Button(
                        onClick = { viewModel.synthesizeTestSentence() },
                        enabled = uiState.engineState is TTSEngineState.Ready,
                        colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تست صدا", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Script Segments Audio Synthesis Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryActionButton(
                text = "سنتز تمام بخش‌های سناریو",
                onClick = { viewModel.synthesizeAllSegments() },
                enabled = uiState.engineState is TTSEngineState.Ready && uiState.segments.isNotEmpty(),
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = DeepNavy) }
            )

            SecondaryActionButton(
                text = "ورود به میکس چندترکه",
                onClick = onNavigateToMixer,
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryText) }
            )
        }

        // Segments Audio List
        SectionHeader(
            title = "قطعات صوتی سناریو (${uiState.segments.size} بخش)",
            subtitle = "سنتز آفلاین، بررسی و پیش‌شنیداری فایل‌های WAV"
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (uiState.segments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "سناریویی یافت نشد. لطفاً ابتدا از استودیو سناریو (Script Studio) سناریو تولید کنید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                        )
                    }
                }
            } else {
                items(uiState.segments) { segment ->
                    SegmentAudioCard(
                        segment = segment,
                        isPlaying = uiState.playbackState.isPlaying && uiState.playbackState.activeSegmentId == segment.id,
                        onPlayPause = {
                            if (segment.generatedAudioPath != null) {
                                viewModel.togglePlayback(File(segment.generatedAudioPath), segment.id)
                            } else {
                                viewModel.synthesizeSegment(segment)
                            }
                        },
                        onSynthesize = { viewModel.synthesizeSegment(segment) },
                        isEngineReady = uiState.engineState is TTSEngineState.Ready
                    )
                }
            }
        }
    }
}

@Composable
private fun EngineStatusBanner(
    uiState: VoiceStudioUiState,
    onLoadEngine: () -> Unit,
    onUnloadEngine: () -> Unit,
    onSelectOnnx: () -> Unit,
    onSelectJson: () -> Unit
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
                        text = "موتور تبدیل متن به گفتار: Sherpa-ONNX",
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "مدل گفتار فارسی: Mana Persian Piper (۲۲,۰۵۰ هرتز)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                StatusIndicator(
                    label = when (uiState.engineState) {
                        is TTSEngineState.Ready -> "آماده سنتز (در RAM)"
                        is TTSEngineState.Synthesizing -> "در حال سنتز..."
                        is TTSEngineState.Loading -> "در حال بارگذاری..."
                        else -> if (uiState.modelStatus.isComplete) "آماده لود" else "فایل‌ها نصب نشده"
                    },
                    color = when (uiState.engineState) {
                        is TTSEngineState.Ready -> SuccessGreen
                        is TTSEngineState.Synthesizing -> Turquoise
                        is TTSEngineState.Loading -> WarningAmber
                        else -> if (uiState.modelStatus.isComplete) Cyan else WarningAmber
                    }
                )
            }

            // If files are not installed, show SAF action buttons
            if (!uiState.modelStatus.isComplete) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSelectOnnx,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Turquoise)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (uiState.modelStatus.isOnnxInstalled) "ONNX نصب شد ✓" else "انتخاب فایل .onnx",
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onSelectJson,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (uiState.modelStatus.isJsonInstalled) "کانفیگ نصب شد ✓" else "انتخاب .onnx.json",
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                // Engine Load / Unload buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.engineState !is TTSEngineState.Ready) {
                        Button(
                            onClick = onLoadEngine,
                            colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("بارگذاری در RAM", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onUnloadEngine,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تخلیه از RAM", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Progress bar if synthesizing
            if (uiState.engineState is TTSEngineState.Synthesizing) {
                val synth = uiState.engineState as TTSEngineState.Synthesizing
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "سنتز بخش ${synth.currentSegment} از ${synth.totalSegments}: ${synth.textSnippet}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Turquoise
                        )
                    }
                    LinearProgressIndicator(
                        progress = { synth.currentSegment.toFloat() / synth.totalSegments },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Turquoise,
                        trackColor = DarkNavyCard
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioPlayerWidget(
    uiState: VoiceStudioUiState,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Turquoise.copy(alpha = 0.5f))
        ),
        modifier = Modifier.fillMaxWidth()
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Turquoise, RoundedCornerShape(18.dp))
                    ) {
                        Icon(
                            imageVector = if (uiState.playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "پخش",
                            tint = DeepNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(32.dp)
                            .background(SecondaryNavy, RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "توقف",
                            tint = PrimaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = File(uiState.playbackState.currentAudioPath ?: "").name,
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "فرمت: WAV PCM (۲۲,۰۵۰ Hz, Mono 16-bit)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan
                        )
                    }
                }

                Text(
                    text = "${formatMs(uiState.playbackState.currentPositionMs)} / ${formatMs(uiState.playbackState.totalDurationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
            }

            Slider(
                value = uiState.playbackState.progress,
                onValueChange = onSeek,
                colors = SliderDefaults.colors(thumbColor = Turquoise, activeTrackColor = Turquoise),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SegmentAudioCard(
    segment: ScriptSegmentEntity,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSynthesize: () -> Unit,
    isEngineReady: Boolean
) {
    val isVoiced = segment.generatedAudioPath != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isPlaying) Turquoise else DarkNavyBorder
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "بخش ${segment.segmentIndex}:", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Text(
                        text = if (segment.speakerRole == "PRESENTER_A") "مجری الف" else "مجری ب",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (segment.speakerRole == "PRESENTER_A") Turquoise else PurpleAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                StatusIndicator(
                    label = if (isVoiced) "صوت آماده (${segment.estimatedDurationSec} ثانیه)" else "نیازمند سنتز",
                    color = if (isVoiced) SuccessGreen else WarningAmber
                )
            }

            Text(
                text = segment.textFa,
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryText,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isVoiced) {
                    Button(
                        onClick = onPlayPause,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) WarningAmber else Turquoise,
                            contentColor = DeepNavy
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPlaying) "توقف" else "پخش WAV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                }

                OutlinedButton(
                    onClick = onSynthesize,
                    enabled = isEngineReady,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isVoiced) "سنتز مجدد" else "تولید صوت", fontSize = 11.sp)
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
