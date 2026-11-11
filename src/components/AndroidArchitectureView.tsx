import React, { useState } from 'react';
import {
  Layers,
  Database,
  FileCode,
  ShieldCheck,
  CheckCircle2,
  Copy,
  Check,
  Download,
  FolderTree,
  Cpu,
  Calendar,
  Volume2
} from 'lucide-react';

export const AndroidArchitectureView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'architecture' | 'erd' | 'code' | 'roadmap'>('architecture');
  const [selectedFile, setSelectedFile] = useState<string>('JalaliCalendar.kt');
  const [copied, setCopied] = useState(false);

  const kotlinFiles: Record<string, string> = {
    'JalaliCalendar.kt': `package com.parspodcast.studio.domain.calendar

data class JalaliDate(
    val year: Int,
    val month: Int, // 1 to 12
    val day: Int,   // 1 to 31
    val dayOfWeek: Int // 1 (Saturday) to 7 (Friday)
) {
    val monthName: String get() = JalaliCalendar.MONTH_NAMES[month - 1]
    val weekdayName: String get() = JalaliCalendar.WEEKDAY_NAMES[dayOfWeek - 1]

    fun toFormattedString(usePersianDigits: Boolean = true): String {
        val str = "$weekdayName $day $monthName $year"
        return if (usePersianDigits) JalaliCalendar.toPersianDigits(str) else str
    }
}`,
    'PersianTextNormalizer.kt': `package com.parspodcast.studio.domain.tts

object PersianTextNormalizer {
    fun normalize(rawText: String): String {
        var text = rawText
        // 1. Unify Arabic Yeh / Kaf to Persian
        text = text.replace('ي', 'ی').replace('ك', 'ک')
        // 2. Expand numbers to Persian words (۱۴۰۵ -> هزار و چهارصد و پنج)
        text = expandNumbersToPersianWords(text)
        // 3. Format punctuation pauses
        return text.trim()
    }
}`,
    'NewsEntities.kt': `package com.parspodcast.studio.data.local.entities

@Entity(tableName = "news_sources")
data class NewsSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val feedUrl: String,
    val parserType: String,
    val category: String,
    val updateIntervalMins: Int = 60,
    val isActive: Boolean = true
)

@Entity(tableName = "news_events")
data class NewsEventEntity(
    @PrimaryKey val id: String,
    val canonicalTitle: String,
    val eventSummary: String,
    val category: String,
    val reportCount: Int = 1,
    val primaryFactsJson: String = "[]",
    val verifiedDifferentialsJson: String = "[]"
)`,
    'ProductionEntities.kt': `package com.parspodcast.studio.data.local.entities

@Entity(tableName = "production_projects")
data class ProductionProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val programType: String, // SINGLE_PRESENTER, TWO_PRESENTERS
    val jalaliDateStr: String,
    val createdAtUtc: Long,
    val status: String,
    val targetDurationSec: Int = 300
)

@Entity(tableName = "script_segments")
data class ScriptSegmentEntity(
    @PrimaryKey val id: String,
    val scriptId: String,
    val segmentIndex: Int,
    val speakerRole: String, // PRESENTER_A, PRESENTER_B
    val textFa: String,
    val estimatedDurationSec: Int
)`,
    'ParsPodcastDatabase.kt': `package com.parspodcast.studio.data.local

@Database(
    entities = [
        NewsSourceEntity::class,
        NewsArticleEntity::class,
        NewsEventEntity::class,
        NewsEventArticleCrossRef::class,
        ProductionProjectEntity::class,
        ScriptEntity::class,
        ScriptSegmentEntity::class,
        VoiceProfileEntity::class,
        AudioTrackEntity::class,
        AudioClipEntity::class,
        PodcastEpisodeEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ParsPodcastDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun productionDao(): ProductionDao
    abstract fun voiceDao(): VoiceDao
    abstract fun publishingDao(): PublishingDao
}`,
    'ParsPodcastMainScaffold.kt': `package com.parspodcast.studio.ui.navigation

@Composable
fun ParsPodcastMainScaffold(navController: NavHostController = rememberNavController()) {
    Scaffold(
        topBar = { ParsPodcastTopBar(...) },
        bottomBar = { ParsPodcastBottomNavigation(...) },
        containerColor = DeepNavy
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = NavRoutes.DASHBOARD) {
            composable(NavRoutes.DASHBOARD) { DashboardScreen(...) }
            composable(NavRoutes.NEWS_CENTER) { NewsCenterScreen(...) }
            composable(NavRoutes.SCRIPT_STUDIO) { ScriptStudioScreen(...) }
            composable(NavRoutes.VOICE_STUDIO) { VoiceStudioScreen(...) }
            composable(NavRoutes.AUDIO_MIXER) { AudioMixerScreen(...) }
            composable(NavRoutes.JALALI_ARCHIVE) { JalaliArchiveScreen(...) }
            composable(NavRoutes.PUBLISHER) { PodcastPublisherScreen(...) }
            composable(NavRoutes.MODEL_MANAGER) { ModelManagerScreen(...) }
            composable(NavRoutes.SETTINGS) { SettingsScreen(...) }
        }
    }
}`,
    'Repositories.kt': `package com.parspodcast.studio.data.repository

interface ModelRepository {
    fun getModelStatuses(): Flow<List<ModelStatus>>
    suspend fun checkModelInstalled(modelId: String): Boolean
}

interface NewsRepository {
    fun getAllArticles(): Flow<List<NewsArticleEntity>>
    fun getAllSources(): Flow<List<NewsSourceEntity>>
}

interface ProductionRepository {
    fun getAllProjects(): Flow<List<ProductionProjectEntity>>
    fun getSegmentsForScript(scriptId: String): Flow<List<ScriptSegmentEntity>>
}`,
    'Theme.kt': `package com.parspodcast.studio.ui.theme

@Composable
fun ParsPodcastTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ParsPodcastDarkColorScheme else ParsPodcastLightColorScheme
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}`
  };

  const handleCopyCode = () => {
    navigator.clipboard.writeText(kotlinFiles[selectedFile] || '');
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="border-b border-[#1C2541] pb-4">
        <h1 className="text-xl font-bold text-white flex items-center gap-2">
          <Layers className="w-5 h-5 text-[#00B4D8]" />
          <span>معماری بومی اندروید فاز ۰ و ۱ (Native Android Architecture & Foundation)</span>
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          مستندات معماری، ساختار ماژولار گریدل، پایگاه داده Room، پایه‌های تقویم جلالی و تم متریال ۳ راست‌چین
        </p>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 p-1 bg-[#0B132B] border border-[#1C2541] rounded-lg text-xs">
        <button
          onClick={() => setActiveTab('architecture')}
          className={`px-3 py-1.5 rounded-md font-medium transition-colors cursor-pointer ${
            activeTab === 'architecture' ? 'bg-[#1C2541] text-[#00B4D8]' : 'text-slate-400 hover:text-white'
          }`}
        >
          اصول معماری و ماژول‌ها
        </button>
        <button
          onClick={() => setActiveTab('erd')}
          className={`px-3 py-1.5 rounded-md font-medium transition-colors cursor-pointer ${
            activeTab === 'erd' ? 'bg-[#1C2541] text-[#00B4D8]' : 'text-slate-400 hover:text-white'
          }`}
        >
          نمودار ERD پایگاه‌داده Room
        </button>
        <button
          onClick={() => setActiveTab('code')}
          className={`px-3 py-1.5 rounded-md font-medium transition-colors cursor-pointer ${
            activeTab === 'code' ? 'bg-[#1C2541] text-[#00B4D8]' : 'text-slate-400 hover:text-white'
          }`}
        >
          فایل‌های کاتلین پیاده‌شده (Phase 1 Codebase)
        </button>
        <button
          onClick={() => setActiveTab('roadmap')}
          className={`px-3 py-1.5 rounded-md font-medium transition-colors cursor-pointer ${
            activeTab === 'roadmap' ? 'bg-[#1C2541] text-[#00B4D8]' : 'text-slate-400 hover:text-white'
          }`}
        >
          نقشه راه فازهای ۰ تا ۸
        </button>
      </div>

      {/* Tab 1: Module Architecture */}
      {activeTab === 'architecture' && (
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-4 space-y-2">
              <div className="flex items-center gap-2 text-white font-bold text-xs">
                <Cpu className="w-4 h-4 text-[#00B4D8]" />
                <span>موتور هوش مصنوعی محلی (:engine:ai-litert)</span>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                ارائه‌دهنده `LocalAIProvider` با ران‌تایم LiteRT-LM برای اجرای مدل‌های Gemma 4 E2B و E4B بدون وابستگی به اینترنت.
              </p>
            </div>

            <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-4 space-y-2">
              <div className="flex items-center gap-2 text-white font-bold text-xs">
                <Volume2 className="w-4 h-4 text-[#48CAE4]" />
                <span>موتور صدای فارسی (:engine:tts-sherpa)</span>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                سنتز گفتار بومی با Sherpa-ONNX و مدل Piper مانای فارسی (fa_IR-mana-medium) همراه با نرمال‌ساز پیشرفته خط و تقویم جلالی.
              </p>
            </div>

            <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-4 space-y-2">
              <div className="flex items-center gap-2 text-white font-bold text-xs">
                <Calendar className="w-4 h-4 text-[#7209B7]" />
                <span>آرشیو و تقویم جلالی (:core:model)</span>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                محاسبات تقویم شمسی بر پایه ریاضیات دقیق نجومی بدون کتابخانه‌های سنگین خارجی و نمایش ارقام و ماه‌های فارسی.
              </p>
            </div>
          </div>

          <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-5 space-y-3">
            <h3 className="text-sm font-bold text-white">مرزهای آفلاین و آنلاین (Offline / Online Boundaries)</h3>
            <div className="space-y-2 text-xs">
              <div className="p-3 bg-[#070B18] rounded-lg border border-[#1C2541] flex items-center justify-between text-slate-300">
                <span>استنتاج هوش مصنوعی (Gemma 4 Instruct)</span>
                <span className="text-emerald-400 font-mono">۱۰۰٪ آفلاین بر روی دستگاه</span>
              </div>
              <div className="p-3 bg-[#070B18] rounded-lg border border-[#1C2541] flex items-center justify-between text-slate-300">
                <span>سنتز صدای فارسی (Sherpa-ONNX Piper)</span>
                <span className="text-emerald-400 font-mono">۱۰۰٪ آفلاین بر روی دستگاه</span>
              </div>
              <div className="p-3 bg-[#070B18] rounded-lg border border-[#1C2541] flex items-center justify-between text-slate-300">
                <span>میکس و مسترینگ چندترکه صوتی</span>
                <span className="text-emerald-400 font-mono">۱۰۰٪ آفلاین بر روی دستگاه</span>
              </div>
              <div className="p-3 bg-[#070B18] rounded-lg border border-[#1C2541] flex items-center justify-between text-slate-300">
                <span>گردآوری اخبار از فیدهای RSS/Atom</span>
                <span className="text-cyan-400 font-mono">نیازمند اتصال به اینترنت</span>
              </div>
              <div className="p-3 bg-[#070B18] rounded-lg border border-[#1C2541] flex items-center justify-between text-slate-300">
                <span>دانلود مدل‌های حجیم و انتشار در سرور</span>
                <span className="text-cyan-400 font-mono">منوط به تأیید صریح کاربر</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: Database ER Diagram */}
      {activeTab === 'erd' && (
        <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Database className="w-4 h-4 text-[#00B4D8]" />
              <span>جداول و روابط پایگاه داده Room (Room Database Schema)</span>
            </h3>
            <span className="font-mono text-xs text-slate-400">11 Entities · ForeignKeys Enabled</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 text-xs">
            <div className="bg-[#070B18] border border-[#1C2541] p-3 rounded-lg space-y-1">
              <div className="font-mono font-bold text-[#00B4D8]">news_sources</div>
              <div className="text-[11px] text-slate-400 font-mono">id (PK), name, feedUrl, parserType, category, updateIntervalMins</div>
            </div>

            <div className="bg-[#070B18] border border-[#1C2541] p-3 rounded-lg space-y-1">
              <div className="font-mono font-bold text-[#00B4D8]">news_articles</div>
              <div className="text-[11px] text-slate-400 font-mono">id (PK), source_id (FK), title, content, publishedAtUtc, deduplication_hash</div>
            </div>

            <div className="bg-[#070B18] border border-[#1C2541] p-3 rounded-lg space-y-1">
              <div className="font-mono font-bold text-[#48CAE4]">news_events</div>
              <div className="text-[11px] text-slate-400 font-mono">id (PK), canonicalTitle, eventSummary, primaryFactsJson, verifiedDifferentialsJson</div>
            </div>

            <div className="bg-[#070B18] border border-[#1C2541] p-3 rounded-lg space-y-1">
              <div className="font-mono font-bold text-purple-400">production_projects</div>
              <div className="text-[11px] text-slate-400 font-mono">id (PK), title, programType, jalaliDateStr, createdAtUtc, status, targetDurationSec</div>
            </div>

            <div className="bg-[#070B18] border border-[#1C2541] p-3 rounded-lg space-y-1">
              <div className="font-mono font-bold text-purple-400">script_segments</div>
              <div className="text-[11px] text-slate-400 font-mono">id (PK), script_id (FK), segmentIndex, speakerRole, textFa, estimatedDurationSec</div>
            </div>

            <div className="bg-[#070B18] border border-[#1C2541] p-3 rounded-lg space-y-1">
              <div className="font-mono font-bold text-emerald-400">audio_tracks & clips</div>
              <div className="text-[11px] text-slate-400 font-mono">id (PK), project_id (FK), trackType, volume, duckingEnabled, startTimeMs, durationMs</div>
            </div>

            <div className="bg-[#070B18] border border-[#1C2541] p-3 rounded-lg space-y-1">
              <div className="font-mono font-bold text-amber-400">podcast_episodes</div>
              <div className="text-[11px] text-slate-400 font-mono">id (PK GUID), project_id (FK), title, jalaliPubDate, pubDateRfc822, enclosureUrl</div>
            </div>
          </div>
        </div>
      )}

      {/* Tab 3: Kotlin Source Code Inspector */}
      {activeTab === 'code' && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            {Object.keys(kotlinFiles).map((fileName) => (
              <button
                key={fileName}
                onClick={() => setSelectedFile(fileName)}
                className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-colors cursor-pointer ${
                  selectedFile === fileName
                    ? 'bg-[#00B4D8] text-[#070B18] font-bold'
                    : 'bg-[#0B132B] text-slate-400 hover:text-white'
                }`}
              >
                {fileName}
              </button>
            ))}
          </div>

          <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl overflow-hidden">
            <div className="p-3 border-b border-[#1C2541] flex items-center justify-between">
              <span className="font-mono text-xs text-[#00B4D8]">{selectedFile}</span>
              <button
                onClick={handleCopyCode}
                className="px-3 py-1 bg-[#1C2541] hover:bg-[#27355B] text-slate-300 rounded text-xs flex items-center gap-1.5 cursor-pointer"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copied ? 'کپی شد!' : 'کپی کد'}</span>
              </button>
            </div>
            <pre className="p-4 bg-[#070B18] font-mono text-[11px] text-slate-200 overflow-x-auto max-h-[420px] text-left" dir="ltr">
              {kotlinFiles[selectedFile]}
            </pre>
          </div>
        </div>
      )}

      {/* Tab 4: Roadmap Phases 0 to 8 */}
      {activeTab === 'roadmap' && (
        <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-5 space-y-4">
          <h3 className="text-sm font-bold text-white">جدول گام‌های اجرایی استودیو پارس‌پادکست</h3>
          <div className="space-y-2 text-xs">
            <div className="p-3 bg-[#070B18] border border-emerald-500/40 rounded-lg flex items-center justify-between text-slate-300">
              <div>
                <span className="font-bold text-white">فاز ۰: معماری و مشخصات فنی</span>
                <p className="text-[11px] text-slate-400">تدوین مستند جامع معماری، ERD، رابط‌ها و نقشه ماژول‌ها</p>
              </div>
              <span className="text-emerald-400 font-mono">تکمیل شد</span>
            </div>

            <div className="p-3 bg-[#070B18] border border-emerald-500/40 rounded-lg flex items-center justify-between text-slate-300">
              <div>
                <span className="font-bold text-white">فاز ۱: زیرساخت بومی اندروید</span>
                <p className="text-[11px] text-slate-400">پایگاه داده Room، تم راست‌چین Jetpack Compose و تقویم جلالی</p>
              </div>
              <span className="text-emerald-400 font-mono">پیاده‌سازی شد</span>
            </div>

            <div className="p-3 bg-[#070B18] border border-[#1C2541] rounded-lg flex items-center justify-between text-slate-400">
              <div>
                <span className="font-medium text-slate-200">فاز ۲: مرکز خبر و هوشمندی</span>
                <p className="text-[11px] text-slate-500">موتور RSS، خوشه‌بندی رویدادها، استخراج فکت</p>
              </div>
              <span className="font-mono text-slate-500">آماده فاز بعدی</span>
            </div>

            <div className="p-3 bg-[#070B18] border border-[#1C2541] rounded-lg flex items-center justify-between text-slate-400">
              <div>
                <span className="font-medium text-slate-200">فاز ۳: مدیریت هوش مصنوعی محلی</span>
                <p className="text-[11px] text-slate-500">اتصال LiteRT-LM و مدل‌های Gemma 4 E2B/E4B</p>
              </div>
              <span className="font-mono text-slate-500">آماده فاز بعدی</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
