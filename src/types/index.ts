export type ProgramType = 'SINGLE_PRESENTER' | 'TWO_PRESENTERS';

export type SpeakerRole = 'PRESENTER_A' | 'PRESENTER_B';

export type SegmentType = 'INTRO' | 'NEWS' | 'DISCUSSION' | 'TRANSITION' | 'OUTRO';

export interface NewsSource {
  id: string;
  name: string;
  feedUrl: string;
  parserType: 'RSS' | 'ATOM' | 'HTML_CUSTOM';
  category: 'فناوری' | 'اقتصاد' | 'جامعه' | 'علم و دانش' | 'بین‌الملل';
  updateIntervalMins: number;
  isActive: boolean;
  lastFetchedUtc: number;
  articleCount: number;
}

export interface NewsArticle {
  id: string;
  sourceId: string;
  sourceName: string;
  title: string;
  content: string;
  originalUrl: string;
  publishedAtUtc: number;
  rawSummary: string;
  category: string;
}

export interface NewsEvent {
  id: string;
  canonicalTitle: string;
  eventSummary: string;
  category: string;
  firstReportedUtc: number;
  lastReportedUtc: number;
  reportCount: number;
  sources: { sourceId: string; sourceName: string; articleId: string; articleTitle: string }[];
  primaryFacts: string[];
  verifiedDifferentials: string[]; // Variations between reports
  isSelected: boolean;
}

export interface AIModel {
  id: string;
  name: string;
  version: string;
  sizeGb: number;
  descriptionFa: string;
  runtime: 'LiteRT-LM';
  supportedBackends: ('CPU' | 'GPU_VULKAN' | 'NPU')[];
  selectedBackend: 'CPU' | 'GPU_VULKAN' | 'NPU';
  sha256Checksum: string;
  isDownloaded: boolean;
  downloadProgress: number; // 0-100
  isActive: boolean;
  memoryUsageMb: number;
  recommendedFor: string;
}

export interface ScriptSegment {
  id: string;
  segmentIndex: number;
  type: SegmentType;
  speaker: SpeakerRole;
  textFa: string;
  estimatedDurationSec: number;
  generatedAudioBlobUrl?: string;
  isAudioGenerated: boolean;
  sourceIds: string[];
}

export interface ScriptProject {
  id: string;
  title: string;
  programType: ProgramType;
  modelIdUsed: string;
  jalaliDateStr: string;
  createdAtUtc: number;
  updatedAtUtc: number;
  status: 'DRAFT' | 'SCRIPTED' | 'VOICED' | 'MIXED' | 'PUBLISHED';
  formalityLevel: 'رسمی خبری' | 'نیمه‌رسمی تحلیلی' | 'صمیمی و پویا';
  analysisDepth: 'فشرده و سریع' | 'عمیق و تحلیلی' | 'گفتگوی چالشی';
  selectedEventIds: string[];
  segments: ScriptSegment[];
}

export interface VoiceProfile {
  id: string;
  role: SpeakerRole;
  titleFa: string;
  characterDescriptionFa: string;
  modelName: string; // fa_IR-mana-medium, etc.
  engineType: 'SHERPA_ONNX_PIPER';
  speechRate: number; // 0.8 to 1.4
  pitch: number;
  isInstalled: boolean;
  sampleAudioUrl?: string;
}

export interface AudioTrack {
  id: string;
  type: 'INTRO' | 'PRESENTER_A' | 'PRESENTER_B' | 'BGM' | 'SFX' | 'OUTRO';
  nameFa: string;
  volume: number; // 0.0 to 1.5
  isMuted: boolean;
  isSolo: boolean;
  duckingEnabled: boolean;
  pan: number; // -1 to +1
  audioBuffer?: AudioBuffer;
}

export interface DuckingSettings {
  enabled: boolean;
  attenuationDb: number; // e.g. -14dB
  attackMs: number;
  releaseMs: number;
}

export interface PodcastEpisode {
  id: string; // GUID
  projectId: string;
  title: string;
  descriptionFa: string;
  jalaliPubDate: string;
  pubDateRfc822: string;
  enclosureUrl: string;
  fileSizeBytes: number;
  mimeType: string;
  durationSeconds: number;
  category: string;
  artworkUrl: string;
  publishingStatus: 'DRAFT' | 'READY' | 'UPLOADING' | 'PUBLISHED' | 'SCHEDULED' | 'FAILED';
}

export interface JalaliDateParts {
  year: number;
  month: number;
  day: number;
  dayOfWeek: number;
  monthName: string;
  weekdayName: string;
  formattedStr: string;
}
