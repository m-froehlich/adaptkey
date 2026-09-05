# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# NATIVE Indonesian Wiktionary edition (`kaikki.org/dictionary/downloads/id/id-extract.jsonl.gz`, 2.87MB
# compressed - genuinely SMALLER than the wrong English-Wiktionary-coverage file's 9.77MB, confirming the
# Guide's own warning that the size relationship can run either direction; the native URL's own structure,
# not its size, is what confirms it is the correct source) and extracts, for nouns/verbs/adjectives, every
# attested, grammatically-tagged inflection form, plus the closed class of prepositions and the set of proper
# nouns - all in one pass. Directly modelled on dictionaries/pl/extract_wiktionary.py.
#
# D-450 REAL FINDING: Indonesian's own verb morphology is documented far more richly than Malay's own native
# edition (dictionaries/ms/, the same underlying isolating/agglutinative-by-affixation Austronesian
# language family) - confirmed directly against real verbs ("melali", "merundung"): real voice/aspect
# prefixes (ku-/kau-/di-/ter- passive constructions), the bare transitive root, and the imperative -lah
# suffix are all documented as real forms[] entries of the SAME lemma, unlike Malay's own near-empty verb
# forms data. This is a genuine, source-specific richness difference between the two native editions, not an
# inconsistency in this project's own extraction logic. Indonesian's own noun plural is also reduplication-
# based (e.g. "petugas" -> "petugas-petugas"), the identical pattern already confirmed for Malay - the
# standard `(-[...]+)*` hyphen-joining `VALID_FORM_RE` shape handles it with no special-casing.
#
# Quelle: https://kaikki.org/dictionary/downloads/id/id-extract.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

INDONESIAN_LETTERS = set("abcdefghijklmnopqrstuvwxyz")
VALID_FORM_RE = re.compile(r"^[a-z]+(-[a-z]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
    "uncommon", "poetic", "ironic", "literary", "depreciative", "elevatedly",
}

EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical", "error-unrecognized-form", "error-unknown-tag", "class"}
MULTIWORD_TAGS = {"multiword-construction"}

# D-450 REAL CALIBRATION BUG, found via the mandatory ratio sanity check (adjective ratio 802.5x, far beyond
# the sane range): this source documents Indonesian's own reduplicated-plural/intensive adjective and noun
# forms (e.g. "berat-berat", "petugas-petugas") as their OWN SEPARATE dictionary entries, whose own forms[]
# then lists the bare singular/root word as if it were a "form of" the reduplicated entry (backwards from
# every other language's own base-lemma-first convention) - confirmed directly against the raw entry for
# "berat-berat" (forms: `[{"form": "berat", "tags": ["singular"]}]`). Since the bare root is always far more
# frequent than its own reduplicated form, treating the reduplicated entry as the authoritative lemma
# produces an inverted, wildly inflated ratio. The CORRECT direction (base -> reduplicated) is separately and
# correctly present via the base word's own entry (e.g. "petugas" genuinely lists "petugas-petugas" as its
# own plural form) - both directions coexist redundantly in this source. Fixed by skipping any entry whose
# own `word` is a self-reduplication (`X-X`) from ever becoming a lemma key itself, while the correct-
# direction relationship (via the base word's own entry) is unaffected.
SELF_REDUPLICATION_RE = re.compile(r"^(.+)-\1$")


def usable_forms(entry, word):
    result = {}
    for f in entry.get("forms", []):
        raw_form = f.get("form", "")
        if not raw_form or raw_form in ("-", "—"):
            continue
        tags = set(f.get("tags", []))
        if not tags or tags & EXCLUDE_FORM_TAGS:
            continue
        if tags & EXCLUDE_QUALIFIERS or tags & MULTIWORD_TAGS:
            continue
        stripped = raw_form.strip().lower()
        if any(ch.isspace() for ch in stripped):
            continue
        if stripped == word or not VALID_FORM_RE.match(stripped):
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


def main(in_path, out_dir):
    nouns = {}
    verbs = {}
    adjectives = {}
    prepositions = set()
    proper_nouns = set()
    all_pos = {}
    total = 0
    with gzip.open(in_path, "rt", encoding="utf-8") as f:
        for line in f:
            total += 1
            if total % 1000000 == 0:
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
            if entry.get("lang_code") != "id":
                continue
            if is_form_of_entry(entry):
                continue
            word = entry.get("word", "")
            if not word or not VALID_FORM_RE.match(word.lower()):
                continue
            word = word.lower()
            if SELF_REDUPLICATION_RE.match(word):
                continue
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

            forms = usable_forms(entry, word)
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "id-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
