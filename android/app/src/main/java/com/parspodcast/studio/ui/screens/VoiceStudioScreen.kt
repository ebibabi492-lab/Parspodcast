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
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.theme.*

data class GeneratedAudioItem(
    val id: String,
    val segmentIndex: Int,
    val speakerRole: String,
    val title: String,
    val durationSec: Int,
    val isReady: Boolean
)

@Composable
fun VoiceStudioScreen(
    onNavigateToMixer: () -> Unit,
    isTtsEngineInstalled: Boolean = false, // Honest state
    modifier: Modifier = Modifier
) {
    var selectedSpeaker by remember { mutableStateOf("PRESENTER_A") }
    var speechSpeed by remember { mutableFloatStateOf(1.0f) }
    var speechPitch by remember { mutableFloatStateOf(1.0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackPositionMs by remember { mutableLongStateOf(0L) }
    var showUninstalledDialog by remember { mutableStateOf(false) }

    val sampleAudioList = remember {
        listOf(
            GeneratedAudioItem("audio_1", 1, "PRESENTER_A", "مقدمه و اعلام برنامه", 14, true),
            GeneratedAudioItem("audio_2", 2, "PRESENTER_A", "شرح رویداد اصلی و فکت‌ها", 22, true),
            GeneratedAudioItem("audio_3", 3, "PRESENTER_B", "تحلیل کارشناسی و دیدگاه‌ها", 18, true),
            GeneratedAudioItem("audio_4", 4, "PRESENTER_A", "جمع‌بندی و پایان‌بخش", 15, false)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // TTS Engine Status Banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
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
                        text = "مدل گفتار فارسی: Piper Mana (fa_IR-mana-medium)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                StatusIndicator(
                    label = if (isTtsEngineInstalled) "آماده سنتز" else "وزن‌ها نصب نشده",
                    color = if (isTtsEngineInstalled) SuccessGreen else WarningAmber
                )
            }
        }

        // Speaker Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = selectedSpeaker == "PRESENTER_A",
                onClick = { selectedSpeaker = "PRESENTER_A" },
                label = { Text("مجری الف (راوی و گوینده خبر)") },
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
                selected = selectedSpeaker == "PRESENTER_B",
                onClick = { selectedSpeaker = "PRESENTER_B" },
                label = { Text("مجری ب (کارشناس و تحلیل‌گر)") },
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

        // Speed & Pitch Controls
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("سرعت کلام (Speech Speed):", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    Text(String.format("%.2fx", speechSpeed), style = MaterialTheme.typography.labelSmall, color = Turquoise)
                }

                Slider(
                    value = speechSpeed,
                    onValueChange = { speechSpeed = it },
                    valueRange = 0.8f..1.3f,
                    colors = SliderDefaults.colors(thumbColor = Turquoise, activeTrackColor = Turquoise)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("گام صدا (Pitch):", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    Text(String.format("%.2f", speechPitch), style = MaterialTheme.typography.labelSmall, color = Cyan)
                }

                Slider(
                    value = speechPitch,
                    onValueChange = { speechPitch = it },
                    valueRange = 0.85f..1.15f,
                    colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Cyan)
                )
            }
        }

        // Generate Audio Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryActionButton(
                text = "سنتز صدای بخش‌های فیلم‌نامه",
                onClick = {
                    if (!isTtsEngineInstalled) {
                        showUninstalledDialog = true
                    }
                },
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.Mic, contentDescription = null, tint = DeepNavy) }
            )

            SecondaryActionButton(
                text = "ورود به میکس چندترکه",
                onClick = onNavigateToMixer,
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryText) }
            )
        }

        // Generated Audio List
        SectionHeader(
            title = "قطعات صوتی تولید شده",
            subtitle = "پیش‌نمایش و بررسی قبل از میکس نهایی"
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(sampleAudioList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "بخش ${item.segmentIndex}:", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                Text(text = item.title, style = MaterialTheme.typography.bodySmall, color = PrimaryText, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "گوینده: ${if (item.speakerRole == "PRESENTER_A") "مجری الف" else "مجری ب"} | مدت: ${item.durationSec} ثانیه",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkNavyCard, RoundedCornerShape(18.dp))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "پخش",
                                tint = Turquoise,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // TTS Uninstalled Dialog
    if (showUninstalledDialog) {
        AlertDialog(
            onDismissRequest = { showUninstalledDialog = false },
            title = {
                Text(
                    text = "مدل صوتی مانا هنوز نصب نشده است",
                    style = MaterialTheme.typography.titleSmall,
                    color = WarningAmber,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "برای سنتز گفتار واقعی با موتور Sherpa-ONNX، فایل وزن‌های مانا (fa_IR-mana-medium) باید ابتدا از بخش «مدیر مدل‌ها» دریافت و در حافظه داخلی ذخیره شود. سیستم از موتور پیش‌فرض اندروید استفاده نمی‌کند تا کیفیت صوت مخدوش نشود.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = { showUninstalledDialog = false },
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
