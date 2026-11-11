# ParsPodcast — Phase 6 Documentation: Real Offline Persian TTS Integration (Sherpa-ONNX + Mana Persian Piper)

## 1. Executive Summary
Phase 6 integrates a real, 100% offline, on-device Persian Text-to-Speech (TTS) synthesis engine into **ParsPodcast (پارس‌پادکست)** using **Sherpa-ONNX** and the **Mana Persian Piper** acoustic voice model (`fa_IR-mana-medium.onnx`).

The generated voice tracks are written as standard 16-bit Mono PCM WAV audio files at 22,050 Hz and are fully persisted with metadata in the local Room database, ready for preview and downstream Phase 8 multitrack audio mixing.

---

## 2. Model & Runtime Specifications

| Parameter | Value |
| :--- | :--- |
| **TTS Engine** | Sherpa-ONNX (`com.github.k2-fsa:sherpa-onnx`) |
| **Engine Version** | `1.10.30` |
| **Model Name** | Mana Persian Piper |
| **Model Files** | `fa_IR-mana-medium.onnx` (~125 MB) & `fa_IR-mana-medium.onnx.json` (~12 KB) |
| **Derived Tokens File** | `tokens.txt` (automatically extracted on-the-fly from `phoneme_id_map`) |
| **Model Source** | [HuggingFace: MahtaFetrat/Mana-Persian-Piper](https://huggingface.co/MahtaFetrat/Mana-Persian-Piper) |
| **Acoustic Architecture**| VITS (Variational Inference for Text-to-Speech) / Piper ONNX |
| **Language** | Persian (Farsi - `fa-IR`) |
| **Sample Rate** | 22,050 Hz (22.05 kHz) |
| **Audio Channels** | 1 (Mono) |
| **Bit Depth** | 16-bit Signed Little-Endian PCM |
| **Speakers Count** | 1 (Single speaker: female Persian voice). Both `HOST_A` and `HOST_B` use distinct acoustic speed/tone parameters until multi-speaker models are introduced. |
| **Target Hardware** | Daria Band (MediaTek Dimensity 7050, 8GB RAM, Android 15) |
| **Local Storage Path** | `/data/user/0/com.parspodcast.studio/files/models/tts/mana/` |

---

## 3. Dependency Configuration

### Version Catalog (`android/gradle/libs.versions.toml`)
```toml
[versions]
sherpaOnnx = "1.10.30"

[libraries]
sherpa-onnx = { group = "com.github.k2-fsa", name = "sherpa-onnx", version.ref = "sherpaOnnx" }
```

### Module Gradle (`android/app/build.gradle.kts`)
```kotlin
dependencies {
    // Phase 6: Sherpa-ONNX Offline Persian TTS
    implementation(libs.sherpa.onnx)
}
```

---

## 4. Architectural Components

### 4.1 Storage Access Framework (SAF) Model Import (`TTSModelFileManager.kt`)
- Allows users to select `fa_IR-mana-medium.onnx` and `fa_IR-mana-medium.onnx.json` from Android system file pickers without hardcoding paths.
- Copies the multi-megabyte binary through a **1MB chunked streaming buffer (`ByteArray(1024 * 1024)`)**, avoiding native heap crashes.
- Automatically inspects the imported JSON configuration to extract `phoneme_id_map` and format it into space-delimited `tokens.txt` as required by Sherpa-ONNX.

### 4.2 Persian TTS Preprocessing (`PersianTTSPreprocessor.kt`)
- Normalizes Arabic Yeh (`ي`) and Kaf (`ك`) to Persian (`ی`, `ک`).
- Expands Persian and English numerals into fully pronounced Persian words (e.g., `۱۴۰۳` ➔ `یک هزار و چهارصد و سه`).
- Expands Iranian broadcasting abbreviations (e.g., `ص.` ➔ `صفحه`, `م.` ➔ `میلادی`, `هـ.ش.` ➔ `هجری شمسی`, `٪` ➔ `درصد`, `km` ➔ `کیلومتر`).
- Adjusts pronunciations for common loanwords and broadcasting terms (`پادکَست`).
- Normalizes pause punctuation (`،`, `؛`, `.`, `؟`, `!`) and strips emojis, URLs, and code blocks that would disrupt acoustic synthesis.
- Preserves the raw text in the Room database while sending the preprocessed stream strictly to the acoustic engine.

### 4.3 Persian Sentence Splitter (`PersianSentenceSplitter.kt`)
- Splits lengthy podcast scripts into acoustic-friendly chunks (default: 180 characters) at grammatical sentence boundaries (`.`, `!`, `؟`, `؛`, `\n`).
- If a sentence exceeds the threshold, it breaks cleanly at comma clauses or word boundaries, preventing word truncations.
- Merges short tail fragments (<15 characters) into preceding sentences.

### 4.4 WAV Audio Writer (`WavAudioWriter.kt`)
- Converts float PCM samples (`-1.0f` to `1.0f`) to 16-bit signed PCM byte arrays with clamping.
- Encodes standard 44-byte RIFF/WAVE headers (RIFF chunk, `fmt ` subchunk, PCM audio format 1, 22050 Hz, 1 channel, 16 bits).
- Appends silence intervals (100ms prefix, 150ms inter-sentence pause, 400ms suffix) to deliver broadcast-quality radio cadences.

### 4.5 Sherpa-ONNX Engine Implementation (`SherpaOnnxTtsEngine.kt`)
- Implements the domain contract `TTSProvider`.
- Manages state machine: `Uninitialized` ➔ `Loading` ➔ `Ready (22050 Hz, 1 speaker)` ➔ `Synthesizing (segment X of Y)` ➔ `Error`.
- Executes inference on background coroutines (`Dispatchers.Default` / `Dispatchers.IO`), keeping the Compose UI responsive at 120 FPS.
- Supports real cancellation via Coroutine job cancellation.

### 4.6 Audio Playback & Room Persistence (`AudioPlaybackManager.kt`)
- Integrates AndroidX Media3 ExoPlayer for seamless, gapless WAV playback.
- Exposes playback progress, total duration, pause, play, seek, and stop controls.
- Updates Room `script_segments` with `generatedAudioPath` and calculated duration upon synthesis completion.

---

## 5. Storage Directory Structure
Generated audio files are organized in application-private storage:
```text
/data/user/0/com.parspodcast.studio/files/
  ├── models/
  │   └── tts/
  │       └── mana/
  │           ├── fa_IR-mana-medium.onnx
  │           ├── fa_IR-mana-medium.onnx.json
  │           └── tokens.txt
  └── audio/
      ├── test/
      │   └── test_synthesis_1700000000000.wav
      └── segments/
          └── <script_id>/
              ├── PRESENTER_A_seg_1.wav
              ├── PRESENTER_A_seg_2.wav
              └── PRESENTER_B_seg_3.wav
```

---

## 6. Offline Verification
- **Zero Cloud Leakage**: No calls to Google Cloud TTS, Azure Speech, ElevenLabs, or Android system SpeechSynthesis.
- **Airplane Mode Ready**: Text preprocessing, sentence splitting, Sherpa-ONNX acoustic synthesis, WAV file writing, and Media3 playback execute entirely offline.

---

## 7. Phase 8 (Audio Mixing) Readiness
All generated audio files expose:
- Canonical file path (`generatedAudioPath`).
- Standard sample rate: 22,050 Hz.
- Monophonic 16-bit PCM WAV container.
- Segment ID and Speaker Role (`PRESENTER_A` / `PRESENTER_B`).
- Accurate duration in seconds.
These clips map directly into Phase 8 multitrack timeline tracks (`AudioTrackEntity` and `AudioClipEntity`).
