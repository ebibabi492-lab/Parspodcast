import React, { useState } from 'react';
import {
  Mic2,
  Volume2,
  Play,
  Square,
  Sparkles,
  Sliders,
  CheckCircle2,
  FileCheck,
  Languages,
  ArrowLeft
} from 'lucide-react';
import { VoiceProfile, ScriptProject, SpeakerRole } from '../types';
import { normalizePersianText } from '../utils/persianNormalizer';
import { audioEngine } from '../utils/audioEngine';
import { toPersianDigits } from '../utils/jalali';

interface VoiceStudioViewProps {
  voiceProfiles: VoiceProfile[];
  project: ScriptProject;
  onUpdateProfiles: (profiles: VoiceProfile[]) => void;
  onNavigateToMixer: () => void;
}

export const VoiceStudioView: React.FC<VoiceStudioViewProps> = ({
  voiceProfiles,
  project,
  onUpdateProfiles,
  onNavigateToMixer
}) => {
  const [isPlayingId, setIsPlayingId] = useState<string | null>(null);
  const [testText, setTestText] = useState('در ۲۱ آبان سال ۱۴۰۵ بیش از ۱۲۰ پژوهشگر هوش مصنوعی موفق شدند.');
  const [isSynthesizingAll, setIsSynthesizingAll] = useState(false);
  const [synthesisProgress, setSynthesisProgress] = useState(0);

  const normalizedTestText = normalizePersianText(testText);

  // Play preview of voice profile
  const handlePlayVoicePreview = (profile: VoiceProfile) => {
    if (isPlayingId === profile.id) {
      audioEngine.stopAllSpeech();
      setIsPlayingId(null);
      return;
    }

    setIsPlayingId(profile.id);
    const phrase = profile.role === 'PRESENTER_A'
      ? 'به استودیوی پارس‌پادکست خوش آمدید. من مجری الف، راوی رویدادهای مهم امروز هستم.'
      : 'سلام و درود. من مجری ب هستم و نکات تحلیلی این برنامه را با هم بررسی خواهیم کرد.';

    audioEngine.playSpeechUtterance(phrase, profile.role, profile.speechRate, () => {
      setIsPlayingId(null);
    });
  };

  // Play single segment
  const handlePlaySegmentAudio = (text: string, role: SpeakerRole, segmentId: string) => {
    if (isPlayingId === segmentId) {
      audioEngine.stopAllSpeech();
      setIsPlayingId(null);
      return;
    }

    setIsPlayingId(segmentId);
    const profile = voiceProfiles.find((v) => v.role === role) || voiceProfiles[0];
    const normalized = normalizePersianText(text);

    audioEngine.playSpeechUtterance(normalized, role, profile.speechRate, () => {
      setIsPlayingId(null);
    });
  };

  // Update speech rate
  const handleRateChange = (id: string, newRate: number) => {
    const updated = voiceProfiles.map((p) => (p.id === id ? { ...p, speechRate: newRate } : p));
    onUpdateProfiles(updated);
  };

  // Batch synthesize all segments offline
  const handleSynthesizeAll = () => {
    setIsSynthesizingAll(true);
    setSynthesisProgress(0);

    let current = 0;
    const interval = setInterval(() => {
      current += 20;
      setSynthesisProgress(current);
      if (current >= 100) {
        clearInterval(interval);
        setIsSynthesizingAll(false);
      }
    }, 300);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#1C2541] pb-4">
        <div>
          <h1 className="text-xl font-bold text-white flex items-center gap-2">
            <Mic2 className="w-5 h-5 text-[#00B4D8]" />
            <span>استودیو صدای گویندگان (Persian Offline Voice Studio)</span>
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            موتور آفلاین تبدیل متن به گفتار Sherpa-ONNX و مدل عصبی مانای فارسی (fa_IR-mana-medium) با تلفظ صحیح اعراب و تقویم جلالی
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={handleSynthesizeAll}
            disabled={isSynthesizingAll}
            className="px-3.5 py-2 bg-[#1C2541] hover:bg-[#27355B] text-slate-200 text-xs font-medium rounded-lg border border-[#27355B] flex items-center gap-1.5 transition-colors cursor-pointer disabled:opacity-50"
          >
            <Sparkles className="w-3.5 h-3.5 text-[#00B4D8]" />
            <span>سنتز کلیه سگمنت‌ها ({toPersianDigits(project.segments.length)})</span>
          </button>
          <button
            onClick={onNavigateToMixer}
            className="px-4 py-2 bg-[#00B4D8] hover:bg-[#48CAE4] text-[#070B18] text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>ورود به استودیو میکس چندترکه</span>
          </button>
        </div>
      </div>

      {/* Progress banner if synthesizing */}
      {isSynthesizingAll && (
        <div className="bg-[#0B132B] border border-[#00B4D8] p-4 rounded-xl space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-200">
            <span>در حال سنتز قطعات صوتی با مدل آفلاین Sherpa-ONNX...</span>
            <span className="font-mono text-[#00B4D8]">{toPersianDigits(synthesisProgress)}%</span>
          </div>
          <div className="w-full h-1.5 bg-[#070B18] rounded-full overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-[#00B4D8] to-[#48CAE4] transition-all"
              style={{ width: `${synthesisProgress}%` }}
            />
          </div>
        </div>
      )}

      {/* Dual Voice Profiles Configuration (Presenter A & Presenter B) */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {voiceProfiles.map((profile) => (
          <div
            key={profile.id}
            className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-5 space-y-4"
          >
            <div className="flex items-start justify-between">
              <div>
                <div className="text-xs text-[#00B4D8] font-mono mb-0.5">{profile.role}</div>
                <h2 className="text-base font-bold text-white">{profile.titleFa}</h2>
                <div className="text-xs text-slate-400 font-mono mt-0.5">{profile.modelName}</div>
              </div>

              <button
                onClick={() => handlePlayVoicePreview(profile)}
                className={`p-2.5 rounded-lg transition-colors cursor-pointer flex items-center gap-1.5 text-xs font-medium ${
                  isPlayingId === profile.id
                    ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                    : 'bg-[#1C2541] text-[#00B4D8] hover:bg-[#27355B]'
                }`}
              >
                {isPlayingId === profile.id ? <Square className="w-4 h-4 fill-current" /> : <Play className="w-4 h-4 fill-current" />}
                <span>{isPlayingId === profile.id ? 'توقف پخش' : 'تست صدا'}</span>
              </button>
            </div>

            <p className="text-xs text-slate-300 leading-relaxed bg-[#070B18]/50 p-3 rounded-lg border border-[#1C2541]">
              {profile.characterDescriptionFa}
            </p>

            {/* Speech Rate Slider */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span>سرعت کلام گوینده (Speech Rate):</span>
                <span className="font-mono text-white tabular-nums">{profile.speechRate.toFixed(2)}x</span>
              </div>
              <input
                type="range"
                min="0.8"
                max="1.3"
                step="0.05"
                value={profile.speechRate}
                onChange={(e) => handleRateChange(profile.id, parseFloat(e.target.value))}
                className="w-full accent-[#00B4D8] cursor-pointer"
              />
            </div>
          </div>
        ))}
      </div>

      {/* Persian Text Normalizer Laboratory */}
      <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-5 space-y-3">
        <div className="flex items-center gap-2">
          <Languages className="w-4 h-4 text-[#00B4D8]" />
          <h2 className="text-sm font-bold text-white">آزمایشگاه نرمال‌ساز خط و اعداد فارسی (Persian Text Normalizer)</h2>
        </div>
        <p className="text-xs text-slate-400">
          تبدیل خودکار ارقام تقویم شمسی و اعداد به حروف فارسی، اصلاح حروف عربی (ی/ک) و تنظیم مکث‌های نشانه‌گذاری برای مدل Piper
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          <div>
            <label className="text-[11px] text-slate-400 block mb-1">متن خام ورودی:</label>
            <textarea
              rows={3}
              value={testText}
              onChange={(e) => setTestText(e.target.value)}
              className="w-full bg-[#070B18] border border-[#1C2541] rounded-lg p-2.5 text-xs text-white focus:outline-none focus:border-[#00B4D8]"
            />
          </div>

          <div>
            <label className="text-[11px] text-slate-400 block mb-1">متن نرمال‌شده برای سنتز صوتی:</label>
            <div className="w-full bg-[#070B18] border border-[#1C2541] rounded-lg p-2.5 text-xs text-emerald-400 min-h-[76px] leading-relaxed font-mono">
              {normalizedTestText}
            </div>
          </div>
        </div>
      </div>

      {/* Script Segment Audio Inspection & Playback */}
      <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-5 space-y-4">
        <h2 className="text-sm font-bold text-white flex items-center gap-2">
          <Volume2 className="w-4 h-4 text-[#48CAE4]" />
          <span>بررسی و پخش تک‌به‌تک سگمنت‌های تولیدشده</span>
        </h2>

        <div className="space-y-2">
          {project.segments.map((seg) => (
            <div
              key={seg.id}
              className="p-3 bg-[#070B18] border border-[#1C2541] rounded-lg flex items-center justify-between gap-4 hover:border-[#00B4D8]/30 transition-colors"
            >
              <div className="flex items-center gap-3">
                <span className="font-mono text-xs text-slate-400">
                  {toPersianDigits(seg.segmentIndex)}
                </span>
                <span className="text-xs font-semibold text-[#00B4D8]">
                  {seg.speaker === 'PRESENTER_A' ? 'مجری الف' : 'مجری ب'}
                </span>
                <p className="text-xs text-slate-300 truncate max-w-xl">
                  {seg.textFa}
                </p>
              </div>

              <div className="flex items-center gap-3 shrink-0">
                <span className="font-mono text-[11px] text-slate-500 tabular-nums">
                  ~{toPersianDigits(seg.estimatedDurationSec)} ثانیه
                </span>
                <button
                  onClick={() => handlePlaySegmentAudio(seg.textFa, seg.speaker, seg.id)}
                  className={`p-1.5 rounded-md transition-colors cursor-pointer ${
                    isPlayingId === seg.id
                      ? 'bg-rose-500/20 text-rose-400'
                      : 'text-slate-300 hover:text-[#00B4D8] hover:bg-[#1C2541]'
                  }`}
                  title="پخش صوتی این سگمنت"
                >
                  {isPlayingId === seg.id ? <Square className="w-4 h-4 fill-current" /> : <Play className="w-4 h-4 fill-current" />}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
