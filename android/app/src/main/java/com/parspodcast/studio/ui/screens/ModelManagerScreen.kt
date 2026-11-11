package com.parspodcast.studio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.parspodcast.studio.data.manager.ModelFileManager
import com.parspodcast.studio.data.repository.ModelStatus
import com.parspodcast.studio.domain.ai.DeviceCapabilityReport
import com.parspodcast.studio.domain.ai.EngineLoadingState
import com.parspodcast.studio.domain.ai.ModelExecutionBackend
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.theme.*

@Composable
fun ModelManagerScreen(
    models: List<ModelStatus>,
    isModelInstalled: Boolean,
    engineLoadingState: EngineLoadingState,
    capabilityReport: DeviceCapabilityReport?,
    copyProgress: Float?, // null if not copying, 0.0 to 1.0 during copy
    onImportModelUri: (Uri) -> Unit,
    onLoadModel: (preferredBackend: ModelExecutionBackend) -> Unit,
    onUnloadModel: () -> Unit,
    onDeleteModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedModelForInfo by remember { mutableStateOf<ModelStatus?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedBackend by remember { mutableStateOf(ModelExecutionBackend.GPU) }

    // SAF File Picker Launcher for .litertlm file
    val safFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportModelUri(uri)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        item {
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
                            text = "مدیریت مدل‌های محلی هوش مصنوعی",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        StatusIndicator(
                            label = "LiteRT-LM آفلاین",
                            color = SuccessGreen
                        )
                    }

                    Text(
                        text = "مدل‌های خانواده Gemma 4 و فایل‌های استنتاج باید در حافظه اختصاصی نرم‌افزار مستقر شوند. ورود فایل‌ها از طریق رابط استاندارد Storage Access Framework انجام می‌گیرد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // Copy Progress Indicator Card (Active import state)
        if (copyProgress != null) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Turquoise)
                    ),
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
                            Text(
                                text = "در حال انتقال امن مدل به حافظه اختصاصی (استریم ۱ مگابایتی)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(copyProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Turquoise
                            )
                        }

                        LinearProgressIndicator(
                            progress = { copyProgress },
                            color = Turquoise,
                            trackColor = SecondaryNavy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        Text(
                            text = "فایل چندگیگابایتی gemma-4-E4B-it.litertlm به صورت قطعه‌قطعه بدون پر کردن رم منتقل می‌گردد.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText
                        )
                    }
                }
            }
        }

        // Primary Model (Gemma 4 E4B) Management Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isModelInstalled) Turquoise.copy(alpha = 0.5f) else DarkNavyBorder
                    )
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
                                text = "Gemma 4 E4B Instruct",
                                style = MaterialTheme.typography.titleSmall,
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "فایل هدف: ${ModelFileManager.EXPECTED_MODEL_FILENAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Cyan
                            )
                        }

                        StatusIndicator(
                            label = if (isModelInstalled) {
                                if (engineLoadingState is EngineLoadingState.Ready) "آماده در رم" else "نصب شده"
                            } else "نصب نشده",
                            color = if (isModelInstalled) SuccessGreen else WarningAmber
                        )
                    }

                    Text(
                        text = "مدل اصلی استودیو برای تدوین خودکار سناریوهای پادکست، بازنویسی متون، خلاصه‌سازی و تحلیل کارشناسی به زبان فارسی بدون اینترنت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )

                    // Hardware Backend Selector (GPU / CPU)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("شتاب‌دهنده سخت‌افزاری:", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedBackend == ModelExecutionBackend.GPU,
                                onClick = { selectedBackend = ModelExecutionBackend.GPU },
                                label = { Text("GPU (OpenCL)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Turquoise,
                                    selectedLabelColor = DeepNavy
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                            FilterChip(
                                selected = selectedBackend == ModelExecutionBackend.CPU,
                                onClick = { selectedBackend = ModelExecutionBackend.CPU },
                                label = { Text("CPU (XNNPACK)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Turquoise,
                                    selectedLabelColor = DeepNavy
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }

                    // Model Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isModelInstalled) {
                            PrimaryActionButton(
                                text = "انتخاب و وارد کردن فایل (.litertlm)",
                                onClick = { safFilePickerLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier.weight(1f),
                                icon = { Icon(Icons.Default.FileOpen, contentDescription = null, tint = DeepNavy) }
                            )
                        } else {
                            if (engineLoadingState is EngineLoadingState.Ready) {
                                SecondaryActionButton(
                                    text = "تخلیه از رم",
                                    onClick = onUnloadModel,
                                    modifier = Modifier.weight(1f),
                                    icon = { Icon(Icons.Default.Memory, contentDescription = null, tint = PrimaryText) }
                                )
                            } else {
                                PrimaryActionButton(
                                    text = "بارگذاری در رم",
                                    onClick = { onLoadModel(selectedBackend) },
                                    modifier = Modifier.weight(1f),
                                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DeepNavy) }
                                )
                            }

                            IconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(DarkNavyCard, RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف فایل مدل", tint = ErrorRed)
                            }
                        }
                    }
                }
            }
        }

        // Hardware Compatibility Report Card
        if (capabilityReport != null) {
            item {
                SectionHeader(
                    title = "گزارش منابع سخت‌افزاری دستگاه (Daria Band / Android 15)",
                    subtitle = "تحلیل توان پردازشی و حافظه قبل از استنتاج"
                )

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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("دستگاه و سیستم‌عامل:", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                            Text("${capabilityReport.deviceModel} | ${capabilityReport.androidVersion}", style = MaterialTheme.typography.labelSmall, color = PrimaryText)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("معماری پردازنده (ABI):", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                            Text(capabilityReport.cpuAbi, style = MaterialTheme.typography.labelSmall, color = Cyan)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("حافظه رم در دسترس:", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                            Text("${capabilityReport.availableRamMb} از ${capabilityReport.totalRamMb} مگابایت", style = MaterialTheme.typography.labelSmall, color = if (capabilityReport.isRamSufficientForE4B) SuccessGreen else WarningAmber)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("فضای ذخیره‌سازی آزاد:", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                            Text("${capabilityReport.availableStorageMb / 1024} گیگابایت", style = MaterialTheme.typography.labelSmall, color = Turquoise)
                        }
                    }
                }
            }
        }

        // TTS Models (Sherpa-ONNX) Section
        item {
            SectionHeader(
                title = "مدل‌های صوتی و تبدیل گفتار (TTS)",
                subtitle = "موتور Sherpa-ONNX و مدل Piper مانای فارسی (فاز بعدی)"
            )
        }

        items(models.filter { it.modelType.contains("TTS") }) { model ->
            ModelStatusCard(
                model = model,
                onActionClick = { selectedModelForInfo = model }
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        ConfirmationDialog(
            title = "حذف فایل مدل Gemma 4",
            message = "آیا از حذف فایل gemma-4-E4B-it.litertlm از حافظه داخلی اطمینان دارید؟ برای استفاده مجدد باید فایل را دوباره انتخاب و وارد نمایید.",
            confirmText = "حذف فایل",
            dismissText = "انصراف",
            onConfirm = {
                onDeleteModel()
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    // Model Info Dialog
    selectedModelForInfo?.let { model ->
        AlertDialog(
            onDismissRequest = { selectedModelForInfo = null },
            title = {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("نوع مدل: ${model.modelType}", style = MaterialTheme.typography.bodySmall, color = Cyan)
                    Text("نیازمندی حافظه: حداقل ${model.minRamRequiredMb} مگابایت", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    Text("وضعیت فعلی: ${if (model.isInstalled) "نصب شده در حافظه" else "هنوز وارد نشده است"}", style = MaterialTheme.typography.bodySmall, color = if (model.isInstalled) SuccessGreen else WarningAmber)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedModelForInfo = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                ) {
                    Text("بستن", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SecondaryNavy,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
