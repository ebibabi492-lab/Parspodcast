import React from 'react';
import {
  Newspaper,
  FileCheck2,
  Mic2,
  Sliders,
  Send,
  Sparkles,
  ArrowLeft,
  CheckCircle2,
  Clock,
  Layers,
  Play
} from 'lucide-react';
import { ScriptProject, NewsEvent, AIModel, VoiceProfile, PodcastEpisode } from '../types';
import { gregorianToJalali, toPersianDigits } from '../utils/jalali';

interface DashboardViewProps {
  currentProject: ScriptProject;
  newsEvents: NewsEvent[];
  aiModels: AIModel[];
  voiceProfiles: VoiceProfile[];
  episodes: PodcastEpisode[];
  onNavigate: (screen: string) => void;
  onStartNewProgram: (type: 'SINGLE_PRESENTER' | 'TWO_PRESENTERS') => void;
  onPlayPreview: () => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  currentProject,
  newsEvents,
  aiModels,
  voiceProfiles,
  episodes,
  onNavigate,
  onStartNewProgram,
  onPlayPreview
}) => {
  const jalali = gregorianToJalali(new Date());
  const activeModel = aiModels.find((m) => m.isActive) || aiModels[0];
  const selectedEvents = newsEvents.filter((e) => e.isSelected);

  const pipelineStages = [
    { title: 'گردآوری اخبار', done: true },
    { title: 'خوشه‌بندی رویداد', done: true },
    { title: 'نگارش متن گما', done: currentProject.status !== 'DRAFT' },
    { title: 'سنتز صدای مانا', done: currentProject.status === 'VOICED' || currentProject.status === 'MIXED' || currentProject.status === 'PUBLISHED' },
    { title: 'میکس چندترکه', done: currentProject.status === 'MIXED' || currentProject.status === 'PUBLISHED' },
    { title: 'انتشار RSS و آرشیو', done: currentProject.status === 'PUBLISHED' }
  ];

  return (
    <div className="space-y-6">
      {/* Welcome & Jalali Broadcast Banner */}
      <div className="bg-gradient-to-r from-[#0B132B] via-[#1C2541] to-[#0B132B] border border-[#27355B] rounded-xl p-6 relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1.5">
            <div className="text-xs text-[#00B4D8] font-medium flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-[#00B4D8]" />
              <span>استودیوی پخش زنده پارس‌پادکست · آماده تولید بولتن خبری و پادکست تحلیلی</span>
            </div>
            <h1 className="text-xl md:text-2xl font-bold text-white">
              پیشخوان مدیریت تولید · {jalali.formattedStr}
            </h1>
            <p className="text-xs text-slate-300 max-w-2xl leading-relaxed">
              تمامی مراحل استنتاج هوش مصنوعی (مدل‌های محلی Gemma 4) و تبدیل متن به گفتار فارسی (Sherpa-ONNX Piper) به صورت ۱۰۰٪ آفلاین بر روی دستگاه بدون ارسال اطلاعات به اینترنت انجام می‌شود.
            </p>
          </div>

          <div className="flex items-center gap-3 shrink-0">
            <button
              onClick={() => onStartNewProgram('TWO_PRESENTERS')}
              className="px-4 py-2.5 bg-[#00B4D8] hover:bg-[#48CAE4] text-[#070B18] font-semibold text-xs rounded-lg transition-colors shadow-sm flex items-center gap-2 cursor-pointer"
            >
              <Sparkles className="w-4 h-4" />
              <span>تولید پادکست دوگوینده</span>
            </button>
            <button
              onClick={() => onStartNewProgram('SINGLE_PRESENTER')}
              className="px-4 py-2.5 bg-[#1C2541] hover:bg-[#27355B] text-white border border-[#27355B] font-medium text-xs rounded-lg transition-colors flex items-center gap-2 cursor-pointer"
            >
              <Mic2 className="w-4 h-4 text-[#00B4D8]" />
              <span>بولتن تک‌گوینده</span>
            </button>
          </div>
        </div>
      </div>

      {/* 4 Metric KPI Cards (Clean Tabular Numbers, Zero-Pill) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-[#0B132B] border border-[#1C2541] p-4 rounded-xl">
          <div className="flex items-center justify-between text-xs text-slate-400 mb-2">
            <span>اخبار گردآوری‌شده امروز</span>
            <Newspaper className="w-4 h-4 text-[#00B4D8]" />
          </div>
          <div className="text-2xl font-bold font-mono text-white tabular-nums">
            {toPersianDigits(447)} <span className="text-xs text-slate-400 font-sans">خبر</span>
          </div>
          <div className="mt-2 text-[11px] text-slate-400">
            ۵ پایگاه خبری فعال · فواصل ۳۰ تا ۶۰ دقیقه
          </div>
        </div>

        <div className="bg-[#0B132B] border border-[#1C2541] p-4 rounded-xl">
          <div className="flex items-center justify-between text-xs text-slate-400 mb-2">
            <span>رویدادهای تحلیلی تجمیع‌شده</span>
            <FileCheck2 className="w-4 h-4 text-[#48CAE4]" />
          </div>
          <div className="text-2xl font-bold font-mono text-white tabular-nums">
            {toPersianDigits(newsEvents.length)} <span className="text-xs text-slate-400 font-sans">رویداد مشترک</span>
          </div>
          <div className="mt-2 text-[11px] text-slate-400">
            {toPersianDigits(selectedEvents.length)} رویداد برای تدوین انتخاب شده است
          </div>
        </div>

        <div className="bg-[#0B132B] border border-[#1C2541] p-4 rounded-xl">
          <div className="flex items-center justify-between text-xs text-slate-400 mb-2">
            <span>مدل محلی فعال</span>
            <Layers className="w-4 h-4 text-[#7209B7]" />
          </div>
          <div className="text-lg font-bold text-white truncate">
            {activeModel.name}
          </div>
          <div className="mt-2 text-[11px] text-emerald-400 flex items-center gap-1 font-mono">
            <span>شتاب‌دهنده {activeModel.selectedBackend}</span>
            <span aria-hidden="true">·</span>
            <span>۲.۱۵ گیگابایت رم</span>
          </div>
        </div>

        <div className="bg-[#0B132B] border border-[#1C2541] p-4 rounded-xl">
          <div className="flex items-center justify-between text-xs text-slate-400 mb-2">
            <span>اپیزودهای منتشرشده</span>
            <Send className="w-4 h-4 text-[#00B4D8]" />
          </div>
          <div className="text-2xl font-bold font-mono text-white tabular-nums">
            {toPersianDigits(episodes.length + 40)} <span className="text-xs text-slate-400 font-sans">اپیزود در فید RSS</span>
          </div>
          <div className="mt-2 text-[11px] text-slate-400">
            آرشیو جلالی کامل و در دسترس
          </div>
        </div>
      </div>

      {/* Production Pipeline Tracker */}
      <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-[#00B4D8]" />
            <h2 className="text-sm font-semibold text-white">
              وضعیت خط تولید پروژه جاری: <span className="text-[#00B4D8]">{currentProject.title}</span>
            </h2>
          </div>
          <span className="text-xs text-slate-400 font-mono">
            وضعیت: {currentProject.status} · تاریخ شمسی: {currentProject.jalaliDateStr}
          </span>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-6 gap-2">
          {pipelineStages.map((stage, idx) => (
            <div
              key={idx}
              className={`p-2.5 rounded-lg border text-right transition-colors ${
                stage.done
                  ? 'bg-[#1C2541]/70 border-[#00B4D8]/40 text-white'
                  : 'bg-[#070B18]/40 border-[#1C2541] text-slate-500'
              }`}
            >
              <div className="flex items-center justify-between mb-1">
                <span className="font-mono text-[10px] text-slate-400">{toPersianDigits(idx + 1)}</span>
                {stage.done ? (
                  <CheckCircle2 className="w-3.5 h-3.5 text-[#00B4D8]" />
                ) : (
                  <div className="w-3.5 h-3.5 rounded-full border border-slate-600" />
                )}
              </div>
              <div className="text-xs font-medium">{stage.title}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Quick Access to Main Workspaces */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Workspace 1: Script Studio */}
        <div
          onClick={() => onNavigate('script-studio')}
          className="bg-[#0B132B] hover:bg-[#1C2541] border border-[#1C2541] hover:border-[#00B4D8]/40 p-5 rounded-xl transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-9 h-9 rounded-lg bg-[#1C2541] group-hover:bg-[#00B4D8]/20 flex items-center justify-center text-[#00B4D8]">
              <FileCheck2 className="w-5 h-5" />
            </div>
            <ArrowLeft className="w-4 h-4 text-slate-400 group-hover:text-[#00B4D8] transition-transform group-hover:-translate-x-1" />
          </div>
          <h3 className="text-sm font-semibold text-white mb-1">استودیو نگارش فیلم‌نامه (AI Script Studio)</h3>
          <p className="text-xs text-slate-400 leading-relaxed mb-3">
            تدوین ساختاریافته تک‌گوینده یا گفتگوی طبیعی دو مجری با رعایت فکت‌های موثق و بدون ابداع رویداد غیرواقعی.
          </p>
          <div className="text-[11px] text-slate-500 flex items-center gap-2">
            <span>{toPersianDigits(currentProject.segments.length)} سگمنت آماده</span>
            <span aria-hidden="true">·</span>
            <span>قالب: {currentProject.programType === 'TWO_PRESENTERS' ? 'دوگوینده' : 'تک‌گوینده'}</span>
          </div>
        </div>

        {/* Workspace 2: Multitrack Audio Mixer */}
        <div
          onClick={() => onNavigate('audio-mixer')}
          className="bg-[#0B132B] hover:bg-[#1C2541] border border-[#1C2541] hover:border-[#00B4D8]/40 p-5 rounded-xl transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-9 h-9 rounded-lg bg-[#1C2541] group-hover:bg-[#00B4D8]/20 flex items-center justify-center text-[#48CAE4]">
              <Sliders className="w-5 h-5" />
            </div>
            <ArrowLeft className="w-4 h-4 text-slate-400 group-hover:text-[#00B4D8] transition-transform group-hover:-translate-x-1" />
          </div>
          <h3 className="text-sm font-semibold text-white mb-1">استودیو میکس چندترکه (Multitrack Mixer)</h3>
          <p className="text-xs text-slate-400 leading-relaxed mb-3">
            تایم‌لاین ۶ ترکه (اینترو، گوینده الف و ب، موسیقی متن با داکینگ خودکار، افکت‌ها و اوترو) با خروجی واقعی.
          </p>
          <div className="text-[11px] text-slate-500 flex items-center gap-2">
            <span>داکینگ خودکار فعال (-14dB)</span>
            <span aria-hidden="true">·</span>
            <span>استاندارد -16 LUFS</span>
          </div>
        </div>

        {/* Workspace 3: Publisher & Jalali Archive */}
        <div
          onClick={() => onNavigate('publisher')}
          className="bg-[#0B132B] hover:bg-[#1C2541] border border-[#1C2541] hover:border-[#00B4D8]/40 p-5 rounded-xl transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-9 h-9 rounded-lg bg-[#1C2541] group-hover:bg-[#00B4D8]/20 flex items-center justify-center text-[#00B4D8]">
              <Send className="w-5 h-5" />
            </div>
            <ArrowLeft className="w-4 h-4 text-slate-400 group-hover:text-[#00B4D8] transition-transform group-hover:-translate-x-1" />
          </div>
          <h3 className="text-sm font-semibold text-white mb-1">توزیع پادکست و فید RSS 2.0</h3>
          <p className="text-xs text-slate-400 leading-relaxed mb-3">
            تولید خروجی MP3 و WAV، فید استاندارد پادکست خودمیزبان سازگار با اپل پادکست و اسپاتیفای.
          </p>
          <div className="text-[11px] text-slate-500 flex items-center gap-2">
            <span>تاریخ‌های معتبر RFC-822</span>
            <span aria-hidden="true">·</span>
            <span>بسته‌های دانلودی کامل</span>
          </div>
        </div>
      </div>
    </div>
  );
};
