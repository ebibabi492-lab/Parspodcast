# ParsPodcast — Phase 7 Documentation: Hybrid News Ingestion System

## 1. Executive Summary
Phase 7 delivers a comprehensive, production-oriented hybrid news ingestion system for **ParsPodcast (پارس‌پادکست)**. The application now supports:
1. **Online Ingestion:** RSS 2.0 and Atom feeds fetched securely via OkHttp with configurable timeouts, XML External Entity (XXE) attack prevention, HTML cleaning, and background periodic synchronization via Android WorkManager.
2. **Offline Ingestion:** 100% offline text file import (`.txt` and `.md`) using Android Storage Access Framework (SAF) with UTF-8 / UTF-8 BOM support, robust Persian delimiter parsing, deduplication analysis against the local Room database, and an interactive preview and editing workflow before persistence.
3. **Seamless Studio Integration:** All ingested news records (both online and offline) populate the unified Room database (`news_articles`), can be filtered, marked as favorites, and selected directly for AI script generation via **Gemma 4 E4B** (Phase 5) and speech synthesis via **Sherpa-ONNX Mana Piper** (Phase 6).

---

## 2. Ingestion Architecture

```
                  ┌──────────────────────────────────────────────┐
                  │             ParsPodcast News Center          │
                  └──────────────────────┬───────────────────────┘
                                         │
                 ┌───────────────────────┴───────────────────────┐
                 ▼                                               ▼
    [1. Online Ingestion Provider]                [2. Offline Ingestion Provider]
    ├── HTTP/HTTPS Feeds (RSS 2.0 / Atom)         ├── SAF File Picker (ACTION_OPEN_DOCUMENT)
    ├── OkHttp Client (15s timeout, User-Agent)   ├── UTF-8 & UTF-8 BOM Handling
    ├── RssAtomParser (Secure XML + XXE Shield)   ├── OfflineTextNewsParser (.txt / .md)
    ├── HTML Entity & Tag Stripping (Jsoup)       ├── Persian Structured Format & Delimiters
    └── WorkManager (NewsSyncWorker)              └── Room Deduplication Checking (SHA-256)
                 │                                               │
                 └───────────────────────┬───────────────────────┘
                                         ▼
                 ┌───────────────────────────────────────────────┐
                 │          Unified NewsNormalizer               │
                 │  - Arabic -> Persian Char Normalization       │
                 │  - Digit Unification (Persian / ASCII)        │
                 │  - Content Hashing (SHA-256)                  │
                 │  - Title Similarity (Jaccard)                 │
                 └───────────────────────┬───────────────────────┘
                                         ▼
                 ┌───────────────────────────────────────────────┐
                 │       Interactive Import Preview Modal        │
                 │  - Edit title, source, category, content      │
                 │  - Duplicate warnings (non-destructive)       │
                 │  - Multi-selection check / Batch save         │
                 └───────────────────────┬───────────────────────┘
                                         ▼
                 ┌───────────────────────────────────────────────┐
                 │              Room Database                    │
                 │  - news_sources, news_articles, news_events   │
                 └───────────────────────┬───────────────────────┘
                                         ▼
                 ┌───────────────────────────────────────────────┐
                 │    Gemma 4 E4B Script Studio (Phase 5)        │
                 │  - Single / Dual-Host Prompt Generation       │
                 └───────────────────────────────────────────────┘
```

---

## 3. Supported Offline Text Formats

### 3.1 Recommended Persian Structured Format
```text
عنوان: عنوان خبر اول
منبع: خبرگزاری ایرنا
تاریخ: ۱۴۰۵/۰۸/۲۲
دسته‌بندی: فناوری

متن:
متن کامل خبر اول در این قسمت قرار می‌گیرد.
می‌تواند دارای چندین پاراگراف باشد.
---
عنوان: خبر دوم
منبع: دیجیاتو
دسته‌بندی: اقتصادی

متن:
مشروح خبر دوم...
```

### 3.2 Supported Delimiters & Variations
- **Delimiters:** `---`, `===`, `***`, `___` or sequential `عنوان:` tags.
- **Tag Aliases:**
  - Title: `عنوان:`, `تیتر:`, `Title:`, `# ` (Markdown header 1)
  - Source: `منبع:`, `خبرگزاری:`, `Source:`
  - Date: `تاریخ:`, `زمان:`, `Date:`
  - Category: `دسته‌بندی:`, `دسته:`, `موضوع:`, `Category:`
  - Content: `متن:`, `شرح:`, `مشروح:`, `Body:`, `Content:`
- **Fallback Format:** Free-form paragraphs separated by delimiters are automatically parsed with the first sentence attributed as the title, defaulting to "فایل محلی" as the source and "عمومی" as the category.

---

## 4. Normalization and Deduplication Logic
- **Persian Text Unification:**
  - Arabic Yeh (`ي`) ➔ Persian Yeh (`ی`)
  - Arabic Kaf (`ك`) ➔ Persian Kaf (`ک`)
  - Removal of kashida (`ـ`) and harakat (diacritics).
- **Content Hash (`contentHash`):**
  - SHA-256 hash calculated over normalized title and the first 500 characters of normalized body.
  - Stored in Room column `deduplication_hash`.
- **Duplicate Detection:**
  1. Exact duplicate hash check against Room database.
  2. Exact title and source match.
  3. Non-destructive handling: duplicate items are flagged with amber warning banners in the preview dialog and unchecked by default, allowing the user to inspect, edit, or override as desired.

---

## 5. Security & XML Protection (XXE)
In compliance with enterprise security requirements:
- Storage access uses Android Storage Access Framework (`ActivityResultContracts.OpenDocument`), eliminating the need for `MANAGE_EXTERNAL_STORAGE`.
- XML Pull Parser explicitly enables `XMLConstants.FEATURE_SECURE_PROCESSING`, disables `disallow-doctype-decl`, and disables external general/parameter entities to eliminate XML External Entity (XXE) injection vulnerabilities.
- HTML content is sanitized via `Jsoup.clean(html, Safelist.none())`.

---

## 6. Offline-First Verification
| Operation | Network Required? | Local / Airplane Mode Compatible? |
| :--- | :---: | :---: |
| Import `.txt` / `.md` via SAF | No | **Yes (100% Offline)** |
| Deduplication Analysis | No | **Yes (Local Room SQLite)** |
| Preview & Edit Imported News | No | **Yes (Local Compose UI)** |
| Batch Save to Database | No | **Yes (Local Room SQLite)** |
| Filter, Search, Favorite | No | **Yes (Local Room SQLite)** |
| Select for Gemma 4 Script Generation | No | **Yes (Local LiteRT-LM)** |
| Ingest Online RSS 2.0 / Atom | Yes | Network only when syncing |

---

## 7. Next Steps for Phase 8
Phase 8 will introduce the **Multitrack Audio Mixing Engine**:
- Taking the generated speech files (`generatedAudioPath` from Phase 6) and assembling multitrack timelines (Presenter A track, Presenter B track, background music with audio ducking, intro/outro jingles).
