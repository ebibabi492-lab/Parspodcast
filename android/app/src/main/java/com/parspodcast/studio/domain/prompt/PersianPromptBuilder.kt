package com.parspodcast.studio.domain.prompt

import com.parspodcast.studio.data.local.entities.NewsArticleEntity

object PersianPromptBuilder {

    private const val SYSTEM_BASE = """
شما مغز متفکر و نویسنده ارشد استودیوی پادکست «پارس‌پادکست» هستید.
زبان خروجی شما منحصراً زبان فارسی معیار، شیوا، حرفه‌ای، برودکست و روان است.
اصول غیرقابل تخطی:
۱. حفظ دقیق فکت‌ها: هرگز فکت‌ها، آمارها، نقل‌قول‌ها یا نام‌های غیرواقعی ابداع نکنید.
۲. تمایز قطعیت و عدم‌قطعیت: در مواردی که اطلاعات کامل در متن خبر وجود ندارد، صراحتاً به محدودیت اطلاعات اشاره کنید و از خود چیزی اضافه نکنید.
۳. نگارش طبیعی و گفتاری: جملات باید برای خوانش صوتی و تبدیل به گفتار (TTS) کاملاً طبیعی، با ضرب‌آهنگ متوازن و علائم نگارشی استاندارد (نقطه، کاما و مکث) باشند.
۴. عدم استفاده از نمادها یا ایموجی‌های تزئینی در متن خوانش صوتی.
"""

    fun buildSingleHostScriptPrompt(
        title: String,
        targetDurationMinutes: Int,
        tone: String,
        articles: List<NewsArticleEntity>
    ): Pair<String, String> {
        val systemInstruction = """
$SYSTEM_BASE
فرمت برنامه: بولتن تک‌گوینده (SINGLE_HOST).
گوینده: فقط یک گوینده ارشد با شناسه «HOST_A».
ساختار خروجی مورد انتظار: یک سند JSON با ساختار مشخص شامل مقدمه (introduction)، بخش‌های خبری (sections)، ترنزیشن‌ها و نتیجه‌گیری (conclusion).
""".trimIndent()

        val articlesContext = buildArticlesContext(articles)

        val prompt = """
عنوان برنامه: $title
مدت زمان هدف: $targetDurationMinutes دقیقه
لحن نگارش: $tone

منابع خبری در دسترس:
$articlesContext

دستور کار:
یک فیلم‌نامه کامل بولتن خبری فارسی برای گوینده «HOST_A» تدوین کنید. ساختار خروجی باید الزاماً یک شیء JSON با ساختار زیر باشد:
{
  "episodeTitle": "$title",
  "language": "fa-IR",
  "format": "single_host",
  "introduction": "متن خوش‌آمدگویی و سرخط خبرها...",
  "sections": [
    {
      "id": "section_1",
      "title": "عنوان بخش",
      "speaker": "HOST_A",
      "content": "متن مشروح بخش خبری...",
      "sourceIds": ["شناسه منبع در صورت وجود"]
    }
  ],
  "conclusion": "متن جمع‌بندی و خداحافظی پایانی...",
  "estimatedDurationMinutes": $targetDurationMinutes
}
فقط و فقط کد JSON معتبر خروجی دهید.
""".trimIndent()

        return Pair(systemInstruction, prompt)
    }

    fun buildDualHostScriptPrompt(
        title: String,
        targetDurationMinutes: Int,
        tone: String,
        articles: List<NewsArticleEntity>
    ): Pair<String, String> {
        val systemInstruction = """
$SYSTEM_BASE
فرمت برنامه: پادکست گفتگوی دونفره (DUAL_HOST).
گویندگان:
- مجری الف (HOST_A): مجری اصلی و راوی خبرها؛ لحن استوار، رسمی و هدایت‌کننده جریان برنامه.
- مجری ب (HOST_B): کارشناس و تحلیل‌گر موضوعات؛ لحن تحلیلی، صمیمی، پرسش‌گر و مکمل بحث.
دیالوگ‌ها باید زنده، پویا، بدون تک‌گویی‌های طولانی و با انتقال‌های منطقی باشند.
""".trimIndent()

        val articlesContext = buildArticlesContext(articles)

        val prompt = """
عنوان پادکست: $title
مدت زمان هدف: $targetDurationMinutes دقیقه
لحن گفتگو: $tone

منابع خبری در دسترس:
$articlesContext

دستور کار:
یک پادکست گفتگوی دونفره طبیعی و عمیق بین «HOST_A» و «HOST_B» بنویسید. خروجی باید یک شیء JSON به فرمت زیر باشد:
{
  "episodeTitle": "$title",
  "language": "fa-IR",
  "format": "dual_host",
  "introduction": "خوش‌آمدگویی و دیالوگ کوتاه آغازین دو مجری...",
  "sections": [
    {
      "id": "section_1",
      "title": "عنوان موضوع",
      "speaker": "HOST_A",
      "content": "متن دیالوگ مجری الف...",
      "sourceIds": ["شناسه منبع"]
    },
    {
      "id": "section_2",
      "title": "تحلیل موضوع",
      "speaker": "HOST_B",
      "content": "پاسخ و تحلیل مجری ب...",
      "sourceIds": ["شناسه منبع"]
    }
  ],
  "conclusion": "دیالوگ جمع‌بندی پایانی و بدرود دو مجری...",
  "estimatedDurationMinutes": $targetDurationMinutes
}
فقط و فقط کد JSON معتبر خروجی دهید.
""".trimIndent()

        return Pair(systemInstruction, prompt)
    }

    fun buildRewritePrompt(text: String, instruction: String): Pair<String, String> {
        val system = "$SYSTEM_BASE\nوظیفه: بازنویسی و بهبود متن فارسی بر اساس دستورالعمل کاربر."
        val prompt = """
دستور بازنویسی: $instruction
متن اصلی:
$text

متن بازنویسی‌شده فارسی را بدون هیچ توضیح اضافی خروجی دهید:
""".trimIndent()
        return Pair(system, prompt)
    }

    fun buildSummarizePrompt(text: String): Pair<String, String> {
        val system = "$SYSTEM_BASE\nوظیفه: خلاصه‌سازی دقیق و فکت‌محور اخبار بدون حذف نکات کلیدی."
        val prompt = """
متن زیر را در ۲ تا ۳ جمله کوتاه، رسا و مناسب خوانش پادکستی خلاصه کنید:
$text
""".trimIndent()
        return Pair(system, prompt)
    }

    fun buildExpandPrompt(text: String, topic: String): Pair<String, String> {
        val system = "$SYSTEM_BASE\nوظیفه: گسترش و تبیین عمیق‌تر موضوع با اضافه کردن زمینه تاریخی یا تحلیلی منطقی، بدون ابداع فکت‌های دروغین."
        val prompt = """
موضوع: $topic
متن پایه:
$text

لطفاً این متن را با توضیحات تکمیلی و مثال‌های تحلیلی مرتبط برای پادکست گسترش دهید:
""".trimIndent()
        return Pair(system, prompt)
    }

    fun buildToneChangePrompt(text: String, targetTone: String): Pair<String, String> {
        val system = "$SYSTEM_BASE\nوظیفه: تغییر لحن نگارش متن فارسی."
        val prompt = """
لحن درخواستی: $targetTone
متن اصلی:
$text

متن را با حفظ دقیق فکت‌ها به لحن درخواستی تبدیل کنید:
""".trimIndent()
        return Pair(system, prompt)
    }

    fun buildTransitionPrompt(previousTopic: String, nextTopic: String, isDualHost: Boolean): Pair<String, String> {
        val system = "$SYSTEM_BASE\nوظیفه: نگارش یک ترنزیشن (پل ارتباطی) نرم، روان و جذاب بین دو موضوع خبری."
        val prompt = """
موضوع قبلی: $previousTopic
موضوع بعدی: $nextTopic
حالت برنامه: ${if (isDualHost) "گفتگوی دوگوینده" else "تک‌گوینده"}

یک جمله یا پاراگراف کوتاه انتقالی برای هدایت شنونده به موضوع بعدی بنویسید:
""".trimIndent()
        return Pair(system, prompt)
    }

    fun buildTitleSuggestionsPrompt(topic: String, summary: String): Pair<String, String> {
        val system = "$SYSTEM_BASE\nوظیفه: پیشنهاد عناوین جذاب، شنیدنی و استاندارد برای اپیزود پادکست."
        val prompt = """
موضوع کلی: $topic
خلاصه محتوا: $summary

۵ عنوان جذاب و شنیدنی فارسی برای این اپیزود پیشنهاد دهید (هر کدام در یک خط):
""".trimIndent()
        return Pair(system, prompt)
    }

    private fun buildArticlesContext(articles: List<NewsArticleEntity>): String {
        if (articles.isEmpty()) return "هیچ خبر خاصی انتخاب نشده است. لطفاً بر اساس دانش عمومی پیرامون موضوع سناریو را تدوین کنید."

        return articles.mapIndexed { idx, art ->
            """
[خبر شماره ${idx + 1} - شناسه: ${art.id}]
عنوان: ${art.title}
منبع: ${art.sourceId}
خلاصه: ${art.rawSummary.ifEmpty { art.content.take(300) }}
""".trimIndent()
        }.joinToString("\n\n")
    }
}
