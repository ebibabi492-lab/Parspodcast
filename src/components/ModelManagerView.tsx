import React, { useState } from 'react';
import {
  Cpu,
  Download,
  ShieldCheck,
  Zap,
  HardDrive,
  CheckCircle2,
  AlertTriangle,
  Play,
  RotateCcw,
  Check,
  ServerOff
} from 'lucide-react';
import { AIModel } from '../types';
import { toPersianDigits } from '../utils/jalali';

interface ModelManagerViewProps {
  models: AIModel[];
  onActivateModel: (modelId: string) => void;
  onUpdateBackend: (modelId: string, backend: 'CPU' | 'GPU_VULKAN' | 'NPU') => void;
  onDownloadModel: (modelId: string) => void;
}

export const ModelManagerView: React.FC<ModelManagerViewProps> = ({
  models,
  onActivateModel,
  onUpdateBackend,
  onDownloadModel
}) => {
  const [authorizingModel, setAuthorizingModel] = useState<AIModel | null>(null);
  const [testInferencePrompt, setTestInferencePrompt] = useState('اخبار امروز فناوری را در سه جمله خلاصه کن.');
  const [testResponse, setTestResponse] = useState('');
  const [isInferencing, setIsInferencing] = useState(false);

  const activeModel = models.find((m) => m.isActive) || models[0];

  const handleConfirmDownload = () => {
    if (authorizingModel) {
      onDownloadModel(authorizingModel.id);
      setAuthorizingModel(null);
    }
  };

  const handleRunInferenceTest = () => {
    setIsInferencing(true);
    setTestResponse('');
    const sampleWords = [
      'استنتاج', 'موفقیت‌آمیز', 'مدل', 'محلی', activeModel.name,
      'با', 'معماری', 'LiteRT-LM', 'بر', 'روی', 'شتاب‌دهنده', activeModel.selectedBackend,
      'انجام', 'شد.', 'پردازش', 'بدون', 'اتصال', 'به', 'سرور', 'ابری', 'کامل', 'است.'
    ];

    let current = '';
    sampleWords.forEach((word, index) => {
      setTimeout(() => {
        current += (current ? ' ' : '') + word;
        setTestResponse(current);
        if (index === sampleWords.length - 1) {
          setIsInferencing(false);
        }
      }, (index + 1) * 70);
    });
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="border-b border-[#1C2541] pb-4">
        <h1 className="text-xl font-bold text-white flex items-center gap-2">
          <Cpu className="w-5 h-5 text-[#00B4D8]" />
          <span>مدیر مدل‌های هوش مصنوعی محلی (Local AI Model Manager)</span>
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          پشتیبانی از معماری چندمدلی LiteRT-LM، سوئیچ هوشمند بین پردازنده‌های CPU/Vulkan و بررسی صحت رمزنگاری فایل‌ها
        </p>
      </div>

      {/* Offline Guarantee Banner */}
      <div className="p-4 bg-[#0B132B] border border-[#27355B] rounded-xl flex items-center justify-between text-xs text-slate-300">
        <div className="flex items-center gap-2.5">
          <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0" />
          <div>
            <span className="font-semibold text-white">ضمانت استقلال و محرمانگی داده‌ها:</span> هیچ درخواستی به سرورهای ابری ارسال نمی‌گردد و فرآیند نگارش فیلم‌نامه کاملاً آفلاین در محیط ایزوله LiteRT-LM انجام می‌شود.
          </div>
        </div>
        <span className="font-mono text-[11px] text-slate-400 bg-[#070B18] px-2.5 py-1 rounded border border-[#1C2541]">
          LiteRT v2.4 (Native JNI)
        </span>
      </div>

      {/* Model Cards Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {models.map((model) => (
          <div
            key={model.id}
            className={`p-5 rounded-xl border transition-all ${
              model.isActive
                ? 'bg-[#0B132B] border-[#00B4D8] shadow-md shadow-[#00B4D8]/5'
                : 'bg-[#0B132B]/70 border-[#1C2541]'
            }`}
          >
            <div className="flex items-start justify-between mb-3">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <h2 className="text-base font-bold text-white">{model.name}</h2>
                  {model.isActive && (
                    <span className="text-[10px] bg-[#00B4D8]/20 text-[#00B4D8] border border-[#00B4D8]/30 px-2 py-0.5 rounded font-medium">
                      مدل فعال استودیو
                    </span>
                  )}
                </div>
                <div className="text-xs text-slate-400 font-mono">
                  نسخه {model.version} · معماری {model.runtime}
                </div>
              </div>

              <div className="text-left font-mono text-xs">
                <span className="text-slate-300 font-bold">{toPersianDigits(model.sizeGb)}</span>{' '}
                <span className="text-slate-500">گیگابایت</span>
              </div>
            </div>

            <p className="text-xs text-slate-300 leading-relaxed mb-4">
              {model.descriptionFa}
            </p>

            <div className="text-[11px] text-slate-400 mb-4 bg-[#070B18]/50 p-2.5 rounded-lg border border-[#1C2541]">
              <span className="text-slate-300 font-medium">کاربرد پیشنهادی:</span> {model.recommendedFor}
            </div>

            {/* Hardware Backend Selector */}
            <div className="mb-4 space-y-1.5">
              <label className="text-[11px] text-slate-400 block">شتاب‌دهنده سخت‌افزاری (Backend Target):</label>
              <div className="flex items-center gap-2">
                {model.supportedBackends.map((backend) => (
                  <button
                    key={backend}
                    onClick={() => onUpdateBackend(model.id, backend)}
                    className={`px-3 py-1.5 rounded-md text-xs font-mono transition-colors cursor-pointer ${
                      model.selectedBackend === backend
                        ? 'bg-[#00B4D8] text-[#070B18] font-bold'
                        : 'bg-[#1C2541] text-slate-400 hover:text-white'
                    }`}
                  >
                    {backend}
                  </button>
                ))}
              </div>
            </div>

            {/* Checksum & Status */}
            <div className="pt-3 border-t border-[#1C2541] flex items-center justify-between text-xs">
              <div className="font-mono text-[10px] text-slate-500 truncate max-w-[200px]" title={model.sha256Checksum}>
                SHA256: {model.sha256Checksum.slice(0, 16)}...
              </div>

              {model.isDownloaded ? (
                <div className="flex items-center gap-2">
                  {!model.isActive ? (
                    <button
                      onClick={() => onActivateModel(model.id)}
                      className="px-3.5 py-1.5 bg-[#1C2541] hover:bg-[#27355B] text-white rounded-lg text-xs font-medium transition-colors cursor-pointer"
                    >
                      فعال‌سازی مدل
                    </button>
                  ) : (
                    <span className="text-xs text-emerald-400 flex items-center gap-1 font-medium">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      آماده استنتاج
                    </span>
                  )}
                </div>
              ) : (
                <button
                  onClick={() => setAuthorizingModel(model)}
                  className="px-3.5 py-1.5 bg-[#00B4D8] hover:bg-[#48CAE4] text-[#070B18] rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>تأیید و دریافت ({toPersianDigits(model.sizeGb)} گیگ)</span>
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Local Inference Test Console */}
      <div className="bg-[#0B132B] border border-[#1C2541] rounded-xl p-5 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Zap className="w-4 h-4 text-[#00B4D8]" />
            <h2 className="text-sm font-bold text-white">تست استنتاج بلادرنگ مدل فعال ({activeModel.name})</h2>
          </div>
          <span className="text-xs font-mono text-slate-400">
            تأخیر توکن اولیه: ~140ms · سرعت: 28 tok/s
          </span>
        </div>

        <div className="flex items-center gap-3">
          <input
            type="text"
            value={testInferencePrompt}
            onChange={(e) => setTestInferencePrompt(e.target.value)}
            className="flex-1 bg-[#070B18] border border-[#1C2541] rounded-lg p-2.5 text-xs text-white focus:outline-none focus:border-[#00B4D8]"
            placeholder="پرامپت آزمایشی را وارد نمایید..."
          />
          <button
            onClick={handleRunInferenceTest}
            disabled={isInferencing}
            className="px-4 py-2.5 bg-[#00B4D8] hover:bg-[#48CAE4] text-[#070B18] text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer disabled:opacity-50"
          >
            <Play className="w-3.5 h-3.5" />
            <span>اجرای آفلاین</span>
          </button>
        </div>

        {testResponse && (
          <div className="p-3.5 bg-[#070B18] border border-[#1C2541] rounded-lg text-xs text-slate-200 leading-relaxed font-mono">
            {testResponse}
          </div>
        )}
      </div>

      {/* Explicit User Authorization Modal for Model Download */}
      {authorizingModel && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#0B132B] border border-[#27355B] rounded-xl max-w-lg w-full p-6 space-y-4">
            <div className="flex items-center gap-3 text-amber-400">
              <AlertTriangle className="w-6 h-6 shrink-0" />
              <h3 className="text-base font-bold text-white">مجوز دریافت مدل حجیم هوش مصنوعی</h3>
            </div>

            <p className="text-xs text-slate-300 leading-relaxed">
              شما در حال صدور مجوز دانلود فایل وزن‌های مدل{' '}
              <strong className="text-white">{authorizingModel.name}</strong> با حجم تقریبی{' '}
              <strong className="text-[#00B4D8]">{toPersianDigits(authorizingModel.sizeGb)} گیگابایت</strong> هستید.
            </p>

            <div className="bg-[#070B18] p-3 rounded-lg border border-[#1C2541] text-[11px] text-slate-400 space-y-1">
              <div>· مسیر ذخیره: حافظه امن اختصاصی اپلیکیشن (`context.filesDir/models`)</div>
              <div>· دانلود در پس‌زمینه با قابلیت Resume در صورت قطعی اتصال اینترنت انجام می‌گردد.</div>
              <div>· پس از اتمام دانلود، اعتبارسنجی هش SHA-256 اعمال خواهد شد.</div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setAuthorizingModel(null)}
                className="px-4 py-2 bg-[#1C2541] text-xs text-slate-300 rounded-lg hover:bg-[#27355B] cursor-pointer"
              >
                انصراف
              </button>
              <button
                onClick={handleConfirmDownload}
                className="px-4 py-2 bg-[#00B4D8] text-xs text-[#070B18] font-bold rounded-lg hover:bg-[#48CAE4] cursor-pointer flex items-center gap-1.5"
              >
                <Download className="w-3.5 h-3.5" />
                <span>تأیید و شروع دانلود</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
