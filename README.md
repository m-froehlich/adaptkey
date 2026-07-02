# AdaptKey

An adaptive Android keyboard (IME) that learns how *you* type — written in Kotlin, built for privacy
first.

## 🔒 Provably offline. No phoning home.

AdaptKey **has no internet permission at all.** This is not a promise you have to take on faith — it is
enforced by Android and **verifiable by anyone**: there is no `android.permission.INTERNET` in the
[app manifest](app/src/main/AndroidManifest.xml), so the operating system makes it *impossible* for the
app to open a network connection. No telemetry, no analytics, no "anonymous usage statistics", no ads,
no cloud sync — the app **cannot** send your keystrokes anywhere, because it cannot reach the network.

- **No internet permission** — kernel-enforced, not a checkbox you trust.
- **No storage permission** — the optional model is imported through the system file picker, which
  grants access to just that one file.
- **Everything runs on-device.** Dictionaries, autocorrect, language detection and the optional mini-LLM
  all execute locally.
- **Free software.** Licensed under **GPL-3.0-or-later** — audit every line yourself.

Even the optional on-device mini-LLM (see below) keeps this promise: the app never downloads it. You
provide the model file once via your browser + the system file picker, and the keyboard keeps working
entirely offline afterwards.

## Features

- **Gboard-style QWERTZ layout** with freely configurable key proportions (space bar / comma / full
  stop / backspace), a persistent number row, and per-key long-press secondary symbols.
- **Learns your touch pattern.** A per-key personal offset model (2-D Gaussian) adapts hit detection to
  how you actually tap; an optional calibration/onboarding step seeds it.
- **Smart suggestions & autocorrect** from a personal SQLite n-gram dictionary, with a stabilised,
  scrollable suggestion bar, a "keep as typed" escape hatch, and one-tap post-commit undo.
- **Real multilingual dictionaries** for **German, English and Greek**, derived from Wikipedia, with
  on-device language detection that switches the whole lexicon per language.
- **Greek input** with its own layout and tonos (accent) long-press.
- **German capitalisation done right** (§6): sentence starts, nouns vs. proper nouns, hyphen segments,
  the e-mail-salutation comma rule, abbreviations, and a configurable shift-grace window.
- **Retroactive fixes**: missed-space split and spurious-space merge, driven by touch ambiguity.
- **Gestures**: swipe-to-delete-word, swipe-to-dismiss, drag-a-suggestion-to-trash to blacklist,
  word-end shift for casing/camelCase.
- **Emoji panel** with recents, and a two-page symbol/number layer.
- **Optional on-device mini-LLM (tier 3, opt-in, work in progress):** when the n-gram is uncertain, a
  small local model (SmolLM2-360M via ONNX Runtime) can improve predictions. It is never bundled or
  downloaded by the app; you import it yourself, and it stays fully offline.

## Building

Requires Android Studio with an Android SDK (compileSdk 35, minSdk 26). Kotlin, AGP 8.7.3.

```
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The pure logic (touch model, suggestion/autocorrect/capitalisation policy, language detection,
tokenizer, …) is covered by JVM unit tests; the thin Android layers (views, service, storage) are left
to instrumented tests.

## License

AdaptKey is licensed under the **GNU General Public License, version 3 or later** — see
[`LICENSE`](LICENSE). Each source file carries an [SPDX](https://spdx.dev/) header
(`SPDX-License-Identifier: GPL-3.0-or-later`).

Copyright (C) 2026 Froehlich Media.

## Third-party software & data

- Software libraries and their licenses: [`THIRD-PARTY-LICENSES.md`](THIRD-PARTY-LICENSES.md).
- Bundled data (dictionaries, language profiles, emoji, mini-LLM tokenizer) and its attribution:
  [`CREDITS.md`](CREDITS.md).
