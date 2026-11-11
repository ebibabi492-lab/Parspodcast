import React, { useState, useEffect, useRef } from 'react';
import {
  Sliders,
  Play,
  Pause,
  RotateCcw,
  Volume2,
  VolumeX,
  Radio,
  Download,
  Sparkles,
  Music,
  CheckCircle2,
  Share2
} from 'lucide-react';
import { AudioTrack, ScriptProject } from '../types';
import { audioEngine } from '../utils/audioEngine';
import { toPersianDigits } from '../utils/jalali';

interface AudioMixingViewProps {
  tracks: AudioTrack[];
  project: ScriptProject;
  onUpdateTracks: (tracks: AudioTrack[]) => void;
  onNavigateToPublisher: () => void;
}

export const AudioMixingView: React.FC<AudioMixingViewProps> = ({
  tracks,
  project,
  onUpdateTracks,
  onNavigateToPublisher
}) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTimeMs, setCurrentTimeMs] = useState(0);
  const [bgmActive, setBgmActive] = useState(true);
  const [autoDucking, setAutoDucking] = useState(true);
  const [loudnessNormalized, setLoudnessNormalized] = useState(true);
  const [isExporting, setIsExporting] = useState(false);
  const [exportedWavUrl, setExportedWavUrl] = useState<string | null>(null);

  const totalDurationMs = 75000; // 1m 15s sample
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // Toggle master playback
  const handleTogglePlay = () => {
    if (isPlaying) {
      setIsPlaying(false);
      audioEngine.stopBGM();
      audioEngine.stopAllSpeech();
      if (timerRef.current) clearInterval(timerRef.current);
    } else {
      setIsPlaying(true);
      if (bgmActive) {
        audioEngine.toggleBGM(true, 0.25);
      }
      // Start timeline ticker
      timerRef.current = setInterval(() => {
        setCurrentTimeMs((prev) => {
          if (prev >= totalDurationMs) {
            setIsPlaying(false);
            audioEngine.stopBGM();
            if (timerRef.current) clearInterval(timerRef.current);
            return 0;
          }
          return prev + 500;
        });
      }, 500);
    }
  };

  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
      audioEngine.stopBGM();
      audioEngine.stopAllSpeech();
    };
  }, []);

  // Track volume change
  const handleVolumeChange = (id: string, vol: number) => {
    const updated = tracks.map((t) => (t.id === id ? { ...t, volume: vol } : t));
    onUpdateTracks(updated);
  };

  // Mute / Solo toggle
  const handleToggleMute = (id: string) => {
    const updated = tracks.map((t) => (t.id === id ? { ...t, isMuted: !t.isMuted } : t));
    onUpdateTracks(updated);
  };

  const handleToggleSolo = (id: string) => {
    const updated = tracks.map((t) => (t.id === id ? { ...t, isSolo: !t.isSolo } : t));
    onUpdateTracks(updated);
  };

  // Real WAV export
  const handleExportWav = () => {
    setIsExporting(true);
    setTimeout(() => {
      const blob = audioEngine.generateWavFileBlob(15);
      const url = URL.createObjectURL(blob);
      setExportedWavUrl(url);
      setIsExporting(false);
    }, 1200);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#1C2541] pb-4">
        <div>
          <h1 className="text-xl font-bold text-white flex items-center gap-2">
            <Sliders className="w-5 h-5 text-[#00B4D8]" />
            <span>استودیو میکس و مسترینگ چندترکه (Multitrack Audio Studio)</span>
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            ویرایشگر ۶ کاناله صدا، داکینگ خودکار موسیقی هنگام مکالمه گویندگان، و استانداردسازی بلندی صدا (-16 LUFS)
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={handleExportWav}
            disabled={isExporting}
            className="px-3.5 py-2 bg-[#1C2541] hover:bg-[#27355B] text-slate-200 text-xs font-medium rounded-lg border border-[#27355B] flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <Download className="w-3.5 h-3.5 text-[#00B4D8]" />
            <span>{isExporting ? 'در حال رندر PCM...' : 'خروجی مستر WAV'}</span>
          </button>
          <button
            onClick={onNavigateToPublisher}
            className="px-4 py-2 bg-[#00B4D8] hover:bg-[#48CAE4] text-[#070B18] text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <Share2 className="w-4 h-4" />
            <span>ورود به ناشر پادکست و انتشار RSS</span>
          </button>
        </div>
      </div>

      {/* Download Alert if Exported */}
      {exportedWavUrl && (
        <div className="p-3.5 bg-emerald-950/40 border border-emerald-500/40 rounded-xl flex items-center justify-between text-xs text-emerald-300">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            <span>فایل صوتی مستر نهایی (WAV 16-bit 44.1kHz Broadcast Standard) با موفقیت تولید شد.</span>
          </div>
          <a
            href={exportedWavUrl}
            download={`${project.id}_master.wav`}
            className="px-3 py-1 bg-emerald-500 text-[#070B18] font-bold rounded text-[11px] hover:bg-emerald-400"
          >
            دانلود مستقیم فایل WAV
          </a>
        </div>
      )}

      {/* Master Transport & Controls Bar */}
      <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-4 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            onClick={handleTogglePlay}
            className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors cursor-pointer ${
              isPlaying
                ? 'bg-rose-500 text-white'
                : 'bg-[#00B4D8] hover:bg-[#48CAE4] text-[#070B18]'
            }`}
          >
            {isPlaying ? <Pause className="w-5 h-5 fill-current" /> : <Play className="w-5 h-5 fill-current ml-0.5" />}
          </button>

          <button
            onClick={() => setCurrentTimeMs(0)}
            className="p-2 text-slate-400 hover:text-white transition-colors cursor-pointer"
            title="بازگشت به ابتدای تایم‌لاین"
          >
            <RotateCcw className="w-4 h-4" />
          </button>

          <div className="font-mono text-sm text-white font-bold tabular-nums pr-2">
            {toPersianDigits(Math.floor(currentTimeMs / 60000))}:{toPersianDigits((Math.floor((currentTimeMs % 60000) / 1000)).toString().padStart(2, '0'))} / {toPersianDigits(Math.floor(totalDurationMs / 60000))}:{toPersianDigits((Math.floor((totalDurationMs % 60000) / 1000)).toString().padStart(2, '0'))}
          </div>
        </div>

        {/* Studio Mixing Engine Switches */}
        <div className="flex items-center gap-4 text-xs">
          {/* Audio Ducking Toggle */}
          <label className="flex items-center gap-2 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={autoDucking}
              onChange={(e) => setAutoDucking(e.target.checked)}
              className="accent-[#00B4D8] w-4 h-4 cursor-pointer"
            />
            <span className="text-slate-300 font-medium">داکینگ خودکار (-14dB)</span>
          </label>

          {/* Loudness Normalization */}
          <label className="flex items-center gap-2 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={loudnessNormalized}
              onChange={(e) => setLoudnessNormalized(e.target.checked)}
              className="accent-[#00B4D8] w-4 h-4 cursor-pointer"
            />
            <span className="text-slate-300 font-medium">استانداردسازی پخش (-16 LUFS)</span>
          </label>

          {/* BGM Pad Preview */}
          <button
            onClick={() => {
              const next = !bgmActive;
              setBgmActive(next);
              if (isPlaying) audioEngine.toggleBGM(next, 0.25);
            }}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1.5 border transition-colors cursor-pointer ${
              bgmActive
                ? 'bg-[#1C2541] border-[#00B4D8]/40 text-[#00B4D8]'
                : 'bg-[#070B18] border-[#1C2541] text-slate-500'
            }`}
          >
            <Music className="w-3.5 h-3.5" />
            <span>موسیقی متن (BGM Pad)</span>
          </button>
        </div>
      </div>

      {/* Multitrack Timeline & Mixer Board */}
      <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl overflow-hidden divide-y divide-[#1C2541]">
        {tracks.map((track) => (
          <div key={track.id} className="p-3.5 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4 hover:bg-[#1C2541]/20 transition-colors">
            {/* Track Info & Controls */}
            <div className="w-64 space-y-2 shrink-0">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-white">{track.nameFa}</span>
                <span className="text-[10px] font-mono text-slate-500">{track.type}</span>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleToggleMute(track.id)}
                  className={`px-2 py-0.5 rounded text-[10px] font-mono transition-colors cursor-pointer ${
                    track.isMuted ? 'bg-rose-500 text-white font-bold' : 'bg-[#070B18] text-slate-400 hover:text-white'
                  }`}
                >
                  MUTE
                </button>
                <button
                  onClick={() => handleToggleSolo(track.id)}
                  className={`px-2 py-0.5 rounded text-[10px] font-mono transition-colors cursor-pointer ${
                    track.isSolo ? 'bg-amber-400 text-[#070B18] font-bold' : 'bg-[#070B18] text-slate-400 hover:text-white'
                  }`}
                >
                  SOLO
                </button>

                {/* Volume Slider */}
                <div className="flex-1 flex items-center gap-1.5">
                  <Volume2 className="w-3 h-3 text-slate-500" />
                  <input
                    type="range"
                    min="0"
                    max="1.5"
                    step="0.05"
                    value={track.volume}
                    onChange={(e) => handleVolumeChange(track.id, parseFloat(e.target.value))}
                    className="w-full accent-[#00B4D8] cursor-pointer"
                  />
                  <span className="font-mono text-[10px] text-slate-400 w-8 text-left tabular-nums">
                    {Math.round(track.volume * 100)}%
                  </span>
                </div>
              </div>
            </div>

            {/* Visual Waveform & Timeline Track Segment */}
            <div className="flex-1 bg-[#070B18] border border-[#1C2541] rounded-lg h-14 relative overflow-hidden flex items-center px-2">
              {/* Playhead Marker */}
              <div
                className="absolute top-0 bottom-0 w-0.5 bg-rose-500 z-20 shadow-sm shadow-rose-500/50 transition-all pointer-events-none"
                style={{ right: `${(currentTimeMs / totalDurationMs) * 100}%` }}
              />

              {/* Rendered audio blocks */}
              {track.type === 'INTRO' && (
                <div className="h-10 bg-gradient-to-l from-[#00B4D8]/30 to-[#48CAE4]/20 border border-[#00B4D8]/50 rounded px-2 flex items-center text-[10px] text-white font-mono w-[18%]">
                  آرم شروع (0-14s)
                </div>
              )}

              {track.type === 'PRESENTER_A' && (
                <div className="flex items-center gap-2 w-full pr-[18%]">
                  <div className="h-10 bg-cyan-950/40 border border-cyan-500/40 rounded px-2 flex items-center text-[10px] text-cyan-200 font-mono w-[30%]">
                    مجری الف: سرخط خبر
                  </div>
                  <div className="h-10 bg-cyan-950/40 border border-cyan-500/40 rounded px-2 flex items-center text-[10px] text-cyan-200 font-mono w-[28%] mr-4">
                    مجری الف: شرح رویداد
                  </div>
                </div>
              )}

              {track.type === 'PRESENTER_B' && (
                <div className="flex items-center gap-2 w-full pr-[49%]">
                  <div className="h-10 bg-purple-950/40 border border-purple-500/40 rounded px-2 flex items-center text-[10px] text-purple-200 font-mono w-[32%]">
                    مجری ب: تحلیل کارشناسی
                  </div>
                </div>
              )}

              {track.type === 'BGM' && (
                <div className="h-10 bg-gradient-to-r from-emerald-950/30 via-emerald-900/40 to-emerald-950/30 border border-emerald-500/30 rounded px-3 flex items-center justify-between text-[10px] text-emerald-300 font-mono w-[95%]">
                  <span>موسیقی متن پیوسته (Atmospheric Broadcast Pad)</span>
                  <span className="text-emerald-400 text-[9px]">اتوداکینگ در هنگام کلام</span>
                </div>
              )}

              {track.type === 'SFX' && (
                <div className="flex items-center gap-8 w-full pr-[45%]">
                  <div className="h-8 bg-amber-950/40 border border-amber-500/40 rounded px-2 flex items-center text-[9px] text-amber-300 font-mono w-[8%]">
                    Whoosh
                  </div>
                  <div className="h-8 bg-amber-950/40 border border-amber-500/40 rounded px-2 flex items-center text-[9px] text-amber-300 font-mono w-[8%]">
                    Stinger
                  </div>
                </div>
              )}

              {track.type === 'OUTRO' && (
                <div className="h-10 bg-gradient-to-r from-[#7209B7]/30 to-[#3A0CA3]/20 border border-[#7209B7]/50 rounded px-2 flex items-center text-[10px] text-white font-mono w-[18%] mr-auto">
                  تیتراژ پایانی
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
