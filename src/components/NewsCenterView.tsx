import React, { useState } from 'react';
import {
  Newspaper,
  Plus,
  RefreshCw,
  CheckSquare,
  Square,
  ExternalLink,
  Layers,
  Search,
  Filter,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import { NewsSource, NewsEvent, NewsArticle } from '../types';
import { toPersianDigits } from '../utils/jalali';

interface NewsCenterViewProps {
  sources: NewsSource[];
  events: NewsEvent[];
  articles: NewsArticle[];
  onToggleEventSelection: (eventId: string) => void;
  onRefreshSources: () => void;
  onAddSource: (source: Partial<NewsSource>) => void;
}

export const NewsCenterView: React.FC<NewsCenterViewProps> = ({
  sources,
  events,
  articles,
  onToggleEventSelection,
  onRefreshSources,
  onAddSource
}) => {
  const [activeTab, setActiveTab] = useState<'events' | 'sources' | 'articles'>('events');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [newSourceName, setNewSourceName] = useState('');
  const [newSourceUrl, setNewSourceUrl] = useState('');
  const [newSourceCategory, setNewSourceCategory] = useState<'فناوری' | 'اقتصاد' | 'جامعه' | 'علم و دانش'>('فناوری');

  const filteredEvents = events.filter((e) => {
    const matchesSearch = e.canonicalTitle.includes(searchQuery) || e.eventSummary.includes(searchQuery);
    const matchesCat = selectedCategory === 'all' || e.category === selectedCategory;
    return matchesSearch && matchesCat;
  });

  const handleRefresh = () => {
    setIsRefreshing(true);
    setTimeout(() => {
      onRefreshSources();
      setIsRefreshing(false);
    }, 800);
  };

  const handleCreateSource = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSourceName || !newSourceUrl) return;
    onAddSource({
      name: newSourceName,
      feedUrl: newSourceUrl,
      category: newSourceCategory,
      parserType: 'RSS',
      updateIntervalMins: 30,
      isActive: true,
      lastFetchedUtc: Date.now(),
      articleCount: 0
    });
    setNewSourceName('');
    setNewSourceUrl('');
    setShowAddModal(false);
  };

  return (
    <div className="space-y-6">
      {/* View Header with Top Bar Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#1C2541] pb-4">
        <div>
          <h1 className="text-xl font-bold text-white flex items-center gap-2">
            <Newspaper className="w-5 h-5 text-[#00B4D8]" />
            <span>مرکز گردآوری و هوشمندی خبر (News Intelligence)</span>
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            تجمیع رویدادمحور اخبار، شناسایی تکرارها و استخراج فکت‌های موثق بدون تحریف روایت‌های منابع
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={handleRefresh}
            disabled={isRefreshing}
            className="px-3.5 py-2 bg-[#1C2541] hover:bg-[#27355B] text-slate-200 text-xs font-medium rounded-lg border border-[#27355B] flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 text-[#00B4D8] ${isRefreshing ? 'animate-spin' : ''}`} />
            <span>به‌روزرسانی فیدها</span>
          </button>
          <button
            onClick={() => setShowAddModal(true)}
            className="px-3.5 py-2 bg-[#00B4D8] hover:bg-[#48CAE4] text-[#070B18] text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>افزودن منبع خبری</span>
          </button>
        </div>
      </div>

      {/* Tabs / Segmented Controls & Search Filter */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-1 p-1 bg-[#0B132B] border border-[#1C2541] rounded-lg">
          <button
            onClick={() => setActiveTab('events')}
            className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors cursor-pointer ${
              activeTab === 'events'
                ? 'bg-[#1C2541] text-[#00B4D8] shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            رویدادهای تجمیع‌شده ({toPersianDigits(events.length)})
          </button>
          <button
            onClick={() => setActiveTab('sources')}
            className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors cursor-pointer ${
              activeTab === 'sources'
                ? 'bg-[#1C2541] text-[#00B4D8] shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            منابع و فیدها ({toPersianDigits(sources.length)})
          </button>
          <button
            onClick={() => setActiveTab('articles')}
            className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors cursor-pointer ${
              activeTab === 'articles'
                ? 'bg-[#1C2541] text-[#00B4D8] shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            مقالات خام ({toPersianDigits(articles.length)})
          </button>
        </div>

        <div className="flex items-center gap-2">
          {/* Category Filter */}
          <div className="flex items-center gap-1 text-xs">
            {['all', 'فناوری', 'اقتصاد', 'علم و دانش', 'جامعه'].map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`px-2.5 py-1 rounded text-[11px] font-medium transition-colors cursor-pointer ${
                  selectedCategory === cat
                    ? 'bg-[#00B4D8]/20 text-[#00B4D8] border border-[#00B4D8]/40'
                    : 'text-slate-400 hover:text-slate-200 bg-[#0B132B]'
                }`}
              >
                {cat === 'all' ? 'همه دسته‌ها' : cat}
              </button>
            ))}
          </div>

          {/* Search Box */}
          <div className="relative w-48">
            <Search className="w-3.5 h-3.5 text-slate-500 absolute right-2.5 top-2.5" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="جستجو در اخبار..."
              className="w-full bg-[#0B132B] border border-[#1C2541] rounded-lg pr-8 pl-2 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-[#00B4D8]"
            />
          </div>
        </div>
      </div>

      {/* Tab Content 1: Clustered Events */}
      {activeTab === 'events' && (
        <div className="space-y-4">
          <div className="text-xs text-slate-400 flex items-center justify-between">
            <span>
              رویدادهای انتخاب‌شده مستقیماً به استودیو فیلم‌نامه محلی برای نگارش متن ارسال خواهند شد:
            </span>
            <span className="font-mono text-[#00B4D8]">
              {toPersianDigits(events.filter((e) => e.isSelected).length)} رویداد فعال
            </span>
          </div>

          {filteredEvents.map((event) => (
            <div
              key={event.id}
              className={`p-5 rounded-xl border transition-all ${
                event.isSelected
                  ? 'bg-[#0B132B] border-[#00B4D8]/50 shadow-sm'
                  : 'bg-[#0B132B]/60 border-[#1C2541] opacity-80 hover:opacity-100'
              }`}
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-3">
                  <button
                    onClick={() => onToggleEventSelection(event.id)}
                    className="mt-1 text-[#00B4D8] cursor-pointer"
                  >
                    {event.isSelected ? (
                      <CheckSquare className="w-5 h-5 text-[#00B4D8]" />
                    ) : (
                      <Square className="w-5 h-5 text-slate-500" />
                    )}
                  </button>
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 text-xs text-slate-400">
                      <span className="text-[#00B4D8] font-medium">{event.category}</span>
                      <span aria-hidden="true">·</span>
                      <span className="font-mono">{toPersianDigits(event.reportCount)} گزارش تجمیع‌شده</span>
                    </div>
                    <h2 className="text-base font-bold text-white hover:text-[#00B4D8] transition-colors">
                      {event.canonicalTitle}
                    </h2>
                    <p className="text-xs text-slate-300 leading-relaxed pt-1">
                      {event.eventSummary}
                    </p>
                  </div>
                </div>
              </div>

              {/* Source Attribution Comparison */}
              <div className="mt-4 pt-3 border-t border-[#1C2541] grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Primary Factual Record */}
                <div className="bg-[#070B18]/50 p-3 rounded-lg border border-[#1C2541]">
                  <div className="text-[11px] font-semibold text-[#00B4D8] mb-1.5 flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>فکت‌های مشترک تأییدشده:</span>
                  </div>
                  <ul className="space-y-1 text-xs text-slate-300 list-disc list-inside">
                    {event.primaryFacts.map((fact, idx) => (
                      <li key={idx}>{fact}</li>
                    ))}
                  </ul>
                </div>

                {/* Sources & Differentials */}
                <div className="bg-[#070B18]/50 p-3 rounded-lg border border-[#1C2541]">
                  <div className="text-[11px] font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
                    <Layers className="w-3.5 h-3.5 text-[#48CAE4]" />
                    <span>منابع گزارش و تمایز روایت‌ها:</span>
                  </div>
                  <div className="space-y-1.5 text-xs">
                    <div className="flex flex-wrap gap-2 text-slate-400">
                      {event.sources.map((src) => (
                        <span key={src.articleId} className="text-[#48CAE4] font-medium">
                          {src.sourceName}
                        </span>
                      ))}
                    </div>
                    {event.verifiedDifferentials.length > 0 ? (
                      <ul className="text-slate-400 space-y-0.5 text-[11px]">
                        {event.verifiedDifferentials.map((diff, idx) => (
                          <li key={idx} className="flex items-center gap-1">
                            <span className="text-amber-400">·</span>
                            <span>{diff}</span>
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <span className="text-slate-500 text-[11px]">روایت یکسان در تمامی گزارش‌ها</span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Tab Content 2: Sources Management */}
      {activeTab === 'sources' && (
        <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl overflow-hidden">
          <div className="p-4 border-b border-[#1C2541] flex items-center justify-between">
            <h2 className="text-xs font-semibold text-white">پایگاه‌ها و فیدهای RSS/Atom فعال</h2>
            <span className="text-xs text-slate-400">به‌روزرسانی طبق زمان‌بندی محلی</span>
          </div>
          <div className="divide-y divide-[#1C2541]">
            {sources.map((source) => (
              <div key={source.id} className="p-4 flex items-center justify-between hover:bg-[#1C2541]/40 transition-colors">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-white">{source.name}</span>
                    <span className="text-[10px] text-slate-400 font-mono bg-[#070B18] px-1.5 py-0.5 rounded">
                      {source.parserType}
                    </span>
                    <span className="text-xs text-[#00B4D8]">{source.category}</span>
                  </div>
                  <div className="text-xs font-mono text-slate-400 flex items-center gap-2">
                    <span>{source.feedUrl}</span>
                    <a href={source.feedUrl} target="_blank" rel="noreferrer" className="text-slate-500 hover:text-[#00B4D8]">
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                </div>

                <div className="flex items-center gap-4 text-xs">
                  <div className="text-left font-mono tabular-nums text-slate-300">
                    <div>{toPersianDigits(source.articleCount)} خبر</div>
                    <div className="text-[10px] text-slate-500">هر {toPersianDigits(source.updateIntervalMins)} دقیقه</div>
                  </div>
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" title="منبع فعال" />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tab Content 3: Raw Articles */}
      {activeTab === 'articles' && (
        <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl overflow-hidden">
          <div className="divide-y divide-[#1C2541]">
            {articles.map((art) => (
              <div key={art.id} className="p-4 hover:bg-[#1C2541]/40 transition-colors space-y-1">
                <div className="flex items-center justify-between text-xs text-slate-400">
                  <span className="text-[#00B4D8] font-medium">{art.sourceName}</span>
                  <span className="font-mono">{art.category}</span>
                </div>
                <h3 className="text-sm font-semibold text-white">{art.title}</h3>
                <p className="text-xs text-slate-300 leading-relaxed">{art.rawSummary}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Add Source Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#0B132B] border border-[#27355B] rounded-xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-base font-bold text-white">افزودن منبع خبری جدید (RSS / Atom)</h3>
            <form onSubmit={handleCreateSource} className="space-y-3">
              <div>
                <label className="text-xs text-slate-300 block mb-1">نام خبرگزاری یا سایت:</label>
                <input
                  type="text"
                  required
                  value={newSourceName}
                  onChange={(e) => setNewSourceName(e.target.value)}
                  placeholder="مثال: خبرگزاری مهر"
                  className="w-full bg-[#070B18] border border-[#1C2541] rounded-lg p-2.5 text-xs text-white focus:outline-none focus:border-[#00B4D8]"
                />
              </div>

              <div>
                <label className="text-xs text-slate-300 block mb-1">آدرس فید RSS یا وب‌سایت:</label>
                <input
                  type="url"
                  required
                  value={newSourceUrl}
                  onChange={(e) => setNewSourceUrl(e.target.value)}
                  placeholder="https://mehrnews.com/rss"
                  className="w-full bg-[#070B18] border border-[#1C2541] rounded-lg p-2.5 text-xs text-white font-mono focus:outline-none focus:border-[#00B4D8]"
                />
              </div>

              <div>
                <label className="text-xs text-slate-300 block mb-1">دسته‌بندی موضوعی:</label>
                <select
                  value={newSourceCategory}
                  onChange={(e) => setNewSourceCategory(e.target.value as 'فناوری' | 'اقتصاد' | 'جامعه' | 'علم و دانش')}
                  className="w-full bg-[#070B18] border border-[#1C2541] rounded-lg p-2.5 text-xs text-white focus:outline-none focus:border-[#00B4D8]"
                >
                  <option value="فناوری">فناوری</option>
                  <option value="اقتصاد">اقتصاد</option>
                  <option value="علم و دانش">علم و دانش</option>
                  <option value="جامعه">جامعه</option>
                </select>
              </div>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 bg-[#1C2541] text-xs text-slate-300 rounded-lg hover:bg-[#27355B]"
                >
                  انصراف
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-[#00B4D8] text-xs text-[#070B18] font-semibold rounded-lg hover:bg-[#48CAE4]"
                >
                  ثبت و راه‌اندازی فید
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
