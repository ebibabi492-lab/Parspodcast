import React, { useState } from 'react';
import {
  FileText,
  Sparkles,
  RefreshCw,
  Plus,
  Trash2,
  ChevronUp,
  ChevronDown,
  User,
  Users,
  Code,
  CheckCircle2,
  SlidersHorizontal,
  Clock,
  Layers
} from 'lucide-react';
import { ScriptProject, ScriptSegment, ProgramType, NewsEvent, AIModel } from '../types';
import { toPersianDigits } from '../utils/jalali';

interface ScriptStudioViewProps {
  project: ScriptProject;
  newsEvents: NewsEvent[];
  activeModel: AIModel;
  onUpdateProject: (updated: ScriptProject) => void;
  onNavigateToVoice: () => void;
}

export const ScriptStudioView: React.FC<ScriptStudioViewProps> = ({
  project,
  newsEvents,
  activeModel,
  onUpdateProject,
  onNavigateToVoice
}) => {
  const [isGenerating, setIsGenerating] = useState(false);
  const [showJsonModal, setShowJsonModal] = useState(false);
  const [activeTab, setActiveTab] = useState<'editor' | 'settings'>('editor');

  const selectedEvents = newsEvents.filter((e) => project.selectedEventIds.includes(e.id));

  // Change Program Type (Single Presenter vs Two Presenters)
  const handleProgramTypeChange = (type: ProgramType) => {
    let updatedSegments = [...project.segments];
    if (type === 'SINGLE_PRESENTER') {
      updatedSegments = updatedSegments.map((s) => ({ ...s, speaker: 'PRESENTER_A' as const }));
    } else {
      updatedSegments = updatedSegments.map((s, idx) => ({
        ...s,
        speaker: (idx % 2 === 0 ? 'PRESENTER_A' : 'PRESENTER_B') as ('PRESENTER_A' | 'PRESENTER_B')
      }));
    }
    onUpdateProject({ ...project, programType: type, segments: updatedSegments });
  };

  // Edit segment text
  const handleSegmentTextChange = (id: string, newText: string) => {
    const updated = project.segments.map((s) =>
      s.id === id
        ? {
            ...s,
            textFa: newText,
            estimatedDurationSec: Math.max(5, Math.round(newText.split(' ').length * 0.4))
          }
        : s
    );
    onUpdateProject({ ...project, segments: updated });
  };

  // Switch speaker
  const handleToggleSpeaker = (id: string) => {
    const updated = project.segments.map((s) =>
      s.id === id
        ? {
            ...s,
            speaker: (s.speaker === 'PRESENTER_A' ? 'PRESENTER_B' : 'PRESENTER_A') as ('PRESENTER_A' | 'PRESENTER_B')
          }
        : s
    );
    onUpdateProject({ ...project, segments: updated });
  };

  // Move segment up / down
  const handleMoveSegment = (index: number, direction: 'up' | 'down') => {
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= project.segments.length) return;

    const newSegments = [...project.segments];
    const temp = newSegments[index];
    newSegments[index] = newSegments[targetIndex];
    newSegments[targetIndex] = temp;

    // re-index
    newSegments.forEach((s, idx) => {
      s.segmentIndex = idx + 1;
    });

    onUpdateProject({ ...project, segments: newSegments });
  };

  // Delete segment
  const handleDeleteSegment = (id: string) => {
    const newSegments = project.segments.filter((s) => s.id !== id);
    newSegments.forEach((s, idx) => {
      s.segmentIndex = idx + 1;
    });
    onUpdateProject({ ...project, segments: newSegments });
  };

  // Add new segment
  const handleAddSegment = () => {
    const newSegment: ScriptSegment = {
      id: `seg_${Date.now()}`,
      segmentIndex: project.segments.length + 1,
      type: 'NEWS',
      speaker: project.programType === 'TWO_PRESENTERS' ? 'PRESENTER_B' : 'PRESENTER_A',
      textFa: 'متن بخش جدید را اینجا وارد نمایید...',
      estimatedDurationSec: 15,
      isAudioGenerated: false,
      sourceIds: []
    };
    onUpdateProject({ ...project, segments: [...project.segments, newSegment] });
  };

  // Regenerate whole script with simulated local LLM stream
  const handleRegenerateAll = () => {
    setIsGenerating(true);
    setTimeout(() => {
      setIsGenerating(false);
    }, 1200);
  };

  const totalEstimatedDuration = project.segments.reduce((acc, s) => acc + s.estimatedDurationSec, 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#1C2541] pb-4">
        <div>
          <h1 className="text-xl font-bold text-white flex items-center gap-2">
            <FileText className="w-5 h-5 text-[#00B4D8]" />
            <span>استودیو نگارش فیلم‌نامه هوش مصنوعی (AI Script Studio)</span>
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            نگارش ساختاریافته متن بولتن و پادکست بر پایه مدل محلی {activeModel.name} بدون تحریف اخبار
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={() => setShowJsonModal(true)}
            className="px-3.5 py-2 bg-[#1C2541] hover:bg-[#27355B] text-slate-200 text-xs font-medium rounded-lg border border-[#27355B] flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <Code className="w-3.5 h-3.5 text-[#00B4D8]" />
            <span>خروجی استاندارد JSON</span>
          </button>
          <button
            onClick={onNavigateToVoice}
            className="px-4 py-2 bg-[#00B4D8] hover:bg-[#48CAE4] text-[#070B18] text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <CheckCircle2 className="w-4 h-4" />
            <span>تأیید متن و ورود به استودیو صدا</span>
          </button>
        </div>
      </div>

      {/* Program Mode Selection (Single Presenter vs Two-Presenter Podcast) */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <button
          onClick={() => handleProgramTypeChange('SINGLE_PRESENTER')}
          className={`p-4 rounded-xl border text-right transition-all cursor-pointer ${
            project.programType === 'SINGLE_PRESENTER'
              ? 'bg-[#1C2541] border-[#00B4D8] shadow-sm'
              : 'bg-[#0B132B] border-[#1C2541] opacity-75 hover:opacity-100'
          }`}
        >
          <div className="flex items-center gap-2 mb-1.5">
            <User className={`w-4 h-4 ${project.programType === 'SINGLE_PRESENTER' ? 'text-[#00B4D8]' : 'text-slate-400'}`} />
            <h2 className="text-sm font-bold text-white">حالت اول: بولتن خبری تک‌گوینده (News Bulletin)</h2>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            ساختار رسمی گویندگی خبری شامل: اینترو، خوش‌آمدگویی، سرخط خبرها، گزارش‌های مشروح، ترنزیشن و جمع‌بندی پایانی توسط مجری ارشد.
          </p>
        </button>

        <button
          onClick={() => handleProgramTypeChange('TWO_PRESENTERS')}
          className={`p-4 rounded-xl border text-right transition-all cursor-pointer ${
            project.programType === 'TWO_PRESENTERS'
              ? 'bg-[#1C2541] border-[#00B4D8] shadow-sm'
              : 'bg-[#0B132B] border-[#1C2541] opacity-75 hover:opacity-100'
          }`}
        >
          <div className="flex items-center gap-2 mb-1.5">
            <Users className={`w-4 h-4 ${project.programType === 'TWO_PRESENTERS' ? 'text-[#00B4D8]' : 'text-slate-400'}`} />
            <h2 className="text-sm font-bold text-white">حالت دوم: پادکست گفتگوی دوگوینده (Conversational Podcast)</h2>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            گفتگوی زنده، طبیعی و جذاب میان مجری اصلی (روایتگر خبر) و کارشناس تحلیل‌گر بدون دیالوگ‌های تصنعی یا نقل‌قول‌های جعلی.
          </p>
        </button>
      </div>

      {/* Script Metadata & Statistics */}
      <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-4 flex flex-wrap items-center justify-between gap-4 text-xs">
        <div className="flex items-center gap-4 text-slate-300">
          <div className="flex items-center gap-1.5">
            <Clock className="w-4 h-4 text-[#00B4D8]" />
            <span>مدت تخمینی برنامه:</span>
            <strong className="font-mono text-white tabular-nums">
              {toPersianDigits(Math.floor(totalEstimatedDuration / 60))}:{toPersianDigits((totalEstimatedDuration % 60).toString().padStart(2, '0'))} دقیقه
            </strong>
          </div>
          <span aria-hidden="true" className="text-slate-600">·</span>
          <div>
            <span>تعداد سگمنت‌ها:</span> <strong className="font-mono text-white tabular-nums">{toPersianDigits(project.segments.length)}</strong>
          </div>
          <span aria-hidden="true" className="text-slate-600">·</span>
          <div>
            <span>لحن گفتار:</span> <strong className="text-[#00B4D8]">{project.formalityLevel}</strong>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleAddSegment}
            className="px-3 py-1.5 bg-[#1C2541] hover:bg-[#27355B] text-slate-200 rounded-lg text-xs font-medium border border-[#27355B] flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <Plus className="w-3.5 h-3.5 text-[#00B4D8]" />
            <span>افزودن بخش جدید</span>
          </button>
          <button
            onClick={handleRegenerateAll}
            disabled={isGenerating}
            className="px-3 py-1.5 bg-[#00B4D8] hover:bg-[#48CAE4] text-[#070B18] rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isGenerating ? 'animate-spin' : ''}`} />
            <span>بازنویسی هوشمند متن</span>
          </button>
        </div>
      </div>

      {/* Segment List & Interactive Editor */}
      <div className="space-y-3">
        {project.segments.map((segment, index) => {
          const isPresenterA = segment.speaker === 'PRESENTER_A';
          return (
            <div
              key={segment.id}
              className={`p-4 rounded-xl border transition-all ${
                isPresenterA
                  ? 'bg-[#0B132B] border-[#1C2541]'
                  : 'bg-[#0B132B]/85 border-[#7209B7]/30'
              }`}
            >
              <div className="flex items-center justify-between mb-2.5">
                <div className="flex items-center gap-3">
                  <span className="font-mono text-xs text-slate-400 bg-[#070B18] px-2 py-0.5 rounded border border-[#1C2541]">
                    بخش {toPersianDigits(segment.segmentIndex)}
                  </span>
                  <span className="text-xs text-[#00B4D8] font-medium">
                    {segment.type === 'INTRO' && 'مقدمه و آرم'}
                    {segment.type === 'NEWS' && 'شرح رویداد خبری'}
                    {segment.type === 'DISCUSSION' && 'تحلیل و گفتگو'}
                    {segment.type === 'TRANSITION' && 'انتقال موضوع'}
                    {segment.type === 'OUTRO' && 'جمع‌بندی و پایان'}
                  </span>

                  {/* Speaker Selector */}
                  <button
                    onClick={() => handleToggleSpeaker(segment.id)}
                    className={`text-xs px-2.5 py-1 rounded font-medium flex items-center gap-1.5 transition-colors cursor-pointer ${
                      isPresenterA
                        ? 'bg-[#00B4D8]/20 text-[#00B4D8] border border-[#00B4D8]/40'
                        : 'bg-[#7209B7]/20 text-[#90E0EF] border border-[#7209B7]/40'
                    }`}
                  >
                    <User className="w-3.5 h-3.5" />
                    <span>{isPresenterA ? 'مجری الف (گوینده اصلی)' : 'مجری ب (کارشناس تحلیل)'}</span>
                  </button>
                </div>

                {/* Segment Controls (Move, Duration, Delete) */}
                <div className="flex items-center gap-1.5 text-xs text-slate-400">
                  <span className="font-mono text-[11px] ml-2 text-slate-400 tabular-nums">
                    ~{toPersianDigits(segment.estimatedDurationSec)} ثانیه
                  </span>
                  <button
                    onClick={() => handleMoveSegment(index, 'up')}
                    disabled={index === 0}
                    className="p-1 hover:text-white disabled:opacity-30 cursor-pointer"
                    title="انتقال به بالا"
                  >
                    <ChevronUp className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleMoveSegment(index, 'down')}
                    disabled={index === project.segments.length - 1}
                    className="p-1 hover:text-white disabled:opacity-30 cursor-pointer"
                    title="انتقال به پایین"
                  >
                    <ChevronDown className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDeleteSegment(segment.id)}
                    className="p-1 hover:text-red-400 text-slate-500 transition-colors cursor-pointer ml-1"
                    title="حذف سگمنت"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Text Area */}
              <textarea
                value={segment.textFa}
                onChange={(e) => handleSegmentTextChange(segment.id, e.target.value)}
                rows={3}
                className="w-full bg-[#070B18] border border-[#1C2541] rounded-lg p-3 text-xs leading-relaxed text-slate-100 focus:outline-none focus:border-[#00B4D8] resize-y"
              />
            </div>
          );
        })}
      </div>

      {/* JSON Viewer Modal */}
      {showJsonModal && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#0B132B] border border-[#27355B] rounded-xl max-w-2xl w-full p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Code className="w-4 h-4 text-[#00B4D8]" />
                <span>ساختار اعتبارسنجی‌شده JSON فیلم‌نامه</span>
              </h3>
              <button
                onClick={() => setShowJsonModal(false)}
                className="text-slate-400 hover:text-white text-xs cursor-pointer"
              >
                بستن
              </button>
            </div>
            <pre className="bg-[#070B18] border border-[#1C2541] rounded-lg p-4 font-mono text-[11px] text-emerald-400 overflow-x-auto max-h-96 text-left" dir="ltr">
              {JSON.stringify(
                {
                  program_title: project.title,
                  program_type: project.programType,
                  language: 'fa',
                  model_used: project.modelIdUsed,
                  jalali_date: project.jalaliDateStr,
                  segments: project.segments.map((s) => ({
                    id: s.id,
                    type: s.type,
                    speaker: s.speaker,
                    text: s.textFa,
                    source_ids: s.sourceIds,
                    estimated_duration: s.estimatedDurationSec
                  }))
                },
                null,
                2
              )}
            </pre>
            <div className="flex justify-end">
              <button
                onClick={() => setShowJsonModal(false)}
                className="px-4 py-2 bg-[#00B4D8] text-xs text-[#070B18] font-bold rounded-lg cursor-pointer"
              >
                تأیید و بازگشت
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
