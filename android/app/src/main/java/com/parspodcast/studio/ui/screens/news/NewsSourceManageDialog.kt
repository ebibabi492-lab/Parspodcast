package com.parspodcast.studio.ui.screens.news

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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.parspodcast.studio.data.local.entities.NewsSourceEntity
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.ui.theme.*

@Composable
fun NewsSourceManageDialog(
    sources: List<NewsSourceEntity>,
    onAddSource: (String, String, String) -> Unit,
    onToggleActive: (NewsSourceEntity) -> Unit,
    onDeleteSource: (NewsSourceEntity) -> Unit,
    onTestFeed: (String) -> Unit,
    testResult: Pair<String, Int>?,
    testError: String?,
    onDismiss: () -> Unit
) {
    var isAddingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("فناوری") }

    val categories = listOf("فناوری", "اقتصاد", "سیاسی", "علمی", "جامعه", "بین‌الملل", "عمومی")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = DeepNavy,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مدیریت منابع خبری و فیدهای اینترنتی",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "فیدهای RSS 2.0 و Atom معتبر جهت دریافت برخط اخبار",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = SecondaryText)
                    }
                }

                // Add New Source Section / Button
                if (!isAddingNew) {
                    Button(
                        onClick = { isAddingNew = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("افزودن منبع خبری جدید", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "مشخصات منبع جدید",
                                style = MaterialTheme.typography.titleSmall,
                                color = Turquoise,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("نام منبع (مثلاً: دیجیاتو، ایسنا)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = newUrl,
                                onValueChange = { newUrl = it },
                                label = { Text("آدرس اینترنتی فید (RSS/Atom URL)") },
                                placeholder = { Text("https://example.com/feed.xml") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onTestFeed(newUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard, contentColor = Cyan),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تست فید", fontSize = 11.sp)
                                }

                                testResult?.let { (title, count) ->
                                    Text(
                                        text = "✓ فید معتبر است ($count خبر یافت شد)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreen
                                    )
                                }
                                testError?.let { err ->
                                    Text(
                                        text = "خطا در اتصال به فید",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarningAmber
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { isAddingNew = false }) {
                                    Text("انصراف", color = SecondaryText)
                                }
                                Button(
                                    onClick = {
                                        if (newName.isNotBlank() && newUrl.isNotBlank()) {
                                            onAddSource(newName, newUrl, newCategory)
                                            isAddingNew = false
                                            newName = ""
                                            newUrl = ""
                                        }
                                    },
                                    enabled = newName.isNotBlank() && newUrl.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                                ) {
                                    Text("ذخیره منبع")
                                }
                            }
                        }
                    }
                }

                // Sources List
                Text(
                    text = "منابع تعریف شده (${sources.size} منبع):",
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryText
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(sources) { src ->
                        SourceItemCard(
                            source = src,
                            onToggleActive = { onToggleActive(src) },
                            onDelete = { onDeleteSource(src) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceItemCard(
    source: NewsSourceEntity,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
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
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${source.parserType})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }

                Text(
                    text = source.feedUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                    maxLines = 1
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "دسته‌بندی: ${source.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                    if (source.lastFetchedUtc > 0) {
                        Text(
                            text = "آخرین بروزرسانی: ${JalaliCalendar.formatShort(source.lastFetchedUtc)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText
                        )
                    }
                }

                source.lastErrorMessage?.let { err ->
                    Text(
                        text = "خطا: $err",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = source.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Turquoise, checkedTrackColor = DarkNavyCard)
                )

                if (source.parserType != "OFFLINE_FILE") {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = SecondaryText, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
