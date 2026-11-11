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
import com.parspodcast.studio.data.repository.ModelStatus
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.theme.*

@Composable
fun ModelManagerScreen(
    models: List<ModelStatus>,
    onToggleModelLoaded: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedModelForInfo by remember { mutableStateOf<ModelStatus?>(null) }
    var selectedModelForDelete by remember { mutableStateOf<ModelStatus?>(null) }

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
                            text = "مدیریت مدل‌های هوش مصنوعی محلی",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        StatusIndicator(
                            label = "استنتاج ۱۰۰٪ آفلاین",
                            color = SuccessGreen
                        )
                    }

                    Text(
                        text = "مدل‌های خانواده Gemma 4 و موتور گفتار Sherpa-ONNX پس از دریافت، بدون ارسال هیچ‌گونه داده‌ای به سرورهای ابری، روی پردازنده گوشی اجرا خواهند شد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // Storage Footprint Summary
        item {
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
                        Text("میزان اشغال حافظه داخلی (محیط ایزوله اپلیکیشن):", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        Text("۰ از ۶۴ گیگابایت", style = MaterialTheme.typography.labelSmall, color = Turquoise)
                    }

                    LinearProgressIndicator(
                        progress = { 0.02f },
                        color = Turquoise,
                        trackColor = DarkNavyCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                }
            }
        }

        // LLM Models Section
        item {
            SectionHeader(
                title = "مدل‌های زبانی بزرگ نگارش فیلم‌نامه (LLM)",
                subtitle = "موتور اجرایی LiteRT-LM برای اندروید"
            )
        }

        items(models.filter { it.modelType.contains("LLM") }) { model ->
            ModelStatusCard(
                model = model,
                onActionClick = { selectedModelForInfo = model }
            )
        }

        // TTS Models Section
        item {
            SectionHeader(
                title = "مدل‌های صوتی و تبدیل متن به گفتار (TTS)",
                subtitle = "موتور Sherpa-ONNX و مدل Piper مانای فارسی"
            )
        }

        items(models.filter { it.modelType.contains("TTS") }) { model ->
            ModelStatusCard(
                model = model,
                onActionClick = { selectedModelForInfo = model }
            )
        }
    }

    // Model Info / Import Requirement Dialog
    selectedModelForInfo?.let { model ->
        AlertDialog(
            onDismissRequest = { selectedModelForInfo = null },
            title = {
                Text(
                    text = "اطلاعات فایل مدل ${model.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "وضعیت: ${if (model.isInstalled) "فایل وزن‌ها در حافظه اختصاصی موجود است" else "فایل وزن‌ها هنوز دانلود یا کپی نشده است."}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (model.isInstalled) SuccessGreen else WarningAmber
                    )
                    Text(
                        text = "حجم فایل: ${(model.sizeBytes / (1024 * 1024)).toInt()} مگابایت",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Text(
                        text = "نیازمندی حافظه رم: حداقل ${model.minRamRequiredMb} مگابایت",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Text(
                        text = "مسیر هدف: context.filesDir/models/${model.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedModelForInfo = null
                    },
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
