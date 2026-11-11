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
import com.parspodcast.studio.data.repository.UserSettings
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.theme.*

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onUpdateSettings: (UserSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var darkTheme by remember { mutableStateOf(settings.darkTheme) }
    var usePersianNumerals by remember { mutableStateOf(settings.usePersianNumerals) }
    var authorName by remember { mutableStateOf(settings.podcastAuthor) }
    var showCacheClearedDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                    Text(
                        text = "تنظیمات پارس‌پادکست",
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "پیکربندی هویت بصری، زبان و ترجیحات موتورهای صوتی و هوش مصنوعی",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Visual & Language Preferences
        item {
            SectionHeader(
                title = "نمایش و زبان",
                subtitle = "تنظیمات خط و رسم و تم برنامه"
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("پوسته تاریک استودیو (Dark Theme)", style = MaterialTheme.typography.bodySmall, color = PrimaryText, fontWeight = FontWeight.Bold)
                            Text("طراحی تیره استاندارد برای کار مداوم استودیویی", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        }
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = {
                                darkTheme = it
                                onUpdateSettings(settings.copy(darkTheme = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepNavy,
                                checkedTrackColor = Turquoise
                            )
                        )
                    }

                    Divider(color = DarkNavyBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("نمایش ارقام فارسی (۱۲۳)", style = MaterialTheme.typography.bodySmall, color = PrimaryText, fontWeight = FontWeight.Bold)
                            Text("تبدیل خودکار اعداد در متون و زمان‌سنج‌ها", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        }
                        Switch(
                            checked = usePersianNumerals,
                            onCheckedChange = {
                                usePersianNumerals = it
                                onUpdateSettings(settings.copy(usePersianNumerals = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepNavy,
                                checkedTrackColor = Turquoise
                            )
                        )
                    }
                }
            }
        }

        // Podcast Defaults
        item {
            SectionHeader(
                title = "مشخصات پیش‌فرض پادکست",
                subtitle = "اطلاعات ناشر در فید RSS"
            )

            PersianTextField(
                value = authorName,
                onValueChange = {
                    authorName = it
                    onUpdateSettings(settings.copy(podcastAuthor = it))
                },
                label = "نام پدیدآورنده پیش‌فرض"
            )
        }

        // Privacy & Storage
        item {
            SectionHeader(
                title = "حریم خصوصی و حافظه",
                subtitle = "مدیریت حافظه کش و امنیت داده‌ها"
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تمامی سناریوها، متن‌ها و خروجی‌های صوتی به طور اختصاصی روی حافظه داخلی دستگاه شما باقی مانده و تله‌متری یا داده‌های شخصی به سرورهای ابری ارسال نمی‌گردد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )

                    SecondaryActionButton(
                        text = "پاکسازی فایل‌های موقت کش",
                        onClick = { showCacheClearedDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // About Box
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "پارس‌پادکست (ParsPodcast Studio)", style = MaterialTheme.typography.titleSmall, color = PrimaryText, fontWeight = FontWeight.Bold)
                    Text(text = "نسخه: 1.0 (Phase 4 Compose Experience)", style = MaterialTheme.typography.labelSmall, color = Turquoise)
                    Text(text = "طراحی شده برای سیستم‌عامل اندروید با معماری تمیز و Jetpack Compose", style = MaterialTheme.typography.labelSmall, color = MutedText)
                }
            }
        }
    }

    if (showCacheClearedDialog) {
        AlertDialog(
            onDismissRequest = { showCacheClearedDialog = false },
            title = {
                Text("پاکسازی حافظه موقت", style = MaterialTheme.typography.titleSmall, color = Turquoise, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("فایل‌های صوتی موقت با موفقیت از حافظه کش پاکسازی شدند. پروژه‌های آرشیو شده بدون تغییر باقی ماندند.", style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
            },
            confirmButton = {
                Button(
                    onClick = { showCacheClearedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                ) {
                    Text("تأیید", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SecondaryNavy,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
