# -*- coding: utf-8 -*-
# Einmal-Skript (nicht Teil der Laufzeit-Pipeline): liest den kaikki.org/wiktextract-JSONL-Dump der
# englischen Wiktionary-Ausgabe EINMAL und extrahiert Substantive (Plural), Verben (3rd person singular,
# present participle, simple past, past participle) und Adjektive (Komparativ/Superlativ), sowie die
# geschlossene Klasse der Praepositionen - alles in einem Durchlauf statt vier, da die Quelldatei (22.9GB
# unkomprimiert) sonst viermal gestreamt wuerde. Mirrors dictionaries/de/extract_wiktionary_nouns.py's
# eigene Grundstruktur (form-of-Ausschluss, "erster Treffer gewinnt" bei Homographen), nur fuer vier
# Wortarten statt einer und ohne Genus (Englisch hat keins).
#
# Quelle: https://kaikki.org/dictionary/raw-wiktextract-data.jsonl.gz (die VOLLE, sprachuebergreifende
# Ausgabe der englischsprachigen Wiktionary - anders als beim Deutschen gibt es fuer "Englisch als
# Zielsprache" keine eigene kleinere Datei bei kaikki.org, da die Quelle selbst schon Englisch ist; Filterung
# auf lang_code=="en" geschieht hier im Skript, wie beim Deutschen auf lang_code=="de").
#
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-INHALTE CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts (Wikipedia-Dumps) bereits stammen.
#
# Tag-Vokabular VERIFIZIERT gegen echte Eintraege (inspect_sample.py/inspect_irregular.py), nicht geraten -
# insbesondere: es gibt KEIN einzelnes "third-person-singular"/"present-participle"/"past-participle"-Tag,
# sondern jeweils eine KOMBINATION mehrerer Einzeltags (siehe TAG-Kommentare unten). Real beobachtet an
# "go"/"be"/"good"/"child"/"mouse"/"sheep": Wiktionary listet oft mehrere Kandidaten pro Slot, inkl.
# veralteter/nichtstandardsprachlicher Varianten (z.B. "goodest" fuer "good", "yode" fuer "go") - "erster
# Treffer gewinnt" allein war beim Deutschen ausreichend, hier zusaetzlich ein expliziter Qualifier-Ausschluss
# (EXCLUDE_QUALIFIERS), damit die Auswahl nicht von zufaelliger Listenreihenfolge abhaengt.

import gzip
import json
import re
import sys

# Formen mit einem dieser Tags werden nie als Kandidat akzeptiert, unabhaengig von Position in der Liste -
# reale Beispiele: "gooder"/"goodest" (humorous/nonstandard fuer "good"), "yode"/"goed" (obsolete/nonstandard
# fuer "went"), "childer" (archaic/dialectal fuer "children"), "sheeps" (childish/humorous/nonstandard).
EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative",
}


def usable_form(f):
    form = f.get("form", "")
    if not form or form in ("-", "—") or " " in form or "'" in form:
        return None
    if set(f.get("tags", [])) & EXCLUDE_QUALIFIERS:
        return None
    return form


# D-Wortfamilien-EN: a whole ENTRY (not just one of its forms) can itself be an archaic/obsolete/dialectal
# *sense* - real example found via spot-check, not guessed: "child" has a genuine Wiktionary `pos: "verb"`
# entry, but its single sense's own gloss reads "(archaic, ambitransitive) To give birth..." - a real
# historical usage (Shakespeare-era quotations), not a modern verb "to child" anyone would want autocorrect
# defending. Wiktionary marks this via a qualifier label in parentheses at the start of each sense's own
# gloss, not via the forms[]-level "tags" EXCLUDE_QUALIFIERS above already filters - a different signal,
# checked here at the whole-entry level. An entry is only rejected when EVERY sense carries such a label
# (some words genuinely have both an archaic AND a live modern sense - those are kept).
ARCHAIC_GLOSS_PATTERN = re.compile(
    r"^\((?:[^)]*\b(?:archaic|obsolete|dialectal|dialect|nonstandard|rare|proscribed)\b[^)]*)\)"
)


def entry_has_modern_sense(entry):
    senses = entry.get("senses", [])
    if not senses:
        return True
    for sense in senses:
        glosses = sense.get("raw_glosses") or sense.get("glosses") or []
        for gloss in glosses:
            if not ARCHAIC_GLOSS_PATTERN.match(gloss.strip()):
                return True
    return False


def extract_noun(entry):
    for f in entry.get("forms", []):
        form = usable_form(f)
        if form and "plural" in set(f.get("tags", [])):
            return form
    return None


def extract_verb(entry):
    # Es gibt keine Einzeltags fuer diese vier Slots - jeweils eine TAG-KOMBINATION, siehe Modul-Kommentar.
    s_form = ing_form = past = participle = None
    for f in entry.get("forms", []):
        form = usable_form(f)
        if form is None:
            continue
        tags = set(f.get("tags", []))
        if s_form is None and {"present", "third-person"} <= tags and "participle" not in tags:
            s_form = form
        elif ing_form is None and {"participle", "present"} <= tags:
            ing_form = form
        elif past is None and "past" in tags and "participle" not in tags:
            past = form
        elif participle is None and {"past", "participle"} <= tags:
            participle = form
    if s_form is None and ing_form is None and past is None and participle is None:
        return None
    return {"s_form": s_form, "ing_form": ing_form, "past": past, "participle": participle}


def extract_adjective(entry):
    comparative = None
    superlative = None
    for f in entry.get("forms", []):
        form = usable_form(f)
        if form is None:
            continue
        tags = set(f.get("tags", []))
        if "comparative" in tags and comparative is None:
            comparative = form
        if "superlative" in tags and superlative is None:
            superlative = form
    if comparative is None and superlative is None:
        return None
    return (comparative or "", superlative or "")


def main(in_path, out_dir):
    nouns = {}
    verbs = {}
    adjectives = {}
    prepositions = set()
    total = 0
    with gzip.open(in_path, "rt", encoding="utf-8") as f:
        for line in f:
            total += 1
            if total % 2000000 == 0:
                print(
                    f"  ... {total} Zeilen, nouns={len(nouns)} verbs={len(verbs)} "
                    f"adjectives={len(adjectives)} preps={len(prepositions)}",
                    file=sys.stderr,
                )
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue
            if entry.get("lang_code") != "en":
                continue
            pos = entry.get("pos")
            if pos not in ("noun", "verb", "adj", "prep"):
                continue
            if "form-of" in entry.get("tags", []):
                continue
            word = entry.get("word", "")
            if not word or not word.isalpha():
                continue
            if not entry_has_modern_sense(entry):
                continue

            if pos == "prep":
                prepositions.add(word)
                continue
            if pos == "noun" and word not in nouns:
                plural = extract_noun(entry)
                if plural is not None:
                    nouns[word] = plural
            elif pos == "verb" and word not in verbs:
                forms = extract_verb(entry)
                if forms is not None:
                    verbs[word] = forms
            elif pos == "adj" and word not in adjectives:
                forms = extract_adjective(entry)
                if forms is not None:
                    adjectives[word] = forms

    with open(f"{out_dir}/wiktionary_nouns.tsv", "wb") as out:
        out.write("word\tplural\n".encode("utf-8"))
        for word, plural in sorted(nouns.items()):
            out.write(f"{word}\t{plural}\n".encode("utf-8"))

    with open(f"{out_dir}/wiktionary_verbs.tsv", "wb") as out:
        out.write("word\ts_form\ting_form\tpast\tparticiple\n".encode("utf-8"))
        for word, forms in sorted(verbs.items()):
            row = [word, forms.get("s_form") or "", forms.get("ing_form") or "", forms.get("past") or "", forms.get("participle") or ""]
            out.write(("\t".join(row) + "\n").encode("utf-8"))

    with open(f"{out_dir}/wiktionary_adjectives.tsv", "wb") as out:
        out.write("word\tcomparative\tsuperlative\n".encode("utf-8"))
        for word, (comparative, superlative) in sorted(adjectives.items()):
            out.write(f"{word}\t{comparative}\t{superlative}\n".encode("utf-8"))

    with open(f"{out_dir}/wiktionary_prepositions.tsv", "wb") as out:
        out.write("word\n".encode("utf-8"))
        for word in sorted(prepositions):
            out.write(f"{word}\n".encode("utf-8"))

    print(
        f"Fertig: {total} Zeilen gelesen. nouns={len(nouns)} verbs={len(verbs)} "
        f"adjectives={len(adjectives)} prepositions={len(prepositions)}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    in_path = sys.argv[1] if len(sys.argv) > 1 else "raw-wiktextract-data.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
