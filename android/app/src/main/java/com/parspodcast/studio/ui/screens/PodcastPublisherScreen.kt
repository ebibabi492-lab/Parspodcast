package com.parspodcast.studio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parspodcast.studio.data.audio.PublishingAudioFormat
import com.parspodcast.studio.data.local.entities.HostingConfigEntity
import com.parspodcast.studio.data.local.entities.PodcastEpisodeEntity
import com.parspodcast.studio.data.local.entities.PodcastShowEntity
import com.parspodcast.studio.data.local.entities.ProductionProjectEntity
import com.parspodcast.studio.domain.calendar.JalaliCalendar
import com.parspodcast.studio.ui.components.*
import com.parspodcast.studio.ui.screens.publisher.PodcastPublisherViewModel
import com.parspodcast.studio.ui.screens.publisher.PublishingUiState
import com.parspodcast.studio.ui.theme.*
import java.io.File

@Composable
fun PodcastPublisherScreen(
    currentEpisodeTitle: String = "اپیزود جدید پارس‌پادکست",
    viewModel: PodcastPublisherViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // State collections from ViewModel (or fallbacks for preview)
    val shows by (viewModel?.shows?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val selectedShow by (viewModel?.selectedShow?.collectAsState() ?: remember {
        mutableStateOf<PodcastShowEntity?>(
            PodcastShowEntity(
                id = "default_show",
                title = "پارس‌پادکست",
                subtitle = "فناوری، هوش مصنوعی و جامعه",
                description = "پادکست تخصصی تحلیل اخبار و رویدادهای فناوری با پردازش صوت روی دستگاه و مدل محلی گما",
                author = "استودیوی پارس‌پادکست",
                publicFeedUrl = "https://parspodcast.ir/feed.xml"
            )
        )
    })
    val episodes by (viewModel?.episodes?.collectAsState() ?: remember {
        mutableStateOf(
            listOf(
                PodcastEpisodeEntity(
                    id = "ep_1",
                    projectId = "proj_1",
                    title = "اپیزود ۱: استقلال پردازش هوش مصنوعی محلی در پادکست‌های فارسی",
                    descriptionFa = "بررسی اجرای مدل‌های خانواده Gemma 4 و معماری LiteRT بدون اتصال به اینترنت",
                    jalaliPubDate = "۲۱ آبان ۱۴۰۵",
                    durationSeconds = 534,
                    publishingStatus = "PUBLISHED",
                    enclosureUrl = "https://archive.parspodcast.ir/audio/ep1.wav",
                    audioMimeType = "audio/wav",
                    audioFileSizeBytes = 24589210L
                )
            )
        )
    })
    val completedProjects by (viewModel?.completedProjects?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val hostingConfigs by (viewModel?.hostingConfigs?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val generatedRssXml by (viewModel?.generatedRssXml?.collectAsState() ?: remember { mutableStateOf("") })
    val validationReport by (viewModel?.validationReport?.collectAsState() ?: remember { mutableStateOf(null) })
    val publishingState by (viewModel?.publishingState?.collectAsState() ?: remember { mutableStateOf(PublishingUiState.Idle) })
    val verificationSummary by (viewModel?.verificationSummary?.collectAsState() ?: remember { mutableStateOf(null) })
    val isVerifyingFeed by (viewModel?.isVerifyingFeed?.collectAsState() ?: remember { mutableStateOf(false) })
    val exportSummary by (viewModel?.exportPackageSummary?.collectAsState() ?: remember { mutableStateOf(null) })
    val uiMessage by (viewModel?.uiMessage?.collectAsState() ?: remember { mutableStateOf(null) })

    // Active sub-tab
    var activeTab by remember { mutableStateOf(0) } // 0: Episodes, 1: RSS Preview, 2: Validator, 3: Remote Verifier

    // Dialog flags
    var showEditShowDialog by remember { mutableStateOf(false) }
    var showCreateShowDialog by remember { mutableStateOf(false) }
    var showNewEpisodeDialog by remember { mutableStateOf(false) }
    var showEditEpisodeDialog by remember { mutableStateOf<PodcastEpisodeEntity?>(null) }
    var showPrepareAudioDialog by remember { mutableStateOf<PodcastEpisodeEntity?>(null) }
    var showHostingDialog by remember { mutableStateOf(false) }
    var showLocalExportDialog by remember { mutableStateOf(false) }
    var showConfirmPublishDialog by remember { mutableStateOf<PodcastEpisodeEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<PodcastEpisodeEntity?>(null) }

    // Artwork picker launcher
    val artworkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && selectedShow != null) {
            viewModel?.processShowArtwork(uri, selectedShow!!)
        }
    }

    // Snackbar / Toast for UI messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel?.clearUiMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepNavy
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header & Show Selector Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
                    ),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مرکز انتشار و فید استاندارد پادکست",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PrimaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تولید استاندارد RSS 2.0، تگ‌های iTunes، صادرات محلی و انتشار سرور خودمیزبان",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Turquoise,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { showHostingDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = "تنظیمات هاست", tint = Cyan)
                                }
                                IconButton(
                                    onClick = { showLocalExportDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.FolderZip, contentDescription = "بسته محلی", tint = Turquoise)
                                }
                                IconButton(
                                    onClick = { showCreateShowDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "پادکست جدید", tint = PrimaryText)
                                }
                            }
                        }

                        Divider(color = DarkNavyBorder.copy(alpha = 0.5f))

                        // Show Details Row
                        if (selectedShow != null) {
                            val show = selectedShow!!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Artwork Thumbnail / Picker Box
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .background(DarkNavyCard, RoundedCornerShape(10.dp))
                                        .border(1.dp, Turquoise.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .clickable { artworkPickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!show.artworkLocalPath.isNullOrBlank()) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = "کاور",
                                            tint = Turquoise,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.AddPhotoAlternate,
                                                contentDescription = "انتخاب کاور",
                                                tint = SecondaryText,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text("کاور", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = show.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = PrimaryText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        StatusIndicator(
                                            label = "${episodes.size} اپیزود",
                                            color = Turquoise
                                        )
                                    }

                                    Text(
                                        text = show.subtitle.ifBlank { show.description },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (!show.publicFeedUrl.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = show.publicFeedUrl!!,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Cyan,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            IconButton(
                                                onClick = {
                                                    clipboard.setText(AnnotatedString(show.publicFeedUrl!!))
                                                    viewModel?.clearUiMessage()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "کپی فید", tint = Cyan, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }

                                IconButton(onClick = { showEditShowDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "ویرایش مشخصات", tint = PrimaryText)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Navigation Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabPill(
                        text = "اپیزودها و انتشار",
                        isSelected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        text = "پیش‌نمایش RSS",
                        isSelected = activeTab == 1,
                        onClick = {
                            activeTab = 1
                            selectedShow?.let { viewModel?.generateRssPreview(it) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        text = "اعتبارسنجی فید",
                        isSelected = activeTab == 2,
                        onClick = {
                            activeTab = 2
                            selectedShow?.let { viewModel?.generateRssPreview(it) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        text = "بررسی سرور",
                        isSelected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Tab Contents
            when (activeTab) {
                0 -> {
                    // TAB 0: EPISODES & PUBLISHING
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "فهرست اپیزودهای پادکست",
                                style = MaterialTheme.typography.titleSmall,
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold
                            )

                            PrimaryActionButton(
                                text = "اپیزود جدید از استودیوی میکس",
                                onClick = { showNewEpisodeDialog = true },
                                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = DeepNavy) }
                            )
                        }
                    }

                    if (episodes.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "هنوز هیچ اپیزودی اضافه نشده است",
                                description = "برای شروع، یک پروژه صوتی میکس شده از استودیو (Phase 8) را به عنوان اپیزود جدید وارد نمایید."
                            )
                        }
                    } else {
                        items(episodes, key = { it.id }) { ep ->
                            EpisodePublishingCard(
                                episode = ep,
                                onEditMetadata = { showEditEpisodeDialog = ep },
                                onPrepareAudio = { showPrepareAudioDialog = ep },
                                onPublish = { showConfirmPublishDialog = ep },
                                onDelete = { showDeleteConfirmDialog = ep },
                                onCopyUrl = {
                                    val u = ep.publicAudioUrl ?: ep.enclosureUrl
                                    if (u.isNotBlank()) clipboard.setText(AnnotatedString(u))
                                }
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: RSS 2.0 XML PREVIEW
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "کد منبع XML استاندارد RSS 2.0",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Turquoise,
                                        fontWeight = FontWeight.Bold
                                    )

                                    IconButton(
                                        onClick = {
                                            if (generatedRssXml.isNotBlank()) {
                                                clipboard.setText(AnnotatedString(generatedRssXml))
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "کپی XML", tint = Cyan)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp, max = 450.dp)
                                        .background(DarkNavyCard, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    LazyColumn {
                                        item {
                                            Text(
                                                text = generatedRssXml.ifBlank { "در حال تولید پیش‌نمایش فید..." },
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    lineHeight = 16.sp
                                                ),
                                                color = PrimaryText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: LIVE FEED VALIDATOR
                    item {
                        val report = validationReport
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "گزارش موتور اعتبارسنجی فید (Podcast Validator)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = PrimaryText,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (report != null) {
                                        StatusIndicator(
                                            label = if (report.isValid) "معتبر برای انتشار" else "دارای خطای ساختاری",
                                            color = if (report.isValid) Turquoise else Color(0xFFEF4444)
                                        )
                                    }
                                }

                                if (report == null) {
                                    Text("در حال اعتبارسنجی...", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                                } else {
                                    Text(
                                        text = "تعداد کل اپیزودهای فید: ${report.totalEpisodes} | خطاها: ${report.errors.size} | هشدارها: ${report.warnings.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Cyan
                                    )

                                    if (report.errors.isNotEmpty()) {
                                        Text(
                                            text = "خطاهای مانع انتشار (Errors):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFEF4444),
                                            fontWeight = FontWeight.Bold
                                        )
                                        report.errors.forEach { err ->
                                            Text(
                                                text = "• [${err.field}]: ${err.messageFa}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFFCA5A5)
                                            )
                                        }
                                    }

                                    if (report.warnings.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "هشدارهای توصیه‌ای (Warnings):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFF59E0B),
                                            fontWeight = FontWeight.Bold
                                        )
                                        report.warnings.forEach { warn ->
                                            Text(
                                                text = "• [${warn.field}]: ${warn.messageFa}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFFDE68A)
                                            )
                                        }
                                    }

                                    if (report.isValid && report.warnings.isEmpty()) {
                                        Text(
                                            text = "فید کاملاً معتبر است و با استانداردهای رسمی Apple Podcasts، Spotify و RSS 2.0 همخوانی کامل دارد.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Turquoise
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: REMOTE VERIFIER
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
                                    text = "بررسی دسترسی عمومی به فید و فایل‌های صوتی آنلاین",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PrimaryText,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "این آزمون نشانی مستقیم فید RSS و لینک‌های Enclosure فایل‌های صوتی را از طریق شبکه بررسی کرده و صحت Content-Type و استریم را بدون دانلود کل فایل آزمایش می‌کند.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )

                                PrimaryActionButton(
                                    text = if (isVerifyingFeed) "در حال بررسی آنلاین..." else "بررسی اکنون نشانی فید",
                                    onClick = {
                                        selectedShow?.publicFeedUrl?.let { url ->
                                            viewModel?.verifyPublicFeed(url)
                                        }
                                    },
                                    enabled = !isVerifyingFeed && !selectedShow?.publicFeedUrl.isNullOrBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = { Icon(Icons.Default.Language, contentDescription = null, tint = DeepNavy) }
                                )

                                val vSummary = verificationSummary
                                if (vSummary != null) {
                                    Divider(color = DarkNavyBorder.copy(alpha = 0.5f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "وضعیت فید: کد HTTP ${vSummary.feedHttpCode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (vSummary.isFeedAccessible) Turquoise else Color(0xFFEF4444)
                                        )

                                        StatusIndicator(
                                            label = if (vSummary.isFullyVerified) "تأیید کامل عمومی" else "عدم دسترسی به برخی منابع",
                                            color = if (vSummary.isFullyVerified) Turquoise else Color(0xFFF59E0B)
                                        )
                                    }

                                    if (vSummary.enclosureDetails.isNotEmpty()) {
                                        Text(
                                            text = "فایل‌های صوتی اپیزودها (${vSummary.enclosureDetails.size}):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        vSummary.enclosureDetails.forEach { enc ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = enc.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = PrimaryText
                                                )
                                                Text(
                                                    text = if (enc.isAccessible) "قابل دسترس (${enc.remoteContentType})" else "ناموفق",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (enc.isAccessible) Turquoise else Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // Create / Edit Show Dialog
    if (showEditShowDialog || showCreateShowDialog) {
        val editingShow = if (showEditShowDialog) selectedShow else null
        ShowMetadataDialog(
            initialShow = editingShow,
            onDismiss = {
                showEditShowDialog = false
                showCreateShowDialog = false
            },
            onSave = { updatedShow ->
                viewModel?.saveShow(updatedShow)
                showEditShowDialog = false
                showCreateShowDialog = false
            }
        )
    }

    // Create Episode from Project Dialog
    if (showNewEpisodeDialog && selectedShow != null) {
        CreateEpisodeFromProjectDialog(
            projects = completedProjects,
            onDismiss = { showNewEpisodeDialog = false },
            onSelect = { proj ->
                viewModel?.createEpisodeFromProject(proj, selectedShow!!.id)
                showNewEpisodeDialog = false
            }
        )
    }

    // Edit Episode Metadata Dialog
    if (showEditEpisodeDialog != null) {
        val ep = showEditEpisodeDialog!!
        EditEpisodeMetadataDialog(
            episode = ep,
            onDismiss = { showEditEpisodeDialog = null },
            onSave = { updated ->
                viewModel?.updateEpisode(updated)
                showEditEpisodeDialog = null
            }
        )
    }

    // Prepare Audio Dialog
    if (showPrepareAudioDialog != null) {
        val ep = showPrepareAudioDialog!!
        PrepareAudioDialog(
            episode = ep,
            onDismiss = { showPrepareAudioDialog = null },
            onPrepare = { format, file ->
                viewModel?.prepareAudioForEpisode(ep, format, file)
                showPrepareAudioDialog = null
            }
        )
    }

    // WebDAV Hosting Settings Dialog
    if (showHostingDialog) {
        HostingSettingsDialog(
            currentConfig = hostingConfigs.firstOrNull(),
            onDismiss = { showHostingDialog = false },
            onSave = { name, sUrl, rBase, pBase, user, pass ->
                viewModel?.saveHostingConfiguration(name, sUrl, rBase, pBase, user, pass)
                showHostingDialog = false
            },
            onTest = { sUrl, rBase, pBase, user, pass ->
                viewModel?.testWebDavConnection(sUrl, rBase, pBase, user, pass) ?: Result.success(true)
            }
        )
    }

    // Local Export Package Dialog
    if (showLocalExportDialog && selectedShow != null) {
        LocalExportPackageDialog(
            show = selectedShow!!,
            onDismiss = { showLocalExportDialog = false },
            onExport = { targetDir, baseUrl ->
                viewModel?.exportLocalPublicationPackage(selectedShow!!, targetDir, baseUrl)
                showLocalExportDialog = false
            }
        )
    }

    // Confirm Publishing Dialog
    if (showConfirmPublishDialog != null && selectedShow != null) {
        val ep = showConfirmPublishDialog!!
        val config = hostingConfigs.firstOrNull()

        AlertDialog(
            onDismissRequest = { showConfirmPublishDialog = null },
            title = {
                Text(
                    text = "تأیید انتشار رسمی در سرور",
                    style = MaterialTheme.typography.titleMedium,
                    color = Turquoise,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "آیا از انتشار اپیزود «${ep.title}» در سرور میزبانی اطمینان دارید؟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryText
                    )
                    Text(
                        text = "مراحل اجرا: ۱. بارگذاری فایل صوتی ۲. اعتبارسنجی دسترسی آنلاین ۳. به‌روزرسانی فید podcast.xml در سرور ۴. بررسی نهایی نشانی‌ها.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                    if (config == null) {
                        Text(
                            text = "توجه: هنوز سرور میزبانی WebDAV تنظیم نشده است. ابتدا از آیکون ابر سرور را پیکربندی نمایید.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (config != null) {
                            viewModel?.publishEpisodeViaWorkManager(ep, selectedShow!!, config)
                        }
                        showConfirmPublishDialog = null
                    },
                    enabled = config != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                ) {
                    Text("آغاز انتشار پس‌زمینه", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPublishDialog = null }) {
                    Text("انصراف", color = SecondaryText)
                }
            },
            containerColor = SecondaryNavy,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Episode Confirm Dialog
    if (showDeleteConfirmDialog != null) {
        val ep = showDeleteConfirmDialog!!
        ConfirmationDialog(
            title = "حذف اپیزود پادکست",
            message = "آیا از حذف اپیزود «${ep.title}» اطمینان دارید؟ فایل‌های صوتی ضبط شده در استودیو باقی مانده و تنها رکورد انتشار حذف خواهد شد.",
            confirmText = "حذف قطعی",
            dismissText = "انصراف",
            onConfirm = {
                viewModel?.deleteEpisode(ep)
                showDeleteConfirmDialog = null
            },
            onDismiss = { showDeleteConfirmDialog = null }
        )
    }

    // Publishing Progress Modal / Status
    if (publishingState is PublishingUiState.InProgress) {
        val inProg = publishingState as PublishingUiState.InProgress
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "فرآیند انتشار در جریان است",
                    style = MaterialTheme.typography.titleSmall,
                    color = Turquoise,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = inProg.stepTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryText
                    )
                    LinearProgressIndicator(
                        progress = { inProg.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Turquoise,
                        trackColor = DarkNavyCard
                    )
                    Text(
                        text = "${inProg.percent}٪ تکمیل شده",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            },
            confirmButton = {},
            containerColor = SecondaryNavy,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

// --- SUB-COMPONENTS & DIALOGS ---

@Composable
fun TabPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Turquoise,
            selectedLabelColor = DeepNavy,
            containerColor = SecondaryNavy,
            labelColor = PrimaryText
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    )
}

@Composable
fun EpisodePublishingCard(
    episode: PodcastEpisodeEntity,
    onEditMetadata: () -> Unit,
    onPrepareAudio: () -> Unit,
    onPublish: () -> Unit,
    onDelete: () -> Unit,
    onCopyUrl: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkNavyBorder)
        ),
        modifier = modifier.fillMaxWidth()
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "اپیزود ${episode.episodeNumber}: ${episode.title}",
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "تاریخ انتشار: ${episode.jalaliPubDate} | مدت: ${episode.durationSeconds} ثانیه",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                StatusBadge(status = episode.publishingStatus)
            }

            Text(
                text = episode.descriptionFa,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Audio & Enclosure info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                val sizeMb = "%.1f".format(episode.audioFileSizeBytes / (1024f * 1024f))
                Text(
                    text = "فرمت: ${episode.audioMimeType} ($sizeMb MB)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan
                )

                if (episode.enclosureUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onCopyUrl, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Link, contentDescription = "کپی نشانی فایل", tint = Turquoise, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Divider(color = DarkNavyBorder.copy(alpha = 0.5f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryActionButton(
                    text = "آماده‌سازی صوت",
                    onClick = onPrepareAudio,
                    modifier = Modifier.weight(1f)
                )

                SecondaryActionButton(
                    text = "ویرایش متادیتا",
                    onClick = onEditMetadata,
                    modifier = Modifier.weight(1f)
                )

                PrimaryActionButton(
                    text = "انتشار",
                    onClick = onPublish,
                    enabled = episode.localAudioPath.isNotBlank() && File(episode.localAudioPath).exists(),
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "PUBLISHED" -> "منتشر شده" to Turquoise
        "READY" -> "آماده انتشار" to Cyan
        "UPLOADING" -> "در حال بارگذاری" to Color(0xFF38BDF8)
        "UPDATE_PENDING" -> "نیازمند به‌روزرسانی" to Color(0xFFF59E0B)
        "FAILED" -> "خطا در انتشار" to Color(0xFFEF4444)
        else -> "پیش‌نویس" to SecondaryText
    }
    StatusIndicator(label = label, color = color)
}

@Composable
fun ShowMetadataDialog(
    initialShow: PodcastShowEntity?,
    onDismiss: () -> Unit,
    onSave: (PodcastShowEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialShow?.title ?: "پارس‌پادکست") }
    var subtitle by remember { mutableStateOf(initialShow?.subtitle ?: "فناوری و هوش مصنوعی") }
    var description by remember { mutableStateOf(initialShow?.description ?: "تحلیل تخصصی رویدادهای فناوری") }
    var author by remember { mutableStateOf(initialShow?.author ?: "استودیوی پارس‌پادکست") }
    var ownerName by remember { mutableStateOf(initialShow?.ownerName ?: "پارس‌پادکست") }
    var ownerEmail by remember { mutableStateOf(initialShow?.ownerEmail ?: "studio@parspodcast.ir") }
    var websiteUrl by remember { mutableStateOf(initialShow?.websiteUrl ?: "https://parspodcast.ir") }
    var category by remember { mutableStateOf(initialShow?.category ?: "News") }
    var subcategory by remember { mutableStateOf(initialShow?.subcategory ?: "Tech News") }
    var copyright by remember { mutableStateOf(initialShow?.copyright ?: "تمامی حقوق محفوظ است © ۱۴۰۵") }
    var explicitContent by remember { mutableStateOf(initialShow?.explicitContent ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialShow == null) "ایجاد پادکست جدید" else "ویرایش مشخصات پادکست",
                style = MaterialTheme.typography.titleMedium,
                color = Turquoise,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PersianTextField(value = title, onValueChange = { title = it }, label = "عنوان پادکست")
                }
                item {
                    PersianTextField(value = subtitle, onValueChange = { subtitle = it }, label = "زیرعنوان")
                }
                item {
                    PersianTextField(value = description, onValueChange = { description = it }, label = "شرح کامل کانال", minLines = 2)
                }
                item {
                    PersianTextField(value = author, onValueChange = { author = it }, label = "نام پدیدآورنده (Author)")
                }
                item {
                    PersianTextField(value = ownerName, onValueChange = { ownerName = it }, label = "نام مالک (iTunes Owner)")
                }
                item {
                    PersianTextField(value = ownerEmail, onValueChange = { ownerEmail = it }, label = "ایمیل مالک")
                }
                item {
                    PersianTextField(value = websiteUrl, onValueChange = { websiteUrl = it }, label = "وبسایت رسمی")
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PersianTextField(value = category, onValueChange = { category = it }, label = "دسته اصلی", modifier = Modifier.weight(1f))
                        PersianTextField(value = subcategory, onValueChange = { subcategory = it }, label = "زیردسته", modifier = Modifier.weight(1f))
                    }
                }
                item {
                    PersianTextField(value = copyright, onValueChange = { copyright = it }, label = "متن کپی‌رایت")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val show = (initialShow ?: PodcastShowEntity(
                        id = "show_${java.util.UUID.randomUUID()}",
                        title = title,
                        description = description
                    )).copy(
                        title = title,
                        subtitle = subtitle,
                        description = description,
                        author = author,
                        ownerName = ownerName,
                        ownerEmail = ownerEmail,
                        websiteUrl = websiteUrl,
                        category = category,
                        subcategory = subcategory,
                        copyright = copyright,
                        explicitContent = explicitContent
                    )
                    onSave(show)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
            ) {
                Text("ذخیره مشخصات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = SecondaryText) }
        },
        containerColor = SecondaryNavy,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun CreateEpisodeFromProjectDialog(
    projects: List<ProductionProjectEntity>,
    onDismiss: () -> Unit,
    onSelect: (ProductionProjectEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "انتخاب پروژه صوتی استودیو",
                style = MaterialTheme.typography.titleMedium,
                color = Turquoise,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (projects.isEmpty()) {
                Text(
                    text = "هیچ پروژه میکس شده‌ای در استودیو یافت نشد. ابتدا یک پروژه در استودیوی میکس تولید نمایید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(projects) { p ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(p) },
                            colors = CardDefaults.cardColors(containerColor = DarkNavyCard)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(p.title, style = MaterialTheme.typography.titleSmall, color = PrimaryText, fontWeight = FontWeight.Bold)
                                Text(p.description, style = MaterialTheme.typography.bodySmall, color = SecondaryText, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = SecondaryText) } },
        containerColor = SecondaryNavy,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun EditEpisodeMetadataDialog(
    episode: PodcastEpisodeEntity,
    onDismiss: () -> Unit,
    onSave: (PodcastEpisodeEntity) -> Unit
) {
    var title by remember { mutableStateOf(episode.title) }
    var subtitle by remember { mutableStateOf(episode.subtitle) }
    var description by remember { mutableStateOf(episode.descriptionFa) }
    var epNumber by remember { mutableStateOf(episode.episodeNumber.toString()) }
    var seasonNumber by remember { mutableStateOf(episode.seasonNumber.toString()) }
    var epType by remember { mutableStateOf(episode.episodeType) }
    var keywords by remember { mutableStateOf(episode.keywords) }
    var isExplicit by remember { mutableStateOf(episode.explicitContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش متادیتای اپیزود", style = MaterialTheme.typography.titleMedium, color = Turquoise, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { PersianTextField(value = title, onValueChange = { title = it }, label = "عنوان اپیزود") }
                item { PersianTextField(value = subtitle, onValueChange = { subtitle = it }, label = "زیرعنوان اپیزود") }
                item { PersianTextField(value = description, onValueChange = { description = it }, label = "توضیحات و شرح", minLines = 2) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PersianTextField(value = epNumber, onValueChange = { epNumber = it }, label = "شماره اپیزود", modifier = Modifier.weight(1f))
                        PersianTextField(value = seasonNumber, onValueChange = { seasonNumber = it }, label = "فصل", modifier = Modifier.weight(1f))
                    }
                }
                item { PersianTextField(value = keywords, onValueChange = { keywords = it }, label = "کلمات کلیدی (جدا شده با کاما)") }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        episode.copy(
                            title = title,
                            subtitle = subtitle,
                            descriptionFa = description,
                            episodeNumber = epNumber.toIntOrNull() ?: 1,
                            seasonNumber = seasonNumber.toIntOrNull() ?: 1,
                            episodeType = epType,
                            keywords = keywords,
                            explicitContent = isExplicit
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
            ) {
                Text("ذخیره متادیتا", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = SecondaryText) } },
        containerColor = SecondaryNavy,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun PrepareAudioDialog(
    episode: PodcastEpisodeEntity,
    onDismiss: () -> Unit,
    onPrepare: (PublishingAudioFormat, File) -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(PublishingAudioFormat.WAV) }

    // Look for master mix wav
    val defaultProjectAudioPath = "${context.filesDir}/audio/projects/${episode.projectId ?: "p1"}/master_mix.wav"
    val fileCandidate = File(if (episode.localAudioPath.isNotBlank()) episode.localAudioPath else defaultProjectAudioPath)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("آماده‌سازی فایل صوتی انتشار", style = MaterialTheme.typography.titleMedium, color = Turquoise, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("فرمت مورد نظر جهت خروجی و انکلوژر RSS را انتخاب نمایید:", style = MaterialTheme.typography.bodySmall, color = PrimaryText)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedFormat = PublishingAudioFormat.WAV },
                    colors = CardDefaults.cardColors(containerColor = if (selectedFormat == PublishingAudioFormat.WAV) DarkNavyCard else SecondaryNavy),
                    border = if (selectedFormat == PublishingAudioFormat.WAV) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Turquoise)) else null
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(PublishingAudioFormat.WAV.displayName, style = MaterialTheme.typography.bodySmall, color = PrimaryText, fontWeight = FontWeight.Bold)
                        Text("بالاترین کیفیت استودیویی، بدون اتلاف کیفیت (Lossless)، سازگار با آرشیوهای صوتی حرفه‌ای", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedFormat = PublishingAudioFormat.M4A_AAC },
                    colors = CardDefaults.cardColors(containerColor = if (selectedFormat == PublishingAudioFormat.M4A_AAC) DarkNavyCard else SecondaryNavy),
                    border = if (selectedFormat == PublishingAudioFormat.M4A_AAC) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Turquoise)) else null
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(PublishingAudioFormat.M4A_AAC.displayName, style = MaterialTheme.typography.bodySmall, color = PrimaryText, fontWeight = FontWeight.Bold)
                        Text("حجم کم (حدود یک‌دهم WAV)، استریم سریع، سازگاری کامل با پادکست‌گیرهای همراه", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPrepare(selectedFormat, fileCandidate) },
                colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
            ) {
                Text("شروع پردازش و خروجی", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = SecondaryText) } },
        containerColor = SecondaryNavy,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun HostingSettingsDialog(
    currentConfig: HostingConfigEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit,
    onTest: suspend (String, String, String, String, String) -> Result<Boolean>
) {
    var name by remember { mutableStateOf(currentConfig?.name ?: "سرور ابری WebDAV") }
    var serverUrl by remember { mutableStateOf(currentConfig?.serverUrl ?: "https://dav.parspodcast.ir/podcasts/") }
    var remoteBasePath by remember { mutableStateOf(currentConfig?.remoteBasePath ?: "/podcasts/parspodcast/") }
    var publicBaseUrl by remember { mutableStateOf(currentConfig?.publicBaseUrl ?: "https://media.parspodcast.ir/podcasts/parspodcast/") }
    var username by remember { mutableStateOf(currentConfig?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنظیمات سرور میزبانی WebDAV (HTTPS)", style = MaterialTheme.typography.titleMedium, color = Turquoise, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { PersianTextField(value = name, onValueChange = { name = it }, label = "عنوان سرور") }
                item { PersianTextField(value = serverUrl, onValueChange = { serverUrl = it }, label = "نشانی سرور WebDAV (الزاما https://)") }
                item { PersianTextField(value = remoteBasePath, onValueChange = { remoteBasePath = it }, label = "شاخه ذخیره‌سازی در سرور") }
                item { PersianTextField(value = publicBaseUrl, onValueChange = { publicBaseUrl = it }, label = "نشانی اینترنتی دسترسی عمومی (Public Base URL)") }
                item { PersianTextField(value = username, onValueChange = { username = it }, label = "نام کاربری") }
                item { PersianTextField(value = password, onValueChange = { password = it }, label = "رمز عبور (رمزنگاری امن در Keystore)") }

                if (testResult != null) {
                    item {
                        Text(testResult!!, style = MaterialTheme.typography.labelSmall, color = if (testResult!!.contains("موفق")) Turquoise else Color(0xFFEF4444))
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            isTesting = true
                            testResult = "در حال اتصال به سرور..."
                            val res = onTest(serverUrl, remoteBasePath, publicBaseUrl, username, password)
                            testResult = if (res.isSuccess) "اتصال با موفقیت برقرار شد!" else "خطا: ${res.exceptionOrNull()?.message}"
                            isTesting = false
                        }
                    },
                    enabled = !isTesting && serverUrl.isNotBlank()
                ) {
                    Text("تست اتصال", color = Cyan)
                }

                Button(
                    onClick = {
                        onSave(name, serverUrl, remoteBasePath, publicBaseUrl, username, password)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
                ) {
                    Text("ذخیره تنظیمات", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = SecondaryText) } },
        containerColor = SecondaryNavy,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun LocalExportPackageDialog(
    show: PodcastShowEntity,
    onDismiss: () -> Unit,
    onExport: (File, String) -> Unit
) {
    val context = LocalContext.current
    var baseUrlPlaceholder by remember { mutableStateOf("https://your-domain.ir/podcast/") }
    val defaultTargetDir = File(context.filesDir, "podcasts/exports/${show.id}")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("صادرات بسته آفلاین پادکست", style = MaterialTheme.typography.titleMedium, color = Turquoise, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "این بسته شامل podcast.xml، فایل‌های صوتی اپیزودها، تصویر کاور و راهنمای فارسی انتشار است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryText
                )

                PersianTextField(
                    value = baseUrlPlaceholder,
                    onValueChange = { baseUrlPlaceholder = it },
                    label = "دامنه یا نشانی پایه هاست شما (Base URL)"
                )

                Text(
                    text = "مسیر ذخیره روی دستگاه:\n${defaultTargetDir.absolutePath}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(defaultTargetDir, baseUrlPlaceholder) },
                colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavy)
            ) {
                Text("تولید بسته آفلاین", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = SecondaryText) } },
        containerColor = SecondaryNavy,
        shape = RoundedCornerShape(16.dp)
    )
}
