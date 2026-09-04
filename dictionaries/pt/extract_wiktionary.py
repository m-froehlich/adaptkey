# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# NATIVE Portuguese Wiktionary edition (Wikcionario/Wikcionário lusófono itself documents Portuguese, NOT
# the English Wiktionary's own coverage of Portuguese - see the Language Contribution Guide's own §3 for the
# full reasoning why this is a real, previously-made mistake for French/Spanish:
# kaikki.org/dictionary/downloads/pt/pt-extract.jsonl.gz, 35.4MB compressed, NOT
# kaikki.org/dictionary/Portuguese/kaikki.org-dictionary-Portuguese.jsonl.gz, 54.2MB - note this pack's own
# native file is actually SMALLER than the wrong one, the reverse of French's/Spanish's own pattern; still
# the correct, mandatory choice per the guide's own unconditional rule, not a size-dependent judgement call)
# and extracts, for nouns/verbs/adjectives, every attested, grammatically-tagged inflection form, plus the
# closed class of prepositions and the set of proper nouns - all in one pass. Directly modelled on
# dictionaries/fr/extract_wiktionary.py/dictionaries/es/extract_wiktionary.py (generic word\tform output,
# not fixed named slots - Portuguese verb conjugation has far too many combinations for that).
#
# ONE REAL, STRUCTURAL DIFFERENCE FROM FRENCH/SPANISH, found by direct inspection of real entries (not
# assumed): Portuguese verbs carry rich, real forms[] tables (confirmed: 79,453 raw "verb" entries, 72,710
# of them senses[].form_of references to another lemma - the identical individually-paged-conjugated-form
# shape French/Spanish already found, so the same senses[].form_of filter applies unchanged) - but nouns and
# adjectives essentially do NOT: of 52,477 noun lemmas, only 137 have any forms[] entry beyond a "canonical"
# tag (a hyphenated syllable-break annotation of the word itself, e.g. "grego" -> "gre.go", NOT a real
# inflected form - filtered out below the same way French/Spanish already drop "table-tags"/
# "inflection-template"); of 18,413 adjective lemmas, only 70. Portuguese noun/adjective regular
# plural/gender inflection is simply not documented as forms[] data in this edition (plausibly because it is
# regular enough that Wiktionary does not bother tabulating it the way German's declension is) - a genuine,
# confirmed source limitation, not a bug in this script. Consequence, agreed with the user before building
# this round: Portuguese Wortfamilien completion (merge_wiktionary.py) is real and complete for VERBS only
# this round; noun/adjective forms stay whatever the base dict.tsv/POS-tagging pass already produced, with
# no generated inflected forms or lemma links for those two categories - documented honestly in
# AdaptKey-Progress.md/AdaptKey-History.md, not silently passed off as complete.
#
# Multi-word forms (Portuguese periphrastic constructions, e.g. comparative "melhor do que") are
# automatically excluded since they contain a space - the same filter German/Greek/English/French/Spanish
# already use, no Portuguese-specific handling needed.
#
# Quelle: https://kaikki.org/dictionary/downloads/pt/pt-extract.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

PORTUGUESE_LETTERS = set("abcdefghijklmnopqrstuvwxyzãõáéíóúàâêôç")
VALID_FORM_RE = re.compile(r"^[a-zãõáéíóúàâêôç]+(-[a-zãõáéíóúàâêôç]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
}

# D-445 (Portuguese, see module docstring): "canonical" marks a hyphenated syllable-break annotation of the
# lemma itself (grego -> gre.go), never a genuine inflected form - excluded the same way French/Spanish
# already exclude "table-tags"/"inflection-template".
EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical"}

MULTIWORD_TAGS = {"multiword-construction"}


def split_alternate_forms(form):
    for sep in ("/", "\\", ","):
        if sep in form:
            return [part.strip() for part in form.split(sep) if part.strip()]
    return [form]


def last_token(form):
    """Defensive port of French's own D-444-followup fix (a combined-pronoun-slot notation, "il/elle/on
    mange") - Portuguese's own forms were checked directly and do not show this shape (every sampled form is
    already a clean single word), but the same defensive rule is kept anyway so every language script shares
    one robust extraction rule rather than relying on this edition happening not to need it."""
    parts = form.split()
    return parts[-1] if parts else form


def usable_forms(entry, word):
    """Every distinct, grammatically-tagged single-word form of `entry` that differs from `word` itself."""
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
        stripped = last_token(raw_form.strip().lower())
        for form in split_alternate_forms(stripped):
            form = form.strip()
            if form == word or not VALID_FORM_RE.match(form):
                continue
            result[form] = True
    return list(result.keys())


def is_form_of_entry(entry):
    """A whole entry is really just a reference page for ONE inflected form of another lemma - real,
    measured shape for this edition (see module docstring) - not a genuine independent lemma."""
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
    # D-444-style broad collision signal (see dictionaries/fr/extract_wiktionary.py's own comment): the FULL
    # set of pos values kaikki documents for a word, not only noun/verb/adjective - needed by the
    # proper-noun collision check in merge_wiktionary.py.
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
            if entry.get("lang_code") != "pt":
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
                    # still register the lemma itself (POS-tagging-only, nothing to link/generate) - an
                    # empty form field is skipped by merge_wiktionary.py's own reader when iterating forms,
                    # but the lemma key itself still reaches add_tag().
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "pt-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
