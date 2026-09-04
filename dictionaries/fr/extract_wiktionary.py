# -*- coding: utf-8 -*-
# Einmal-Skript (nicht Teil der Laufzeit-Pipeline): liest den kaikki.org/wiktextract-JSONL-Dump der
# FRANZOESISCHEN Wiktionary-Ausgabe (Wiktionnaire selbst dokumentiert Franzoesisch, NICHT die englische
# Wiktionary-Ausgabe ueber Franzoesisch - siehe die Language Contribution Guide's eigenen Abschnitt 3 fuer
# die volle Begruendung, warum das ein echter, bereits einmal falsch gemachter Unterschied ist:
# kaikki.org/dictionary/downloads/fr/fr-extract.jsonl.gz, 714,6MB komprimiert, NICHT
# kaikki.org/dictionary/French/kaikki.org-dictionary-French.jsonl.gz, nur 56,5MB) und extrahiert fuer
# Substantive, Verben und Adjektive JEDE belegte, grammatisch getaggte Flexionsform, sowie die geschlossene
# Klasse der Praepositionen und die Menge der Eigennamen - alles in einem Durchlauf. Direkt an
# dictionaries/el/extract_wiktionary.py angelehnt (generische word\tform-Ausgabe statt fester Slots - siehe
# dessen eigenen Kommentar fuer die Begruendung: Franzoesische Verbkonjugation hat zu viele Kombinationen
# fuer feste, von Hand benannte Slots wie beim Englischen).
#
# ZWEI ECHTE, STRUKTURELLE UNTERSCHIEDE zur muttersprachlichen deutschen/griechischen Ausgabe, gefunden
# durch direkte Inspektion echter Eintraege (nicht geraten), die diese Version gegenueber Deutsch/Griechisch
# neu behandelt:
#  1. "form-of"-Markierung sitzt NICHT im obersten `tags`-Feld des Eintrags (das ist bei dieser Ausgabe fast
#     immer None), sondern in JEDER einzelnen `senses[].form_of`/`senses[].tags` - eine einzelne konjugierte
#     Form (z.B. "hablo") ist hier oft ihr EIGENER Top-Level-Eintrag (pos="verb", KEIN eigenes forms[]),
#     nicht nur ein Element in der forms[]-Liste des Infinitivs. Ungefiltert wuerde das die Verb-Anzahl um
#     das 30-fache aufblasen (echt gemessen: 1.265.901 rohe "verb"-Eintraege, nur 40.263 davon echte Lemmata
#     mit eigener Konjugationstabelle). Fix: ein Eintrag mit irgendeiner form_of-tragenden `senses[]`-Zeile
#     wird komplett uebersprungen, unabhaengig vom obersten `tags`-Feld.
#  2. Archaisch/veraltet/derb-Markierungen sitzen hier direkt als strukturiertes `senses[].tags`-Feld
#     (`obsolete`/`archaic`/`colloquial`/`slang`/...), nicht in Klammern im Fliesstext der Glosse wie bei
#     Deutsch/Griechisch/Englisch - direkt darauf gepruefte, verlaesslichere Variante statt Regex auf
#     Fliesstext.
#
# Mehrwortige Formen (frz. periphrastische Verbformen wie "avoir lu"/"en ayant lu" - Verbundzeiten,
# gerundive Verlaufsformen) werden automatisch ausgeschlossen, da sie ein Leerzeichen enthalten - derselbe
# Filter wie bei Deutsch/Griechisch/Englisch, kein franzoesischspezifischer Sonderfall noetig.
#
# Quelle: https://kaikki.org/dictionary/downloads/fr/fr-extract.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-INHALTE CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

FRENCH_LETTERS = set("abcdefghijklmnopqrstuvwxyzàâäéèêëîïôöùûüÿçœæ")
VALID_FORM_RE = re.compile(r"^[a-zàâäéèêëîïôöùûüÿçœæ]+(-[a-zàâäéèêëîïôöùûüÿçœæ]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
}


def split_alternate_forms(form):
    for sep in ("/", "\\", ","):
        if sep in form:
            return [part.strip() for part in form.split(sep) if part.strip()]
    return [form]


# D-444-followup: French verb conjugation tables in this native edition write the "il/elle/on mange"-style
# ambiguous-subject present/imperfect/future/conditional slots as ONE string, the alternatives joined by "/"
# on the PRONOUN alone, with the actual conjugated verb glued on after a space only on the LAST alternative
# ("il/elle/on étourdit", not "il étourdit/elle étourdit/on étourdit"). Naively splitting that whole string
# on "/" first (as split_alternate_forms/Greek's own approach does) produces two bare pronoun fragments
# ("il", "elle") that pass every other check (real letters, no exclude-qualifier tag) and get wrongly
# recorded as if they were genuine one-word verb forms - confirmed the hard way: "il"/"elle" ended up
# attached to thousands of unrelated verb lemmas, each at "il"'s own real corpus frequency (106,581),
# which then poisoned the frequency-ratio calibration (a verb ratio of ~163x, physically impossible for a
# real inflected-form/lemma relationship) before a spot-check of the calibration's own top outliers caught
# it. Fixed by taking the LAST whitespace-separated token FIRST (stripping any pronoun-subject/auxiliary-
# verb prefix, "il/elle/on étourdit" -> "étourdit", "avoir lu" -> "lu"), only THEN splitting that final
# token on "/" for a genuine word-level alternation (e.g. a real alternate spelling with no space at all,
# unaffected by this fix). A periphrastic tense's own explicit "multiword-construction" tag is excluded
# outright before this even runs - a compound form's only single-word content worth keeping ("lu" from
# "avoir lu") is already captured directly via its own dedicated participle-tagged forms[] entry, so nothing
# is actually lost by dropping the periphrastic listing itself.
MULTIWORD_TAGS = {"multiword-construction"}


def last_token(form):
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
        if not tags or "table-tags" in tags or "inflection-template" in tags:
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
    measured shape for this edition (see module docstring point 1) - not a genuine independent lemma."""
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
    # D-444: the FULL set of pos values kaikki documents for a word (every pos type, not only the four this
    # script builds forms for) - the proper-noun collision check needs this broader signal, not just
    # "does this word also appear as a noun/verb/adjective in this same run": a closed-class reading
    # (pronoun, adverb, conjunction, determiner, interjection - none of which PartOfSpeech has a category
    # for) is just as real a collision as a noun/verb one. Real, found example: French "les" (a pronoun,
    # "them") also has a genuine but rare Wiktionary "name" (surname) entry - without this broader check it
    # would have been wrongly force-capitalised as PROPER_NOUN every time, the exact class of bug this
    # round's own bare-noun safety check exists to prevent, just for PROPER_NOUN instead of NOUN.
    all_pos = {}
    total = 0
    with gzip.open(in_path, "rt", encoding="utf-8") as f:
        for line in f:
            total += 1
            if total % 1000000 == 0:
                print(
                    f"  ... {total} Zeilen, nouns={len(nouns)} verbs={len(verbs)} "
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
            if entry.get("lang_code") != "fr":
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
                    # still register the lemma itself (POS-tagging-only, nothing to link/generate) -
                    # an empty form field is skipped by merge_wiktionary.py's own reader when iterating
                    # forms, but the lemma key itself still reaches add_tag().
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
        f"Fertig: {total} Zeilen gelesen. nouns={len(nouns)} verbs={len(verbs)} "
        f"adjectives={len(adjectives)} prepositions={len(prepositions)} propernouns={len(proper_nouns)}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    in_path = sys.argv[1] if len(sys.argv) > 1 else "fr-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
