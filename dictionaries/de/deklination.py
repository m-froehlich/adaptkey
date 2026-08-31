# -*- coding: utf-8 -*-
# Phase 4 (Wortfamilien-Projekt): Nomen-Deklinationsgenerator.
# Erzeugt Nominativ/Genitiv/Dativ/Akkusativ Singular (und spaeter Plural) fuer ein Substantiv, gegeben sein
# per Genus-Heuristik (genus.py) geschaetztes Genus.

# Kuratierte Ausnahmeliste "schwache Maskulina" (n-Deklination): diese Woerter bekommen in ALLEN drei
# obliquen Faellen (Gen/Dat/Akk Singular) dieselbe -n/-en-Endung, nicht nur im Genitiv wie bei starken
# Maskulina. Validiert im urspruenglichen Genitiv-Generator-Test (87% Exact-Match gegen echte Korpusdaten).
WEAK_MASCULINE_EXCEPTIONS = {
    "mensch", "bär", "herr", "prinz", "fürst", "graf", "christ", "bauer", "nachbar",
    "held", "hase", "spatz",
}

# Lange Fremdwoerter mit diesen Endungen sind fast immer schwache Maskulina, ABER NUR wenn sie eine Person
# bezeichnen (Journalist, Präsident, Doktorand, Fotograf, Biologe, Ökonom, Philosoph, Demokrat,
# Dramaturg, ...) - dieselben Endungen (v.a. "-ent"/"-ment") kommen auch bei starken, unbelebten
# Maskulina/Neutra vor (Prozent, Kontinent, Element, Instrument, Dokument, Argument, ...), die faelschlich
# als schwach erkannt wuerden. Da "bezeichnet eine Person" nicht aus der Schreibung ableitbar ist, werden
# die bekanntesten unbelebten Faelle als Ausnahme von der Ausnahme kuratiert.
WEAK_PRODUCTIVE_SUFFIXES = ("ist", "ent", "and", "graph", "loge", "nom", "soph", "krat", "urg")
WEAK_PRODUCTIVE_MIN_LENGTH = 7
WEAK_PRODUCTIVE_NONPERSON_EXCEPTIONS = {
    "prozent", "kontinent", "element", "instrument", "dokument", "argument", "moment",
    "talent", "experiment", "fundament", "testament", "parlament", "department",
    "segment", "fragment", "kompliment", "sediment", "temperament",
}


def _is_weak_masculine(noun_lower):
    # Endungs- statt Exaktvergleich: deutsche Nomen sind ueberwiegend Komposita und erben die Deklination
    # vom letzten Bestandteil (Kronprinz endet auf "prinz", Bauelement auf "element" - beide sollen sich
    # wie ihr jeweiliges Grundwort verhalten, nicht wie ein unbekanntes Wort).
    if any(noun_lower.endswith(w) for w in WEAK_MASCULINE_EXCEPTIONS):
        return True
    if any(noun_lower.endswith(w) for w in WEAK_PRODUCTIVE_NONPERSON_EXCEPTIONS):
        return False
    if noun_lower.endswith("e") and len(noun_lower) > 1:
        # Unbetontes -e am Wortende ist die produktivste schwache Maskulinum-Endung (Kunde, Löwe, Junge, ...).
        return True
    if len(noun_lower) >= WEAK_PRODUCTIVE_MIN_LENGTH and noun_lower.endswith(WEAK_PRODUCTIVE_SUFFIXES):
        return True
    return False


def _oblique_ending(noun_lower):
    """Endung fuer die schwache Deklination (Gen=Dat=Akk Singular): -n nach -e, sonst -en."""
    return "n" if noun_lower.endswith("e") else "en"


def _genitiv_stark(noun_lower):
    """Genitiv Singular fuer starke Maskulina und Neutra (validiert, 87% Exact-Match, siehe Historie).
    Reihenfolge der Pruefungen ist bedeutsam - spezifischere Muster vor generischen."""
    if noun_lower.endswith("nis"):
        return noun_lower + "ses"
    if noun_lower.endswith(("e", "el", "er", "en", "um")):
        return noun_lower + "s"
    if noun_lower and noun_lower[-1] in "aiouy":
        return noun_lower + "s"
    if noun_lower.endswith(("ss", "ß", "z", "x", "sch", "tz")):
        return noun_lower + "es"
    return noun_lower + "es"


def decline_singular(noun, genus):
    """noun: Nominativ-Singular-Form (dict.tsv-Schreibweise, z.B. 'Frau'). genus: 'f'|'m'|'n'.
    Gibt dict mit 'nominativ'/'genitiv'/'dativ'/'akkusativ' zurueck (jeweils in Originalschreibweise
    inkl. Grossbuchstabe am Anfang, wie im Woerterbuch ueblich)."""
    lw = noun.lower()
    cap = noun[0].isupper()

    def restore_case(s):
        return s[0].upper() + s[1:] if cap else s

    if genus == "f":
        # Feminina flektieren im Singular ueberhaupt nicht - Nom=Gen=Dat=Akk sind immer identisch.
        return {"nominativ": noun, "genitiv": noun, "dativ": noun, "akkusativ": noun}

    if genus == "m" and _is_weak_masculine(lw):
        oblique_form = lw + "n" if lw.endswith("herr") else lw + _oblique_ending(lw)
        oblique = restore_case(oblique_form)
        return {"nominativ": noun, "genitiv": oblique, "dativ": oblique, "akkusativ": oblique}

    # Starke Maskulina und Neutra: Nom=Akk=Dat (modernes Standarddeutsch, das archaische Dativ-e "im Hause"
    # wird bewusst nicht erzeugt - optional/veraltet, nicht die Standardform), nur Genitiv aendert sich.
    genitiv = restore_case(_genitiv_stark(lw))
    return {"nominativ": noun, "genitiv": genitiv, "dativ": noun, "akkusativ": noun}


if __name__ == "__main__":
    tests = [("Frau", "f"), ("Mann", "m"), ("Kunde", "m"), ("Herr", "m"), ("Journalist", "m"),
             ("Haus", "n"), ("Auto", "n"), ("Verständnis", "n"), ("Staat", "m")]
    for noun, genus in tests:
        print(noun, genus, decline_singular(noun, genus))
