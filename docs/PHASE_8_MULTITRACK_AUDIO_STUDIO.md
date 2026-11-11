# ParsPodcast — Phase 8 Documentation: Professional Offline Multitrack Audio Studio

## 1. Executive Summary
Phase 8 implements a professional, fully offline multitrack audio editing and mixing studio for the **ParsPodcast (پارس‌پادکست)** Android application. The studio automatically imports voice segments synthesized by **Sherpa-ONNX Mana Piper** (Phase 6), allows arrangement alongside local background music and sound effects, and mixes all tracks with broadcast-quality precision into a 48,000 Hz 16-bit Stereo WAV master file.

All operations execute 100% locally on the device (targeting Daria Band on Android 15) without cloud dependencies.

---

## 2. Multitrack Audio Architecture

```
 ┌───────────────────────────────────────────────────────────────┐
 │                   ParsPodcast Audio Mixer                     │
 └──────────────────────────────┬────────────────────────────────┘
                                │
        ┌───────────────────────┴───────────────────────┐
        ▼                                               ▼
 [Phase 6 Voice Segments]                     [Local Music / SFX]
 (22,050 Hz Mono WAV files)                   (44.1k / 48k WAV/MP3)
        │                                               │
        └───────────────────────┬───────────────────────┘
                                ▼
 ┌───────────────────────────────────────────────────────────────┐
 │           Audio Decoding & Sample-Rate Conversion             │
 │  - WavFileReader: 16-bit PCM Linear Reader                    │
 │  - Resampling: High-accuracy linear interpolation to 48kHz    │
 └──────────────────────────────┬────────────────────────────────┘
                                ▼
 ┌───────────────────────────────────────────────────────────────┐
 │               Timeline Track & Clip Arrangement               │
 │  - Track 1: INTRO (آرم آغازین)                                │
 │  - Track 2: PRESENTER_A (مجری الف - گوینده خبر)                │
 │  - Track 3: PRESENTER_B (مجری ب - کارشناس تحلیل)               │
 │  - Track 4: BGM (موسیقی پس‌زمینه با داکینگ خودکار)             │
 │  - Track 5: SFX (افکت‌های صوتی و ترنزیشن‌ها)                   │
 │  - Track 6: OUTRO (تیتراژ پایانی)                             │
 └──────────────────────────────┬────────────────────────────────┘
                                ▼
 ┌───────────────────────────────────────────────────────────────┐
 │             Non-Destructive Editing Engine                    │
 │  - Non-destructive Trimming (sourceStartMs, sourceEndMs)      │
 │  - Non-destructive Splitting at Playhead                      │
 │  - Constant-Power Stereo Panning:                             │
 │      Left = cos((pan + 1) * π / 4)                            │
 │      Right = sin((pan + 1) * π / 4)                           │
 │  - Smooth Fade-in & Fade-out Envelopes                        │
 │  - Automatic Voice Activity Ducking (-12 dB on BGM)           │
 │  - Snapshot-based Undo / Redo (TimelineHistoryManager)        │
 └──────────────────────────────┬────────────────────────────────┘
                                ▼
 ┌───────────────────────────────────────────────────────────────┐
 │                  Synchronized Preview Player                  │
 │  - Media3 ExoPlayer with background preview buffer            │
 │  - Sample-accurate playback across all tracks without drift   │
 └──────────────────────────────┬────────────────────────────────┘
                                ▼
 ┌───────────────────────────────────────────────────────────────┐
 │            Master Processing & Final WAV Export               │
 │  - Master Volume Multiplication                               │
 │  - Anti-pop project boundary fades (100ms in, 300ms out)      │
 │  - Soft-knee Limiter via tanh compression to prevent clipping │
 │  - Standard 44-byte RIFF/WAVE header, 16-bit Stereo PCM       │
 │  - Registered into Room as finalAudioPath                     │
 └───────────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Audio Processing Pipeline

### 3.1 Internal Mixing Format
- **Data Type:** Float32 PCM normalized to `[-1.0f, 1.0f]`
- **Sample Rate:** 48,000 Hz (Broadcast Industry Standard)
- **Channels:** 2 (Stereo: Left & Right interleaved)

### 3.2 Sample-Rate Conversion (Resampling)
Source audio generated in Phase 6 uses 22,050 Hz (the native Mana Piper model rate). The `WavFileReader.resample` component converts source samples to 48,000 Hz using linear interpolation:
$$\text{Sample}_{\text{interpolated}} = S_{\text{low}} + \text{frac} \times (S_{\text{high}} - S_{\text{low}})$$
This preserves pitch and timing accuracy without audio artifacts.

### 3.3 Stereo Panning (Constant Power)
Mono voice tracks and stereo instruments are panned smoothly across the stereo field using constant power trigonometric laws:
- $\text{Angle} = (\text{pan} + 1.0) \times \frac{\pi}{4}$
- $\text{Gain}_{\text{left}} = \cos(\text{Angle})$
- $\text{Gain}_{\text{right}} = \sin(\text{Angle})$
This ensures perceived acoustic loudness remains constant as a clip is panned between left and right.

### 3.4 Soft-Knee Limiter (Clipping Protection)
When multiple audio tracks sum together, cumulative peaks might exceed digital full scale ($> 1.0$). Rather than allowing harsh integer wrap-around distortion, `MultitrackAudioMixer` applies a soft-knee hyperbolic tangent (`tanh`) compression to peaks exceeding $|0.95f|$:
$$\text{Sample}_{\text{limited}} = \tanh(\text{Sample})$$
This protects listening devices and produces warm analog-like saturation rather than digital clicks.

---

## 4. Jetpack Compose Multitrack Studio UI
- **Timeline Ruler:** Time ruler with dynamic major (10s) and minor ticks scaled by the timeline zoom level.
- **Waveform Canvas:** Fast downsampled peak extraction (`WaveformExtractor`) rendering audio waveforms within each clip block.
- **Track Controls:** Track Mute (`M`), Track Solo (`S`), Ducking toggle (`DUCK`), volume slider and pan slider.
- **Synchronized Transport:** Scrubber bar, current time elapsed / total duration, Master volume, Play/Pause/Stop.
- **Clip Properties Sheet:** Contextual panel for fine-tuning clip start time, source trim, gain in decibels, fade-in ms, fade-out ms, splitting, and duplication.
- **Render & Export Dialog:** Offers 48 kHz (Broadcast standard) and 44.1 kHz (CD quality) export, shows real-time progress bar, and saves the resulting file to Room.

---

## 5. Offline & Resource Safety (Daria Band / Android 15)
- **Streaming & Chunked Buffers:** Avoids allocating massive memory arrays by chunking mix operations.
- **Background Dispatchers:** Heavy PCM decoding and mixing operations run strictly on `Dispatchers.Default` and `Dispatchers.IO`, keeping the Compose UI responsive at 120 FPS.
- **Zero Cloud Leakage:** All mixing and decoding execute entirely offline without sending audio to cloud servers.
