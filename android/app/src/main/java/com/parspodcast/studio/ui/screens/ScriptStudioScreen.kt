package com.parspodcast.studio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.parspodcast.studio.data.local.entities.ScriptSegmentEntity
import com.parspodcast.studio.domain.ai.EngineLoadingState
import com.parspodcast.studio.domain.ai.GenerationSettings
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.screens.script.ScriptGenerationUiState
import com.parspodcast.studio.ui.theme.*

@Composable
fun ScriptStudioScreen(
    engineState: EngineLoadingState,
    isModelFileInstalled: Boolean,
    generationUiState: ScriptGenerationUiState,
    generationSettings: GenerationSettings,
    segments: List<ScriptSegmentEntity>,
    selectedArticles: List<NewsArticleEntity> = emptyList(),
    onLoadModel: () -> Unit,
    onUnloadModel: () -> Unit,
    onUpdateSettings: (GenerationSettings) -> Unit,
    onGenerateFullScript: (title: String, durationMin: Int, isDualHost: Boolean, tone: String) -> Unit,
    onCancelGeneration: () -> Unit,
    onUpdateSegmentText: (String, String) -> Unit,
    onToggleSpeaker: (String) -> Unit,
    onRewriteSegment: (String, String) -> Unit,
    onSummarizeSegment: (String) -> Unit,
    onExpandSegment: (String, String) -> Unit,
    onChangeToneSegment: (String, String) -> Unit,
    onSaveDraft: () -> Unit,
    onNavigateToModelManager: () -> Unit,
    onNavigateToVoiceStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var episodeTitle by remember { mutableStateOf("پادکست تحلیلی بررسی فناوری‌های هوش مصنوعی محلی") }
    var programType by remember { mutableStateOf("TWO_PRESENTERS") }
    var targetDurationMinutes by remember { mutableIntStateOf(10) }
    var selectedTone by remember { mutableStateOf("تحلیلی و کارشناسی") }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    var editingSegmentForAction by remember { mutableStateOf<ScriptSegmentEntity?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "rewrite", "expand", "tone"
    var actionParamInput by remember { mutableStateOf("") }

    val isDualHost = programType == "TWO_PRESENTERS"
    val isModelReady = engineState is EngineLoadingState.Ready
    val totalWords = segments.sumOf { it.textFa.split("\\s+".toRegex()).size }
    val totalEstimatedSec = segments.sumOf { it.estimatedDurationSec }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp)
    ) {
        // Model Readiness Status Banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (isModelReady) SuccessGreen.copy(alpha = 0.5f) else DarkNavyBorder
                )
            ),
            modifier = Modifier.fillMaxWidth()
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = if (isModelReady) Turquoise else WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "موتور استنتاج محلی: Gemma 4 E4B",
                                style = MaterialTheme.typography.titleSmall,
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (engineState) {
                                    is EngineLoadingState.Ready -> "آماده استنتاج روی ${engineState.backend} (رم: ${engineState.ramUsageMb}MB)"
                                    is EngineLoadingState.Loading -> engineState.stageFa
                                    is EngineLoadingState.CheckingResources -> "در حال ارزیابی حافظه رم و توان پردازشی..."
                                    is EngineLoadingState.Error -> engineState.messageFa
                                    EngineLoadingState.Uninitialized -> if (isModelFileInstalled) "فایل مدل موجود است (نیاز به بارگذاری)" else "فایل مدل نصب نشده است"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isModelReady) SuccessGreen else Cyan
                            )
                        }
                    }

                    when (engineState) {
                        is EngineLoadingState.Ready -> {
                            Button(
                                onClick = onUnloadModel,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard, contentColor = SecondaryText),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("تخلیه رم", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        is EngineLoadingState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Turquoise,
                                strokeWidth = 2.dp
                            )
                        }
                        else -> {
                            if (isModelFileInstalled) {
                                Button(
                                    onClick = onLoadModel,
                                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("بارگذاری در رم", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = onNavigateToModelManager,
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard, contentColor = Turquoise),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("وارد کردن فایل", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Episode Title Input
        PersianTextField(
            value = episodeTitle,
            onValueChange = { episodeTitle = it },
            label = "عنوان اپیزود پادکست"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Program Mode (Single vs Dual Host)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = !isDualHost,
                onClick = { programType = "SINGLE_PRESENTER" },
                label = { Text("تک‌گوینده (HOST_A)") },
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
                selected = isDualHost,
                onClick = { programType = "TWO_PRESENTERS" },
                label = { Text("دوگوینده (HOST_A و HOST_B)") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Turquoise,
                    selectedLabelColor = DeepNavy,
                    containerColor = SecondaryNavy,
                    labelColor = PrimaryText
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Generation Toolbar & Settings Expander
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryNavy)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "کلمات: ${JalaliCalendar.toPersianDigits(totalWords.toString())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText
                        )
                        Text(
                            text = "مدت: ${JalaliCalendar.toPersianDigits((totalEstimatedSec / 60).toString())}:${JalaliCalendar.toPersianDigits(((totalEstimatedSec % 60)).toString().padStart(2, '0'))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Turquoise
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Toggle Advanced Settings
                        IconButton(
                            onClick = { showAdvancedSettings = !showAdvancedSettings },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "تنظیمات استنتاج",
                                tint = if (showAdvancedSettings) Turquoise else SecondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Generate Full Script with Gemma 4
                        Button(
                            onClick = {
                                onGenerateFullScript(episodeTitle, targetDurationMinutes, isDualHost, selectedTone)
                            },
                            enabled = isModelReady && generationUiState !is ScriptGenerationUiState.Generating,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تولید سناریو با گما", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Advanced Settings Panel
                AnimatedVisibility(visible = showAdvancedSettings) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(color = DarkNavyBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("دمای پاسخ (Temperature):", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                            Text(String.format("%.2f", generationSettings.temperature), style = MaterialTheme.typography.labelSmall, color = Cyan)
                        }
                        Slider(
                            value = generationSettings.temperature,
                            onValueChange = { onUpdateSettings(generationSettings.copy(temperature = it)) },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Cyan)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("حداکثر طول خروجی (Max Tokens):", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                            Text("${generationSettings.maxTokens} توکن", style = MaterialTheme.typography.labelSmall, color = Turquoise)
                        }
                        Slider(
                            value = generationSettings.maxTokens.toFloat(),
                            onValueChange = { onUpdateSettings(generationSettings.copy(maxTokens = it.toInt())) },
                            valueRange = 512f..4096f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = Turquoise, activeTrackColor = Turquoise)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Streaming Display Card (Active generation state)
        if (generationUiState is ScriptGenerationUiState.Generating) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Turquoise)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Turquoise, strokeWidth = 2.dp)
                            Text(
                                text = "در حال تولید زنده با Gemma 4 E4B...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Turquoise,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onCancelGeneration,
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = PrimaryText),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("توقف استنتاج", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Text(
                        text = "توکن‌های تولید شده: ${generationUiState.tokensGenerated} | زمان: ${generationUiState.elapsedMs / 1000}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )

                    Text(
                        text = generationUiState.partialText.takeLast(400),
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryText,
                        maxLines = 4
                    )
                }
            }
        }

        // Script Segments List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(segments) { index, segment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "بخش ${JalaliCalendar.toPersianDigits((index + 1).toString())}:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedText
                                )
                                Button(
                                    onClick = { onToggleSpeaker(segment.id) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (segment.speakerRole == "PRESENTER_A") Turquoise.copy(alpha = 0.2f) else PurpleAccent.copy(alpha = 0.2f),
                                        contentColor = if (segment.speakerRole == "PRESENTER_A") Turquoise else PurpleAccent
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(
                                        text = if (segment.speakerRole == "PRESENTER_A") "مجری الف (HOST_A)" else "مجری ب (HOST_B)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            // AI Action Buttons for this segment
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        editingSegmentForAction = segment
                                        actionType = "rewrite"
                                        actionParamInput = "جملات را رسمی‌تر و شیواتر کن"
                                    },
                                    enabled = isModelReady,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "بازنویسی با هوش مصنوعی", tint = if (isModelReady) Cyan else MutedText, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { onSummarizeSegment(segment.id) },
                                    enabled = isModelReady,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Default.Compress, contentDescription = "خلاصه‌سازی", tint = if (isModelReady) Turquoise else MutedText, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = segment.textFa,
                            onValueChange = { onUpdateSegmentText(segment.id, it) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText,
                                focusedContainerColor = DarkNavyCard,
                                unfocusedContainerColor = DarkNavyCard,
                                focusedBorderColor = Turquoise,
                                unfocusedBorderColor = DarkNavyBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Actions (Save draft & proceed to Voice Studio)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryActionButton(
                text = "ذخیره پیش‌نویس",
                onClick = onSaveDraft,
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.Save, contentDescription = null, tint = PrimaryText) }
            )

            PrimaryActionButton(
                text = "تأیید و ارسال به استودیو صدا",
                onClick = onNavigateToVoiceStudio,
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = DeepNavy) }
            )
        }
    }

    // Segment AI Action Parameter Dialog (Rewrite / Expand / Tone)
    editingSegmentForAction?.let { seg ->
        AlertDialog(
            onDismissRequest = { editingSegmentForAction = null },
            title = {
                Text(
                    text = when (actionType) {
                        "rewrite" -> "بازنویسی بخش ${seg.segmentIndex} با Gemma 4"
                        "expand" -> "گسترش متن بخش با هوش مصنوعی"
                        else -> "تغییر لحن بخش خبری"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("دستور یا راهنمایی برای مدل:", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    PersianTextField(
                        value = actionParamInput,
                        onValueChange = { actionParamInput = it },
                        label = "دستور کار",
                        placeholder = "مثال: لحن را خودمانی‌تر کن / جزئیات فنی را بیشتر توضیح بده"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val act = actionType
                        val targetSeg = seg
                        editingSegmentForAction = null
                        if (act == "rewrite") {
                            onRewriteSegment(targetSeg.id, actionParamInput)
                        } else if (act == "expand") {
                            onExpandSegment(targetSeg.id, actionParamInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                ) {
                    Text("اجرای استنتاج", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSegmentForAction = null }) {
                    Text("انصراف", color = SecondaryText)
                }
            },
            containerColor = SecondaryNavy,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
