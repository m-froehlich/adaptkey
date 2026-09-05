# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# ENGLISH Wiktionary's own coverage of Slovak (`kaikki.org/dictionary/Slovak/kaikki.org-dictionary-
# Slovak.jsonl.gz`, 5.3MB - by far the smallest fallback source checked so far this project) - Slovak has NO
# native Wiktionary edition on kaikki.org (confirmed directly).
#
# D-450 REAL, STRUCTURALLY DIFFERENT DATA SHAPE (found by direct inspection before writing any parsing logic,
# not assumed to match any prior language): Slovak NOUN entries encode their declension table as a FLAT,
# POSITIONAL stream of `forms[]` entries with NO `tags` key on the actual word-form values at all - instead,
# English case/number LABEL WORDS ("singular", "plural", "nominative", "genitive", ...) appear as their own,
# also-untagged entries interleaved among the real word forms, acting as row/column headers for a table that
# wiktextract has flattened without preserving row/column position reliably (confirmed directly: one real
# noun's own "accusative" label was followed by ZERO forms and its own "instrumental" label by only ONE
# form instead of the expected two, i.e. the raw form COUNT following a label cannot be trusted to reliably
# encode which number a given form belongs to). Since this project's own dict.tsv format only ever needs the
# SET of real form-strings per lemma (see merge_wiktionary.py's own `read_grouped()` - it discards any
# per-form grammatical tag entirely, only `word` and `form` matter downstream), the case/number distinction
# does not need to be recovered at all - only "is this raw string a genuine word form, or a row/column label
# artifact" needs deciding. `usable_forms()` below therefore does NOT require a `tags` key to accept a form
# (unlike every prior language's own version) - it instead explicitly excludes the small, closed, English-
# only label vocabulary these table headers use (confirmed there is no real Slovak-word collision risk: none
# of "nominative"/"genitive"/"singular"/etc. are themselves real Slovak words). Verbs and adjectives were
# checked separately and do NOT share this shape - both carry full, real tags on every form entry, the
# ordinary shape every other language in this project already uses.
#
# D-450 MULTI-WORD FORM CHECK: Slovak verbs DO have genuine periphrastic/analytic forms with an auxiliary
# ("cestoval som" - "I travelled", literally "travelled I"; "bol som cestoval" - pluperfect) - confirmed by
# direct inspection of a real verb's own forms[] table. The same "reject any whitespace-containing form
# outright" rule already used for every Scandinavian language and Dutch/Turkish this project has built
# applies here too, no `last_token()` recovery attempted.
#
# Quelle: https://kaikki.org/dictionary/Slovak/kaikki.org-dictionary-Slovak.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

SLOVAK_LETTERS = set("abcdefghijklmnopqrstuvwxyzáäčďéíĺľňóôŕšťúýž")
VALID_FORM_RE = re.compile(r"^[a-záäčďéíĺľňóôŕšťúýž]+(-[a-záäčďéíĺľňóôŕšťúýž]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
    "uncommon", "poetic", "ironic", "literary", "depreciative", "elevatedly",
    # Slovak-specific addition, per direct inspection: a real abbreviation ("aug" for "august") turned up
    # tagged only `["alternative", "abbreviation"]` - "alternative" alone is not exclusive to abbreviations
    # elsewhere, so "abbreviation" is added explicitly rather than assumed already covered.
    "abbreviation",
}

EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical", "error-unrecognized-form", "error-unknown-tag", "class"}
MULTIWORD_TAGS = {"multiword-construction"}

# D-450: Slovak's own noun-table row/column English label vocabulary - confirmed by direct inspection, not
# guessed - excluded ONLY when the entry carries no tags of its own (a real Slovak word form with this exact
# spelling would still carry a `tags` list and would not be affected; none of these are real Slovak words).
TABLE_LABEL_WORDS = {
    "singular", "plural", "dual",
    "nominative", "genitive", "dative", "accusative", "locative", "instrumental", "vocative",
    "masculine", "feminine", "neuter", "animate", "inanimate", "virile", "nonvirile",
}


def usable_forms(entry, word):
    result = {}
    for f in entry.get("forms", []):
        raw_form = f.get("form", "")
        if not raw_form or raw_form in ("-", "—"):
            continue
        tags = set(f.get("tags", []))
        if tags & EXCLUDE_FORM_TAGS:
            continue
        if tags & EXCLUDE_QUALIFIERS or tags & MULTIWORD_TAGS:
            continue
        stripped = raw_form.strip().lower()
        if not tags and stripped in TABLE_LABEL_WORDS:
            continue
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
            if entry.get("lang_code") != "sk":
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "sk-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
