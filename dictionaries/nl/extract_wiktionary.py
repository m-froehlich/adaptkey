# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# NATIVE Dutch Wiktionary edition (WikiWoordenboek itself documents Dutch, NOT the English Wiktionary's own
# coverage of Dutch - see the Language Contribution Guide's own §3 for the full reasoning:
# kaikki.org/dictionary/downloads/nl/nl-extract.jsonl.gz, 127.8MB compressed, NOT
# kaikki.org/dictionary/Dutch/kaikki.org-dictionary-Dutch.jsonl.gz, 29.2MB - here the native file follows
# the expected French/Spanish pattern (much bigger than the wrong file, ~4.4x), unlike Portuguese's/Italian's
# own reversed sizes this same round found) and extracts, for nouns/verbs/adjectives, every attested,
# grammatically-tagged inflection form, plus the closed class of prepositions and the set of proper nouns -
# all in one pass. Directly modelled on dictionaries/fr/extract_wiktionary.py/dictionaries/it/
# extract_wiktionary.py (generic word\tform output).
#
# Verified directly against real entries before writing this: Dutch's own native edition documents noun/
# adjective inflection very richly - 130,196 of 149,010 real noun lemmas have real forms (plural, AND a
# genuine Dutch-specific "diminutive" form, e.g. huis -> huisje - kept as a real, generated NOUN form/lemma
# link, not excluded, since it is a genuine everyday word Dutch speakers type constantly); 15,706 of 19,440
# adjective lemmas have real forms (inflected/comparative/superlative/partitive). Verbs show the identical
# individually-paged-conjugated-form shape (141,165 raw "verb" entries, 128,424 senses[].form_of references -
# ~12,700 real lemmas), so the same senses[].form_of filter applies unchanged.
#
# D-447 REAL BUG, found the hard way (a calibration ratio of ~173x, the identical class of impossible value
# French's own D-444-followup "il/elle/on mange" bug produced, caught the same way - a sanity-check of the
# calibration's own top outliers): Dutch's own conjugation tables are FULL periphrastic-tense tables (e.g.
# "ingebakerd zullen hebben" - future perfect infinitive of "inbakeren" - or, for separable verbs, a
# two-word single-clause form like "baker in", verb stem first, separable particle last). French's own
# last_token()+split_alternate_forms() recovery (designed for French's OPPOSITE shape - "il/elle/on mange",
# pronoun-prefix(es) first, the real verb LAST) silently extracted the wrong half here: last_token("baker
# in") -> "in"; last_token("ingebakerd zullen hebben") -> "hebben"; and so on for "worden"/"zijn" via other
# periphrastic passive/perfect forms. Every one of these bare auxiliaries/particles is an extremely common,
# semantically unrelated standalone Dutch word (confirmed: 18,025 rows across the whole file wrongly linked
# some verb lemma to bare "in"/"hebben"/"worden"/"zijn"), and their astronomical own frequency (491,058 for
# "in" alone) swamped the real per-verb signal, producing the impossible ~173x ratio. Fixed at the root:
# Dutch has no equivalent to French's pronoun-prefix pattern, so `last_token()` is not used here at all - any
# raw form containing whitespace is rejected outright (the same "multi-word forms are excluded" convention
# every language already applies for forms containing a space, simply applied BEFORE any attempt to recover
# a trailing word rather than after). Dutch's own genuinely useful single-word forms (subordinate-clause
# forms like "inbaker"/"inbakert"/"inbakerde"/"inbakerden", the two participles "inbakerend"/"ingebakerd")
# are already present as their own single-word entries in the source data, so nothing real is lost by this
# fix - only the contaminated periphrastic/separable-verb noise. Confirmed after the fix: median verb ratio
# dropped from the impossible 172.78 to a real, plausible value (see the final `LanguagePackCatalog.Entry`
# for the actual number) - re-verified directly against real dict.tsv rows, not assumed.
#
# Multi-word forms are automatically excluded since they contain a space - the same filter every other
# language script already uses, no Dutch-specific handling needed.
#
# Quelle: https://kaikki.org/dictionary/downloads/nl/nl-extract.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

DUTCH_LETTERS = set("abcdefghijklmnopqrstuvwxyzëïöüéè")
VALID_FORM_RE = re.compile(r"^[a-zëïöüéè]+(-[a-zëïöüéè]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
}

EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical"}
MULTIWORD_TAGS = {"multiword-construction"}


def split_alternate_forms(form):
    for sep in ("/", "\\", ","):
        if sep in form:
            return [part.strip() for part in form.split(sep) if part.strip()]
    return [form]


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
        # D-447: no last_token() recovery for Dutch - see the module docstring's own real-bug account for
        # why that French-shaped heuristic silently extracts the wrong half of a Dutch periphrastic/
        # separable-verb form. Any whitespace at all means this is not a genuine single-word form.
        if any(ch.isspace() for ch in stripped):
            continue
        for form in split_alternate_forms(stripped):
            form = form.strip()
            if form == word or not VALID_FORM_RE.match(form):
                continue
            result[form] = True
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
            if entry.get("lang_code") != "nl":
                continue
            if is_form_of_entry(entry):
                continue
            word = entry.get("word", "")
            if not word or not VALID_FORM_RE.match(word.lower()):
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "nl-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
