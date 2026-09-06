# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's postprocessed JSONL data
# for the SHARED "Serbo-Croatian" Wiktionary edition (`kaikki.org/dictionary/Serbo-Croatian/kaikki.org-
# dictionary-SerboCroatian.jsonl`, 276.5MB uncompressed - note this specific per-language postprocessed file
# is marked DEPRECATED by kaikki.org itself and may be removed in the future; downloaded and archived before
# that happens). Confirmed directly: every entry uses `lang_code == "sh"` - kaikki.org treats Croatian,
# Bosnian, and Serbian as one single Wiktionary language edition, not three separate ones. Per the user's own
# explicit decision (the pre-round `AskUserQuestion`), this ONE shared Wiktionary source feeds all three of
# this project's own Croatian/Bosnian/Serbian language packs, while each still gets its own separate
# Wikipedia-frequency corpus (hrwiki/bswiki/srwiki) - this script's own output (`wiktionary_*.tsv`) is
# generated ONCE here and then copied identically into each of `dictionaries/hr/`, `dictionaries/bs/`,
# `dictionaries/sr/`.
#
# D-450 REAL FINDING 1, found by direct inspection before trusting any output: this source's own inflection-
# table forms are written in traditional PITCH-ACCENT dictionary notation (e.g. "kȕća" for "kuća"/"house",
# using precomposed Unicode pitch-accent letters like U+0215 LATIN SMALL LETTER U WITH DOUBLE GRAVE) - NOT
# the plain orthography anyone actually types. These accent marks fall outside the ordinary hr/bs/sr Latin
# alphabet, so `VALID_FORM_RE` naturally (and safely) rejects them outright with no special-casing - but that
# would silently lose every single inflected form this source documents. Confirmed directly that each such
# form instead carries a `links` field whose SECOND element is the real, plain-orthography spelling (e.g.
# `[["kȕća", "kuća#Serbo-Croatian"]]` - the accent-free target, with a `#Serbo-Croatian` page-anchor suffix
# to strip). `usable_forms()` below therefore prefers `links[0][1]` (with the anchor stripped) over the raw,
# pitch-accented `form` string whenever a `links` entry is present - recovering real coverage this source
# would otherwise silently lose, not merely working around the accent marks by discarding them.
#
# D-450 REAL FINDING 2: this same source explicitly tags a second, alternate-script spelling for many entries
# with a dedicated `["Cyrillic"]` tag (e.g. "кућа" for "kuća") - this project's own choice of which of
# Croatian/Bosnian/Serbian use which script is handled entirely by each language's own consuming script (see
# `dictionaries/hr/`, `dictionaries/bs/`, `dictionaries/sr/` for the per-language script decision), NOT here;
# this shared extractor keeps BOTH the Latin and Cyrillic spellings available (querying by SCRIPT variable
# below) so each downstream language can select the one it actually needs without re-parsing 276.5MB again.
#
# D-450 REAL FINDING 3, a genuinely different meaning for an already-known diagnostic tag: this source's own
# `"error-unrecognized-form"` tag (already excluded elsewhere in this project, e.g. Finnish's D-450 entry, as
# a genuine noise signal) turns up here on entries that ARE real, valid words (confirmed directly: "pisati"/
# "to write" has two real participle forms, "pišući"/"pisavši", both tagged `error-unrecognized-form` purely
# because wiktextract's own parser could not recognise this source's own conjugation-table template - the
# `links` field still resolves them correctly). `EXCLUDE_FORM_TAGS` for this shared source therefore
# deliberately OMITS `"error-unrecognized-form"`, unlike every other language this project has built - a
# genuine, source-specific exception, not an oversight.
#
# D-450 REAL FINDING 4 - a genuine source-richness limitation, honestly documented rather than hidden: this
# source's own verb conjugation tables turned out unusually SPARSE compared to every other language this
# project has built - confirmed directly against two common verbs ("raditi"/"to work", "pisati"/"to write"):
# only the infinitive, two participle forms, and one deverbative noun are documented per verb, with NO
# personal present/past-tense conjugation paradigm at all. This is a real property of this specific fallback
# source, not a parsing bug.
#
# D-450-followup REAL BUG, found while actually building Serbian (not caught during the original hr/bs round,
# since both use want_cyrillic=False and never touch this code path): usable_forms()'s original Cyrillic
# branch kept a form only when that specific form entry itself carried an explicit "Cyrillic" tag, mirroring
# the Latin branch's own "Cyrillic"-tag-excludes logic verbatim instead of its true mirror image. Directly
# inspected the raw source data for both directions before fixing (not guessed): a LATIN-headword entry
# ("žena") documents its paradigm in Latin by default, with a rare explicit `["Cyrillic"]`-tagged alternate
# (confirmed: 20 real forms, only 1 Cyrillic-tagged) - but the SEPARATE CYRILLIC-headword entry for the same
# lemma ("жена", which `main()`'s own word-pattern filter is what actually selects for want_cyrillic=True) is
# the exact mirror: its paradigm is documented in Cyrillic **by default**, with a rare explicit
# `["romanization"]`-tagged Latin alternate instead - there is no `"Cyrillic"` tag anywhere on this entry's own
# forms at all, so the original logic (requiring one) kept zero of them. The correct exclusion tag is therefore
# direction-dependent - `"Cyrillic"` marks the rare alternate inside a Latin-primary entry, `"romanization"`
# marks the rare alternate inside a Cyrillic-primary entry - not a single constant either way. No
# transliteration is needed: the source already provides genuine native-script forms for both entries (verified
# directly for "скот"/"cattle": its own `nominative/singular` form carries a `links` field resolving the
# pitch-accented "ско̏т" to the plain "скот" via the exact same Finding-1 mechanism already used for Latin).
# hr/bs are unaffected (their own want_cyrillic=False branch already used the correct "Cyrillic"-exclusion tag).
#
# Quelle: https://kaikki.org/dictionary/Serbo-Croatian/kaikki.org-dictionary-SerboCroatian.jsonl
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import json
import re
import sys

LATIN_LETTERS = set("abcčćdđefghijklmnoprsštuvzž")
VALID_LATIN_RE = re.compile(r"^[a-bčćdđefghijklmnoprsštuvzž]+(-[a-bčćdđefghijklmnoprsštuvzž]+)*$")
CYRILLIC_RE = re.compile(r"^[а-шђјљњћџ]+(-[а-шђјљњћџ]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
    "uncommon", "poetic", "ironic", "literary", "depreciative", "elevatedly",
}

# D-450 Finding 3: "error-unrecognized-form" deliberately OMITTED here - see module docstring.
EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical", "error-unknown-tag", "class"}
MULTIWORD_TAGS = {"multiword-construction"}
# D-450-followup: the alternate-script marker tag is direction-dependent - see the module's own KDoc above.
CYRILLIC_TAG = "Cyrillic"
ROMANIZATION_TAG = "romanization"


def resolve_form_string(f):
    """Prefers the real, plain-orthography spelling from `links` (D-450 Finding 1) over the raw, possibly
    pitch-accented `form` field.

    D-450-followup REAL BUG, found via the mandatory calibration-ratio sanity check (Serbian's own verb ratio
    came back an implausible outlier): a periphrastic/compound-tense raw form (e.g. "будем клечао", future-ii
    of "клечати"/"to kneel") carries ONE `links` PAIR PER WORD, not one for the whole phrase - naively reading
    only `links[0]` (as this function always did before) recovers just the AUXILIARY verb's own inflected form
    ("будем"), never the actual content verb ("клечао"), silently attaching an unrelated - and typically far
    more frequent, since "biti"/"to be" is one of the language's commonest verbs - word's frequency to this
    lemma's own Wortfamilien completion. The exact same symptom-class French's D-444-followup and Dutch's
    D-447 already document for this project (see the Guide's own step-4 calibration section), a different root
    cause each time. Fixed the same way Dutch's own fix chose: skip the whole form when its RAW string (before
    any `links` substitution) is multi-word - out of scope by construction, and nothing real is lost, since a
    periphrastic tense's own single-word building blocks (the plain "клечао" participle, "будем" as its own
    verb's form) are already documented as their own separate, correctly single-word forms elsewhere in the
    same source. Checked BEFORE any `links` lookup, so a genuine single-word pitch-accented form (Finding 1's
    own case, e.g. "ско̏т" -> "скот") is unaffected - its raw form has no whitespace at all.
    """
    raw = f.get("form", "")
    if any(ch.isspace() for ch in raw):
        return ""
    links = f.get("links")
    if links and len(links[0]) > 1 and links[0][1]:
        target = links[0][1].split("#")[0]
        if target:
            return target
    return raw


def usable_forms(entry, word, want_cyrillic):
    # D-450-followup: which tag marks "the other script's alternate, skip it" depends on which script this
    # entry is primarily written in - see the module's own KDoc for the real, directly-verified evidence.
    alt_script_tag = ROMANIZATION_TAG if want_cyrillic else CYRILLIC_TAG
    pattern = CYRILLIC_RE if want_cyrillic else VALID_LATIN_RE
    result = {}
    for f in entry.get("forms", []):
        tags = set(f.get("tags", []))
        if tags & EXCLUDE_FORM_TAGS:
            continue
        if tags & EXCLUDE_QUALIFIERS or tags & MULTIWORD_TAGS:
            continue
        if alt_script_tag in tags:
            continue
        raw_form = resolve_form_string(f)
        if not raw_form or raw_form in ("-", "—"):
            continue
        stripped = raw_form.strip().lower()
        if any(ch.isspace() for ch in stripped):
            continue
        if stripped == word:
            continue
        if not pattern.match(stripped):
            continue
        result[stripped] = True
    return list(result.keys())


def is_form_of_entry(entry):
    return any(s.get("form_of") for s in entry.get("senses", []))


def entry_has_modern_sense(entry):
    senses = entry.get("senses", [])
    if not senses:
        return True
    for sense in senses:
        if not (set(sense.get("tags", [])) & EXCLUDE_QUALIFIERS):
            return True
    return False


def main(in_path, out_dir, want_cyrillic):
    nouns = {}
    verbs = {}
    adjectives = {}
    prepositions = set()
    proper_nouns = set()
    all_pos = {}
    total = 0
    with open(in_path, "r", encoding="utf-8") as f:
        for line in f:
            total += 1
            if total % 20000 == 0:
                print(
                    f"  ... {total} lines, nouns={len(nouns)} verbs={len(verbs)} "
                    f"adjectives={len(adjectives)} preps={len(prepositions)} names={len(proper_nouns)}",
                    file=sys.stderr,
                )
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue
            if entry.get("lang_code") != "sh":
                continue
            if is_form_of_entry(entry):
                continue
            word = entry.get("word", "")
            pattern = CYRILLIC_RE if want_cyrillic else VALID_LATIN_RE
            if not word or not pattern.match(word.lower()):
                continue
            word = word.lower()
            if not entry_has_modern_sense(entry):
                continue

            pos = entry.get("pos")
            all_pos.setdefault(word, set()).add(pos)

            if pos not in ("noun", "verb", "adj", "prep", "prep_phrase", "name"):
                continue
            if pos in ("prep", "prep_phrase"):
                prepositions.add(word)
                continue
            if pos == "name":
                proper_nouns.add(word)
                continue

            forms = usable_forms(entry, word, want_cyrillic)
            target = {"noun": nouns, "verb": verbs, "adj": adjectives}[pos]
            if word not in target:
                target[word] = forms

    with open(f"{out_dir}/wiktionary_allpos.tsv", "wb") as out:
        out.write("word\tpos_values\n".encode("utf-8"))
        for word in sorted(all_pos):
            out.write(f"{word}\t{','.join(sorted(all_pos[word]))}\n".encode("utf-8"))

    def write_forms(path, mapping):
        with open(path, "wb") as out:
            out.write("word\tform\n".encode("utf-8"))
            for word, forms in sorted(mapping.items()):
                if not forms:
                    out.write(f"{word}\t\n".encode("utf-8"))
                    continue
                for form in forms:
                    out.write(f"{word}\t{form}\n".encode("utf-8"))

    write_forms(f"{out_dir}/wiktionary_nouns.tsv", nouns)
    write_forms(f"{out_dir}/wiktionary_verbs.tsv", verbs)
    write_forms(f"{out_dir}/wiktionary_adjectives.tsv", adjectives)

    with open(f"{out_dir}/wiktionary_prepositions.tsv", "wb") as out:
        out.write("word\n".encode("utf-8"))
        for word in sorted(prepositions):
            out.write(f"{word}\n".encode("utf-8"))

    with open(f"{out_dir}/wiktionary_propernouns.tsv", "wb") as out:
        out.write("word\n".encode("utf-8"))
        for word in sorted(proper_nouns):
            out.write(f"{word}\n".encode("utf-8"))

    print(
        f"Done: {total} lines read. nouns={len(nouns)} verbs={len(verbs)} "
        f"adjectives={len(adjectives)} prepositions={len(prepositions)} propernouns={len(proper_nouns)}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    in_path = sys.argv[1] if len(sys.argv) > 1 else "sh-extract.jsonl"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    want_cyrillic = len(sys.argv) > 3 and sys.argv[3] == "cyrillic"
    main(in_path, out_dir, want_cyrillic)
