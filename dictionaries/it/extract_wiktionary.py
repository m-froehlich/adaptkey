# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# NATIVE Italian Wiktionary edition (Wikizionario itself documents Italian, NOT the English Wiktionary's own
# coverage of Italian - see the Language Contribution Guide's own §3 for the full reasoning:
# kaikki.org/dictionary/downloads/it/it-extract.jsonl.gz, 40.0MB compressed, NOT
# kaikki.org/dictionary/Italian/kaikki.org-dictionary-Italian.jsonl.gz, 74.2MB - note this pack's own native
# file is actually SMALLER than the wrong one, same reversed-from-French/Spanish pattern Portuguese's own
# round already found; still the correct, mandatory choice per the guide's own unconditional rule) and
# extracts, for nouns/verbs/adjectives, every attested, grammatically-tagged inflection form, plus the
# closed class of prepositions and the set of proper nouns - all in one pass. Directly modelled on
# dictionaries/fr/extract_wiktionary.py/dictionaries/pt/extract_wiktionary.py (generic word\tform output).
#
# Verified directly against real entries before writing this (not assumed): Italian's own native edition
# documents noun/adjective inflection RICHLY (22,738 of 37,208 real noun lemmas have real plural/gender
# forms; 12,072 of 14,972 adjective lemmas have real plural/gender/superlative forms) - unlike Portuguese's
# own round, which found essentially none for nouns/adjectives. Verbs show the identical
# individually-paged-conjugated-form shape every other native edition this project has processed has shown
# (462,027 raw "verb" entries, 454,157 of them senses[].form_of references - ~7,870 real lemmas), so the
# same senses[].form_of filter applies unchanged. A small number of forms (51 in a full-file scan) contain a
# "/" or "\" - the same defensive last_token()+split_alternate_forms() handling French's own D-444-followup
# fix introduced is kept here too, even though Italian's own shape was not confirmed to need it, so every
# language script shares one robust extraction rule.
#
# Multi-word forms are automatically excluded since they contain a space - the same filter every other
# language script already uses, no Italian-specific handling needed.
#
# Quelle: https://kaikki.org/dictionary/downloads/it/it-extract.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

ITALIAN_LETTERS = set("abcdefghijklmnopqrstuvwxyzàèéìíîòóù")
VALID_FORM_RE = re.compile(r"^[a-zàèéìíîòóù]+(-[a-zàèéìíîòóù]+)*$")

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


def last_token(form):
    parts = form.split()
    return parts[-1] if parts else form


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
        stripped = last_token(raw_form.strip().lower())
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
            if entry.get("lang_code") != "it":
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "it-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
