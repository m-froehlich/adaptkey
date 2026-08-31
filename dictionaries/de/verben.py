# -*- coding: utf-8 -*-
# Phase 2 (Wortfamilien-Projekt): Zentrale Verb-Formen-Auskunft.
# Analog zu nomen.py: Primärquelle wiktionary_verben.tsv (echte, aus der deutschen Wiktionary belegte
# Konjugationstabellen, siehe extract_wiktionary_verbs.py) - Nutzerentscheidung 2026-08-31 ("so viel
# Nutzen aus dem Wiktionary-Extrakt ziehen wie moeglich, gleicher Ansatz wie bei den Verben" - gemeint war
# der bei den Nomen etablierte Ansatz, hier auf Verben zurueckuebertragen). Fallback: konjugation.py
# (classify_and_conjugate: starke/schwache Verben + kuratierte Praefix-Ausnahmetabelle).

from konjugation import classify_and_conjugate

_WIKT_CACHE = None


def load_wiktionary_verbs(path="wiktionary_verben.tsv"):
    data = {}
    with open(path, encoding="utf-8") as f:
        next(f)  # Header ueberspringen.
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 16:
                continue
            word = parts[0]
            if " " in word:
                continue  # mehrwortige Phrasal-Eintraege ("ab sein") passen nicht ins Ein-Wort-Schema.
            praesens = parts[1:7]
            praeteritum = parts[7:13]
            partizip2 = parts[13]
            imp_sg = parts[14]
            imp_pl = parts[15]
            if not any(praesens) and not any(praeteritum) and not partizip2:
                continue
            data[word] = {
                "praesens": praesens,
                "praeteritum": praeteritum,
                "partizip2": partizip2 or None,
                "imperativ": [imp_sg, imp_pl] if imp_sg and imp_pl else None,
            }
    return data


def get_verb_forms(infinitiv, wiktionary=None, known_weak_bases=None):
    """Liefert alle Formen fuer 'infinitiv' (Kleinschreibung, z.B. 'gehen').
    Nutzt wiktionary_verben.tsv wo vorhanden (belegt, hohe Konfidenz), sonst classify_and_conjugate()
    als Fallback (Regelgenerator + kuratierte Praefix-Ausnahmetabelle, siehe konjugation.py).
    Gibt (formen_dict, quelle) zurueck, formen_dict ist None wenn nichts ermittelbar ist.
    'quelle' ist 'wiktionary' oder 'regel'."""
    global _WIKT_CACHE
    if wiktionary is None:
        if _WIKT_CACHE is None:
            _WIKT_CACHE = load_wiktionary_verbs()
        wiktionary = _WIKT_CACHE

    entry = wiktionary.get(infinitiv)
    if entry and (any(entry["praesens"]) or any(entry["praeteritum"]) or entry["partizip2"]):
        return entry, "wiktionary"

    if known_weak_bases is None:
        known_weak_bases = set()
    forms, is_strong = classify_and_conjugate(infinitiv, known_weak_bases)
    if forms is None:
        return None, None
    return {
        "praesens": forms["praesens"],
        "praeteritum": forms["praeteritum"],
        "partizip2": forms["partizip2"],
        "imperativ": forms["imperativ"],
    }, "regel"


if __name__ == "__main__":
    wikt = load_wiktionary_verbs()
    for w in ("gehen", "übertragen", "schmelzen", "erblühen"):
        forms, quelle = get_verb_forms(w, wiktionary=wikt)
        print(w, quelle, forms)
