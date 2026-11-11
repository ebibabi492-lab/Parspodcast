package com.parspodcast.studio.ui.screens.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.parspodcast.studio.data.local.entities.NewsArticleEntity
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.ui.components.StatusIndicator
import com.parspodcast.studio.ui.theme.*

@Composable
fun NewsDetailDialog(
    article: NewsArticleEntity,
    isSelectedForScript: Boolean,
    onToggleSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onSaveEdit: (newTitle: String, newContent: String, newCategory: String) -> Unit,
    onDismiss: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember(article.title) { mutableStateOf(article.title) }
    var editContent by remember(article.content) { mutableStateOf(article.content.ifBlank { article.rawSummary }) }
    var editCategory by remember(article.category) { mutableStateOf(article.category) }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (article.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "نشان‌گذاری",
                                tint = if (article.isFavorite) GoldAccent else SecondaryText
                            )
                        }

                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = Turquoise)
                        }

                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = WarningAmber)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = SecondaryText)
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isEditing) {
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )

                        // Meta details
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "منبع: ${article.sourceName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Turquoise
                                    )
                                    Text(
                                        text = "دسته‌بندی: ${article.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecondaryText
                                    )
                                    article.author?.let {
                                        Text(text = "نویسنده: $it", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    StatusIndicator(
                                        label = if (article.inputType == "RSS" || article.inputType == "ATOM") "فید برخط (${article.inputType})" else "فایل محلی (${article.inputType})",
                                        color = if (article.inputType == "RSS" || article.inputType == "ATOM") Cyan else SuccessGreen
                                    )
                                    Text(
                                        text = "انتشار: ${JalaliCalendar.formatShort(article.publishedAtUtc)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedText
                                    )
                                }
                            }
                        }

                        article.originalUrl?.let { url ->
                            Text(
                                text = "پیوند منبع: $url",
                                style = MaterialTheme.typography.labelSmall,
                                color = Cyan
                            )
                        }

                        HorizontalDivider(color = DarkNavyBorder)

                        Text(
                            text = article.content.ifBlank { article.rawSummary },
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryText,
                            lineHeight = 26.sp
                        )
                    } else {
                        // Editing mode
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("عنوان خبر") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = { editCategory = it },
                            label = { Text("دسته‌بندی") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editContent,
                            onValueChange = { editContent = it },
                            label = { Text("متن خبر") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 8
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isEditing = false }) {
                                Text("انصراف", color = SecondaryText)
                            }
                            Button(
                                onClick = {
                                    onSaveEdit(editTitle, editContent, editCategory)
                                    isEditing = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                            ) {
                                Text("ذخیره تغییرات")
                            }
                        }
                    }
                }

                // Bottom Action for Gemma Script Generation
                Button(
                    onClick = onToggleSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelectedForScript) WarningAmber else Turquoise,
                        contentColor = DeepNavy
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isSelectedForScript) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isSelectedForScript) "از انتخاب سناریو خارج شد (برای حذف بزنید)" else "انتخاب این خبر برای نگارش فیلم‌نامه پادکست",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
