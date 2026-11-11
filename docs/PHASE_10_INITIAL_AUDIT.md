# گزارش ممیزی اولیه مخزن — فاز ۱۰ استودیوی پارس‌پادکست
# ParsPodcast Studio — Phase 10 Initial Repository & Architecture Audit

**تاریخ ممیزی:** ۲۱ آبان ۱۴۰۵ (11 November 2026)  
**معمار و مهندس ارشد سیستم:** ParsPodcast Mobile Architecture Team  
**دستگاه مرجع هدف:** Daria Band (پردازنده Dimensity 7050، پردازنده گرافیکی Mali-G68 MC4، رم ۸ گیگابایت، سیستم‌عامل اندروید ۱۵، معماری ARM64)  

---

## ۱. خلاصه ساختار مخزن و متدولوژی ممیزی
مخزن پروژه شامل دو بخش اصلی است:
1. **پروژه بومی اندروید (`/android`):** ساختار کامل Clean Architecture + MVVM + Jetpack Compose مبتنی بر ابزارهای Gradle، Room Database، Hilt، Media3، WorkManager، LiteRT-LM، و Sherpa-ONNX.
2. **پروتوتایپ مرجع وب (`/src`):** برنامه پورت ۳۰۰۰ تحت React/Vite/TypeScript به عنوان مرجع دیداری و رفتاری رابط کاربری فارسی (RTL).

ممیزی با رویکرد «بررسی کد واقعی، عدم اکتفا به گزارش‌ها و عدم تکیه بر شبیه‌سازی» صورت پذیرفت.

---

## ۲. وضعیت مولفه‌های پیاده‌سازی شده در فازهای ۳ تا ۹

### ۲.۱. فاز ۵ — ادغام مدل زبانی Gemma 4 E4B با موتور LiteRT-LM
* **وضعیت:** ✅ **پیاده‌سازی شده و معتبر**
* **فایل‌ها:** `LiteRTLMEngine.kt`، `LocalLLMProvider.kt`، `ModelFileManager.kt`، `PersianPromptBuilder.kt`، `ScriptJsonParser.kt`
* **یافته‌ها:**
  - موتور `LiteRTLMEngine` به صورت کامپوننت بومی با فراخوانی کتابخانه `com.google.ai.edge.litertlm` پیاده‌سازی شده است.
  - مکانیزم Fallback از GPU (Vulkan/OpenCL) به CPU (XNNPACK) به صورت صریح با `try/catch` مدیریت می‌شود.
  - فرآیند تولید توکن به صورت جریان واکنشی (`Flow<String>`) بدون انسداد رشته اصلی (Main Thread) پیاده‌سازی گردیده است.
  - لغو فرآیند (`cancelInference`) با مدیریت `Job` همگام است.

### ۲.۲. فاز ۶ — سنتز صوت آفلاین فارسی با Sherpa-ONNX و مدل مانا
* **وضعیت:** ✅ **پیاده‌سازی شده و معتبر**
* **فایل‌ها:** `SherpaOnnxTtsEngine.kt`، `TTSProvider.kt`، `TTSModelFileManager.kt`، `PersianTTSPreprocessor.kt`، `PersianSentenceSplitter.kt`، `WavAudioWriter.kt`، `AudioPlaybackManager.kt`
* **یافته‌ها:**
  - فرآیند پیش‌پردازش متن فارسی، تبدیل اعداد به حروف و قطعه‌بندی بر اساس علائم نگارشی پیاده‌سازی شده است.
  - نگارش فایل‌های هدر استاندارد RIFF WAV با نرخ نمونه‌برداری ۲۲,۰۵۰ هرتز مونو ۱۶ بیتی به‌صورت دقیق کار می‌کند.
  - شفافیت هویتی: مدل مانا تک‌گوینده زن است و در رابط کاربری `VoiceStudioScreen` به درستی ثبت شده که تمایز مجری الف و ب با پارامتر سرعت و لحن صورت می‌گیرد.

### ۲.۳. فاز ۷ — سیستم ترکیبی دریافت خبر (RSS/Atom و فایل آفلاین)
* **وضعیت:** ✅ **پیاده‌سازی شده و معتبر**
* **فایل‌ها:** `NewsEntities.kt`، `RssAtomParser.kt`، `OfflineTextNewsParser.kt`، `NewsNormalizer.kt`، `RssFeedFetcher.kt`، `OfflineNewsImportManager.kt`، `NewsSyncWorker.kt`
* **یافته‌ها:**
  - تفکیک کامل لایه دریافت خبر از لایه انتشار پادکست به منظور جلوگیری از تداخل مدل‌ها رعایت شده است.
  - محافظت در برابر حملات تزریق XML (XXE Protection) در `RssAtomParser` با غیرفعال‌سازی DTD و نهادهای خارجی فعال است.
  - الگوریتم هش محتوا و الگوریتم تطبیق شباهت فازی جهت پیشگیری از ثبت اخبار تکراری فعال است.

### ۲.۴. فاز ۸ — استودیوی چندترکه میکس و پردازش صوت
* **وضعیت:** ✅ **پیاده‌سازی شده و معتبر**
* **فایل‌ها:** `MultitrackAudioMixer.kt`، `WavFileReader.kt`، `TimelineHistoryManager.kt`، `WaveformExtractor.kt`، `SynchronizedMultitrackPlayer.kt`
* **یافته‌ها:**
  - رندر صوت استودیویی در نرخ نمونه‌برداری ۴۸,۰۰۰ هرتز استریو با محاسبات ممیز شناور (Float32).
  - قانون پن استریو توان ثابت (`Constant Power Panning Law`) پیاده‌سازی شده است.
  - لیمیتر نرم بر پایه تابع هذلولوی (`tanh soft-knee limiter`) برای جلوگیری قطعی از برش دیجیتال (Clipping).
  - موتور تاریخچه Undo/Redo با ظرفیت ۲۰ اسنپ‌شات عمیق بدون نشت حافظه.

### ۲.۵. فاز ۹ — انتشار، تولید فید RSS 2.0 و میزبانی
* **وضعیت:** ✅ **پیاده‌سازی شده و معتبر**
* **فایل‌ها:** `PodcastShowEntity`، `HostingConfigEntity`، `PodcastRssGenerator.kt`، `PodcastFeedValidator.kt`، `WebDavHostingProvider.kt`، `LocalExportProvider.kt`، `PublicFeedVerifier.kt`، `PodcastUploadWorker.kt`، `SecureCredentialManager.kt`، `AudioPublishingPreparer.kt`
* **یافته‌ها:**
  - ساختار فید RSS 2.0 منطبق با استانداردهای رسمی اپل پادکست و اسپاتیفای.
  - شناسه‌های پایدار GUID که با ویرایش متادیتا تغییر نمی‌کنند.
  - ذخیره‌سازی رمز عبور سرور WebDAV با کلید سخت‌افزاری Android Keystore (الگوریتم AES-GCM).
  - صادرات بسته آفلاین با راهنمای جامع فارسی.

---

## ۳. نواقص و ریسک‌های شناسایی شده در ممیزی

### نقص ۱: اعلام سرویس در مانیفست بدون پیاده‌سازی فایل مربوطه (Critical)
* **مشکل:** در فایل `AndroidManifest.xml` خط ۳۸ تا ۴۱، تگ زیر تعریف شده است:
  `<service android:name=".service.AudioProcessingService" android:exported="false" android:foregroundServiceType="mediaPlayback" />`
  اما پکیج `com.parspodcast.studio.service` و کلاس `AudioProcessingService` در درخت فایل‌های جاوا/کاتلین وجود ندارد.
* **ریسک:** پرتاب خطای `ClassNotFoundException` هنگام فراخوانی سرویس پخش پس‌زمینه توسط اندروید یا هنگام بیلد سخت‌گیرانه.
* **راهکار:** ایجاد کلاس `AudioProcessingService.kt` بر پایه Media3 `MediaSessionService` جهت مدیریت یکپارچه سشن‌های صوتی و اعلان پیش‌زمینه.

### نقص ۲: فقدان هماهنگ‌کننده منابع حافظه رم میان Gemma و TTS (Medium Risk)
* **مشکل:** مدل Gemma 4 E4B حدود ۴.۶ گیگابایت و مدل مانا حدود ۱۲۵ مگابایت رم نیاز دارند. در دستگاه Daria Band با ۸ گیگابایت رم (که حدود ۳.۵ گیگابایت آن توسط اندروید مصرف می‌شود)، بارگذاری همزمان هر دو موتور می‌تواند به OOM یا افت سرعت منجر گردد.
* **راهکار:** پیاده‌سازی `ModelMemoryCoordinator` برای مدیریت سهمیه‌های حافظه به طوری که پیش از اجرای فرآیند سنگین تولید متن، موتور صوت آزاد شود و پیش از سنتز دسته‌ای صوت، نشست گفتگوی LLM بهینه گردد.

### نقص ۳: عدم وجود آزمون خودکار برای اعتبارسنجی مهاجرت دیتابیس Room (MIGRATION_1_2)
* **مشکل:** پایگاه داده از نسخه ۱ به ۲ ارتقا یافته و `MIGRATION_1_2` نوشته شده است، اما آزمون واحد برای اجرای چرخه کامل مهاجرت در آزمون‌های خودکار اضافه نشده بود.
* **راهکار:** افزودن `RoomMigrationTest.kt` با بررسی ساخت جداول `podcast_shows`، `hosting_configs` و انتقال داده‌های `podcast_episodes`.

---

## ۴. برنامه اقدامات اجرایی فاز ۱۰
1. ایجاد `AudioProcessingService.kt` در پکیج `service`.
2. ایجاد `ModelMemoryCoordinator.kt` جهت بهینه‌سازی مصرف حافظه رم روی Daria Band.
3. ایجاد آزمون مهاجرت پایگاه داده `RoomMigrationTest.kt`.
4. ایجاد آزمون جامع ادغام سرتاسری `EndToEndPodcastWorkflowTest.kt`.
5. به‌روزرسانی مستندات کامل پکیج ریلیز و استقرار آفلاین.
