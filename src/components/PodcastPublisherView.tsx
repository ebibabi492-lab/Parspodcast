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
  Copy,
  Plus,
  Edit3,
  Trash2,
  ExternalLink,
  ShieldCheck,
  Server,
  FolderArchive,
  Music2,
  FileAudio
} from 'lucide-react';
import { ScriptProject, PodcastEpisode, PodcastShow, HostingConfig } from '../types';
import { audioEngine } from '../utils/audioEngine';
import { toPersianDigits } from '../utils/jalali';

interface PodcastPublisherViewProps {
  project: ScriptProject;
  episodes: PodcastEpisode[];
  onAddEpisode: (episode: PodcastEpisode) => void;
}

export const PodcastPublisherView: React.FC<PodcastPublisherViewProps> = ({
  project,
  episodes: initialEpisodes,
  onAddEpisode
}) => {
  // Local state for interactive prototype
  const [shows, setShows] = useState<PodcastShow[]>([
    {
      id: 'default_show',
      title: 'پارس‌پادکست (ParsPodcast)',
      subtitle: 'فناوری، هوش مصنوعی و جامعه',
      description: 'پادکست تخصصی تحلیل اخبار و رویدادهای فناوری با پردازش صوت روی دستگاه و مدل محلی گما',
      author: 'استودیوی پارس‌پادکست',
      ownerName: 'پارس‌پادکست',
      ownerEmail: 'studio@parspodcast.ir',
      websiteUrl: 'https://parspodcast.ir',
      language: 'fa',
      category: 'News',
      subcategory: 'Tech News',
      copyright: 'تمامی حقوق محفوظ است © ۱۴۰۵',
      explicitContent: false,
      showType: 'episodic',
      publicFeedUrl: 'https://parspodcast.ir/feed.xml',
      artworkRemoteUrl: 'https://parspodcast.ir/cover.jpg'
    }
  ]);
  const [selectedShowId, setSelectedShowId] = useState('default_show');
  const selectedShow = shows.find(s => s.id === selectedShowId) || shows[0];

  const [episodes, setEpisodes] = useState<PodcastEpisode[]>([
    {
      id: 'ep_1',
      projectId: project.id,
      showId: 'default_show',
      title: 'اپیزود ۴۳: استقلال پردازش هوش مصنوعی محلی در پادکست‌های فارسی',
      subtitle: 'بررسی اجرای مدل‌های خانواده Gemma 4 و معماری LiteRT بدون اتصال به اینترنت',
      descriptionFa: 'بررسی جامع اجرای مدل‌های زبانی سبک و موتور سنتز گفتار Sherpa-ONNX به صورت ۱۰۰٪ آفلاین بر روی دستگاه کاربر.',
      summary: 'تحلیل داده‌های آماری رشد شنوندگان محتوای صوتی مستقل',
      episodeNumber: 43,
      seasonNumber: 1,
      episodeType: 'full',
      author: 'استودیوی پارس‌پادکست',
      keywords: 'پادکست, هوش مصنوعی, گما ۴, آفلاین',
      jalaliPubDate: '۲۱ آبان ۱۴۰۵',
      pubDateRfc822: 'Tue, 10 Nov 2026 21:00:00 GMT',
      enclosureUrl: 'https://media.parspodcast.ir/podcasts/ep43.m4a',
      fileSizeBytes: 18450120,
      mimeType: 'audio/mp4',
      durationSeconds: 534,
      category: 'News',
      artworkUrl: 'https://parspodcast.ir/cover.jpg',
      publishingStatus: 'PUBLISHED'
    },
    ...initialEpisodes
  ]);

  const [activeTab, setActiveTab] = useState<'episodes' | 'xml' | 'validator' | 'hosting' | 'export'>('episodes');
  const [copiedRss, setCopiedRss] = useState(false);
  const [copiedFeedUrl, setCopiedFeedUrl] = useState(false);

  // Modals & workflows
  const [showEditShowModal, setShowEditShowModal] = useState(false);
  const [showNewEpModal, setShowNewEpModal] = useState(false);
  const [showPrepareAudioModal, setShowPrepareAudioModal] = useState<PodcastEpisode | null>(null);
  const [showWebDavPublishModal, setShowWebDavPublishModal] = useState<PodcastEpisode | null>(null);
  const [isPublishing, setIsPublishing] = useState(false);
  const [publishingStep, setPublishingStep] = useState('');
  const [publishingProgress, setPublishingProgress] = useState(0);

  // WebDAV Configuration state
  const [webDavConfig, setWebDavConfig] = useState<HostingConfig>({
    id: 'cfg_1',
    name: 'سرور اختصاصی WebDAV (HTTPS)',
    providerType: 'WEBDAV',
    serverUrl: 'https://dav.parspodcast.ir/podcasts/',
    remoteBasePath: '/podcasts/parspodcast/',
    publicBaseUrl: 'https://media.parspodcast.ir/podcasts/parspodcast/',
    username: 'studio_publisher',
    isDefault: true
  });
  const [webDavPassword, setWebDavPassword] = useState('••••••••••••');
  const [connectionTestResult, setConnectionTestResult] = useState<string | null>(null);

  // Generate compliant Podcast RSS 2.0 XML
  const generateRssXml = () => {
    const pubDate = new Date().toUTCString();
    return `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:atom="http://www.w3.org/2005/Atom" xmlns:content="http://purl.org/rss/1.0/modules/content/">
  <channel>
    <title>${selectedShow.title}</title>
    <link>${selectedShow.websiteUrl || 'https://parspodcast.ir'}</link>
    <description>${selectedShow.description}</description>
    <language>${selectedShow.language || 'fa'}</language>
    <copyright>${selectedShow.copyright}</copyright>
    <generator>ParsPodcast Studio Android v1.0 (LiteRT-LM &amp; On-Device Persian AI)</generator>
    <pubDate>${pubDate}</pubDate>
    <lastBuildDate>${pubDate}</lastBuildDate>
    <atom:link href="${selectedShow.publicFeedUrl || 'https://parspodcast.ir/feed.xml'}" rel="self" type="application/rss+xml"/>
    
    <image>
      <url>${selectedShow.artworkRemoteUrl || 'https://parspodcast.ir/cover.jpg'}</url>
      <title>${selectedShow.title}</title>
      <link>${selectedShow.websiteUrl || 'https://parspodcast.ir'}</link>
    </image>
    
    <itunes:author>${selectedShow.author}</itunes:author>
    <itunes:summary>${selectedShow.subtitle}</itunes:summary>
    <itunes:type>${selectedShow.showType}</itunes:type>
    <itunes:explicit>${selectedShow.explicitContent ? 'true' : 'false'}</itunes:explicit>
    <itunes:image href="${selectedShow.artworkRemoteUrl || 'https://parspodcast.ir/cover.jpg'}"/>
    <itunes:owner>
      <itunes:name>${selectedShow.ownerName}</itunes:name>
      <itunes:email>${selectedShow.ownerEmail || 'studio@parspodcast.ir'}</itunes:email>
    </itunes:owner>
    <itunes:category text="${selectedShow.category}">
      ${selectedShow.subcategory ? `<itunes:category text="${selectedShow.subcategory}"/>` : ''}
    </itunes:category>

    ${episodes.map(ep => `
    <item>
      <title>${ep.title}</title>
      <description>${ep.descriptionFa}</description>
      <link>${selectedShow.websiteUrl}/episodes/${ep.id}</link>
      <guid isPermaLink="false">${ep.id}</guid>
      <pubDate>${ep.pubDateRfc822}</pubDate>
      <enclosure url="${ep.enclosureUrl}" length="${ep.fileSizeBytes}" type="${ep.mimeType}"/>
      <itunes:title>${ep.title}</itunes:title>
      <itunes:author>${ep.author || selectedShow.author}</itunes:author>
      <itunes:summary>${ep.summary || ep.descriptionFa}</itunes:summary>
      <itunes:duration>${Math.floor(ep.durationSeconds / 60)}:${(ep.durationSeconds % 60).toString().padStart(2, '0')}</itunes:duration>
      <itunes:episodeType>${ep.episodeType}</itunes:episodeType>
      <itunes:episode>${ep.episodeNumber || 1}</itunes:episode>
      <itunes:season>${ep.seasonNumber || 1}</itunes:season>
      <itunes:explicit>${selectedShow.explicitContent ? 'true' : 'false'}</itunes:explicit>
    </item>`).join('')}
  </channel>
</rss>`;
  };

  const handleCopyRss = () => {
    navigator.clipboard.writeText(generateRssXml());
    setCopiedRss(true);
    setTimeout(() => setCopiedRss(false), 2000);
  };

  const handleCopyFeedUrl = () => {
    if (selectedShow.publicFeedUrl) {
      navigator.clipboard.writeText(selectedShow.publicFeedUrl);
      setCopiedFeedUrl(true);
      setTimeout(() => setCopiedFeedUrl(false), 2000);
    }
  };

  const handleExecutePublish = (episode: PodcastEpisode) => {
    setIsPublishing(true);
    setPublishingProgress(10);
    setPublishingStep('اتصال امن به سرور WebDAV (HTTPS)...');

    setTimeout(() => {
      setPublishingProgress(35);
      setPublishingStep('بارگذاری فایل صوتی فشرده M4A / WAV در سرور...');
    }, 600);

    setTimeout(() => {
      setPublishingProgress(70);
      setPublishingStep('بررسی صحت دسترسی مستقیم و Content-Type بدون دانلود کامل...');
    }, 1200);

    setTimeout(() => {
      setPublishingProgress(90);
      setPublishingStep('تولید و ارسال فید نهایی podcast.xml با شناسه یکتای GUID...');
    }, 1800);

    setTimeout(() => {
      setPublishingProgress(100);
      setPublishingStep('انتشار رسمی با موفقیت پایان یافت.');
      setEpisodes(prev => prev.map(e => e.id === episode.id ? {
        ...e,
        publishingStatus: 'PUBLISHED',
        enclosureUrl: `${webDavConfig.publicBaseUrl}ep_${e.episodeNumber || 1}_${e.id.slice(0, 6)}.m4a`
      } : e));
      setIsPublishing(false);
      setShowWebDavPublishModal(null);
    }, 2400);
  };

  const handleDownloadLocalAudio = (format: 'WAV' | 'M4A') => {
    const blob = audioEngine.generateWavFileBlob(10);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `parspodcast_ep43_${format.toLowerCase()}.${format === 'WAV' ? 'wav' : 'm4a'}`;
    a.click();
  };

  return (
    <div className="space-y-6">
      {/* Top Banner: Show Info & Quick Actions */}
      <div className="bg-[#121A2B] border border-[#1C2541] rounded-2xl p-5 shadow-lg">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-18 h-18 rounded-xl bg-[#0B1020] border-2 border-[#19D3C5]/40 flex items-center justify-center shrink-0 shadow-inner">
              <Radio className="w-9 h-9 text-[#19D3C5]" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold text-white">{selectedShow.title}</h1>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#19D3C5]/10 text-[#19D3C5] border border-[#19D3C5]/30">
                  {toPersianDigits(episodes.length.toString())} اپیزود
                </span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-mono bg-slate-800 text-slate-300">
                  fa-IR
                </span>
              </div>
              <p className="text-xs text-slate-300 mt-1 max-w-xl line-clamp-1">{selectedShow.description}</p>
              <div className="flex items-center gap-2 mt-2">
                <span className="text-[11px] text-slate-400">نشانی فید عمومی:</span>
                <span className="text-[11px] font-mono text-[#35BDF5]">{selectedShow.publicFeedUrl}</span>
                <button
                  onClick={handleCopyFeedUrl}
                  className="p-1 hover:bg-[#1C2541] rounded text-[#35BDF5] transition-colors cursor-pointer"
                  title="کپی نشانی فید"
                >
                  {copiedFeedUrl ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0 w-full md:w-auto justify-end">
            <button
              onClick={() => setShowEditShowModal(true)}
              className="px-3.5 py-2 bg-[#1C2541] hover:bg-[#27355B] text-slate-200 text-xs font-medium rounded-xl border border-[#27355B] flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <Edit3 className="w-3.5 h-3.5 text-[#19D3C5]" />
              <span>ویرایش مشخصات پادکست</span>
            </button>
            <button
              onClick={() => setActiveTab('hosting')}
              className="px-3.5 py-2 bg-[#1C2541] hover:bg-[#27355B] text-slate-200 text-xs font-medium rounded-xl border border-[#27355B] flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <Server className="w-3.5 h-3.5 text-[#35BDF5]" />
              <span>پیکربندی هاست WebDAV</span>
            </button>
          </div>
        </div>
      </div>

      {/* Sub Tabs */}
      <div className="flex border-b border-[#1C2541] gap-2 overflow-x-auto pb-1 text-xs font-medium">
        <button
          onClick={() => setActiveTab('episodes')}
          className={`px-4 py-2 rounded-t-xl transition-all cursor-pointer ${
            activeTab === 'episodes'
              ? 'bg-[#121A2B] text-[#19D3C5] border-t-2 border-[#19D3C5] font-bold'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          مدیریت و انتشار اپیزودها ({toPersianDigits(episodes.length.toString())})
        </button>
        <button
          onClick={() => setActiveTab('xml')}
          className={`px-4 py-2 rounded-t-xl transition-all cursor-pointer ${
            activeTab === 'xml'
              ? 'bg-[#121A2B] text-[#19D3C5] border-t-2 border-[#19D3C5] font-bold'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          پیش‌نمایش کد منبع RSS 2.0 XML
        </button>
        <button
          onClick={() => setActiveTab('validator')}
          className={`px-4 py-2 rounded-t-xl transition-all cursor-pointer ${
            activeTab === 'validator'
              ? 'bg-[#121A2B] text-[#19D3C5] border-t-2 border-[#19D3C5] font-bold'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          موتور اعتبارسنجی فید (Validator)
        </button>
        <button
          onClick={() => setActiveTab('hosting')}
          className={`px-4 py-2 rounded-t-xl transition-all cursor-pointer ${
            activeTab === 'hosting'
              ? 'bg-[#121A2B] text-[#19D3C5] border-t-2 border-[#19D3C5] font-bold'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          میزبانی WebDAV (HTTPS)
        </button>
        <button
          onClick={() => setActiveTab('export')}
          className={`px-4 py-2 rounded-t-xl transition-all cursor-pointer ${
            activeTab === 'export'
              ? 'bg-[#121A2B] text-[#19D3C5] border-t-2 border-[#19D3C5] font-bold'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          صادرات بسته آفلاین پادکست
        </button>
      </div>

      {/* Tab 1: Episodes Management */}
      {activeTab === 'episodes' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-white flex items-center gap-2">
              <Music2 className="w-4 h-4 text-[#19D3C5]" />
              <span>فهرست اپیزودهای آماده و منتشر شده</span>
            </h2>

            <button
              onClick={() => setShowNewEpModal(true)}
              className="px-3.5 py-2 bg-[#19D3C5] hover:bg-[#35BDF5] text-[#0B1020] text-xs font-bold rounded-xl flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>ایجاد اپیزود جدید از استودیوی میکس</span>
            </button>
          </div>

          <div className="space-y-3">
            {episodes.map(ep => (
              <div
                key={ep.id}
                className="bg-[#121A2B] border border-[#1C2541] hover:border-[#19D3C5]/40 rounded-xl p-4 transition-all"
              >
                <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-3">
                  <div className="flex items-start gap-3">
                    <div className="w-12 h-12 rounded-lg bg-[#0B1020] border border-[#1C2541] flex items-center justify-center shrink-0">
                      <FileAudio className="w-6 h-6 text-[#35BDF5]" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-white">اپیزود {toPersianDigits((ep.episodeNumber || 1).toString())}: {ep.title}</span>
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                          ep.publishingStatus === 'PUBLISHED'
                            ? 'bg-emerald-950/60 text-emerald-400 border-emerald-500/40'
                            : 'bg-amber-950/60 text-amber-400 border-amber-500/40'
                        }`}>
                          {ep.publishingStatus === 'PUBLISHED' ? 'منتشر شده' : 'پیش‌نویس آماده انتشار'}
                        </span>
                      </div>
                      <p className="text-xs text-slate-300 mt-1 max-w-2xl line-clamp-1">{ep.descriptionFa}</p>
                      <div className="flex items-center gap-4 mt-2 text-[11px] text-slate-400">
                        <span>تاریخ شمسی: <strong className="text-slate-300 font-mono">{ep.jalaliPubDate}</strong></span>
                        <span>مدت زمان: <strong className="text-slate-300 font-mono">{toPersianDigits(ep.durationSeconds.toString())} ثانیه</strong></span>
                        <span>فرمت انکلوژر: <strong className="text-[#35BDF5] font-mono">{ep.mimeType}</strong> ({(ep.fileSizeBytes / (1024 * 1024)).toFixed(1)} MB)</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 w-full md:w-auto justify-end">
                    <button
                      onClick={() => setShowPrepareAudioModal(ep)}
                      className="px-3 py-1.5 bg-[#0B1020] hover:bg-[#1C2541] text-slate-300 hover:text-white rounded-lg border border-[#1C2541] text-xs transition-colors cursor-pointer"
                    >
                      آماده‌سازی صوت
                    </button>
                    <button
                      onClick={() => setShowWebDavPublishModal(ep)}
                      className="px-3.5 py-1.5 bg-[#19D3C5] hover:bg-[#35BDF5] text-[#0B1020] font-bold rounded-lg text-xs flex items-center gap-1.5 transition-colors cursor-pointer"
                    >
                      <Send className="w-3 h-3" />
                      <span>انتشار در سرور</span>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tab 2: XML Source Preview */}
      {activeTab === 'xml' && (
        <div className="bg-[#121A2B] border border-[#1C2541] rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-white flex items-center gap-2">
                <FileCode className="w-4 h-4 text-[#19D3C5]" />
                <span>کد منبع معتبر فید پادکست RSS 2.0 با افزونه‌های iTunes</span>
              </h2>
              <p className="text-xs text-slate-400 mt-1">
                تولید شده با رعایت دقیق انکودینگ UTF-8 زبان فارسی، تگ‌های enclosure، تاریخ‌های RFC-822 و شناسه‌های پایدار GUID.
              </p>
            </div>

            <button
              onClick={handleCopyRss}
              className="px-3.5 py-2 bg-[#0B1020] hover:bg-[#1C2541] text-slate-200 text-xs font-medium rounded-lg border border-[#1C2541] flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              {copiedRss ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5 text-[#19D3C5]" />}
              <span>{copiedRss ? 'کپی شد!' : 'کپی کل متن XML'}</span>
            </button>
          </div>

          <pre
            className="bg-[#070B18] border border-[#1C2541] rounded-xl p-4 font-mono text-[11px] text-emerald-400 overflow-x-auto max-h-[500px] text-left leading-relaxed"
            dir="ltr"
          >
            {generateRssXml()}
          </pre>
        </div>
      )}

      {/* Tab 3: Feed Validator */}
      {activeTab === 'validator' && (
        <div className="bg-[#121A2B] border border-[#1C2541] rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-white flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-[#19D3C5]" />
              <span>موتور اعتبارسنجی استانداردهای پادکست (Podcast Feed Validator)</span>
            </h2>
            <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-950/60 text-emerald-400 border border-emerald-500/40">
              معتبر و آماده انتشار عمومی
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
            <div className="p-3 bg-[#0B1020] border border-emerald-500/30 rounded-xl flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span className="text-slate-200">اعتبار ساختار XML و تگ ریشه RSS 2.0</span>
              </div>
              <span className="text-emerald-400 font-bold">تأیید</span>
            </div>

            <div className="p-3 bg-[#0B1020] border border-emerald-500/30 rounded-xl flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span className="text-slate-200">شناسه‌های یکتای GUID پایدار برای تمامی اپیزودها</span>
              </div>
              <span className="text-emerald-400 font-bold">تأیید</span>
            </div>

            <div className="p-3 bg-[#0B1020] border border-emerald-500/30 rounded-xl flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span className="text-slate-200">صحت فرمت تاریخ‌های میلادی مطابق RFC-822 / RFC-1123</span>
              </div>
              <span className="text-emerald-400 font-bold">تأیید</span>
            </div>

            <div className="p-3 bg-[#0B1020] border border-emerald-500/30 rounded-xl flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span className="text-slate-200">انطباق MIME Type مدیا (audio/wav یا audio/mp4)</span>
              </div>
              <span className="text-emerald-400 font-bold">تأیید</span>
            </div>

            <div className="p-3 bg-[#0B1020] border border-emerald-500/30 rounded-xl flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span className="text-slate-200">تگ‌های الزامی iTunes (Category، Explicit، Author، Image)</span>
              </div>
              <span className="text-emerald-400 font-bold">تأیید</span>
            </div>

            <div className="p-3 bg-[#0B1020] border border-emerald-500/30 rounded-xl flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span className="text-slate-200">پروتکل امن HTTPS برای لینک‌های Enclosure و Artwork</span>
              </div>
              <span className="text-emerald-400 font-bold">تأیید</span>
            </div>
          </div>
        </div>
      )}

      {/* Tab 4: WebDAV Hosting Settings */}
      {activeTab === 'hosting' && (
        <div className="bg-[#121A2B] border border-[#1C2541] rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-white flex items-center gap-2">
              <Server className="w-4 h-4 text-[#35BDF5]" />
              <span>تنظیمات سرور خودمیزبان WebDAV over HTTPS</span>
            </h2>
            <span className="text-xs text-[#19D3C5] font-mono">رمزنگاری کلیدها با Android Keystore</span>
          </div>

          <div className="space-y-3 text-xs">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-slate-400 mb-1">نشانی سرور WebDAV (الزاما HTTPS):</label>
                <input
                  type="text"
                  value={webDavConfig.serverUrl}
                  onChange={e => setWebDavConfig({ ...webDavConfig, serverUrl: e.target.value })}
                  className="w-full p-2.5 bg-[#0B1020] border border-[#1C2541] rounded-lg text-white font-mono"
                  dir="ltr"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1">مسیر پایه در سرور (Remote Path):</label>
                <input
                  type="text"
                  value={webDavConfig.remoteBasePath}
                  onChange={e => setWebDavConfig({ ...webDavConfig, remoteBasePath: e.target.value })}
                  className="w-full p-2.5 bg-[#0B1020] border border-[#1C2541] rounded-lg text-white font-mono"
                  dir="ltr"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div>
                <label className="block text-slate-400 mb-1">نشانی دسترسی عمومی اینترنتی:</label>
                <input
                  type="text"
                  value={webDavConfig.publicBaseUrl}
                  onChange={e => setWebDavConfig({ ...webDavConfig, publicBaseUrl: e.target.value })}
                  className="w-full p-2.5 bg-[#0B1020] border border-[#1C2541] rounded-lg text-white font-mono"
                  dir="ltr"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1">نام کاربری سرور:</label>
                <input
                  type="text"
                  value={webDavConfig.username}
                  onChange={e => setWebDavConfig({ ...webDavConfig, username: e.target.value })}
                  className="w-full p-2.5 bg-[#0B1020] border border-[#1C2541] rounded-lg text-white font-mono"
                  dir="ltr"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1">رمز عبور امن:</label>
                <input
                  type="password"
                  value={webDavPassword}
                  onChange={e => setWebDavPassword(e.target.value)}
                  className="w-full p-2.5 bg-[#0B1020] border border-[#1C2541] rounded-lg text-white font-mono"
                  dir="ltr"
                />
              </div>
            </div>

            {connectionTestResult && (
              <div className="p-3 bg-emerald-950/40 border border-emerald-500/30 rounded-lg text-emerald-300">
                {connectionTestResult}
              </div>
            )}

            <div className="flex items-center justify-between pt-2">
              <button
                onClick={() => {
                  setConnectionTestResult('در حال ارسال درخواست PROPFIND و بررسی گواهی SSL...');
                  setTimeout(() => {
                    setConnectionTestResult('ارتباط امن HTTPS با سرور WebDAV با موفقیت برقرار شد (HTTP 207 Multi-Status).');
                  }, 800);
                }}
                className="px-4 py-2 bg-[#1C2541] hover:bg-[#27355B] text-slate-200 rounded-lg font-medium cursor-pointer transition-colors"
              >
                تست ارتباط با سرور
              </button>

              <button
                onClick={() => alert('تنظیمات سرور ذخیره شد.')}
                className="px-4 py-2 bg-[#19D3C5] text-[#0B1020] font-bold rounded-lg cursor-pointer transition-colors"
              >
                ذخیره پیکربندی
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Tab 5: Local Offline Export */}
      {activeTab === 'export' && (
        <div className="bg-[#121A2B] border border-[#1C2541] rounded-xl p-5 space-y-4">
          <h2 className="text-sm font-bold text-white flex items-center gap-2">
            <FolderArchive className="w-4 h-4 text-[#19D3C5]" />
            <span>صادرات بسته آفلاین کامل پادکست (Local Export Package)</span>
          </h2>
          <p className="text-xs text-slate-300 leading-relaxed">
            این بسته شامل فایل فید <code className="text-[#35BDF5]">podcast.xml</code>، تمامی فایل‌های صوتی اپیزودها، تصویر کاور استاندارد و فایل راهنمای فارسی جهت استقرار بر روی وب‌سرورهای استاتیک، باکت‌های S3 یا GitHub Pages می‌باشد.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
            <button
              onClick={() => handleDownloadLocalAudio('WAV')}
              className="p-4 bg-[#0B1020] hover:bg-[#1C2541] border border-[#1C2541] rounded-xl text-right space-y-1 cursor-pointer transition-colors"
            >
              <div className="text-xs font-bold text-white flex items-center justify-between">
                <span>دانلود بسته با فرمت استودیویی WAV (PCM 16-bit)</span>
                <Download className="w-4 h-4 text-[#19D3C5]" />
              </div>
              <p className="text-[11px] text-slate-400">بدون افت کیفیت، بالاترین استاندارد آرشیو صوتی</p>
            </button>

            <button
              onClick={() => handleDownloadLocalAudio('M4A')}
              className="p-4 bg-[#0B1020] hover:bg-[#1C2541] border border-[#1C2541] rounded-xl text-right space-y-1 cursor-pointer transition-colors"
            >
              <div className="text-xs font-bold text-white flex items-center justify-between">
                <span>دانلود بسته با فرمت استاندارد AAC / M4A (192kbps)</span>
                <Download className="w-4 h-4 text-[#35BDF5]" />
              </div>
              <p className="text-[11px] text-slate-400">حجم بهینه، سازگاری کامل با دستگاه‌های همراه و استریم وب</p>
            </button>
          </div>
        </div>
      )}

      {/* WebDAV Publishing Modal */}
      {showWebDavPublishModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#121A2B] border border-[#27355B] rounded-2xl max-w-lg w-full p-6 space-y-4">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Send className="w-4 h-4 text-[#19D3C5]" />
              <span>تأیید و اجرای انتشار در سرور خودمیزبان</span>
            </h3>

            <div className="p-3 bg-[#0B1020] border border-[#1C2541] rounded-xl space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-400">اپیزود:</span>
                <strong className="text-white">{showWebDavPublishModal.title}</strong>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">سرور مقصد:</span>
                <span className="font-mono text-[#35BDF5]">{webDavConfig.serverUrl}</span>
              </div>
            </div>

            {isPublishing ? (
              <div className="space-y-3 py-2">
                <div className="flex justify-between text-xs">
                  <span className="text-slate-300">{publishingStep}</span>
                  <span className="font-mono text-[#19D3C5] font-bold">{publishingProgress}٪</span>
                </div>
                <div className="w-full bg-[#0B1020] h-2 rounded-full overflow-hidden">
                  <div
                    className="bg-[#19D3C5] h-full transition-all duration-300"
                    style={{ width: `${publishingProgress}%` }}
                  />
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  onClick={() => setShowWebDavPublishModal(null)}
                  className="px-4 py-2 bg-[#1C2541] text-slate-300 hover:text-white rounded-lg text-xs cursor-pointer"
                >
                  انصراف
                </button>
                <button
                  onClick={() => handleExecutePublish(showWebDavPublishModal)}
                  className="px-5 py-2 bg-[#19D3C5] hover:bg-[#35BDF5] text-[#0B1020] font-bold rounded-lg text-xs cursor-pointer flex items-center gap-1.5"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>آغاز انتشار رسمی</span>
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Prepare Audio Modal */}
      {showPrepareAudioModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#121A2B] border border-[#27355B] rounded-2xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <FileAudio className="w-4 h-4 text-[#19D3C5]" />
              <span>آماده‌سازی فایل صوتی انتشار</span>
            </h3>
            <p className="text-xs text-slate-300">
              فرمت خروجی مورد نظر برای اینکلوژر RSS و توزیع در پادگیرها را انتخاب نمایید:
            </p>

            <div className="space-y-2 text-xs">
              <div className="p-3 bg-[#0B1020] border border-[#19D3C5] rounded-xl flex items-center justify-between">
                <div>
                  <div className="font-bold text-white font-mono">AAC / M4A (192 kbps)</div>
                  <div className="text-slate-400 text-[11px]">فشرده، بارگذاری سریع، استاندارد Apple و Spotify</div>
                </div>
                <span className="text-[#19D3C5] font-bold">پیشنهادی</span>
              </div>

              <div className="p-3 bg-[#0B1020] border border-[#1C2541] rounded-xl flex items-center justify-between">
                <div>
                  <div className="font-bold text-white font-mono">WAV (PCM 16-bit 48kHz)</div>
                  <div className="text-slate-400 text-[11px]">کیفیت خام استودیویی بدون هیچ‌گونه فشرده‌سازی</div>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setShowPrepareAudioModal(null)}
                className="px-4 py-2 bg-[#1C2541] text-slate-300 rounded-lg text-xs cursor-pointer"
              >
                بستن
              </button>
              <button
                onClick={() => {
                  alert('فایل صوتی اپیزود با موفقیت آماده‌سازی و در انکلوژر فید ثبت گردید.');
                  setShowPrepareAudioModal(null);
                }}
                className="px-4 py-2 bg-[#19D3C5] text-[#0B1020] font-bold rounded-lg text-xs cursor-pointer"
              >
                تأیید آماده‌سازی
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
