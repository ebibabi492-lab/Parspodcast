import React, { useState } from 'react';
import {
  CalendarDays,
  Search,
  FolderOpen,
  Play,
  RotateCcw,
  Sparkles,
  Clock,
  Layers,
  CheckCircle2
} from 'lucide-react';
import { ScriptProject, PodcastEpisode } from '../types';
import { PERSIAN_MONTHS, toPersianDigits, gregorianToJalali } from '../utils/jalali';

interface JalaliArchiveViewProps {
  currentProject: ScriptProject;
  episodes: PodcastEpisode[];
  onRestoreProject: (project: ScriptProject) => void;
}

export const JalaliArchiveView: React.FC<JalaliArchiveViewProps> = ({
  currentProject,
  episodes,
  onRestoreProject
}) => {
  const [selectedMonth, setSelectedMonth] = useState<string>('آبان');
  const [selectedYear, setSelectedYear] = useState<number>(1405);
  const [searchQuery, setSearchQuery] = useState('');
  const [restoredNotice, setRestoredNotice] = useState<string | null>(null);

  const jalaliNow = gregorianToJalali(new Date());

  const handleRestore = (ep: PodcastEpisode) => {
    // Restore project structure
    onRestoreProject({
      ...currentProject,
      id: ep.projectId,
      title: ep.title,
      jalaliDateStr: ep.jalaliPubDate
    });
    setRestoredNotice(`پروژه «${ep.title}» جهت ادامه ویرایش و بازتولید با موفقیت بارگذاری گردید.`);
    setTimeout(() => setRestoredNotice(null), 3500);
  };

  const filteredEpisodes = episodes.filter((ep) => {
    const matchesSearch = ep.title.includes(searchQuery) || ep.descriptionFa.includes(searchQuery);
    const matchesMonth = ep.jalaliPubDate.includes(selectedMonth);
    return matchesSearch && matchesMonth;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="border-b border-[#1C2541] pb-4">
        <h1 className="text-xl font-bold text-white flex items-center gap-2">
          <CalendarDays className="w-5 h-5 text-[#00B4D8]" />
          <span>آرشیو جامع تقویم جلالی (Solar Hijri Production Archive)</span>
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          ذخیره‌سازی زمان‌های کانونیکال در استاندارد UTC و نمایش محاسباتی بر پایه سال، ماه و روز خورشیدی همراه با قابلیت بازگردانی کامل پروژه‌ها
        </p>
      </div>

      {/* Restored Toast */}
      {restoredNotice && (
        <div className="p-3 bg-emerald-950/40 border border-emerald-500/40 rounded-xl text-xs text-emerald-300 flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>{restoredNotice}</span>
        </div>
      )}

      {/* Solar Hijri Calendar Navigation Strip */}
      <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-4 space-y-3">
        <div className="flex items-center justify-between text-xs text-slate-300">
          <div className="flex items-center gap-2">
            <span>سال شمسی:</span>
            <div className="flex items-center gap-1 font-mono font-bold text-white">
              <button
                onClick={() => setSelectedYear((y) => y - 1)}
                className="px-2 py-0.5 bg-[#1C2541] hover:bg-[#27355B] rounded text-xs cursor-pointer"
              >
                -
              </button>
              <span>{toPersianDigits(selectedYear)}</span>
              <button
                onClick={() => setSelectedYear((y) => y + 1)}
                className="px-2 py-0.5 bg-[#1C2541] hover:bg-[#27355B] rounded text-xs cursor-pointer"
              >
                +
              </button>
            </div>
          </div>

          <div className="font-mono text-slate-400">
            امروز: {jalaliNow.formattedStr}
          </div>
        </div>

        {/* 12 Persian Months Bar */}
        <div className="flex flex-wrap items-center gap-1.5 pt-1">
          {PERSIAN_MONTHS.map((month) => (
            <button
              key={month}
              onClick={() => setSelectedMonth(month)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer ${
                selectedMonth === month
                  ? 'bg-[#00B4D8] text-[#070B18] font-bold shadow-sm'
                  : 'bg-[#070B18] text-slate-400 hover:text-white hover:bg-[#1C2541]'
              }`}
            >
              {month}
            </button>
          ))}
        </div>
      </div>

      {/* Search & Episodes Archive List */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-white">
            برنامه‌ها و اپیزودهای ثبت‌شده در ماه {selectedMonth} سال {toPersianDigits(selectedYear)}
          </h2>

          <div className="relative w-64">
            <Search className="w-3.5 h-3.5 text-slate-500 absolute right-2.5 top-2.5" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="جستجو در آرشیو..."
              className="w-full bg-[#0B132B] border border-[#1C2541] rounded-lg pr-8 pl-2 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-[#00B4D8]"
            />
          </div>
        </div>

        {filteredEpisodes.length > 0 ? (
          <div className="grid grid-cols-1 gap-4">
            {filteredEpisodes.map((ep) => (
              <div
                key={ep.id}
                className="bg-[#0B132B] border border-[#1C2541] hover:border-[#00B4D8]/30 rounded-xl p-5 space-y-3 transition-colors"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 text-xs text-slate-400">
                      <span className="text-[#00B4D8] font-medium">{ep.jalaliPubDate}</span>
                      <span aria-hidden="true">·</span>
                      <span className="font-mono">{ep.category}</span>
                      <span aria-hidden="true">·</span>
                      <span className="font-mono tabular-nums">{toPersianDigits(Math.floor(ep.durationSeconds / 60))} دقیقه</span>
                    </div>
                    <h3 className="text-base font-bold text-white">{ep.title}</h3>
                    <p className="text-xs text-slate-300 leading-relaxed max-w-3xl">
                      {ep.descriptionFa}
                    </p>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={() => handleRestore(ep)}
                      className="px-3.5 py-2 bg-[#1C2541] hover:bg-[#27355B] text-slate-200 rounded-lg text-xs font-medium border border-[#27355B] flex items-center gap-1.5 transition-colors cursor-pointer"
                    >
                      <RotateCcw className="w-3.5 h-3.5 text-[#00B4D8]" />
                      <span>بازنشانی در استودیو</span>
                    </button>
                  </div>
                </div>

                <div className="pt-2 border-t border-[#1C2541] flex items-center justify-between text-xs text-slate-400 font-mono text-[11px]">
                  <span>GUID: {ep.id}</span>
                  <span>حجم فایل: {toPersianDigits((ep.fileSizeBytes / (1024 * 1024)).toFixed(1))} مگابایت</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-8 bg-[#0B132B] border border-[#1C2541] rounded-xl text-center text-xs text-slate-500">
            هیچ برنامه‌ای در ماه {selectedMonth} ثبت نشده است. از منوی بالا ماه دیگری را انتخاب کنید یا برنامه جدیدی تولید نمایید.
          </div>
        )}
      </div>
    </div>
  );
};
