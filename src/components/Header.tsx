import React from 'react';
import { Radio, Cpu, Volume2, Sparkles, FolderArchive, Layers } from 'lucide-react';
import { gregorianToJalali } from '../utils/jalali';

interface HeaderProps {
  activeScreen: string;
  onNavigate: (screen: string) => void;
  activeModelName: string;
  activeVoiceName: string;
  onNewProduction: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  activeScreen,
  onNavigate,
  activeModelName,
  activeVoiceName,
  onNewProduction
}) => {
  const jalali = gregorianToJalali(new Date());

  return (
    <header className="sticky top-0 z-40 bg-[#070B18]/95 backdrop-blur-md border-b border-[#1C2541] px-6 py-3.5 flex items-center justify-between gap-8">
      {/* Zone 1: Single text element wordmark */}
      <div className="flex items-center gap-3 shrink-0">
        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[#00B4D8] to-[#7209B7] flex items-center justify-center text-white shadow-sm">
          <Radio className="w-4 h-4 text-white" />
        </div>
        <div className="flex flex-col">
          <span className="text-base font-bold tracking-tight text-white whitespace-nowrap">
            پارس‌پادکست <span className="text-[#00B4D8] text-xs font-semibold mr-1">ParsPodcast</span>
          </span>
          <span className="text-[11px] text-slate-400 font-mono">
            استودیوی بومی تولید پادکست و خبر صوتی
          </span>
        </div>
      </div>

      {/* Zone 2: Clean unboxed metadata & quick nav status */}
      <div className="hidden lg:flex items-center gap-6 text-xs text-slate-400">
        <div className="flex items-center gap-2">
          <span className="text-slate-300 font-medium">{jalali.formattedStr}</span>
        </div>
        <span aria-hidden="true" className="text-slate-600">·</span>
        <div className="flex items-center gap-1.5 text-slate-300" title="مدل محلی فعال">
          <Cpu className="w-3.5 h-3.5 text-[#00B4D8]" />
          <span>{activeModelName}</span>
          <span className="text-[10px] text-emerald-400 font-mono">(LiteRT-LM)</span>
        </div>
        <span aria-hidden="true" className="text-slate-600">·</span>
        <div className="flex items-center gap-1.5 text-slate-300" title="موتور صدای فعال">
          <Volume2 className="w-3.5 h-3.5 text-[#48CAE4]" />
          <span>{activeVoiceName}</span>
          <span className="text-[10px] text-cyan-400 font-mono">(Sherpa-ONNX)</span>
        </div>
        <span aria-hidden="true" className="text-slate-600">·</span>
        <button
          onClick={() => onNavigate('android-architecture')}
          className={`hover:text-[#00B4D8] transition-colors flex items-center gap-1.5 ${
            activeScreen === 'android-architecture' ? 'text-[#00B4D8] font-semibold' : ''
          }`}
        >
          <Layers className="w-3.5 h-3.5" />
          <span>معماری اندروید فاز ۰ و ۱</span>
        </button>
      </div>

      {/* Zone 3: Primary CTA action */}
      <div className="flex items-center gap-3 shrink-0">
        <button
          onClick={onNewProduction}
          className="px-4 py-2 text-xs font-semibold text-[#070B18] bg-[#00B4D8] hover:bg-[#48CAE4] rounded-lg transition-colors shadow-sm flex items-center gap-2 whitespace-nowrap cursor-pointer"
        >
          <Sparkles className="w-3.5 h-3.5" />
          <span>تولید برنامه جدید</span>
        </button>
      </div>
    </header>
  );
};
