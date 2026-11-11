# ParsPodcast — Phase 5 Documentation: Real On-Device Gemma 4 E4B Integration via LiteRT-LM

## 1. Executive Summary
Phase 5 integrates a real, 100% offline, on-device Large Language Model (LLM) into **ParsPodcast (پارس‌پادکست)** using **Google AI Edge LiteRT-LM** and the instruction-tuned **Gemma 4 E4B** model (`gemma-4-E4B-it.litertlm`).

All script generation, segment rewriting, summarization, conversational transitions, and headline generation occur entirely on the physical device without requiring internet access, cloud AI tokens, or proxy servers.

---

## 2. Model & Runtime Specifications

| Attribute | Specification |
| :--- | :--- |
| **Model Name** | Gemma 4 E4B Instruction Tuned |
| **Model Filename** | `gemma-4-E4B-it.litertlm` |
| **File Size** | ~4.6 GB |
| **Quantization** | Int4 / Block-quantized optimized for Mobile Edge |
| **Runtime Engine** | Google AI Edge LiteRT-LM (Android Kotlin API) |
| **Artifact Dependency** | `com.google.ai.edge.litertlm:litertlm-android:0.19.0` |
| **Execution Backends** | GPU (`Backend.GPU()`) with automatic fallback to CPU (`Backend.CPU()`) |
| **Target OS** | Android 15 (API 35), minimum Android 14 (API 34) |
| **Target Reference Device**| Daria Band (MediaTek Dimensity / Mali GPU, 8GB RAM) |
| **Storage Destination** | `context.filesDir/models/gemma-4-E4B-it.litertlm` |

---

## 3. Dependency Configuration

### Version Catalog (`android/gradle/libs.versions.toml`)
```toml
[versions]
litertlm = "0.19.0"

[libraries]
litertlm-android = { group = "com.google.ai.edge.litertlm", name = "litertlm-android", version.ref = "litertlm" }
```

### Module Gradle (`android/app/build.gradle.kts`)
```kotlin
dependencies {
    // Phase 5: Google AI Edge LiteRT-LM for On-Device LLM Inference
    implementation(libs.litertlm.android)
}
```

---

## 4. Architectural Components

### 4.1 Domain Contracts (`domain/ai/LocalLLMProvider.kt`)
- `EngineLoadingState`: Explicit state machine (`UNLOADED`, `LOADING(progress, message)`, `READY(backend, modelPath, ramMb)`, `GENERATING`, `ERROR(message, recoverable)`).
- `ModelExecutionBackend`: `GPU`, `CPU_XNNPACK`, `AUTO`.
- `GenerationSettings`: Temperature, Top-K, Max output tokens, system prompt overrides.
- `GenerationRequest`: Task type (`FULL_SCRIPT`, `REWRITE_SEGMENT`, `SUMMARIZE`, `EXPAND`, `TRANSITION`, `SUGGEST_TITLES`), selected news items, tone, format, custom instructions.
- `DeviceCapabilityReport`: Available RAM, total RAM, low RAM status, free disk space, GPU acceleration support status, and recommendation readiness for Gemma 4 E4B.
- `LocalLLMProvider`: Coroutine-based contract with `loadModel()`, `unloadModel()`, `generateStreaming()`, `cancelGeneration()`, and `getDeviceCapabilities()`.

### 4.2 Storage Access Framework & Model Management (`data/manager/ModelFileManager.kt`)
- Safe chunked streaming: multi-gigabyte models are copied from SAF content URIs in 1MB buffer chunks. This prevents `OutOfMemoryError` on Android heap boundaries.
- File integrity checking: verifies non-zero byte size, minimum size (~500MB sanity threshold), readability, and reports accurate disk footprint.
- Device memory analysis: reads `/proc/meminfo` and `ActivityManager.MemoryInfo` to calculate whether the device can safely load Gemma 4 into RAM (recommended >= 4.5GB available memory).
- Clean model deletion: frees application storage upon user confirmation.

### 4.3 LiteRT-LM Integration Engine (`data/ai/LiteRTLMEngine.kt`)
- Initializes `com.google.ai.edge.litertlm.EngineConfig` with chosen backend.
- Wraps inference in a dedicated `CoroutineDispatcher` (`Dispatchers.Default`).
- Creates isolated sessions using `ConversationConfig` and streaming output via `sendMessageAsync()`.
- Implements graceful cancellation via coroutine cancellation and atomic engine lifecycle management.
- Handles OpenCL/Vulkan GPU initialization errors by falling back to `Backend.CPU()` using XNNPACK SIMD acceleration without terminating the application process.

### 4.4 Persian Prompt Architecture (`domain/prompt/PersianPromptBuilder.kt`)
- Crafted specifically for Persian grammar, editorial conventions, and Iranian podcast presentation styles.
- Supports two distinct formats:
  - **Single Presenter (تک‌گوینده)**: authoritative, journalistic, structured flow (`HOST_A`).
  - **Two-Presenter Conversational (دو نفره گفتگومحور)**: dynamic banter, authentic Persian turns-of-phrase, natural interruptions, and complementary perspectives (`HOST_A`, `HOST_B`).
- Supports editorial actions: tone shifts (formal, storytelling, analytical, conversational), text expansion, summarization, and headline synthesis.
- Instructs the model to output strict JSON schemas while adhering to Iranian news broadcasting standards.

### 4.5 Resilient Output Parsing (`domain/parser/ScriptJsonParser.kt`)
- Parses structured JSON response into `ScriptSegment` objects with assigned speakers, order indices, and editorial director notes.
- Extracts JSON blocks wrapped in markdown code fences (` ```json `).
- Features `repairMalformedScript()`: if the LLM produces truncated or conversational raw text (e.g., `گوینده ۱: ...`, `گوینده ۲: ...`), the parser reconstructs valid script segments with accurate speaker attribution, ensuring zero data loss for the user.

### 4.6 UI & Jetpack Compose Integration
- **Model Manager Screen (`ModelManagerScreen.kt`)**:
  - Live hardware capability inspection card (RAM, disk space, GPU capability, Daria Band readiness).
  - One-tap SAF file picker (`ActivityResultContracts.OpenDocument`) for `.litertlm` model import.
  - Streaming copy progress indicator with transfer speed and byte tally.
  - Explicit RAM load/unload controls allowing users to reclaim memory when not generating scripts.
  - Confirmation dialog for model deletion.
- **Script Studio Screen (`ScriptStudioScreen.kt`)**:
  - Engine status indicator in top bar (shows model loaded status, execution backend, and memory footprint).
  - Quick action toolbar: Load Model, Unload, Generate Script, Cancel.
  - Real-time streaming token display with live animated typing cursor.
  - Interactive segment actions: AI Rewrite, Make Storytelling, Expand, Tone adjustment.
  - Direct persistence into local Room database (`podcast_scripts` and `script_segments`).

---

## 5. Offline & Self-Sufficiency Verification
- **Zero Cloud Leakage**: No calls to `https://generativelanguage.googleapis.com`, OpenAI, or remote inference APIs.
- **Network Independence**: The entire inference pipeline functions when airplane mode is enabled.
- **Local Artifact Placement**: Model file is placed in private sandboxed internal storage: `/data/user/0/com.parspodcast.studio/files/models/gemma-4-E4B-it.litertlm`.

---

## 6. Next Steps for Phase 6
Phase 6 will build upon the structured `ScriptSegment` outputs of Phase 5 to deliver:
- Offline Persian TTS synthesis using **Sherpa-ONNX** and the **Mana Persian Piper** voice model.
- Speaker voice assignment (`HOST_A` and `HOST_B` distinct vocal models).
- Multitrack WAV audio rendering and mixing.
