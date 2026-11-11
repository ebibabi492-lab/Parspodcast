package com.parspodcast.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.parspodcast.studio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParsPodcastTopBar(
    title: String,
    currentDestination: String,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "پارس‌پادکست | $title",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "استودیوی بومی تولید پادکست هوش مصنوعی",
                    style = MaterialTheme.typography.labelSmall,
                    color = Turquoise
                )
            }
        },
        actions = {
            IconButton(onClick = { onNavigateTo("model_manager") }) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "مدیر مدل‌ها",
                    tint = if (currentDestination == "model_manager") Turquoise else SecondaryText
                )
            }

            IconButton(onClick = { onNavigateTo("settings") }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "تنظیمات",
                    tint = if (currentDestination == "settings") Turquoise else SecondaryText
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "منوی بیشتر",
                        tint = SecondaryText
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SecondaryNavy)
                ) {
                    DropdownMenuItem(
                        text = { Text("استودیو صدا و گویندگان", color = PrimaryText) },
                        leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = Turquoise) },
                        onClick = {
                            showMenu = false
                            onNavigateTo("voice_studio")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("ناشر و توزیع RSS", color = PrimaryText) },
                        leadingIcon = { Icon(Icons.Default.Publish, contentDescription = null, tint = Cyan) },
                        onClick = {
                            showMenu = false
                            onNavigateTo("publisher")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("مدیریت مدل‌های محلی", color = PrimaryText) },
                        leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null, tint = PurpleAccent) },
                        onClick = {
                            showMenu = false
                            onNavigateTo("model_manager")
                        }
                    )
                    Divider(color = DarkNavyBorder)
                    DropdownMenuItem(
                        text = { Text("تنظیمات برنامه", color = PrimaryText) },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = SecondaryText) },
                        onClick = {
                            showMenu = false
                            onNavigateTo("settings")
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DeepNavy,
            titleContentColor = PrimaryText
        ),
        modifier = modifier
    )
}

@Composable
fun ParsPodcastBottomNavigation(
    currentDestination: String,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = SecondaryNavy,
        contentColor = PrimaryText,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        val items = listOf(
            Triple("dashboard", "پیشخوان", Icons.Default.Dashboard),
            Triple("news_center", "مرکز خبر", Icons.Default.Article),
            Triple("script_studio", "فیلم‌نامه", Icons.Default.Description),
            Triple("audio_mixer", "میکس صدا", Icons.Default.Tune),
            Triple("jalali_archive", "آرشیو", Icons.Default.CalendarToday)
        )

        items.forEach { (route, label, icon) ->
            val isSelected = currentDestination == route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateTo(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Turquoise else SecondaryText
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Turquoise else SecondaryText,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = DarkNavyCard
                )
            )
        }
    }
}
