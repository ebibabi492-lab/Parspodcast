import React from 'react';
import {
  LayoutDashboard,
  Newspaper,
  Cpu,
  FileText,
  Mic2,
  Sliders,
  CalendarDays,
  Send,
  Layers,
  HardDrive
} from 'lucide-react';

interface SidebarProps {
  currentScreen: string;
  onSelectScreen: (screen: string) => void;
  newsCount: number;
  modelsCount: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentScreen,
  onSelectScreen,
  newsCount,
  modelsCount
}) => {
  const menuItems = [
    { id: 'dashboard', labelFa: 'پیشخوان استودیو', icon: LayoutDashboard },
    { id: 'news-center', labelFa: 'مرکز خبر و هوشمندی', icon: Newspaper, count: newsCount },
    { id: 'model-manager', labelFa: 'مدیر مدل‌های محلی', icon: Cpu, count: modelsCount },
    { id: 'script-studio', labelFa: 'استودیو فیلم‌نامه', icon: FileText },
    { id: 'voice-studio', labelFa: 'استودیو صدا و گوینده', icon: Mic2 },
    { id: 'audio-mixer', labelFa: 'میکس چندترکه صدا', icon: Sliders },
    { id: 'jalali-archive', labelFa: 'آرشیو تقویم جلالی', icon: CalendarDays },
    { id: 'publisher', labelFa: 'ناشر پادکست و RSS', icon: Send },
    { id: 'android-architecture', labelFa: 'معماری بومی فاز ۰ و ۱', icon: Layers }
  ];

  return (
    <aside className="w-64 bg-[#0B132B] border-l border-[#1C2541] flex flex-col shrink-0 h-[calc(100vh-61px)] sticky top-[61px] select-none">
      <div className="p-3 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
        بخش‌های استودیو
      </div>

      <nav className="flex-1 px-2 space-y-1 overflow-y-auto">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentScreen === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectScreen(item.id)}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-xs font-medium transition-colors text-right cursor-pointer ${
                isActive
                  ? 'bg-[#1C2541] text-[#00B4D8] border border-[#00B4D8]/30 shadow-sm'
                  : 'text-slate-300 hover:text-white hover:bg-[#1C2541]/50'
              }`}
            >
              <div className="flex items-center gap-2.5">
                <Icon className={`w-4 h-4 ${isActive ? 'text-[#00B4D8]' : 'text-slate-400'}`} />
                <span>{item.labelFa}</span>
              </div>
              {item.count !== undefined && (
                <span className="font-mono text-[10px] text-slate-400 bg-[#070B18] px-1.5 py-0.5 rounded">
                  {item.count}
                </span>
              )}
            </button>
          );
        })}
      </nav>

      {/* Storage and Offline Status Panel */}
      <div className="p-3.5 m-2 bg-[#070B18]/60 border border-[#1C2541] rounded-lg">
        <div className="flex items-center justify-between text-xs text-slate-300 mb-1.5">
          <div className="flex items-center gap-1.5">
            <HardDrive className="w-3.5 h-3.5 text-[#00B4D8]" />
            <span className="font-medium">حافظه مدل و پروژه</span>
          </div>
          <span className="font-mono text-[11px] text-slate-400">۲.۸ / ۳۲ گیگابایت</span>
        </div>
        <div className="w-full h-1.5 bg-[#1C2541] rounded-full overflow-hidden">
          <div className="h-full bg-gradient-to-r from-[#00B4D8] to-[#48CAE4] rounded-full" style={{ width: '8.7%' }} />
        </div>
        <div className="mt-2 text-[10px] text-emerald-400 flex items-center gap-1">
          <span>پردازش هوش مصنوعی ۱۰۰٪ آفلاین</span>
        </div>
      </div>
    </aside>
  );
};
