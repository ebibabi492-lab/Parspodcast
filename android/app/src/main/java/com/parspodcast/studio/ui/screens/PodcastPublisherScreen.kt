package com.parspodcast.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun PodcastPublisherScreen(
    currentEpisodeTitle: String = "اپیزود جدید پارس‌پادکست",
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(currentEpisodeTitle) }
    var description by remember { mutableStateOf("بررسی جامع مهم‌ترین رویدادهای فناوری، هوش مصنوعی و اخبار صوتی") }
    var author by remember { mutableStateOf("استودیوی پارس‌پادکست") }
    var category by remember { mutableStateOf("Technology") }
    var isExplicit by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("local") } // "local" or "remote"
    var showConfirmPublishDialog by remember { mutableStateOf(false) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Publisher Header
        item {
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
                            text = "ناشر و توزیع‌کننده پادکست",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "فید استاندارد RSS 2.0 و بسته‌های صادراتی صوتی",
                            style = MaterialTheme.typography.labelSmall,
                            color = Turquoise,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    StatusIndicator(
                        label = "آماده توزیع",
                        color = Turquoise
                    )
                }
            }
        }

        // Cover Artwork Preview Box
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(DarkNavyCard, RoundedCornerShape(10.dp))
                            .border(1.dp, Turquoise.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Podcasts,
                            contentDescription = "کاور پادکست",
                            tint = Turquoise,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تصویر کاور استاندارد (Cover Artwork)",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ابعاد ۳۰۰۰×۳۰۰۰ پیکسل، RGB، سازگار با اپل پادکست و اسپاتیفای",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Metadata Fields
        item {
            SectionHeader(
                title = "متادیتای استاندارد پادکست",
                subtitle = "مشخصات لازم برای اینکلوژر و فید RSS"
            )

            PersianTextField(
                value = title,
                onValueChange = { title = it },
                label = "عنوان اپیزود"
            )

            Spacer(modifier = Modifier.height(10.dp))

            PersianTextField(
                value = description,
                onValueChange = { description = it },
                label = "توضیحات و شرح خلاصه",
                minLines = 3,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PersianTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = "نام پدیدآورنده",
                    modifier = Modifier.weight(1f)
                )

                PersianTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = "دسته‌بندی (iTunes)",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Publishing Mode Tabs: Local Export vs Remote RSS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = activeTab == "local",
                    onClick = { activeTab = "local" },
                    label = { Text("صادرات و ذخیره محلی (Local Export)") },
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
                    selected = activeTab == "remote",
                    onClick = { activeTab = "remote" },
                    label = { Text("انتشار در فید RSS خودمیزبان") },
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
        }

        // Local Export Actions
        if (activeTab == "local") {
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "فرمت‌های خروجی روی دستگاه:",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )

                        PrimaryActionButton(
                            text = "خروجی باکیفیت MP3 (320 kbps)",
                            onClick = { showExportSuccessDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            icon = { Icon(Icons.Default.Download, contentDescription = null, tint = DeepNavy) }
                        )

                        SecondaryActionButton(
                            text = "خروجی استودیویی WAV (16-bit PCM)",
                            onClick = { showExportSuccessDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            icon = { Icon(Icons.Default.AudioFile, contentDescription = null, tint = PrimaryText) }
                        )
                    }
                }
            }
        }

        // Remote Publishing Actions
        if (activeTab == "remote") {
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "نشانی فید پادکست: https://parspodcast.ir/feed.xml",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan
                        )

                        Text(
                            text = "تولید تگ‌های انکلوژر، تاریخ‌های RFC-822، شناسه GUID یکتا و اعتبارسنجی سرور خودمیزبان",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )

                        PrimaryActionButton(
                            text = "تأیید و انتشار نهایی اپیزود",
                            onClick = { showConfirmPublishDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            icon = { Icon(Icons.Default.Publish, contentDescription = null, tint = DeepNavy) }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmPublishDialog) {
        ConfirmationDialog(
            title = "تأیید انتشار عمومی اپیزود",
            message = "آیا از انتشار این اپیزود در فید RSS 2.0 اطمینان دارید؟ فید عمومی به‌روزرسانی شده و در دایرکتوری‌ها منعکس خواهد شد.",
            confirmText = "انتشار رسمی",
            dismissText = "انصراف",
            onConfirm = { showConfirmPublishDialog = false },
            onDismiss = { showConfirmPublishDialog = false }
        )
    }

    // Export Prepared Dialog
    if (showExportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            title = {
                Text(
                    text = "آماده‌سازی فایل صوتی پادکست",
                    style = MaterialTheme.typography.titleSmall,
                    color = Turquoise,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "پروژه و متادیتا آماده صادرات در مسیر امن داخلی اپلیکیشن است. در فاز بعدی خط رندر فایل فیزیکی را بر روی حافظه دستگاه ذخیره خواهد نمود.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = { showExportSuccessDialog = false },
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
