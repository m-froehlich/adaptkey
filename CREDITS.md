# Third-party data credits

AdaptKey bundles data derived from third-party sources. The application source code and these derived
data files are distinct works under distinct licences.

## Language dictionaries (`app/src/main/assets/dict_*.tsv`, `bigram_*.tsv`)

The per-language dictionaries (German `de`, English `en`, Greek `el`) — word lists, frequencies,
part-of-speech (noun) annotations and bigrams — were derived by AdaptKey from the text of **Wikipedia**
(the German, English and Greek Wikipedias, `pages-articles` dumps).

- Source: Wikipedia — https://www.wikipedia.org/ (dumps: https://dumps.wikimedia.org/)
- Licence of the source text and of these derived data files:
  **Creative Commons Attribution-ShareAlike 4.0 (CC BY-SA 4.0)** —
  https://creativecommons.org/licenses/by-sa/4.0/
- These derived `dict_*.tsv` / `bigram_*.tsv` files are therefore licensed under **CC BY-SA 4.0**.
  If you redistribute them (they ship inside the app and in this repository), keep this attribution
  and license them under CC BY-SA 4.0 (or a compatible licence). This ShareAlike obligation applies to
  the data files only; it does not affect the licence of the AdaptKey source code.

The extraction was performed by the throwaway builder script kept with the project notes
(`scratchpad/build_dict.py`); casing statistics from running text were used to derive the noun /
proper-noun tags used by the capitalisation rules (§6).

## Language-detection profiles (`app/src/main/assets/language_profiles.tsv`)

The character-n-gram language-identification profiles (A-03) were derived from the
**Universal Declaration of Human Rights (UDHR)**, which is in the public domain.

- Source: UDHR — https://www.un.org/en/about-us/universal-declaration-of-human-rights
  (via the machine-readable collection at https://github.com/eric-muller/udhr)
- These derived profiles are distributed as part of AdaptKey.

## Emoji dataset (`app/src/main/assets/emoji_dataset.tsv`)

A curated list of Unicode emoji code points grouped by category. Emoji are Unicode characters; the
curated grouping is part of AdaptKey.

## Tier-3 mini-LLM tokenizer (`app/src/main/assets/tier3/vocab.json`, `merges.txt`)

The byte-level BPE tokenizer vocabulary and merges bundled for the optional tier-3 mini-LLM (§9 / C-06)
are the tokenizer files of **SmolLM2-360M-Instruct** by Hugging Face.

- Source: https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct
- Licence: **Apache License 2.0** — https://www.apache.org/licenses/LICENSE-2.0
- Only the tokenizer files are bundled. The model weights (`model.onnx`) are **not** shipped with the
  app; they are provided by the user into the app's private storage (the app has no internet
  permission). The same model/tokenizer are also under Apache-2.0.
