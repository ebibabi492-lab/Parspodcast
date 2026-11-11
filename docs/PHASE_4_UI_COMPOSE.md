# گزارش جامع اجرای فاز چهارم: استودیوی بومی پارس‌پادکست (ParsPodcast)
## پیاده‌سازی کامل رابط کاربری بومی Jetpack Compose و تجربه کاربری فارسی (Phase 4)

---

## ۱. خلاصه اجرایی و دستاوردهای فاز چهارم

در فاز چهارم از نقشه راه توسعه تدریجی، کل رابط کاربری بومی اپلیکیشن اندروید پارس‌پادکست با تکیه بر **Jetpack Compose**، **Material 3** و استانداردهای طراحی برودکست راست‌چین (RTL) پیاده‌سازی شد. معماری لایه‌ای، پایگاه داده Room، مدل‌های محلی و ساختار پروژه‌های فازهای پیشین بدون کوچک‌ترین تغییری حفظ شده و تمامی ۹ بخش اصلی نرم‌افزار به صفحاتی تعاملی، واکنش‌گرا و متصل به مخازن داده تبدیل گردیدند.

---

## ۲. سیستم طراحی پارس‌پادکست (Design System Specifications)

### ۲.۱. پالت رنگی رسمی فاز ۴:
- **سرمه‌ای عمیق (Deep Navy):** `#0B1020` (رنگ پس‌زمینه اصلی بوم برودکست)
- **سرمه‌ای ثانویه (Secondary Navy):** `#121A2B` (رنگ سطوح، کارت‌ها و نوارهای ناوبری)
- **سرمه‌ای تیره کارت (Dark Navy Card):** `#1A243B` (رنگ کانتینر فیلدهای متنی و اسلایدرها)
- **مرز سطوح (Dark Navy Border):** `#283552` (خطوط تفکیک کننده و بوردرها)
- **فیروزه‌ای استودیو (Turquoise):** `#19D3C5` (رنگ اقدام اصلی، دکمه‌های اولیه و هایلایت‌ها)
- **سیان (Cyan Accent):** `#35BDF5` (رنگ متادیتا، منابع و لینک‌ها)
- **بنفش کم‌رنگ (Purple Accent):** `#8B7CFF` (رنگ تمایز مجری ب و ترنزیشن‌ها)
- **طلایی استودیو (Gold Accent):** `#D6B46A` (رنگ نشانگرهای سولو، زمان‌بندی و افتخارات)
- **متن اصلی (Primary Text):** `#F4F6FA` (خوانایی کامل در زمینه تاریک)
- **متن ثانویه (Secondary Text):** `#A8B1C2` (توضیحات و عناوین کم‌اهمیت‌تر)

### ۲.۲. اصول رابط کاربری و تم:
- **تم تاریک به عنوان پیش‌فرض:** با قابلیت پشتیبانی سیستمی از تم روشن در صورت نیاز کاربر.
- **تایپوگرافی و جهت راست‌چین:** اعمال سراسری `LocalLayoutDirection provides LayoutDirection.Rtl` در ریشه `ParsPodcastTheme`.
- **ارقام فارسی:** ماژول مبدل اعداد به ارقام خورشیدی (۱۲۳۴۵۶۷۸۹۰) و اتصال به تنظیمات کاربر.

---

## ۳. معماری ناوبری و تفکیک مقاصد (Navigation Hierarchy)

ناوبری نرم‌افزار از طریق اسکفولد یکپارچه `ParsPodcastMainScaffold` با تفکیک مقاصد زیر سازمان‌دهی شده است:

### نوارهای ناوبری:
1. **نوار ناوبری پایینی (Bottom Navigation):** برای ۵ مقصد پرکاربرد روزانه:
   - `dashboard`: پیشخوان استودیو
   - `news_center`: مرکز گردآوری و هوشمندی خبر
   - `script_studio`: استودیو نگارش فیلم‌نامه
   - `audio_mixer`: میکس چندترکه صدا
   - `jalali_archive`: آرشیو تقویم جلالی
2. **نوار بالا و منوی تکمیلی (TopBar & Overflow Menu):** برای بخش‌های مدیریتی و تنظیمات:
   - `voice_studio`: استودیو صدا و گویندگان
   - `publisher`: ناشر پادکست و تولید فید RSS 2.0
   - `model_manager`: مدیریت مدل‌های محلی
   - `settings`: تنظیمات برنامه

---

## ۴. تشریح صفحات ۹ گانه پیاده‌سازی‌شده (Screen Manifest)

| شماره | صفحه و فایل کامپوز | قابلیت‌های پیاده‌سازی شده | وضعیت صحت و صداقت داده‌ها |
|:---:|:---|:---|:---|
| **۱** | `DashboardScreen.kt` | خوش‌آمدگویی، تاریخ جلالی روز، آمار مقالات و اپیزودها، وضعیت خط تولید، شروع سریع برنامه | نمایش آمار واقعی و برچسب عدم نصب مدل‌ها در صورت عدم استقرار |
| **۲** | `NewsCenterScreen.kt` | جستجوی فارسی، فیلتر دسته‌بندی‌ها (فناوری، اقتصاد، دانش، جامعه)، انتخاب چندگانه برای سناریو، دیالوگ جزئیات | آماده اتصال به کرولر و فیدخوان |
| **۳** | `ScriptStudioScreen.kt` | عنوان اپیزود، سوییچ تک‌گوینده/دوگوینده، ویرایشگر سگمنت‌ها، شمارنده کلمات و زمان تخمینی، دکمه‌های دستیار نگارش | نمایش پیام اخطار شفاف در صورت عدم استقرار مدل Gemma 4 |
| **۴** | `VoiceStudioScreen.kt` | انتخاب مجری الف و ب، اسلایدر سرعت و گام کلام، پلیر قطعات صوتی تولید شده، دکمه سنتز صدا | عدم استفاده از موتور جعلی اندروید؛ اعلام صریح نیاز به وزن‌های مانا |
| **۵** | `AudioMixerScreen.kt` | تایم‌لاین بصری ۶ ترکه، دکمه‌های Play/Pause و اسکرابر، فیدرهای ولوم، دکمه‌های Mute و Solo برای هر ترک، دکمه رندر | نمایش وضعیت آماده‌سازی خط رندر Media3 بدون فریب کاربر |
| **۶** | `JalaliArchiveScreen.kt` | فهرست اپیزودها، جستجو در آرشیو، فیلتر ۱۲ ماه خورشیدی، حذف با دیالوگ تأیید، ویرایش مجدد | اتصال به دیتابیس محلی Room بدون آرشیو مجازی در رم |
| **۷** | `PodcastPublisherScreen.kt` | مشخصات متادیتا، پیش‌نمایش کاور ۳۰۰۰×۳۰۰۰ پیکسل، تفکیک خروجی محلی (MP3/WAV) و فید RSS خودمیزبان | عدم نمایش موفقیت کاذب تا زمان اجرای تراکنش واقعی |
| **۸** | `ModelManagerScreen.kt` | مدیریت مدل‌های Gemma 4 E4B و E2B، بررسی Sherpa-ONNX، نمایش حجم و نیازمندی رم، نمایش مصرف حافظه داخلی | نمایش صادقانه وضعیت «نصب نشده» تا زمان دانلود با اجازه کاربر |
| **۹** | `SettingsScreen.kt` | زبان، تم تاریک، ارقام فارسی، مشخصات پیش‌فرض ناشر، پاکسازی کش، سند حریم خصوصی و عدم ارسال ابری | متصل به ساختار تنظیمات و ذخیره‌سازی محلی |

---

## ۵. مؤلفه‌های بازکاربردپذیر کامپوز (Reusable Component Library)

- `ParsPodcastTopBar`: نوار ابزار با عنوان فارسی، وضعیت مدل‌ها و منوی دسترسی سریع.
- `ParsPodcastBottomNavigation`: نوار ۵ گزینه‌ای با رنگ‌بندی تم تاریک.
- `SectionHeader`: تیتر استاندارد بخش‌ها همراه با زیرتیتر و دکمه اقدام اختیاری.
- `EpisodeCard`: کارت اپیزود با نشانگر وضعیت، تاریخ جلالی و دکمه پخش.
- `NewsCard`: کارت خبر با منبع، چک‌باکس انتخاب و خلاصه فکت‌ها.
- `ModelStatusCard`: کارت مشخصات مدل هوش مصنوعی با حجم و نیازمندی رم.
- `AudioPlayerControls`: کنترل‌های پخش صوتی با اسلایدر و پرش ۵ ثانیه‌ای.
- `AudioTimeline`: پیش‌نمایش خطوط ترک‌های چندگانه صوتی.
- `EmptyState`: نمایش پیام تمیز و راهنمای اقدام در زمان خالی بودن فهرست‌ها.
- `LoadingState`: نمایش لودینگ استاندارد فیروزه‌ای.
- `ErrorState`: نمایش خطا و دکمه تلاش مجدد.
- `ConfirmationDialog`: دیالوگ تأیید استاندارد برای عملیات حساس (حذف و انتشار).
- `PersianTextField`: فیلد متنی استاندارد با پشتیبانی راست‌چین.
- `PrimaryActionButton` و `SecondaryActionButton`: دکمه‌های اصلی و ثانویه برودکست.
- `StatusIndicator`: نشانگر رنگی وضعیت با حاشیه و پس‌زمینه کنترل‌شده.

---

## ۶. فهرست فایل‌های ایجاد شده در فاز ۴

1. `android/app/src/main/java/com/parspodcast/studio/ui/theme/Color.kt`
2. `android/app/src/main/java/com/parspodcast/studio/ui/theme/Theme.kt`
3. `android/app/src/main/java/com/parspodcast/studio/data/repository/Repositories.kt`
4. `android/app/src/main/java/com/parspodcast/studio/ui/components/CommonComponents.kt`
5. `android/app/src/main/java/com/parspodcast/studio/ui/components/CardsAndAudioComponents.kt`
6. `android/app/src/main/java/com/parspodcast/studio/ui/components/NavigationBars.kt`
7. `android/app/src/main/java/com/parspodcast/studio/ui/navigation/NavRoutes.kt`
8. `android/app/src/main/java/com/parspodcast/studio/ui/navigation/ParsPodcastMainScaffold.kt`
9. `android/app/src/main/java/com/parspodcast/studio/ui/screens/DashboardScreen.kt`
10. `android/app/src/main/java/com/parspodcast/studio/ui/screens/NewsCenterScreen.kt`
11. `android/app/src/main/java/com/parspodcast/studio/ui/screens/ScriptStudioScreen.kt`
12. `android/app/src/main/java/com/parspodcast/studio/ui/screens/VoiceStudioScreen.kt`
13. `android/app/src/main/java/com/parspodcast/studio/ui/screens/AudioMixerScreen.kt`
14. `android/app/src/main/java/com/parspodcast/studio/ui/screens/JalaliArchiveScreen.kt`
15. `android/app/src/main/java/com/parspodcast/studio/ui/screens/PodcastPublisherScreen.kt`
16. `android/app/src/main/java/com/parspodcast/studio/ui/screens/ModelManagerScreen.kt`
17. `android/app/src/main/java/com/parspodcast/studio/ui/screens/SettingsScreen.kt`
18. `android/app/src/main/java/com/parspodcast/studio/MainActivity.kt`
19. `docs/PHASE_4_UI_COMPOSE.md`

---

## ۷. دستورالعمل بیلد و اعتبارسنجی در محیط اندروید استودیو

1. باز کردن شاخه `/android` در محیط **Android Studio Ladybug / Meerkat (یا بالاتر)**.
2. اطمینان از تنظیم JDK روی نسخه ۱۷ (`Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK`).
3. اجرای فرمان همگام‌سازی گریدل (`Sync Project with Gradle Files`).
4. اجرای دستور ساخت:
   ```bash
   ./gradlew assembleDebug
   ```
5. نصب مستقیم روی دستگاه هدف داریا یا شبیه‌ساز اندروید ۱۵:
   ```bash
   ./gradlew installDebug
   ```
