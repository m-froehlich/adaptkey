# Third-party licenses

AdaptKey is licensed under **GPL-3.0-or-later** (see [`LICENSE`](LICENSE)). It uses the third-party
**software libraries** listed below. Bundled third-party **data** (dictionaries, language profiles,
emoji grouping, the mini-LLM tokenizer) is documented separately in [`CREDITS.md`](CREDITS.md).

All of these libraries are license-compatible with distributing AdaptKey under the GPLv3.

## Runtime dependencies (shipped in the app)

| Library | Version | License |
|---|---|---|
| androidx.core:core-ktx | 1.15.0 | Apache License 2.0 |
| androidx.appcompat:appcompat | 1.7.0 | Apache License 2.0 |
| com.google.android.material:material | 1.12.0 | Apache License 2.0 |
| androidx.preference:preference-ktx | 1.2.1 | Apache License 2.0 |

The Kotlin standard library (JetBrains) is Apache License 2.0.

## Planned optional runtime dependency (tier-3 mini-LLM)

| Library | License |
|---|---|
| com.microsoft.onnxruntime:onnxruntime-android | MIT License |

This is only pulled in for the optional on-device mini-LLM (§9 / C-06). The model **weights** are not
shipped with the app; they are provided by the user (the app has no internet permission).

## Test-only dependencies (not shipped)

| Library | Version | License |
|---|---|---|
| org.junit.jupiter:junit-jupiter | 5.11.3 | Eclipse Public License 2.0 |
| org.junit.platform:junit-platform-launcher | 1.11.3 | Eclipse Public License 2.0 |

## License texts

- Apache License 2.0 — https://www.apache.org/licenses/LICENSE-2.0
- MIT License — https://opensource.org/license/mit
- Eclipse Public License 2.0 — https://www.eclipse.org/legal/epl-2.0/
- GNU GPL v3 — see [`LICENSE`](LICENSE)
