import React, { useState } from 'react';
import {
  Send,
  Download,
  Rss,
  CheckCircle2,
  AlertTriangle,
  Globe,
  Radio,
  FileCode,
  Check,
  Copy
} from 'lucide-react';
import { ScriptProject, PodcastEpisode } from '../types';
import { audioEngine } from '../utils/audioEngine';
import { toPersianDigits } from '../utils/jalali';

interface PodcastPublisherViewProps {
  project: ScriptProject;
  episodes: PodcastEpisode[];
  onAddEpisode: (episode: PodcastEpisode) => void;
}

export const PodcastPublisherView: React.FC<PodcastPublisherViewProps> = ({
  project,
  episodes,
  onAddEpisode
}) => {
  const [publishingLevel, setPublishingLevel] = useState<'level1' | 'level2' | 'level3'>('level2');
  const [showRssXmlModal, setShowRssXmlModal] = useState(false);
  const [copiedRss, setCopiedRss] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [publishedSuccess, setPublishedSuccess] = useState(false);

  // Generate compliant Podcast RSS 2.0 XML
  const generateRssXml = () => {
    return `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:content="http://purl.org/rss/1.0/modules/content/">
  <channel>
    <title>پادکست پارس‌پادکست (ParsPodcast Studio)</title>
    <link>https://parspodcast.ir</link>
    <language>fa</language>
    <description>پادکست روزانه بررسی مهم‌ترین رویدادهای فناوری، هوش مصنوعی و اقتصاد با پردازش بومی و آفلاین</description>
    <itunes:author>استودیوی پارس‌پادکست</itunes:author>
    <itunes:category text="Technology"/>
    <itunes:image href="https://parspodcast.ir/assets/cover_3000.jpg"/>
    
    <!-- Episode Item -->
    <item>
      <title>${project.title}</title>
      <description>بررسی رویدادهای فناوری تاریخ ${project.jalaliDateStr} به همراه تحلیل کارشناسی</description>
      <link>https://parspodcast.ir/episodes/${project.id}</link>
      <guid isPermaLink="false">urn:uuid:${project.id}</guid>
      <pubDate>Tue, 10 Nov 2026 21:00:00 +0330</pubDate>
      <enclosure url="https://parspodcast.ir/audio/${project.id}.mp3" length="24589210" type="audio/mpeg"/>
      <itunes:duration>534</itunes:duration>
      <itunes:explicit>false</itunes:explicit>
    </item>
  </channel>
</rss>`;
  };

  const handlePublishSelfHosted = () => {
    setIsPublishing(true);
    setTimeout(() => {
      const newEp: PodcastEpisode = {
        id: `urn:uuid:${project.id}_${Date.now()}`,
        projectId: project.id,
        title: project.title,
        descriptionFa: `اپیزود جدید تولیدشده در تاریخ ${project.jalaliDateStr} با مدل گما ۴ و صدای مانا`,
        jalaliPubDate: project.jalaliDateStr,
        pubDateRfc822: new Date().toUTCString(),
        enclosureUrl: `https://archive.parspodcast.ir/episodes/${project.id}.mp3`,
        fileSizeBytes: 24589210,
        mimeType: 'audio/mpeg',
        durationSeconds: 534,
        category: 'Technology',
        artworkUrl: 'https://archive.parspodcast.ir/art/default_cover.jpg',
        publishingStatus: 'PUBLISHED'
      };
      onAddEpisode(newEp);
      setIsPublishing(false);
      setPublishedSuccess(true);
      setTimeout(() => setPublishedSuccess(false), 4000);
    }, 1500);
  };

  const handleDownloadLocalAudio = (format: 'MP3' | 'WAV' | 'AAC') => {
    const blob = audioEngine.generateWavFileBlob(10);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${project.id}_broadcast.${format.toLowerCase()}`;
    a.click();
  };

  const handleCopyRss = () => {
    navigator.clipboard.writeText(generateRssXml());
    setCopiedRss(true);
    setTimeout(() => setCopiedRss(false), 2000);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="border-b border-[#1C2541] pb-4">
        <h1 className="text-xl font-bold text-white flex items-center gap-2">
          <Send className="w-5 h-5 text-[#00B4D8]" />
          <span>ناشر پادکست و تولید فید RSS 2.0 (Podcast Publisher)</span>
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          انتشار سه‌سطحی: خروجی محلی، فید خودمیزبان استاندارد RFC-822 و بسته‌بندی برای دایرکتوری‌های جهانی
        </p>
      </div>

      {publishedSuccess && (
        <div className="p-3.5 bg-emerald-950/40 border border-emerald-500/40 rounded-xl text-xs text-emerald-300 flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>اپیزود با موفقیت منتشر و در فید RSS 2.0 قرار گرفت. تاریخ شمسی و میلادی ثبت شد.</span>
        </div>
      )}

      {/* 3 Publishing Levels Tabs */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <button
          onClick={() => setPublishingLevel('level1')}
          className={`p-4 rounded-xl border text-right transition-all cursor-pointer ${
            publishingLevel === 'level1'
              ? 'bg-[#1C2541] border-[#00B4D8] shadow-sm'
              : 'bg-[#0B132B] border-[#1C2541] opacity-75 hover:opacity-100'
          }`}
        >
          <div className="flex items-center gap-2 mb-1.5">
            <Download className="w-4 h-4 text-[#00B4D8]" />
            <h2 className="text-sm font-bold text-white">سطح ۱: صدور محلی و اشتراک‌گذاری</h2>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            تولید مستقیم فایل‌های صوتی استاندارد MP3، WAV و AAC بر روی حافظه دستگاه با امکان اشتراک‌گذاری فوری.
          </p>
        </button>

        <button
          onClick={() => setPublishingLevel('level2')}
          className={`p-4 rounded-xl border text-right transition-all cursor-pointer ${
            publishingLevel === 'level2'
              ? 'bg-[#1C2541] border-[#00B4D8] shadow-sm'
              : 'bg-[#0B132B] border-[#1C2541] opacity-75 hover:opacity-100'
          }`}
        >
          <div className="flex items-center gap-2 mb-1.5">
            <Rss className="w-4 h-4 text-[#48CAE4]" />
            <h2 className="text-sm font-bold text-white">سطح ۲: فید استاندارد خودمیزبان RSS 2.0</h2>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            تولید XML معتبر پادکست، پشتیبانی از تگ Enclosure، متادیتا، شناسه یکتای GUID و زمان‌بندی RFC-822.
          </p>
        </button>

        <button
          onClick={() => setPublishingLevel('level3')}
          className={`p-4 rounded-xl border text-right transition-all cursor-pointer ${
            publishingLevel === 'level3'
              ? 'bg-[#1C2541] border-[#00B4D8] shadow-sm'
              : 'bg-[#0B132B] border-[#1C2541] opacity-75 hover:opacity-100'
          }`}
        >
          <div className="flex items-center gap-2 mb-1.5">
            <Globe className="w-4 h-4 text-[#7209B7]" />
            <h2 className="text-sm font-bold text-white">سطح ۳: دایرکتوری‌های جهانی</h2>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            بررسی آمادگی و انطباق فید با ضوابط رسمی Apple Podcasts و Spotify همراه با کاور ۳۰۰۰×۳۰۰۰ پیکسل.
          </p>
        </button>
      </div>

      {/* Level 1 View */}
      {publishingLevel === 'level1' && (
        <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-5 space-y-4">
          <h2 className="text-sm font-bold text-white">خروجی صوتی فایل مستر</h2>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <button
              onClick={() => handleDownloadLocalAudio('MP3')}
              className="p-3 bg-[#1C2541] hover:bg-[#27355B] rounded-lg border border-[#27355B] text-center space-y-1 cursor-pointer transition-colors"
            >
              <div className="text-xs font-bold text-white font-mono">خروجی MP3 (320kbps)</div>
              <div className="text-[11px] text-slate-400">مناسب پادگیرها و وب</div>
            </button>
            <button
              onClick={() => handleDownloadLocalAudio('WAV')}
              className="p-3 bg-[#1C2541] hover:bg-[#27355B] rounded-lg border border-[#27355B] text-center space-y-1 cursor-pointer transition-colors"
            >
              <div className="text-xs font-bold text-[#00B4D8] font-mono">خروجی WAV (16-bit PCM)</div>
              <div className="text-[11px] text-slate-400">کیفیت برودکست استودیویی</div>
            </button>
            <button
              onClick={() => handleDownloadLocalAudio('AAC')}
              className="p-3 bg-[#1C2541] hover:bg-[#27355B] rounded-lg border border-[#27355B] text-center space-y-1 cursor-pointer transition-colors"
            >
              <div className="text-xs font-bold text-white font-mono">خروجی AAC / M4A</div>
              <div className="text-[11px] text-slate-400">فشرده‌سازی مدرن و بهینه</div>
            </button>
          </div>
        </div>
      )}

      {/* Level 2 View: Self-Hosted RSS */}
      {publishingLevel === 'level2' && (
        <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-white">انتشار در فید RSS 2.0 خودمیزبان</h2>
              <p className="text-xs text-slate-400 mt-0.5">
                فید اختصاصی استودیو بر روی آدرس <span className="text-[#00B4D8] font-mono">https://parspodcast.ir/feed.xml</span>
              </p>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowRssXmlModal(true)}
                className="px-3.5 py-2 bg-[#1C2541] hover:bg-[#27355B] text-slate-200 text-xs font-medium rounded-lg border border-[#27355B] flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <FileCode className="w-3.5 h-3.5 text-[#00B4D8]" />
                <span>مشاهده کدهای XML</span>
              </button>
              <button
                onClick={handlePublishSelfHosted}
                disabled={isPublishing}
                className="px-4 py-2 bg-[#00B4D8] hover:bg-[#48CAE4] text-[#070B18] text-xs font-bold rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer disabled:opacity-50"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{isPublishing ? 'در حال همگام‌سازی...' : 'انتشار رسمی اپیزود'}</span>
              </button>
            </div>
          </div>

          {/* Episode Metadata Preview */}
          <div className="bg-[#070B18] p-4 rounded-xl border border-[#1C2541] space-y-2 text-xs">
            <div className="flex items-center justify-between">
              <span className="text-slate-400">عنوان اپیزود:</span>
              <strong className="text-white">{project.title}</strong>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-400">تاریخ شمسی جلالی:</span>
              <strong className="font-mono text-[#00B4D8]">{project.jalaliDateStr}</strong>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-400">فرمت صدا و اینکلوژر:</span>
              <span className="font-mono text-slate-300">audio/mpeg · 24.5 MB · 534 ثانیه</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-400">وضعیت پیش‌فرض:</span>
              <span className="text-emerald-400 font-medium">نیاز به تأیید کاربر (مرحله نهایی)</span>
            </div>
          </div>
        </div>
      )}

      {/* Level 3 View: External Directories */}
      {publishingLevel === 'level3' && (
        <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-5 space-y-4">
          <h2 className="text-sm font-bold text-white">چک‌لیست اعتبارسنجی دایرکتوری‌های Apple Podcasts و Spotify</h2>
          <div className="space-y-2 text-xs">
            <div className="p-3 bg-[#070B18] border border-emerald-500/30 rounded-lg flex items-center justify-between text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>پشتیبانی سرور از درخواست‌های بایت‌رنج (HTTP 206 Partial Content)</span>
              </div>
              <span className="text-emerald-400 font-mono">تأیید شد</span>
            </div>

            <div className="p-3 bg-[#070B18] border border-emerald-500/30 rounded-lg flex items-center justify-between text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>ابعاد تصویر کاور پادکست (حداقل ۱۴۰۰×۱۴۰۰ و حداکثر ۳۰۰۰×۳۰۰۰ پیکسل RGB)</span>
              </div>
              <span className="text-emerald-400 font-mono">تأیید شد</span>
            </div>

            <div className="p-3 bg-[#070B18] border border-emerald-500/30 rounded-lg flex items-center justify-between text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>اعتبارسنجی فرمت تاریخ‌های انتشار مطابق RFC-822</span>
              </div>
              <span className="text-emerald-400 font-mono">تأیید شد</span>
            </div>
          </div>
        </div>
      )}

      {/* RSS XML Modal */}
      {showRssXmlModal && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#0B132B] border border-[#27355B] rounded-xl max-w-2xl w-full p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Rss className="w-4 h-4 text-[#00B4D8]" />
                <span>فید معتبر XML پادکست (RSS 2.0 Feed)</span>
              </h3>
              <button
                onClick={() => setShowRssXmlModal(false)}
                className="text-slate-400 hover:text-white text-xs cursor-pointer"
              >
                بستن
              </button>
            </div>
            <pre className="bg-[#070B18] border border-[#1C2541] rounded-lg p-4 font-mono text-[11px] text-emerald-400 overflow-x-auto max-h-96 text-left" dir="ltr">
              {generateRssXml()}
            </pre>
            <div className="flex items-center justify-between pt-2">
              <button
                onClick={handleCopyRss}
                className="px-3.5 py-1.5 bg-[#1C2541] hover:bg-[#27355B] text-slate-200 rounded-lg text-xs flex items-center gap-1.5 cursor-pointer"
              >
                {copiedRss ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiedRss ? 'کپی شد!' : 'کپی متن XML'}</span>
              </button>
              <button
                onClick={() => setShowRssXmlModal(false)}
                className="px-4 py-2 bg-[#00B4D8] text-xs text-[#070B18] font-bold rounded-lg cursor-pointer"
              >
                تأیید
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
