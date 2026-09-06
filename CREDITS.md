# Third-party data credits

AdaptKey bundles data derived from third-party sources. The application source code and these derived
data files are distinct works under distinct licences.

## Language dictionaries (`app/src/main/assets/en/dict.tsv`, `bigram.tsv`; `dictionaries/<code>/`, `language-packs/adaptkey-lang-<code>.zip`)

**Only English (`en`) ships bundled inside the APK** (`app/src/main/assets/en/`) - the one language
`DictionaryLoader.BUNDLED_LANGUAGES` always guarantees is present, so the app is typable the moment it is
installed with no download. Every other language listed below - German and Greek included, both bundled
in earlier releases before the D-280 language-pack system existed - is a separate, optional download
(`language-packs/adaptkey-lang-<code>.zip`, installed via Settings → Languages) built from working files
kept in this repository under `dictionaries/<code>/`. As of this writing, **30 downloadable languages**
are available, alongside the bundled English:

Azerbaijani (`az`), Bosnian (`bs`), Croatian (`hr`), Czech (`cs`), Danish (`da`), Dutch (`nl`),
Estonian (`et`), Finnish (`fi`), French (`fr`), German (`de`), Greek (`el`), Hungarian (`hu`),
Indonesian (`id`), Italian (`it`), Latvian (`lv`), Lithuanian (`lt`), Malay (`ms`),
Norwegian Bokmål (`nb`), Polish (`pl`), Portuguese (`pt`), Romanian (`ro`), Russian (`ru`),
Serbian (`sr`), Slovak (`sk`), Spanish (`es`), Swahili (`sw`), Swedish (`sv`), Tagalog (`tl`),
Turkish (`tr`), Ukrainian (`uk`), Uzbek (`uz`).

Every one of these dictionaries - word lists, frequencies, part-of-speech annotations, bigrams, and
(where present) `lemma`/word-family links - was derived by AdaptKey from the text of **Wikipedia**, each
language's own `pages-articles` dump, and for most of them additionally from that language's own
**Wiktionary** coverage (via the [kaikki.org](https://kaikki.org/) `wiktextract` project - MIT-licensed
tool, CC BY-SA/GFDL-licensed underlying Wiktionary content) for real part-of-speech tags and inflected
word-family forms. See each language's own entry in `app/src/main/kotlin/de/froehlichmedia/adaptkey/
dictionary/LanguagePackCatalog.kt` and `AdaptKey-Progress.md`'s own D-4xx/D-450-followup entries for the
exact source (native vs. English-Wiktionary-coverage fallback), corpus size, and build method used for
that specific language - they were not all built the same way or to the same depth of native-speaker
review, and those per-language entries say so honestly rather than implying uniform quality.

- Source: Wikipedia — https://www.wikipedia.org/ (dumps: https://dumps.wikimedia.org/) and, for most
  languages, Wiktionary via kaikki.org — https://kaikki.org/dictionary/rawdata.html
- Licence of the source text and of these derived data files:
  **Creative Commons Attribution-ShareAlike 4.0 (CC BY-SA 4.0)** —
  https://creativecommons.org/licenses/by-sa/4.0/ (Wiktionary content is dual CC BY-SA/GFDL; the
  `wiktextract` extraction tool itself is MIT-licensed)
- These derived `dict.tsv` / `bigram.tsv` (and, where present, `hints.tsv` / `diacritics.tsv` /
  `abbreviations.tsv`) files are therefore licensed under **CC BY-SA 4.0**. If you redistribute them
  (the bundled English copy ships inside the APK; every language's own working files and built archive
  ship in this repository), keep this attribution and license them under CC BY-SA 4.0 (or a compatible
  licence). This ShareAlike obligation applies to the data files only; it does not affect the licence of
  the AdaptKey source code.

The extraction scripts (`dictionaries/<code>/extract_wiki_dump.py`, `extract_wiktionary.py`,
`merge_dict.py`, `merge_wiktionary.py`) are kept in this repository, one set per language, rather than as
a single throwaway builder - each language's own real orthography (stress marks, casing rules, apostrophe
conventions, and more) needed its own real, documented handling, not a one-size-fits-all script. Older
languages' own dictionaries (German, English, Greek) were originally built by an earlier, since-replaced
throwaway builder; casing statistics from running text were used to derive the noun/proper-noun tags used
by the capitalisation rules (§6) before the current per-language pipeline existed.

## Language-detection profiles (`app/src/main/assets/language_profiles.tsv`)

The character-n-gram language-identification profiles (A-03) come from two different sources depending
on when each language was added:

- The original eight languages (German, English, Greek, French, Spanish, Italian, Dutch, Portuguese)
  were derived from the **Universal Declaration of Human Rights (UDHR)**, which is in the public domain.
  - Source: UDHR — https://www.un.org/en/about-us/universal-declaration-of-human-rights
    (via the machine-readable collection at https://github.com/eric-muller/udhr)
- Every later addition's profile (`dictionaries/build_language_profiles.py`) is instead built directly
  from that language's own real Wikipedia-derived `dict.tsv` word-frequency table (see the language
  dictionaries section above) - the same CC BY-SA 4.0 Wikipedia source, not UDHR text. This covers every
  language added from the D-450 round onward, including Russian, Ukrainian, Serbian, and Azerbaijani.
- These derived profiles are distributed as part of AdaptKey, either way.

## Emoji dataset (`app/src/main/assets/emoji_dataset.tsv`)

A curated list of Unicode emoji code points grouped by category. Emoji are Unicode characters; the
curated grouping is part of AdaptKey.

## Emoji search keywords (`app/src/main/assets/emoji_keywords.tsv`)

The per-emoji search terms used by the emoji-search feature (L-03, D-317) - both the German and English
short names and full keyword-synonym lists - were extracted from the **Unicode CLDR** annotation data
(`common/annotations/de.xml`, `common/annotations/en.xml`).

- Source: Unicode CLDR — https://github.com/unicode-org/cldr
- Licence: **Unicode License v3** — https://www.unicode.org/license.txt
- The extraction was performed by the throwaway builder script kept with the project notes
  (`scratchpad/build_emoji_keywords.py`); only the emoji already bundled in `emoji_dataset.tsv` are
  covered.

## Tier-3 mini-LLM tokenizer (`app/src/main/assets/tier3/vocab.json`, `merges.txt`)

The byte-level BPE tokenizer vocabulary and merges bundled for the optional tier-3 mini-LLM (§9 / C-06)
are the tokenizer files of **SmolLM2-360M-Instruct** by Hugging Face.

- Source: https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct
- Licence: **Apache License 2.0** — https://www.apache.org/licenses/LICENSE-2.0
- Only the tokenizer files are bundled. The model weights (`model.onnx`) are **not** shipped with the
  app; they are provided by the user into the app's private storage (the app has no internet
  permission). The same model/tokenizer are also under Apache-2.0.
