package com.parspodcast.studio.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.parspodcast.studio.data.local.entities.NewsArticleEntity
import com.parspodcast.studio.data.local.entities.NewsSourceEntity
import com.parspodcast.studio.data.local.entities.PodcastEpisodeEntity
import com.parspodcast.studio.data.repository.ModelStatus
import com.parspodcast.studio.data.repository.UserSettings
import com.parspodcast.studio.ui.components.ParsPodcastBottomNavigation
import com.parspodcast.studio.ui.components.ParsPodcastTopBar
import com.parspodcast.studio.ui.screens.*
import com.parspodcast.studio.ui.theme.DeepNavy

@Composable
fun ParsPodcastMainScaffold(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoutes.DASHBOARD

    // Sample/Seeded Data for UI demonstration
    var episodes by remember {
        mutableStateOf(
            listOf(
                PodcastEpisodeEntity(
                    id = "ep_42",
                    projectId = "proj_1",
                    title = "اپیزود ۴۲: استقلال پردازش هوش مصنوعی محلی در پادکست‌های فارسی",
                    descriptionFa = "بررسی اجرای مدل‌های خانواده Gemma 4 و معماری LiteRT بدون اتصال به اینترنت",
                    jalaliPubDate = "۲۱ آبان ۱۴۰۵",
                    pubDateRfc822 = "Tue, 10 Nov 2026 21:00:00 +0330",
                    enclosureUrl = "https://archive.parspodcast.ir/audio/ep42.mp3",
                    fileSizeBytes = 24589210L,
                    durationSeconds = 534,
                    publishingStatus = "PUBLISHED"
                ),
                PodcastEpisodeEntity(
                    id = "ep_41",
                    projectId = "proj_2",
                    title = "اپیزود ۴۱: روندهای سرمایه‌گذاری بر روی پادکست‌های تخصصی",
                    descriptionFa = "تحلیل داده‌های آماری رشد شنوندگان محتوای صوتی در خاورمیانه",
                    jalaliPubDate = "۱۴ آبان ۱۴۰۵",
                    pubDateRfc822 = "Tue, 03 Nov 2026 21:00:00 +0330",
                    enclosureUrl = "https://archive.parspodcast.ir/audio/ep41.mp3",
                    fileSizeBytes = 21940120L,
                    durationSeconds = 480,
                    publishingStatus = "PUBLISHED"
                )
            )
        )
    }

    var selectedNewsIds by remember { mutableStateOf(setOf<String>()) }

    val sampleArticles = remember {
        listOf(
            NewsArticleEntity(
                id = "art_1",
                sourceId = "دیجیاتو",
                title = "رونمایی از نسل نوین پردازنده‌های هوش مصنوعی موبایل با شتاب‌دهنده محلی",
                content = "شرکت‌های فناوری نسل جدیدی از شتاب‌دهنده‌های لبه‌ای را معرفی کردند که قادر به اجرای مدل‌های چندمیلیاردی بدون نیاز به اتصال شبکه است.",
                originalUrl = "https://digiato.com/offline-ai",
                publishedAtUtc = System.currentTimeMillis() - 3600000,
                rawSummary = "توسعه تراشه‌های هوش مصنوعی آفلاین برای اجرای مدل‌های زبانی بر روی دستگاه‌های هوشمند.",
                deduplicationHash = "hash_1"
            ),
            NewsArticleEntity(
                id = "art_2",
                sourceId = "زومیت",
                title = "معماری LiteRT و بهینه‌سازی استنتاج مدل‌های خانواده Gemma روی اندروید",
                content = "کتابخانه LiteRT-LM امکان استنتاج موازی مدل‌های E2B و E4B را با مصرف حافظه رم کمتر از ۲.۵ گیگابایت فراهم کرده است.",
                originalUrl = "https://zoomit.ir/litert-gemma",
                publishedAtUtc = System.currentTimeMillis() - 7200000,
                rawSummary = "آزمایش‌های بنچمارک روی گوشی‌های همراه حاکی از راندمان بالای مدل‌های سبک روی پلتفرم اندروید است.",
                deduplicationHash = "hash_2"
            ),
            NewsArticleEntity(
                id = "art_3",
                sourceId = "ایسنا",
                title = "پیشرفت پژوهشگران ایرانی در توسعه مدل‌های گفتار عصبی متن‌باز زبان فارسی",
                content = "پژوهشگران با بهینه‌سازی معماری پایپر، موفق به تولید صداهای طبیعی با تلفظ دقیق اعراب شدند.",
                originalUrl = "https://isna.ir/persian-tts",
                publishedAtUtc = System.currentTimeMillis() - 10800000,
                rawSummary = "مدل مانای فارسی به عنوان یکی از سبک‌ترین و باکیفیت‌ترین مدل‌های TTS آفلاین معرفی شد.",
                deduplicationHash = "hash_3"
            )
        )
    }

    val sampleSources = remember {
        listOf(
            NewsSourceEntity("src_1", "دیجیاتو", "https://digiato.com/feed", "RSS", "فناوری"),
            NewsSourceEntity("src_2", "زومیت", "https://zoomit.ir/feed", "ATOM", "فناوری"),
            NewsSourceEntity("src_3", "ایسنا", "https://isna.ir/feed", "RSS", "علم و دانش")
        )
    }

    val modelStatuses = remember {
        listOf(
            ModelStatus("gemma-4-e4b-instruct", "Gemma 4 E4B Instruct", "4.0-E4B", "LLM (متنی)", 4600000000L, 4096, false, null, false),
            ModelStatus("gemma-4-e2b-instruct", "Gemma 4 E2B Instruct", "4.0-E2B", "LLM (متنی سبک)", 2400000000L, 2500, false, null, false),
            ModelStatus("fa-ir-mana-piper", "Mana Persian Piper", "fa_IR-mana-medium", "TTS (صوت فارسی)", 125000000L, 512, false, null, false)
        )
    }

    var userSettings by remember { mutableStateOf(UserSettings()) }

    val screenTitle = when (currentRoute) {
        NavRoutes.DASHBOARD -> "پیشخوان استودیو"
        NavRoutes.NEWS_CENTER -> "مرکز خبر و هوشمندی"
        NavRoutes.SCRIPT_STUDIO -> "استودیو فیلم‌نامه"
        NavRoutes.VOICE_STUDIO -> "استودیو صدا و گویندگان"
        NavRoutes.AUDIO_MIXER -> "میکس چندترکه صدا"
        NavRoutes.JALALI_ARCHIVE -> "آرشیو تقویم جلالی"
        NavRoutes.PUBLISHER -> "ناشر پادکست و RSS"
        NavRoutes.MODEL_MANAGER -> "مدیریت مدل‌های محلی"
        NavRoutes.SETTINGS -> "تنظیمات برنامه"
        else -> "استودیو"
    }

    val navigateTo: (String) -> Unit = { route ->
        if (route != currentRoute) {
            navController.navigate(route) {
                popUpTo(NavRoutes.DASHBOARD) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ParsPodcastTopBar(
                title = screenTitle,
                currentDestination = currentRoute,
                onNavigateTo = navigateTo
            )
        },
        bottomBar = {
            ParsPodcastBottomNavigation(
                currentDestination = currentRoute,
                onNavigateTo = navigateTo
            )
        },
        containerColor = DeepNavy
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.DASHBOARD,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepNavy)
        ) {
            composable(NavRoutes.DASHBOARD) {
                DashboardScreen(
                    onNavigateTo = navigateTo,
                    episodes = episodes,
                    modelStatuses = modelStatuses,
                    newsCount = sampleArticles.size,
                    activeProjectTitle = "پادکست بررسی تحولات هوش مصنوعی"
                )
            }

            composable(NavRoutes.NEWS_CENTER) {
                NewsCenterScreen(
                    articles = sampleArticles,
                    sources = sampleSources,
                    selectedArticleIds = selectedNewsIds,
                    onToggleSelectArticle = { id ->
                        selectedNewsIds = if (selectedNewsIds.contains(id)) selectedNewsIds - id else selectedNewsIds + id
                    },
                    onNavigateToScriptStudio = { navigateTo(NavRoutes.SCRIPT_STUDIO) }
                )
            }

            composable(NavRoutes.SCRIPT_STUDIO) {
                var engineState by remember { mutableStateOf<EngineLoadingState>(EngineLoadingState.Uninitialized) }
                var isInstalled by remember { mutableStateOf(false) }
                var genState by remember { mutableStateOf<ScriptGenerationUiState>(ScriptGenerationUiState.Idle) }
                var genSettings by remember { mutableStateOf(GenerationSettings()) }
                var scriptSegments by remember {
                    mutableStateOf(
                        listOf(
                            ScriptSegmentEntity("seg_1", "script_1", 1, "INTRO", "PRESENTER_A", "به استودیوی پارس‌پادکست خوش آمدید. امروز با بررسی تازه‌ترین رویدادهای هوش مصنوعی محلی در خدمت شما هستیم.", 14, isApproved = true),
                            ScriptSegmentEntity("seg_2", "script_1", 2, "NEWS", "PRESENTER_A", "در سرخط اخبار: مدل Gemma 4 E4B بر روی پردازنده‌های موبایل با معماری LiteRT بدون نیاز به اینترنت اجرا می‌شود.", 22, isApproved = true),
                            ScriptSegmentEntity("seg_3", "script_1", 3, "DISCUSSION", "PRESENTER_B", "نکته مهم حفظ ۱۰۰٪ محرمانگی و عدم وابستگی به هیچ‌گونه سرور ابری است.", 18, isApproved = true),
                            ScriptSegmentEntity("seg_4", "script_1", 4, "OUTRO", "PRESENTER_A", "سپاس از همراهی شما. تا برنامه‌ای دیگر خدانگهدار.", 15, isApproved = true)
                        )
                    )
                }

                ScriptStudioScreen(
                    engineState = engineState,
                    isModelFileInstalled = isInstalled,
                    generationUiState = genState,
                    generationSettings = genSettings,
                    segments = scriptSegments,
                    selectedArticles = sampleArticles.filter { selectedNewsIds.contains(it.id) },
                    onLoadModel = {
                        engineState = EngineLoadingState.Ready("GPU (OpenCL / Vulkan)", "/data/user/0/com.parspodcast.studio/files/models/gemma-4-E4B-it.litertlm", 1950L)
                    },
                    onUnloadModel = {
                        engineState = EngineLoadingState.Uninitialized
                    },
                    onUpdateSettings = { genSettings = it },
                    onGenerateFullScript = { title, duration, isDual, tone ->
                        genState = ScriptGenerationUiState.Generating("در حال تدوین دیالوگ‌های فارسی بر پایه فکت‌های خبری...", 48, 1200L)
                    },
                    onCancelGeneration = {
                        genState = ScriptGenerationUiState.Idle
                    },
                    onUpdateSegmentText = { id, text ->
                        scriptSegments = scriptSegments.map { if (it.id == id) it.copy(textFa = text) else it }
                    },
                    onToggleSpeaker = { id ->
                        scriptSegments = scriptSegments.map {
                            if (it.id == id) it.copy(speakerRole = if (it.speakerRole == "PRESENTER_A") "PRESENTER_B" else "PRESENTER_A") else it
                        }
                    },
                    onRewriteSegment = { id, _ -> },
                    onSummarizeSegment = { id -> },
                    onExpandSegment = { id, _ -> },
                    onChangeToneSegment = { id, _ -> },
                    onSaveDraft = { },
                    onNavigateToModelManager = { navigateTo(NavRoutes.MODEL_MANAGER) },
                    onNavigateToVoiceStudio = { navigateTo(NavRoutes.VOICE_STUDIO) }
                )
            }

            composable(NavRoutes.VOICE_STUDIO) {
                VoiceStudioScreen(
                    onNavigateToMixer = { navigateTo(NavRoutes.AUDIO_MIXER) }
                )
            }

            composable(NavRoutes.AUDIO_MIXER) {
                AudioMixerScreen(
                    onNavigateToPublisher = { navigateTo(NavRoutes.PUBLISHER) }
                )
            }

            composable(NavRoutes.JALALI_ARCHIVE) {
                JalaliArchiveScreen(
                    episodes = episodes,
                    onDeleteEpisode = { ep -> episodes = episodes.filter { it.id != ep.id } },
                    onNavigateToEdit = { navigateTo(NavRoutes.SCRIPT_STUDIO) }
                )
            }

            composable(NavRoutes.PUBLISHER) {
                PodcastPublisherScreen(
                    currentEpisodeTitle = "اپیزود ۴۳: تحولات پردازش لبه‌ای در تولید محتوا"
                )
            }

            composable(NavRoutes.MODEL_MANAGER) {
                var isInstalled by remember { mutableStateOf(false) }
                var copyProgress by remember { mutableStateOf<Float?>(null) }
                var engineState by remember { mutableStateOf<EngineLoadingState>(EngineLoadingState.Uninitialized) }

                val capabilityReport = remember {
                    DeviceCapabilityReport(
                        androidVersion = "Android 15 (API 35)",
                        deviceModel = "Daria Band (5G)",
                        cpuAbi = "arm64-v8a",
                        totalRamMb = 8192L,
                        availableRamMb = 4850L,
                        totalStorageMb = 256000L,
                        availableStorageMb = 142000L,
                        isRamSufficientForE4B = true,
                        isStorageSufficientForE4B = true,
                        recommendedBackend = ModelExecutionBackend.GPU
                    )
                }

                ModelManagerScreen(
                    models = modelStatuses,
                    isModelInstalled = isInstalled,
                    engineLoadingState = engineState,
                    capabilityReport = capabilityReport,
                    copyProgress = copyProgress,
                    onImportModelUri = {
                        copyProgress = 0.05f
                        // Simulating chunked SAF copy progress
                        copyProgress = 1.0f
                        isInstalled = true
                        copyProgress = null
                    },
                    onLoadModel = { backend ->
                        engineState = EngineLoadingState.Ready(
                            backend = if (backend == ModelExecutionBackend.GPU) "GPU (OpenCL)" else "CPU (XNNPACK)",
                            modelPath = "/data/user/0/com.parspodcast.studio/files/models/gemma-4-E4B-it.litertlm",
                            ramUsageMb = 1950L
                        )
                    },
                    onUnloadModel = {
                        engineState = EngineLoadingState.Uninitialized
                    },
                    onDeleteModel = {
                        isInstalled = false
                        engineState = EngineLoadingState.Uninitialized
                    }
                )
            }

            composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                    settings = userSettings,
                    onUpdateSettings = { userSettings = it }
                )
            }
        }
    }
}
