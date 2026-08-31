# -*- coding: utf-8 -*-
# Phase 4 (Wortfamilien-Projekt): Zentrale Nomen-Formen-Auskunft.
# Primärquelle: wiktionary_nomen.tsv (echte, aus der deutschen Wiktionary belegte Genus-/Genitiv-/
# Pluralangaben, siehe extract_wiktionary_nouns.py) - Nutzerentscheidung 2026-08-31, nachdem sich die rein
# regelbasierte Generierung (genus.py Bigramm-Heuristik + deklination.py/plural.py Regeln) als zu
# fehleranfaellig fuer die "nackten" Maskulina/Neutra ohne erkennbare Endung erwiesen hatte.
# Fallback: die Regel-Module, nur fuer Woerter, die im Wiktionary-Extrakt fehlen (siehe
# AdaptKey-Plan-Wortfamilien.md - erwartete Abdeckung laut Validierung: ~38% der Basisformen, aber ~64%
# der Vorkommenshaeufigkeit, da haeufige Woerter deutlich besser abgedeckt sind als seltene).

from genus import load_bigrams, guess_genus
from deklination import decline_singular
from plural import plural as generate_plural, dativ_plural as generate_dativ_plural

_WIKT_CACHE = None


def load_wiktionary_nouns(path="wiktionary_nomen.tsv"):
    data = {}
    with open(path, encoding="utf-8") as f:
        next(f)  # Header ueberspringen.
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 4:
                word, genus, genitiv, plur = parts[0], parts[1], parts[2], parts[3]
                data[word] = {
                    "genus": genus or None,
                    "genitiv": genitiv or None,
                    "plural": plur or None,
                }
    return data


def get_noun_forms(noun, wiktionary=None, bigrams=None):
    """Liefert alle Deklinationsformen fuer 'noun' (Nominativ-Singular-Schreibweise, z.B. 'Haus').
    Nutzt wiktionary_nomen.tsv wo vorhanden (belegt, hohe Konfidenz), sonst die Bigramm-Genus-Heuristik
    + Regelgeneratoren als Fallback (niedrigere Konfidenz, siehe deklination.py/plural.py).
    Gibt None zurueck, wenn weder Wiktionary-Daten noch eine Genus-Schaetzung verfuegbar sind.
    Rueckgabe: dict mit 'genus', 'quelle' ('wiktionary'|'regel'), 'nominativ_singular',
    'genitiv_singular', 'dativ_singular', 'akkusativ_singular', 'plural', 'dativ_plural'."""
    if wiktionary is None:
        global _WIKT_CACHE
        if _WIKT_CACHE is None:
            _WIKT_CACHE = load_wiktionary_nouns()
        wiktionary = _WIKT_CACHE

    entry = wiktionary.get(noun)
    if entry and entry["genus"]:
        genus = entry["genus"]
        # Auch bei Wiktionary-Treffer: Dativ/Akkusativ Singular sind nicht separat in der TSV gespeichert
        # (nur Genus+Genitiv+Plural wurden extrahiert), werden also weiterhin ueber die - fuer diese beiden
        # Faelle sehr zuverlaessige, weil kaum Ausnahmen habende - Regel abgeleitet; NUR der Genitiv und
        # der Plural kommen, wo vorhanden, direkt aus der belegten Quelle statt geraten zu werden.
        decl = decline_singular(noun, genus)
        genitiv = entry["genitiv"] or decl["genitiv"]
        plur = entry["plural"] or generate_plural(noun, genus)
        return {
            "genus": genus,
            "quelle": "wiktionary",
            "nominativ_singular": noun,
            "genitiv_singular": genitiv,
            "dativ_singular": decl["dativ"],
            "akkusativ_singular": decl["akkusativ"],
            "plural": plur,
            "dativ_plural": generate_dativ_plural(plur),
        }

    # Fallback: Bigramm-Genus-Heuristik + reine Regelgenerierung (niedrigere Konfidenz).
    if bigrams is None:
        bigrams = load_bigrams()
    genus = guess_genus(noun, bigrams)
    if genus is None:
        return None
    decl = decline_singular(noun, genus)
    plur = generate_plural(noun, genus)
    return {
        "genus": genus,
        "quelle": "regel",
        "nominativ_singular": noun,
        "genitiv_singular": decl["genitiv"],
        "dativ_singular": decl["dativ"],
        "akkusativ_singular": decl["akkusativ"],
        "plural": plur,
        "dativ_plural": generate_dativ_plural(plur),
    }


if __name__ == "__main__":
    wikt = load_wiktionary_nouns()
    bigrams = load_bigrams()
    for w in ("Haus", "Frau", "Herr", "Tag", "Widerspruch", "Beispiel", "Prozent", "Selteneswort"):
        forms = get_noun_forms(w, wiktionary=wikt, bigrams=bigrams)
        print(w, forms)
