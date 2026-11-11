# مستند معماری فاز صفر: استودیوی پارس‌پادکست (ParsPodcast Studio)
## نسخه ۱.۰ — پلتفرم بومی اندروید تولید پادکست و خبر صوتی با هوش مصنوعی آفلاین

---

## ۱. نمای کلی سیستم و اهداف محصول (Product Vision & Core Decisions)

پارس‌پادکست (ParsPodcast) یک استودیوی بومی، جامع و مستقل در بستر سیستم‌عامل اندروید است که کلیه مراحل چرخه تولید پادکست و بولتن خبری صوتی به زبان فارسی را پوشش می‌دهد:
1. **گردآوری اخبار (News Collection)** از فیدهای RSS/Atom و وب‌سایت‌های عمومی.
2. **هوشمندی و تجمیع رویدادها (News Intelligence)** با خوشه‌بندی گزارش‌های متناظر و استخراج فکت‌ها بدون تحریف یا ابداع محتوا.
3. **نگارش خودکار متن برنامه (AI Script Generation)** با تکیه بر مدل‌های محلی لایت‌آرتی Gemma 4 E2B و Gemma 4 E4B به صورت ۱۰۰٪ آفلاین.
4. **تولید صدای باکیفیت فارسی (Persian Neural TTS)** بر بستر Sherpa-ONNX و مدل‌های Piper فارسی (fa_IR-mana-medium) با نرمال‌ساز پیشرفته خط و زبان فارسی.
5. **میکس و مسترینگ چندترکه (Multitrack Audio Mixing)** با موتور پردازش بلادرنگ صوتی، داکینگ خودکار موسیقی متن هنگام گفتار (Audio Ducking) و استانداردسازی بلندی صدا (Loudness Normalization).
6. **مدیریت آرشیو بر پایه تقویم جلالی (Solar Hijri Archive)** با قابلیت بازیابی کامل پروژه‌ها، نسخه‌های فیلم‌نامه و قطعات صوتی.
7. **انتشار پادکست (Podcast Publishing)** در سه سطح: خروجی محلی، توزیع بر بستر فید استاندارد خودمیزبان RSS 2.0، و اتصال به دایرکتوری‌های جهانی (Apple Podcasts و Spotify).

> **اصل کلیدی معماری:** هیچ‌یک از عملیات نگارش متن، پردازش هوش مصنوعی، سنتز گفتار و میکس صدا نباید به سرورهای ابری یا APIهای خارجی وابسته باشند. تمامی این فرایندها باید به صورت کاملاً آفلاین در داخل دستگاه پردازش شوند.

---

## ۲. معماری ماژولار پروژه (Multi-Module Android Architecture)

پروژه به صورت ماژولار بر پایه استانداردهای Clean Architecture و MVVM با تفکیک دقیق وظایف (Separation of Concerns) طراحی شده است:

```
                                  ┌──────────────────────────┐
                                  │       :app (Root)        │
                                  └─────────────┬────────────┘
                                                │
         ┌──────────────────────────────────────┼──────────────────────────────────────┐
         │                                      │                                      │
┌────────▼─────────┐                  ┌─────────▼────────┐                   ┌─────────▼────────┐
│ :feature:news    │                  │  :feature:script │                   │  :feature:audio  │
│ - News Center    │                  │  - Script Studio │                   │  - Multitrack Mix│
│ - Sources UI     │                  │  - Segment Editor│                   │  - Waveform UI   │
└────────┬─────────┘                  └─────────┬────────┘                   └─────────┬────────┘
         │                                      │                                      │
┌────────▼─────────┐                  ┌─────────▼────────┐                   ┌─────────▼────────┐
│ :feature:ai-mgr  │                  │  :feature:voice  │                   │  :feature:publish│
│ - Model Manager  │                  │  - Persian TTS UI│                   │  - RSS Generator │
│ - Download Mgr   │                  │  - Normalizer    │                   │  - Jalali Archive│
└────────┬─────────┘                  └─────────┬────────┘                   └─────────┬────────┘
         │                                      │                                      │
         └──────────────────────────────────────┼──────────────────────────────────────┘
                                                │
         ┌──────────────────────────────────────┼──────────────────────────────────────┐
         │                                      │                                      │
┌────────▼─────────┐                  ┌─────────▼────────┐                   ┌─────────▼────────┐
│ :core:database   │                  │   :core:network  │                   │  :core:model     │
│ - Room Database  │                  │   - OkHttp       │                   │  - Domain Models │
│ - DAOs & Entities│                  │   - Retrofit     │                   │  - Enums & DTOs  │
│ - TypeConverters │                  │   - Jsoup Parser │                   │  - Jalali Helpers│
└──────────────────┘                  └──────────────────┘                   └──────────────────┘
```

### فهرست ماژول‌ها و وظایف:

1. **`:core:model`**: مدل‌های دامنه، مقادیر تغییرناپذیر، کلاس‌های داده و تقویم جلالی.
2. **`:core:database`**: پایگاه‌داده محلی SQLite/Room، جداول، روابط و تراکنش‌ها.
3. **`:core:network`**: زیرساخت شبکه OkHttp/Retrofit، دانلودر قطعه‌قطعه فایل با Resume و پارسر Jsoup.
4. **`:engine:ai-litert`**: پیاده‌سازی `LocalAIProvider` با استفاده از کتابخانه بومی LiteRT-LM برای مدل‌های Gemma 4 E2B و Gemma 4 E4B.
5. **`:engine:tts-sherpa`**: پیاده‌سازی `PersianTTSProvider` با استفاده از Sherpa-ONNX C++ و مدل‌های Piper فارسی.
6. **`:engine:audio-mixer`**: موتور پردازش صوتی AndroidX Media3 / Oboe / AudioTrack با قابلیت چندترکه، داکینگ و میکس PCM/WAV.
7. **`:feature:news`**: صفحه مرکز اخبار، مدیریت منابع، خوشه‌بندی رویدادها بر پایه شباهت برداری/کلمات کلیدی.
8. **`:feature:script`**: استودیوی نگارش فیلم‌نامه در دو حالت تک‌گوینده و دوگوینده.
9. **`:feature:voice`**: مدیریت صداهای گویندگان و نرمال‌سازی متون فارسی.
10. **`:feature:audio`**: تایم‌لاین بصری چندترکه و پنل میکس.
11. **`:feature:archive`**: آرشیو تقویم جلالی و بازنشانی کامل پروژه‌ها.
12. **`:feature:publisher`**: ماژول تولید RSS 2.0، اعتبارسنجی فید و خروجی صوتی.
13. **`:app`**: لایه الحاق Hilt، ناوبری اصلی Compose، تم، و نقطه ورود `MainActivity`.

---

## ۳. نمودار رابطه موجودیت‌ها در پایگاه داده (Room Database ER Diagram)

```
┌─────────────────────────┐           1:N          ┌─────────────────────────┐
│     news_sources        ├───────────────────────►│      news_articles      │
├─────────────────────────┤                        ├─────────────────────────┤
│ id (PK)                 │                        │ id (PK)                 │
│ name: String            │                        │ source_id (FK)          │
│ feed_url: String        │                        │ title: String           │
│ parser_type: String     │                        │ content: String         │
│ category: String        │                        │ original_url: String    │
│ update_interval_mins: Int                        │ published_at_utc: Long  │
│ is_active: Boolean      │                        │ raw_summary: String     │
└─────────────────────────┘                        │ deduplication_hash: Str │
                                                   └────────────┬────────────┘
                                                                │
                                                     M:N        │
                                                (CrossRef)      │
                                                                ▼
┌─────────────────────────┐               ┌──────────────────────────────────┐
│   production_projects   │               │           news_events            │
├─────────────────────────┤               ├──────────────────────────────────┤
│ id (PK)                 │               │ id (PK)                          │
│ title: String           │               │ canonical_title: String          │
│ program_type: String    │               │ event_summary: String            │
│ jalali_date_str: String │               │ category: String                 │
│ created_at_utc: Long    │               │ first_reported_utc: Long         │
│ updated_at_utc: Long    │               │ report_count: Int                │
│ status: String          │               │ primary_facts_json: String       │
│ target_duration_sec: Int│               └──────────────────────────────────┘
└───────────┬─────────────┘
            │
            ├─────────────────────────────────────┐
       1:1  │                                1:N  │
            ▼                                     ▼
┌─────────────────────────┐               ┌──────────────────────────────────┐
│         scripts         │               │          audio_tracks            │
├─────────────────────────┤               ├──────────────────────────────────┤
│ id (PK)                 │               │ id (PK)                          │
│ project_id (FK)         │               │ project_id (FK)                  │
│ model_id_used: String   │               │ track_type: String (INTRO/A/B/BGM│
│ total_segments: Int     │               │ volume: Float                    │
│ raw_json: String        │               │ is_muted: Boolean                │
│ version: Int            │               │ is_solo: Boolean                 │
└───────────┬─────────────┘               │ ducking_enabled: Boolean         │
            │                             └────────────────┬─────────────────┘
       1:N  │                                              │ 1:N
            ▼                                              ▼
┌─────────────────────────┐               ┌──────────────────────────────────┐
│     script_segments     │               │           audio_clips            │
├─────────────────────────┤               ├──────────────────────────────────┤
│ id (PK)                 │               │ id (PK)                          │
│ script_id (FK)          │               │ track_id (FK)                    │
│ segment_index: Int      │               │ start_time_ms: Long              │
│ speaker: String (A / B) │               │ duration_ms: Long                │
│ text_fa: String         │               │ file_path: String                │
│ estimated_duration: Int │               │ fade_in_ms: Long                 │
│ generated_audio_path: Str               │ fade_out_ms: Long                │
│ is_approved: Boolean    │               │ gain_db: Float                   │
└─────────────────────────┘               └──────────────────────────────────┘
            ▲
            │ 1:1 Reference
┌───────────┴─────────────┐               ┌──────────────────────────────────┐
│     voice_profiles      │               │         podcast_episodes         │
├─────────────────────────┤               ├──────────────────────────────────┤
│ id (PK)                 │               │ id (PK, GUID)                    │
│ profile_role: String    │               │ project_id (FK)                  │
│ model_name: String      │               │ title: String                    │
│ speech_rate: Float      │               │ description_fa: String           │
│ pitch: Float            │               │ jalali_pub_date: String          │
│ is_verified: Boolean    │               │ pub_date_rfc822: String          │
│ sample_audio_path: Str  │               │ enclosure_url: String            │
└─────────────────────────┘               │ file_size_bytes: Long            │
                                          │ duration_seconds: Int            │
                                          │ artwork_path: String             │
                                          │ publishing_status: String        │
                                          └──────────────────────────────────┘
```

---

## ۴. رابط‌های کلیدی سیستم (Core Engine Interfaces)

### ۴.۱. رابط ارائه‌دهنده هوش مصنوعی محلی (`LocalAIProvider`)

```kotlin
package com.parspodcast.studio.domain.ai

import kotlinx.coroutines.flow.Flow

interface LocalAIProvider {
    val modelId: String
    val isLoaded: Boolean
    val memoryUsageMb: Long

    suspend fun loadModel(modelPath: String, backend: AIBackend): Result<Unit>
    suspend fun unloadModel(): Result<Unit>
    
    fun generateScriptStream(
        prompt: String,
        systemInstruction: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 4096
    ): Flow<String>

    suspend fun generateScriptSync(
        prompt: String,
        systemInstruction: String
    ): Result<String>
    
    fun cancelInference()
}

enum class AIBackend { CPU, GPU_VULKAN, NPU }

data class ModelMetadata(
    val id: String,
    val name: String,
    val descriptionFa: String,
    val fileSizeBytes: Long,
    val downloadUrl: String,
    val sha256Checksum: String,
    val minRamRequiredMb: Int,
    val recommendedBackend: AIBackend,
    val isInstalled: Boolean,
    val localFilePath: String?
)
```

### ۴.۲. رابط موتور تبدیل متن به گفتار آفلاین (`PersianTTSProvider`)

```kotlin
package com.parspodcast.studio.domain.tts

interface PersianTTSProvider {
    val isReady: Boolean
    val installedVoiceCount: Int

    suspend fun initialize(modelDir: String): Result<Unit>
    
    suspend fun synthesizeSegment(
        text: String,
        voiceId: String,
        speechRate: Float,
        outputWavPath: String
    ): Result<SynthesizedAudioResult>
    
    fun normalizePersianText(rawText: String): String
    
    suspend fun previewVoice(voiceId: String, testPhrase: String): Result<ByteArray>
}

data class SynthesizedAudioResult(
    val filePath: String,
    val durationMs: Long,
    val sampleRate: Int = 22050,
    val channelCount: Int = 1
)
```

### ۴.۳. رابط موتور میکس و مسترینگ صوتی چندترکه (`AudioMixingEngine`)

```kotlin
package com.parspodcast.studio.domain.audio

interface AudioMixingEngine {
    suspend fun renderProject(
        project: MultitrackProject,
        outputFormat: AudioOutputFormat,
        progressCallback: (Float) -> Unit
    ): Result<RenderedAudioFile>

    fun previewPlayback(project: MultitrackProject, startPositionMs: Long)
    fun stopPreview()
    fun pausePreview()
    fun seekTo(positionMs: Long)
}

data class MultitrackProject(
    val projectId: String,
    val title: String,
    val tracks: List<TrackConfiguration>,
    val duckingSettings: DuckingSettings,
    val normalizeLoudness: Boolean = true,
    val targetLufs: Float = -16.0f
)

data class DuckingSettings(
    val enabled: Boolean = true,
    val musicAttenuationDb: Float = -14.0f,
    val attackMs: Long = 150L,
    val releaseMs: Long = 600L
)

enum class AudioOutputFormat { MP3_320KBPS, WAV_16BIT, AAC_256KBPS }
```

### ۴.۴. رابط سیستم انتشار و تولید فید (`PodcastPublisher`)

```kotlin
package com.parspodcast.studio.domain.publisher

interface PodcastPublisher {
    suspend fun generateRssXml(feed: PodcastFeedInfo, episodes: List<PodcastEpisodeInfo>): Result<String>
    suspend fun validateFeed(feedXml: String): Result<FeedValidationReport>
    suspend fun exportPackage(episodeId: String, targetDirectory: String): Result<ExportedEpisodePackage>
    suspend fun publishToSelfHosted(episode: PodcastEpisodeInfo, config: SelfHostedServerConfig): Result<PublishResult>
}
```

---

## ۵. امنیت، حریم خصوصی و ذخیره‌سازی داده‌ها (Security & Storage Policy)

1. **مدل امنیت بدون اینترنت برای پردازش هوش مصنوعی**:
   - هیچ متنی، فیلم‌نامه‌ای، یا صدایی به خارج از تلفن همراه مخابره نمی‌شود.
   - دسترسی اینترنت منحصراً برای دریافت فیدهای RSS/Atom و دانلود فایل مدل‌ها پس از تأیید صریح کاربر استفاده می‌شود.
2. **صحت‌سنجی فایل‌های مدل (Cryptographic Hash Validation)**:
   - فایل‌های حجیم مدل هوش مصنوعی و مدل صدا قبل از بارگذاری با هش SHA-256 کنترل می‌شوند تا از دست‌خوردگی یا خرابی حافظه جلوگیری شود.
3. **ذخیره‌سازی اسکوپ‌شده (Scoped Storage)**:
   - مدل‌ها، وزن‌های شبکه عصبی و کلیپ‌های موقت PCM در حافظه اختصاصی امن برنامه (`context.filesDir` و `context.cacheDir`) ذخیره می‌گردند.
   - صادرات نهایی فقط از طریق Storage Access Framework یا MediaStore با اجازه کاربر در حافظه عمومی قرار می‌گیرد.
4. **رمزنگاری اطلاعات حساس انتشار**:
   - کلیدهای دسترسی سرور میزبان با `EncryptedSharedPreferences` مبتنی بر Android KeyStore ذخیره می‌شوند.

---

## ۶. نقشه راه پیاده‌سازی گام‌به‌گام (Roadmap: Phases 0 to 8)

| فاز | عنوان | اقدامات اصلی | وضعیت |
|:---:|:---|:---|:---:|
| **۰** | معماری و مشخصات فنی | تدوین داکیومنت معماری، ERD پایگاه‌داده، رابط‌های ماژول‌ها و نقشه راه | ✅ تکمیل شد |
| **۱** | زیرساخت بومی اندروید | راه‌اندازی پروژه Gradle، تم Material 3 راست‌چین، Room Database، پایه‌های تقویم جلالی | ⏳ پیاده‌سازی فعلی |
| **۲** | گردآوری و هوشمندی خبر | موتور RSS/Atom/Jsoup، خوشه‌بندی رویدادها، استخراج فکت | در صف |
| **۳** | مدیریت هوش مصنوعی محلی | اتصال LiteRT-LM، مدل‌های Gemma 4 E2B/E4B، دانلود با تایید کاربر | در صف |
| **۴** | موتور صدای فارسی | پیاده‌سازی Sherpa-ONNX/Piper، نرمال‌ساز خط و اعداد فارسی | در صف |
| **۵** | استودیوی میکس چندترکه | پیاده‌سازی موتور میکس چندترکه، داکینگ خودکار، پیش‌نمایش | در صف |
| **۶** | آرشیو جامع جلالی | سازماندهی تقویم شمسی، بازگردانی کامل پروژه‌ها | در صف |
| **۷** | موتور انتشار و RSS | خروجی MP3/WAV/AAC، تولید استاندارد RSS 2.0، اعتبارسنجی | در صف |
| **۸** | تست‌های جامع و انتشار | آزمایش نهایی حافظه، کارایی بدون اینترنت و خروجی پایانی | در صف |
