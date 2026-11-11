import React, { useState } from 'react';
import { Header } from './components/Header';
import { Sidebar } from './components/Sidebar';
import { DashboardView } from './components/DashboardView';
import { NewsCenterView } from './components/NewsCenterView';
import { ModelManagerView } from './components/ModelManagerView';
import { ScriptStudioView } from './components/ScriptStudioView';
import { VoiceStudioView } from './components/VoiceStudioView';
import { AudioMixingView } from './components/AudioMixingView';
import { JalaliArchiveView } from './components/JalaliArchiveView';
import { PodcastPublisherView } from './components/PodcastPublisherView';
import { AndroidArchitectureView } from './components/AndroidArchitectureView';

import {
  INITIAL_NEWS_SOURCES,
  INITIAL_NEWS_ARTICLES,
  INITIAL_NEWS_EVENTS,
  INITIAL_AI_MODELS,
  INITIAL_VOICE_PROFILES,
  SAMPLE_SCRIPT_PROJECT,
  INITIAL_AUDIO_TRACKS,
  INITIAL_EPISODES
} from './data/seedData';
import {
  NewsSource,
  NewsArticle,
  NewsEvent,
  AIModel,
  VoiceProfile,
  ScriptProject,
  AudioTrack,
  PodcastEpisode,
  ProgramType
} from './types';
import { gregorianToJalali } from './utils/jalali';
import { audioEngine } from './utils/audioEngine';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<string>('dashboard');
  const [newsSources, setNewsSources] = useState<NewsSource[]>(INITIAL_NEWS_SOURCES);
  const [newsArticles, setNewsArticles] = useState<NewsArticle[]>(INITIAL_NEWS_ARTICLES);
  const [newsEvents, setNewsEvents] = useState<NewsEvent[]>(INITIAL_NEWS_EVENTS);
  const [aiModels, setAiModels] = useState<AIModel[]>(INITIAL_AI_MODELS);
  const [voiceProfiles, setVoiceProfiles] = useState<VoiceProfile[]>(INITIAL_VOICE_PROFILES);
  const [currentProject, setCurrentProject] = useState<ScriptProject>(SAMPLE_SCRIPT_PROJECT);
  const [audioTracks, setAudioTracks] = useState<AudioTrack[]>(INITIAL_AUDIO_TRACKS);
  const [episodes, setEpisodes] = useState<PodcastEpisode[]>(INITIAL_EPISODES);

  const activeModel = aiModels.find((m) => m.isActive) || aiModels[0];
  const activeVoice = voiceProfiles.find((v) => v.role === 'PRESENTER_A') || voiceProfiles[0];

  // News Handlers
  const handleToggleEventSelection = (eventId: string) => {
    setNewsEvents((prev) =>
      prev.map((e) => (e.id === eventId ? { ...e, isSelected: !e.isSelected } : e))
    );
  };

  const handleRefreshSources = () => {
    setNewsSources((prev) =>
      prev.map((s) => ({
        ...s,
        lastFetchedUtc: Date.now(),
        articleCount: s.articleCount + Math.floor(Math.random() * 3 + 1)
      }))
    );
  };

  const handleAddSource = (sourceData: Partial<NewsSource>) => {
    const newSrc: NewsSource = {
      id: `src_${Date.now()}`,
      name: sourceData.name || 'منبع جدید',
      feedUrl: sourceData.feedUrl || '',
      parserType: sourceData.parserType || 'RSS',
      category: sourceData.category || 'فناوری',
      updateIntervalMins: sourceData.updateIntervalMins || 30,
      isActive: true,
      lastFetchedUtc: Date.now(),
      articleCount: 12
    };
    setNewsSources((prev) => [...prev, newSrc]);
  };

  // Model Handlers
  const handleActivateModel = (modelId: string) => {
    setAiModels((prev) =>
      prev.map((m) => ({
        ...m,
        isActive: m.id === modelId
      }))
    );
  };

  const handleUpdateBackend = (modelId: string, backend: 'CPU' | 'GPU_VULKAN' | 'NPU') => {
    setAiModels((prev) =>
      prev.map((m) => (m.id === modelId ? { ...m, selectedBackend: backend } : m))
    );
  };

  const handleDownloadModel = (modelId: string) => {
    setAiModels((prev) =>
      prev.map((m) =>
        m.id === modelId
          ? {
              ...m,
              isDownloaded: true,
              downloadProgress: 100,
              isActive: true
            }
          : { ...m, isActive: false }
      )
    );
  };

  // Start New Production
  const handleStartNewProgram = (type: ProgramType) => {
    const jalali = gregorianToJalali(new Date());
    const newProj: ScriptProject = {
      id: `proj_${Date.now()}`,
      title: type === 'TWO_PRESENTERS' ? 'پادکست گفتگوی دوگوینده تحلیلی' : 'بولتن خبری روزانه پارس‌پادکست',
      programType: type,
      modelIdUsed: activeModel.id,
      jalaliDateStr: jalali.formattedStr,
      createdAtUtc: Date.now(),
      updatedAtUtc: Date.now(),
      status: 'DRAFT',
      formalityLevel: 'نیمه‌رسمی تحلیلی',
      analysisDepth: 'عمیق و تحلیلی',
      selectedEventIds: newsEvents.filter((e) => e.isSelected).map((e) => e.id),
      segments: [
        {
          id: `seg_${Date.now()}_1`,
          segmentIndex: 1,
          type: 'INTRO',
          speaker: 'PRESENTER_A',
          textFa: `به نام خدا. با بسته خبری استودیو پارس‌پادکست در ${jalali.formattedStr} در خدمت شما هستیم.`,
          estimatedDurationSec: 12,
          isAudioGenerated: false,
          sourceIds: []
        },
        {
          id: `seg_${Date.now()}_2`,
          segmentIndex: 2,
          type: 'NEWS',
          speaker: 'PRESENTER_A',
          textFa: 'سرخط مهم‌ترین اخبار امروز بر پایه تجمیع گزارش‌های خبرگزاری‌های معتبر کشور...',
          estimatedDurationSec: 20,
          isAudioGenerated: false,
          sourceIds: []
        },
        ...(type === 'TWO_PRESENTERS'
          ? [
              {
                id: `seg_${Date.now()}_3`,
                segmentIndex: 3,
                type: 'DISCUSSION' as const,
                speaker: 'PRESENTER_B' as const,
                textFa: 'سلام به همه شنوندگان گرامی. نکته کلیدی در گزارش‌های امروز، حرکت سریع به سوی پردازش محلی هوش مصنوعی است.',
                estimatedDurationSec: 18,
                isAudioGenerated: false,
                sourceIds: []
              }
            ]
          : []),
        {
          id: `seg_${Date.now()}_4`,
          segmentIndex: type === 'TWO_PRESENTERS' ? 4 : 3,
          type: 'OUTRO',
          speaker: type === 'TWO_PRESENTERS' ? 'PRESENTER_B' : 'PRESENTER_A',
          textFa: 'سپاس از اینکه شنونده این بخش خبری بودید. روز و روزگار بر شما خوش.',
          estimatedDurationSec: 10,
          isAudioGenerated: false,
          sourceIds: []
        }
      ]
    };
    setCurrentProject(newProj);
    setCurrentScreen('script-studio');
  };

  const handlePlayPreview = () => {
    audioEngine.playSpeechUtterance(
      'استودیوی پارس‌پادکست آماده اجرای فرآیند است.',
      'PRESENTER_A'
    );
  };

  return (
    <div className="min-h-screen bg-[#070B18] text-[#E2E8F0] flex flex-col antialiased">
      {/* Universal Top Bar Contract */}
      <Header
        activeScreen={currentScreen}
        onNavigate={setCurrentScreen}
        activeModelName={activeModel.name}
        activeVoiceName={activeVoice.modelName.split(' ')[0]}
        onNewProduction={() => handleStartNewProgram('TWO_PRESENTERS')}
      />

      {/* Main Studio Frame */}
      <div className="flex-1 flex w-full">
        {/* Navigation Sidebar */}
        <Sidebar
          currentScreen={currentScreen}
          onSelectScreen={setCurrentScreen}
          newsCount={newsEvents.length}
          modelsCount={aiModels.length}
        />

        {/* Dynamic Workspace Canvas */}
        <main className="flex-1 p-6 overflow-y-auto max-w-7xl mx-auto w-full">
          {currentScreen === 'dashboard' && (
            <DashboardView
              currentProject={currentProject}
              newsEvents={newsEvents}
              aiModels={aiModels}
              voiceProfiles={voiceProfiles}
              episodes={episodes}
              onNavigate={setCurrentScreen}
              onStartNewProgram={handleStartNewProgram}
              onPlayPreview={handlePlayPreview}
            />
          )}

          {currentScreen === 'news-center' && (
            <NewsCenterView
              sources={newsSources}
              events={newsEvents}
              articles={newsArticles}
              onToggleEventSelection={handleToggleEventSelection}
              onRefreshSources={handleRefreshSources}
              onAddSource={handleAddSource}
            />
          )}

          {currentScreen === 'model-manager' && (
            <ModelManagerView
              models={aiModels}
              onActivateModel={handleActivateModel}
              onUpdateBackend={handleUpdateBackend}
              onDownloadModel={handleDownloadModel}
            />
          )}

          {currentScreen === 'script-studio' && (
            <ScriptStudioView
              project={currentProject}
              newsEvents={newsEvents}
              activeModel={activeModel}
              onUpdateProject={setCurrentProject}
              onNavigateToVoice={() => setCurrentScreen('voice-studio')}
            />
          )}

          {currentScreen === 'voice-studio' && (
            <VoiceStudioView
              voiceProfiles={voiceProfiles}
              project={currentProject}
              onUpdateProfiles={setVoiceProfiles}
              onNavigateToMixer={() => setCurrentScreen('audio-mixer')}
            />
          )}

          {currentScreen === 'audio-mixer' && (
            <AudioMixingView
              tracks={audioTracks}
              project={currentProject}
              onUpdateTracks={setAudioTracks}
              onNavigateToPublisher={() => setCurrentScreen('publisher')}
            />
          )}

          {currentScreen === 'jalali-archive' && (
            <JalaliArchiveView
              currentProject={currentProject}
              episodes={episodes}
              onRestoreProject={(p) => {
                setCurrentProject(p);
                setCurrentScreen('script-studio');
              }}
            />
          )}

          {currentScreen === 'publisher' && (
            <PodcastPublisherView
              project={currentProject}
              episodes={episodes}
              onAddEpisode={(ep) => setEpisodes((prev) => [ep, ...prev])}
            />
          )}

          {currentScreen === 'android-architecture' && (
            <AndroidArchitectureView />
          )}
        </main>
      </div>
    </div>
  );
}
