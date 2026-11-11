import { NewsSource, NewsArticle, NewsEvent, AIModel, VoiceProfile, ScriptProject, AudioTrack, PodcastEpisode } from '../types';

export const INITIAL_NEWS_SOURCES: NewsSource[] = [
  {
    id: 'src_irna',
    name: 'خبرگزاری ایرنا (IRNA)',
    feedUrl: 'https://www.irna.ir/rss',
    parserType: 'RSS',
    category: 'جامعه',
    updateIntervalMins: 30,
    isActive: true,
    lastFetchedUtc: Date.now() - 15 * 60 * 1000,
    articleCount: 142
  },
  {
    id: 'src_isna',
    name: 'خبرگزاری ایسنا (ISNA)',
    feedUrl: 'https://www.isna.ir/rss',
    parserType: 'RSS',
    category: 'علم و دانش',
    updateIntervalMins: 45,
    isActive: true,
    lastFetchedUtc: Date.now() - 25 * 60 * 1000,
    articleCount: 98
  },
  {
    id: 'src_digiato',
    name: 'دیجیاتو (Digiato)',
    feedUrl: 'https://digiato.com/feed',
    parserType: 'RSS',
    category: 'فناوری',
    updateIntervalMins: 60,
    isActive: true,
    lastFetchedUtc: Date.now() - 10 * 60 * 1000,
    articleCount: 65
  },
  {
    id: 'src_zoomit',
    name: 'زومیت (Zoomit)',
    feedUrl: 'https://www.zoomit.ir/feed/',
    parserType: 'ATOM',
    category: 'فناوری',
    updateIntervalMins: 60,
    isActive: true,
    lastFetchedUtc: Date.now() - 40 * 60 * 1000,
    articleCount: 54
  },
  {
    id: 'src_donya',
    name: 'دنیای اقتصاد (Donya-e-Eqtesad)',
    feedUrl: 'https://donya-e-eqtesad.com/rss',
    parserType: 'RSS',
    category: 'اقتصاد',
    updateIntervalMins: 30,
    isActive: true,
    lastFetchedUtc: Date.now() - 20 * 60 * 1000,
    articleCount: 88
  }
];

export const INITIAL_NEWS_ARTICLES: NewsArticle[] = [
  {
    id: 'art_101',
    sourceId: 'src_digiato',
    sourceName: 'دیجیاتو',
    title: 'رونمایی از نسل نوین تراشه‌های هوش مصنوعی محلی با تمرکز بر پردازش آفلاین',
    content: 'شرکت‌های فناوری نسل جدیدی از شتاب‌دهنده‌های لبه‌ای را معرفی کردند که قادر به اجرای مدل‌های چندمیلیاردی پارامتری بدون نیاز به اتصال شبکه است...',
    originalUrl: 'https://digiato.com/article/local-ai-chips-offline',
    publishedAtUtc: Date.now() - 2 * 3600 * 1000,
    rawSummary: 'توسعه تراشه‌های هوش مصنوعی آفلاین با شتاب‌دهنده NPU برای اجرای مدل‌های متنی بر روی دستگاه‌های هوشمند.',
    category: 'فناوری'
  },
  {
    id: 'art_102',
    sourceId: 'src_zoomit',
    sourceName: 'زومیت',
    title: 'معماری LiteRT و بهینه‌سازی مدل‌های خانواده Gemma بر روی سخت‌افزار موبایل',
    content: 'کتابخانه LiteRT-LM امکان استنتاج موازی مدل‌های E2B و E4B را با مصرف حافظه رم کمتر از ۲.۵ گیگابایت بر روی پردازنده‌های گرافیکی ولکان فراهم کرده است...',
    originalUrl: 'https://zoomit.ir/mobile/litert-gemma-optimization',
    publishedAtUtc: Date.now() - 3 * 3600 * 1000,
    rawSummary: 'آزمایش‌های بنچمارک روی گوشی‌های همراه حاکی از راندمان بالای مدل‌های سبک روی پلتفرم اندروید است.',
    category: 'فناوری'
  },
  {
    id: 'art_103',
    sourceId: 'src_isna',
    sourceName: 'ایسنا',
    title: 'پیشرفت پژوهشگران ایرانی در توسعه مدل‌های گفتار عصبی متن‌باز زبان فارسی',
    content: 'پژوهشگران هوش مصنوعی در دانشگاه‌های کشور با بهینه‌سازی معماری پایپر و انیکس، موفق به تولید صداهای طبیعی با دقت بالای تلفظ اعراب شدند...',
    originalUrl: 'https://isna.ir/news/persian-tts-piper-breakthrough',
    publishedAtUtc: Date.now() - 5 * 3600 * 1000,
    rawSummary: 'مدل مانای فارسی به عنوان یکی از سبک‌ترین و باکیفیت‌ترین وزن‌های TTS آفلاین معرفی شد.',
    category: 'علم و دانش'
  },
  {
    id: 'art_104',
    sourceId: 'src_donya',
    sourceName: 'دنیای اقتصاد',
    title: 'گزارش فصلی از روند سرمایه‌گذاری در فناوری‌های محتواساز و استودیوهای دیجیتال',
    content: 'بازار پادکست‌های تخصصی در خاورمیانه طی سال گذشته رشد ۲۸ درصدی را تجربه کرده و بیش از ۶۰ درصد سازندگان به سوی ابزارهای خودکارسازی میکس تمایل پیدا کرده‌اند...',
    originalUrl: 'https://donya-e-eqtesad.com/news/podcast-market-growth',
    publishedAtUtc: Date.now() - 6 * 3600 * 1000,
    rawSummary: 'سرمایه‌گذاری روی ابزارهای خودکارسازی ویرایش صدا و پادکست به بالاترین میزان خود رسیده است.',
    category: 'اقتصاد'
  }
];

export const INITIAL_NEWS_EVENTS: NewsEvent[] = [
  {
    id: 'event_001',
    canonicalTitle: 'جهش فناوری پردازش آفلاین هوش مصنوعی روی موبایل و رونمایی معماری LiteRT',
    eventSummary: 'دو پایگاه خبری دیجیاتو و زومیت گزارش دادند که پردازش هوش مصنوعی بدون اینترنت از طریق معماری بهینه‌شده LiteRT و پردازنده‌های عصبی تلفن همراه به بلوغ تجاری رسیده است.',
    category: 'فناوری',
    firstReportedUtc: Date.now() - 4 * 3600 * 1000,
    lastReportedUtc: Date.now() - 2 * 3600 * 1000,
    reportCount: 2,
    sources: [
      { sourceId: 'src_digiato', sourceName: 'دیجیاتو', articleId: 'art_101', articleTitle: 'رونمایی از نسل نوین تراشه‌های هوش مصنوعی محلی' },
      { sourceId: 'src_zoomit', sourceName: 'زومیت', articleId: 'art_102', articleTitle: 'معماری LiteRT و بهینه‌سازی مدل‌های خانواده Gemma' }
    ],
    primaryFacts: [
      'اجرای مدل‌های چندمیلیاردی بدون نیاز به اتصال دائم اینترنت.',
      'کاهش مصرف رم مدل Gemma 4 E2B به کمتر از ۲.۵ گیگابایت.',
      'پشتیبانی سخت‌افزاری از Vulkan GPU و NPU در سیستم‌عامل اندروید.'
    ],
    verifiedDifferentials: [
      'دیجیاتو بر روی سخت‌افزار تراشه‌های لبه‌ای تمرکز کرده است.',
      'زومیت بنچمارک نرم‌افزاری LiteRT-LM روی اندروید را تحلیل نموده است.'
    ],
    isSelected: true
  },
  {
    id: 'event_002',
    canonicalTitle: 'انتشار نسخه بهینه‌شده مدل صوتی عصبی مانای فارسی برای سیستم‌های مستقل',
    eventSummary: 'ایسنا از انتشار مدل پیشرفته تبدیل متن به گفتار آفلاین Piper با نام fa_IR-mana خبر داد که دقت چشمگیری در تلفظ اسامی خاص و اعداد فارسی دارد.',
    category: 'علم و دانش',
    firstReportedUtc: Date.now() - 5 * 3600 * 1000,
    lastReportedUtc: Date.now() - 5 * 3600 * 1000,
    reportCount: 1,
    sources: [
      { sourceId: 'src_isna', sourceName: 'ایسنا', articleId: 'art_103', articleTitle: 'پیشرفت پژوهشگران ایرانی در توسعه مدل‌های گفتار عصبی' }
    ],
    primaryFacts: [
      'مدل آفلاین با موتور اجرایی Sherpa-ONNX سازگار است.',
      'حجم فایل وزن‌ها حدود ۱۲۰ مگابایت است.',
      'نرمال‌سازی خودکار تقویم جلالی و تلفظ اعداد به حروف.'
    ],
    verifiedDifferentials: [
      'گزارش منحصراً توسط خبرگزاری ایسنا منتشر گردیده است.'
    ],
    isSelected: true
  },
  {
    id: 'event_003',
    canonicalTitle: 'رشد ۲۸ درصدی شنوندگان پادکست و تقاضای فزاینده برای ابزارهای تولید هوشمند',
    eventSummary: 'تحلیل دنیای اقتصاد نشان‌دهنده رونق اقتصاد تولید محتوای صوتی و ترجیح برنامه‌سازان به ابزارهای ویرایش چندترکه بومی است.',
    category: 'اقتصاد',
    firstReportedUtc: Date.now() - 6 * 3600 * 1000,
    lastReportedUtc: Date.now() - 6 * 3600 * 1000,
    reportCount: 1,
    sources: [
      { sourceId: 'src_donya', sourceName: 'دنیای اقتصاد', articleId: 'art_104', articleTitle: 'گزارش فصلی از روند سرمایه‌گذاری در فناوری‌های محتواساز' }
    ],
    primaryFacts: [
      'رشد سالانه ۲۸ درصدی مخاطبان پادکست در بازار منطقه.',
      'صرفه‌جویی ۷۰ درصدی در زمان تدوین با استفاده از داکینگ خودکار صدا.'
    ],
    verifiedDifferentials: [],
    isSelected: false
  }
];

export const INITIAL_AI_MODELS: AIModel[] = [
  {
    id: 'gemma-4-e2b-instruct',
    name: 'Gemma 4 E2B Instruct',
    version: '4.0-E2B',
    sizeGb: 2.4,
    descriptionFa: 'مدل سبک، سریع و کم‌مصرف گوگل برای نگارش ساختاریافته بولتن‌های خبری و گفتگوهای دونفره با سرعت بالا بر روی گوشی‌های همراه.',
    runtime: 'LiteRT-LM',
    supportedBackends: ['CPU', 'GPU_VULKAN', 'NPU'],
    selectedBackend: 'GPU_VULKAN',
    sha256Checksum: '8e1f5a9b7402c38d4e9a0f12c8b73a4d9e021a3f6c5b8e9d1a2c3b4e5f6a7b8c',
    isDownloaded: true,
    downloadProgress: 100,
    isActive: true,
    memoryUsageMb: 2150,
    recommendedFor: 'بولتن‌های خبری روزانه، خلاصه‌سازی و تولید دیالوگ‌های پرسرعت'
  },
  {
    id: 'gemma-4-e4b-instruct',
    name: 'Gemma 4 E4B Instruct',
    version: '4.0-E4B',
    sizeGb: 4.6,
    descriptionFa: 'مدل قدرتمند با عمق استدلال بالا و دایره واژگان غنی‌تر فارسی، مناسب برای تحلیل‌های کارشناسی عمیق و مقایسه چندجانبه گزارش‌ها.',
    runtime: 'LiteRT-LM',
    supportedBackends: ['CPU', 'GPU_VULKAN'],
    selectedBackend: 'CPU',
    sha256Checksum: 'a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8',
    isDownloaded: false,
    downloadProgress: 0,
    isActive: false,
    memoryUsageMb: 0,
    recommendedFor: 'پادکست‌های تحلیلی عمیق، مباحثات کارشناسی و تحلیل داده‌های اقتصادی'
  }
];

export const INITIAL_VOICE_PROFILES: VoiceProfile[] = [
  {
    id: 'voice_anchor_a',
    role: 'PRESENTER_A',
    titleFa: 'مجری الف (گوینده ارشد و راوی خبر)',
    characterDescriptionFa: 'لحن استوار، شمرده، مقتدر و استاندارد بخش خبری با گام صدایی بم و باوقار.',
    modelName: 'fa_IR-mana-medium (Profile: Anchor)',
    engineType: 'SHERPA_ONNX_PIPER',
    speechRate: 1.0,
    pitch: 0.95,
    isInstalled: true
  },
  {
    id: 'voice_analyst_b',
    role: 'PRESENTER_B',
    titleFa: 'مجری ب (کارشناس و تحلیل‌گر گفتگو)',
    characterDescriptionFa: 'لحن تحلیلی، صمیمی، پرسش‌گر و پویا با سرعت کلام منعطف و انتقال‌های طبیعی.',
    modelName: 'fa_IR-mana-medium (Profile: Analyst)',
    engineType: 'SHERPA_ONNX_PIPER',
    speechRate: 1.08,
    pitch: 1.15,
    isInstalled: true
  }
];

export const SAMPLE_SCRIPT_PROJECT: ScriptProject = {
  id: 'proj_daily_14050821',
  title: 'پادکست بررسی جهش هوش مصنوعی محلی و آینده پردازش ابری',
  programType: 'TWO_PRESENTERS',
  modelIdUsed: 'gemma-4-e2b-instruct',
  jalaliDateStr: '۲۱ آبان ۱۴۰۵',
  createdAtUtc: Date.now() - 3600 * 1000,
  updatedAtUtc: Date.now() - 600 * 1000,
  status: 'MIXED',
  formalityLevel: 'نیمه‌رسمی تحلیلی',
  analysisDepth: 'عمیق و تحلیلی',
  selectedEventIds: ['event_001', 'event_002'],
  segments: [
    {
      id: 'seg_01',
      segmentIndex: 1,
      type: 'INTRO',
      speaker: 'PRESENTER_A',
      textFa: 'به استودیوی پارس‌پادکست خوش آمدید. امروز سه‌شنبه بیست و یکم آبان‌ماه سال هزار و چهارصد و پنج، با بررسی مهم‌ترین تحولات فناوری در خدمت شما هستیم.',
      estimatedDurationSec: 14,
      isAudioGenerated: true,
      sourceIds: []
    },
    {
      id: 'seg_02',
      segmentIndex: 2,
      type: 'NEWS',
      speaker: 'PRESENTER_A',
      textFa: 'در سرخط خبرها: پردازش هوش مصنوعی بدون نیاز به اینترنت با عرضه معماری بهینه‌شده لایت‌آرتی وارد مرحله جدیدی شده است. گزارش‌ها حاکی است مدل‌های سری گما چهار اکنون بر روی پردازنده‌های گرافیکی موبایل با مصرف رم کمتر از دو و نیم گیگابایت اجرا می‌شوند.',
      estimatedDurationSec: 22,
      isAudioGenerated: true,
      sourceIds: ['event_001']
    },
    {
      id: 'seg_03',
      segmentIndex: 3,
      type: 'DISCUSSION',
      speaker: 'PRESENTER_B',
      textFa: 'دقیقاً همین‌طور است. اهمیت این دستاورد در حفظ صددرصدی حریم خصوصی و استقلال استودیوهاست. اکنون دستگاه بدون ارسال حتی یک بایت داده به سرورهای ابری، متن برنامه را تدوین و صدا را مستقیماً سنتز می‌کند.',
      estimatedDurationSec: 18,
      isAudioGenerated: true,
      sourceIds: ['event_001']
    },
    {
      id: 'seg_04',
      segmentIndex: 4,
      type: 'NEWS',
      speaker: 'PRESENTER_A',
      textFa: 'همچنین پژوهشگران از نسخه جدید مدل صوتی مانای فارسی با موتور پردازش شرپا اونیکس پرده‌برداری کرده‌اند که تلفظ اعداد، تاریخ‌های تقویم شمسی و عبارات خاص را با دقتی بی‌سابقه انجام می‌دهد.',
      estimatedDurationSec: 20,
      isAudioGenerated: true,
      sourceIds: ['event_002']
    },
    {
      id: 'seg_05',
      segmentIndex: 5,
      type: 'OUTRO',
      speaker: 'PRESENTER_B',
      textFa: 'با تشکر از همراهی شما با این اپیزود از پارس‌پادکست. فایل کامل صوتی و فید آر‌اس‌اس در بخش انتشار در دسترس شما قرار دارد. تا برنامه‌ای دیگر، خدانگهدار.',
      estimatedDurationSec: 15,
      isAudioGenerated: true,
      sourceIds: []
    }
  ]
};

export const INITIAL_AUDIO_TRACKS: AudioTrack[] = [
  { id: 'track_intro', type: 'INTRO', nameFa: 'اینترو و آرم استودیو', volume: 0.9, isMuted: false, isSolo: false, duckingEnabled: false, pan: 0 },
  { id: 'track_anchor', type: 'PRESENTER_A', nameFa: 'مجری الف (گوینده اصلی)', volume: 1.1, isMuted: false, isSolo: false, duckingEnabled: false, pan: -0.1 },
  { id: 'track_analyst', type: 'PRESENTER_B', nameFa: 'مجری ب (کارشناس و تحلیل)', volume: 1.1, isMuted: false, isSolo: false, duckingEnabled: false, pan: 0.1 },
  { id: 'track_bgm', type: 'BGM', nameFa: 'موسیقی پس‌زمینه (داکینگ فعال)', volume: 0.35, isMuted: false, isSolo: false, duckingEnabled: true, pan: 0 },
  { id: 'track_sfx', type: 'SFX', nameFa: 'افکت‌های صوتی و انتقال', volume: 0.8, isMuted: false, isSolo: false, duckingEnabled: false, pan: 0 },
  { id: 'track_outro', type: 'OUTRO', nameFa: 'اوترو و تیتراژ پایانی', volume: 0.9, isMuted: false, isSolo: false, duckingEnabled: false, pan: 0 }
];

export const INITIAL_EPISODES: PodcastEpisode[] = [
  {
    id: 'ep_98b7-4f21-8841-a129',
    projectId: 'proj_daily_14050821',
    title: 'اپیزود شماره ۴۲: استقلال پردازش هوش مصنوعی و سنتز بومی زبان فارسی',
    descriptionFa: 'در این اپیزود به بررسی رونمایی از تراشه‌های پردازش آفلاین، معماری LiteRT برای مدل‌های خانواده Gemma و مدل صوتی مانا برای گویندگی فارسی می‌پردازیم.',
    episodeNumber: 42,
    seasonNumber: 1,
    episodeType: 'full',
    jalaliPubDate: '۲۱ آبان ۱۴۰۵',
    pubDateRfc822: 'Tue, 10 Nov 2026 21:00:00 +0330',
    enclosureUrl: 'https://archive.parspodcast.ir/episodes/ep42_ai_studio_master.mp3',
    fileSizeBytes: 24589210,
    mimeType: 'audio/mpeg',
    durationSeconds: 534,
    category: 'Technology',
    artworkUrl: 'https://archive.parspodcast.ir/art/ep42_cover.jpg',
    publishingStatus: 'PUBLISHED'
  },
  {
    id: 'ep_110a-32e9-7712-c451',
    projectId: 'proj_prev_14050814',
    title: 'اپیزود شماره ۴۱: اقتصاد دیجیتال و تحول استودیوهای تولید محتوا',
    descriptionFa: 'بررسی رشد سرمایه‌گذاری بر روی پادکست‌های تخصصی خاورمیانه و صرفه‌جویی زمان با ویرایشگرهای چندترکه بومی.',
    episodeNumber: 41,
    seasonNumber: 1,
    episodeType: 'full',
    jalaliPubDate: '۱۴ آبان ۱۴۰۵',
    pubDateRfc822: 'Tue, 03 Nov 2026 21:00:00 +0330',
    enclosureUrl: 'https://archive.parspodcast.ir/episodes/ep41_digital_economy.mp3',
    fileSizeBytes: 21940120,
    mimeType: 'audio/mpeg',
    durationSeconds: 480,
    category: 'Business',
    artworkUrl: 'https://archive.parspodcast.ir/art/ep41_cover.jpg',
    publishingStatus: 'PUBLISHED'
  }
];
