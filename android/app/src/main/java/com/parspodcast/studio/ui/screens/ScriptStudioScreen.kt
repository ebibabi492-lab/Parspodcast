package com.parspodcast.studio.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parspodcast.studio.data.local.entities.ScriptSegmentEntity
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.theme.*

@Composable
fun ScriptStudioScreen(
    onNavigateToVoiceStudio: () -> Unit,
    isLocalModelAvailable: Boolean = false, // Honest state
    modifier: Modifier = Modifier
) {
    var episodeTitle by remember { mutableStateOf("پادکست بررسی تحولات هوش مصنوعی و اقتصاد دیجیتال") }
    var programType by remember { mutableStateOf("TWO_PRESENTERS") } // SINGLE_PRESENTER, TWO_PRESENTERS
    var showAiUnavailableDialog by remember { mutableStateOf(false) }

    var segments by remember {
        mutableStateOf(
            listOf(
                ScriptSegmentEntity(
                    id = "seg_1",
                    scriptId = "script_1",
                    segmentIndex = 1,
                    segmentType = "INTRO",
                    speakerRole = "PRESENTER_A",
                    textFa = "به استودیوی پارس‌پادکست خوش آمدید. امروز با بررسی تازه‌ترین رویدادهای فناوری و دنیای هوش مصنوعی در خدمت شما هستیم.",
                    estimatedDurationSec = 14,
                    isApproved = true
                ),
                ScriptSegmentEntity(
                    id = "seg_2",
                    scriptId = "script_1",
                    segmentIndex = 2,
                    segmentType = "NEWS",
                    speakerRole = "PRESENTER_A",
                    textFa = "در سرخط اخبار: پیشرفت‌های چشمگیر در توسعه تراشه‌های پردازش محلی، امکان اجرای مدل‌های زبانی را بدون ارسال اطلاعات به اینترنت فراهم کرده است.",
                    estimatedDurationSec = 22,
                    isApproved = true
                ),
                ScriptSegmentEntity(
                    id = "seg_3",
                    scriptId = "script_1",
                    segmentIndex = 3,
                    segmentType = "DISCUSSION",
                    speakerRole = "PRESENTER_B",
                    textFa = "دقیقاً همین‌طور است. حفظ کامل حریم خصوصی کاربران و کاهش زمان تأخیر از بزرگ‌ترین مزایای اجرای آفلاین مدل‌هاست.",
                    estimatedDurationSec = 18,
                    isApproved = true
                ),
                ScriptSegmentEntity(
                    id = "seg_4",
                    scriptId = "script_1",
                    segmentIndex = 4,
                    segmentType = "OUTRO",
                    speakerRole = "PRESENTER_A",
                    textFa = "از همراهی شما با این بخش خبری سپاسگزاریم. فایل‌های صوتی و متن کامل در بخش آرشیو در دسترس شما قرار دارد. تا برنامه‌ای دیگر خدانگهدار.",
                    estimatedDurationSec = 15,
                    isApproved = true
                )
            )
        )
    }

    val totalWords = segments.sumOf { it.textFa.split("\\s+".toRegex()).size }
    val totalEstimatedSec = segments.sumOf { it.estimatedDurationSec }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp)
    ) {
        // Episode Title Input
        PersianTextField(
            value = episodeTitle,
            onValueChange = { episodeTitle = it },
            label = "عنوان اپیزود پادکست"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Program Mode Selector (Single Host vs Dual Host)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = programType == "SINGLE_PRESENTER",
                onClick = { programType = "SINGLE_PRESENTER" },
                label = { Text("بولتن تک‌گوینده (خبر رسمی)") },
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
                selected = programType == "TWO_PRESENTERS",
                onClick = { programType = "TWO_PRESENTERS" },
                label = { Text("پادکست دوگوینده (گفتگو)") },
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

        Spacer(modifier = Modifier.height(12.dp))

        // Statistics & Action Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryNavy)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "کلمات: ${JalaliCalendar.toPersianDigits(totalWords.toString())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                    Text(
                        text = "مدت تخمینی: ${JalaliCalendar.toPersianDigits((totalEstimatedSec / 60).toString())}:${JalaliCalendar.toPersianDigits(((totalEstimatedSec % 60)).toString().padStart(2, '0'))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Turquoise
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // AI Assistant Action
                    IconButton(
                        onClick = {
                            if (!isLocalModelAvailable) {
                                showAiUnavailableDialog = true
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(DarkNavyCard, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "دستیار هوش مصنوعی نگارش",
                            tint = if (isLocalModelAvailable) Turquoise else WarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Save and Continue
                    Button(
                        onClick = onNavigateToVoiceStudio,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("ارسال به استودیو صدا", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
                                // Speaker Role Selector
                                Button(
                                    onClick = {
                                        val newRole = if (segment.speakerRole == "PRESENTER_A") "PRESENTER_B" else "PRESENTER_A"
                                        segments = segments.mapIndexed { i, s ->
                                            if (i == index) s.copy(speakerRole = newRole) else s
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (segment.speakerRole == "PRESENTER_A") Turquoise.copy(alpha = 0.2f) else PurpleAccent.copy(alpha = 0.2f),
                                        contentColor = if (segment.speakerRole == "PRESENTER_A") Turquoise else PurpleAccent
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (segment.speakerRole == "PRESENTER_A") "مجری الف (اصلی)" else "مجری ب (تحلیل‌گر)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Text(
                                text = "~${JalaliCalendar.toPersianDigits(segment.estimatedDurationSec.toString())} ثانیه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Text Content
                        OutlinedTextField(
                            value = segment.textFa,
                            onValueChange = { newText ->
                                val words = newText.split("\\s+".toRegex()).size
                                segments = segments.mapIndexed { i, s ->
                                    if (i == index) s.copy(
                                        textFa = newText,
                                        estimatedDurationSec = maxOf(4, (words * 0.4).toInt())
                                    ) else s
                                }
                            },
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
    }

    // Honest AI Unavailable State Modal
    if (showAiUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showAiUnavailableDialog = false },
            title = {
                Text(
                    text = "موتور هوش مصنوعی محلی در دسترس نیست",
                    style = MaterialTheme.typography.titleSmall,
                    color = WarningAmber,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "مدل محلی Gemma 4 بر روی حافظه دستگاه نصب یا بارگذاری نشده است. برای بازنویسی، خلاصه‌سازی یا گسترش متن، لطفاً ابتدا وزن‌های مدل را در بخش «مدیر مدل‌ها» دریافت نمایید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = { showAiUnavailableDialog = false },
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
