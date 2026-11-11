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
import com.parspodcast.studio.domain.news.ParsedNewsDraft
import com.parspodcast.studio.ui.components.StatusIndicator
import com.parspodcast.studio.ui.theme.*

@Composable
fun NewsImportPreviewDialog(
    drafts: List<ParsedNewsDraft>,
    onToggleSelect: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onRemoveItem: (String) -> Unit,
    onUpdateDraft: (String, String, String, String, String) -> Unit,
    onConfirmImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedCount = drafts.count { it.isSelected }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "پیش‌نمایش ورود اخبار از فایل محلی",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$selectedCount از ${drafts.size} خبر برای ذخیره در پایگاه داده انتخاب شده است.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = SecondaryText)
                    }
                }

                // Action Bar (Select all, deselect all)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onSelectAll(true) }) {
                            Text("انتخاب همه", color = Turquoise, fontSize = 12.sp)
                        }
                        TextButton(onClick = { onSelectAll(false) }) {
                            Text("لغو انتخاب همه", color = SecondaryText, fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "فرمت آفلاین: UTF-8",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }

                // Drafts List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(drafts, key = { it.tempId }) { draft ->
                        DraftItemCard(
                            draft = draft,
                            onToggleSelect = { onToggleSelect(draft.tempId) },
                            onRemove = { onRemoveItem(draft.tempId) },
                            onSaveEdit = { newTitle, newSource, newCat, newContent ->
                                onUpdateDraft(draft.tempId, newTitle, newSource, newCat, newContent)
                            }
                        )
                    }
                }

                // Bottom Confirmation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryText)
                    ) {
                        Text("انصراف")
                    }

                    Button(
                        onClick = onConfirmImport,
                        enabled = selectedCount > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ذخیره در آرشیو ($selectedCount)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftItemCard(
    draft: ParsedNewsDraft,
    onToggleSelect: () -> Unit,
    onRemove: () -> Unit,
    onSaveEdit: (String, String, String, String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember(draft.title) { mutableStateOf(draft.title) }
    var editSource by remember(draft.sourceName) { mutableStateOf(draft.sourceName) }
    var editCategory by remember(draft.category) { mutableStateOf(draft.category) }
    var editContent by remember(draft.content) { mutableStateOf(draft.content) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (draft.isDuplicate) WarningAmber.copy(alpha = 0.8f)
                else if (draft.isSelected) Turquoise.copy(alpha = 0.5f)
                else DarkNavyBorder
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
            // Duplicate warning if applicable
            if (draft.isDuplicate) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarningAmber.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                    Text(
                        text = "هشدار تکرار: ${draft.duplicateReason ?: "رکورد مشابه در دیتابیس یافت شد"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber
                    )
                }
            }

            if (!isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = draft.isSelected,
                            onCheckedChange = { onToggleSelect() },
                            colors = CheckboxDefaults.colors(checkedColor = Turquoise, checkmarkColor = DeepNavy)
                        )
                        Column {
                            Text(
                                text = draft.title.ifBlank { "(بدون عنوان)" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "منبع: ${draft.sourceName} | دسته‌بندی: ${draft.category} | تاریخ: ${draft.jalaliDateStr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Cyan
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { isEditing = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = Turquoise, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = SecondaryText, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Text(
                    text = draft.summary.ifBlank { draft.content },
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 3
                )
            } else {
                // Editing mode
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("عنوان خبر") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editSource,
                        onValueChange = { editSource = it },
                        label = { Text("منبع") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("دسته‌بندی") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    label = { Text("متن خبر") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { isEditing = false }) {
                        Text("انصراف", color = SecondaryText)
                    }
                    Button(
                        onClick = {
                            onSaveEdit(editTitle, editSource, editCategory, editContent)
                            isEditing = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                    ) {
                        Text("اعمال تغییرات")
                    }
                }
            }
        }
    }
}
