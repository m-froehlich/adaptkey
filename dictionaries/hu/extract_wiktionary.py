# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# ENGLISH Wiktionary's own coverage of Hungarian (`kaikki.org/dictionary/Hungarian/kaikki.org-dictionary-
# Hungarian.jsonl.gz`, 53.4MB) - Hungarian has NO native Wiktionary edition on kaikki.org (confirmed
# directly).
#
# D-450 REAL FINDING 1 - the SAME paradigm-size class of decision already resolved for Finnish this round,
# applied consistently rather than re-litigated: Hungarian, like Finnish, is agglutinative and its own noun
# declension table ALSO carries genuine possessive-suffix forms on top of its (famously large, ~18-case)
# case x number paradigm - confirmed directly against a real entry ("fa"/"tree": 12 real possessive-suffix
# forms like "fam"/"fád"/"fája" tagged `["possessive", "possessed-single"/"possessed-many", "first-person"/
# "second-person"/"third-person"]`, on top of ~36 real case x number forms). Per the user's own explicit
# decision for Finnish's identical structural situation, the same cap is applied here proactively rather than
# re-asking the identical question: `EXCLUDE_FORM_TAGS` excludes `"possessive"`/`"possessed-single"`/
# `"possessed-many"` forms, keeping the core case x number paradigm only. (Unlike Finnish, Hungarian's own
# possessive paradigm here was NOT crossed with the full case system in this data - only ~12 possessive forms
# per noun, not ~130 - so this is a smaller-scale application of the same principle, not a second full
# structural fork requiring its own separate pause.)
#
# D-450 REAL FINDING 2 - a genuine per-language tag-vocabulary collision, found by direct inspection before
# reusing the shared `EXCLUDE_QUALIFIERS` set unmodified: Hungarian verb conjugation forms use `"formal"`/
# `"informal"` tags for a REAL GRAMMATICAL distinction (the T-V polite/informal second-person address form,
# e.g. formal "van" vs informal "vagy" for "you are") - NOT a stylistic-register/slang marker the way every
# other language's own `"informal"` tag has meant so far. Confirmed directly against a real verb ("van"):
# blindly reusing the shared `EXCLUDE_QUALIFIERS` (which includes `"informal"` for slang-register exclusion
# in every other language) would have silently dropped the entire informal-address conjugation paradigm for
# every Hungarian verb - a real, silent, systematic under-coverage bug specific to this language. Hungarian's
# own `EXCLUDE_QUALIFIERS` below deliberately omits `"informal"` for this reason (accepting that a handful of
# genuinely slang-register lexical items may not be filtered by this one tag alone - "colloquial"/"slang"/
# "rare" etc. still catch most of those).
#
# D-450 MULTI-WORD FORM CHECK: a genuine English-prose noise pattern was found and confirmed - Hungarian's own
# "definite" conjugation (used when the object is a specific, known entity) does not apply to intransitive
# verbs, and this fact is recorded as literal English prose values ("intransitive verb", "definite forms are
# not used") in the same forms[] table - caught for free by the standard whitespace-rejection rule, no
# special-casing needed.
#
# Quelle: https://kaikki.org/dictionary/Hungarian/kaikki.org-dictionary-Hungarian.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

HUNGARIAN_LETTERS = set("abcdefghijklmnopqrstuvwxyzáéíóöőúüű")
VALID_FORM_RE = re.compile(r"^[a-záéíóöőúüű]+(-[a-záéíóöőúüű]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    # "informal" deliberately OMITTED here - see module docstring Finding 2 above.
    "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
    "uncommon", "poetic", "ironic", "literary", "depreciative", "elevatedly",
}

EXCLUDE_FORM_TAGS = {
    "table-tags", "inflection-template", "canonical", "error-unrecognized-form", "error-unknown-tag", "class",
    # D-450 paradigm-size cap, applied consistently with the user's own Finnish decision - see module
    # docstring Finding 1 above.
    "possessive", "possessed-single", "possessed-many",
}
MULTIWORD_TAGS = {"multiword-construction"}


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
            if entry.get("lang_code") != "hu":
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

            if pos not in ("noun", "verb", "adj", "prep", "postp", "prep_phrase", "name"):
                continue
            if pos in ("prep", "prep_phrase", "postp"):
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "hu-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
