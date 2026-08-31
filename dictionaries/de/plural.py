# -*- coding: utf-8 -*-
# Phase 4 (Wortfamilien-Projekt): Nomen-Pluralgenerator.
# Anders als bei Verb-Konjugation und Genitiv-Singular ist die Pluralklasse (-e/-er/-(e)n/-s/Nullplural)
# oft NICHT aus der Singularform ableitbar - besonders bei "nackten" Maskulina/Neutra ohne kennzeichnende
# Endung ist die Wahl zwischen -e/-er/Nullplural UND die Umlaut-Frage lexikalisch, nicht regelhaft
# (Tag->Tage, aber Sohn->Söhne - beide einsilbig, maskulin, kein Unterschied im Singular erkennbar).
# Nutzerentscheidung (2026-08-31): volle Regelabdeckung inkl. dieser unsicheren Faelle, mit kuratierter
# Ausnahmetabelle, niedrigere Gesamtgenauigkeit akzeptiert als beim Genitiv-Generator (~87%).
#
# Die Ausnahmetabellen unten sind eine ANFANGSABDECKUNG der haeufigsten Faelle, kein Anspruch auf
# Vollstaendigkeit - analog zur STAMMFORMEN-Tabelle bei Verben, gedacht zum Erweitern via Stichprobenrunden.

from deklination import _is_weak_masculine, _oblique_ending

# -in-Feminina (Berufsbezeichnungen etc.) verdoppeln das n vor -en: Lehrerin -> Lehrerinnen.
FEM_IN_SUFFIX = "in"

# Produktive feminine Ableitungssuffixe - nehmen praktisch ausnahmslos -en (keine Umlaut-Frage, keine
# Ausnahmen bekannt).
FEM_PRODUCTIVE_SUFFIXES = (
    "heit", "keit", "ung", "schaft", "tät", "ion", "ik", "ur", "enz", "anz", "age", "ade", "ie", "esse",
)

# Feminine einsilbige Nomen mit Umlaut+e-Plural (geschlossene, traditionelle Klasse).
FEM_UMLAUT_E = {
    "kuh", "wand", "stadt", "kraft", "nacht", "hand", "maus", "frucht", "braut", "faust",
    "laus", "luft", "macht", "wurst", "kunst", "brust", "gans", "haut", "angst", "axt",
    "bank", "sau", "schnur", "wurzel", "nuss",
}
# Feminine Nomen mit Nullplural+Umlaut (sehr kleine, geschlossene Klasse).
FEM_UMLAUT_NULL = {"mutter", "tochter"}

# "Nackte" Maskulina/Neutra, die -er nehmen (statt des haeufigeren -e), mit Umlaut wo der Vokal
# umlautfaehig ist (a/o/u/au). Ueberwiegend Neutra.
ER_PLURAL_UMLAUT = {
    "buch", "haus", "land", "glas", "blatt", "rad", "amt", "dach", "fach", "gut", "loch",
    "schild", "volk", "wort", "bad", "grab", "schloss", "mann", "wald", "rand", "gott",
    "irrtum", "reichtum", "mund", "dorf", "horn", "korn", "band",
}
# "Nackte" Neutra/Maskulina, die -er OHNE Umlaut nehmen (kein umlautfaehiger Vokal im Stamm).
ER_PLURAL_NO_UMLAUT = {
    "kind", "bild", "brett", "feld", "geld", "licht", "kleid", "lied", "glied", "geist",
    "leib", "ei", "bein",
}

# "Nackte" Maskulina, die -e MIT Umlaut nehmen.
E_PLURAL_UMLAUT_MASC = {
    "baum", "sohn", "ton", "zug", "kopf", "fuß", "satz", "platz", "fall", "stuhl", "fluss",
    "kuss", "hut", "ball", "gast", "arzt", "schwanz", "gruß", "schluss", "betrag", "zustand",
    "abstand", "anspruch", "ausdruck", "absatz", "anschluss", "bahnhof", "stock", "sturm",
    "zahn", "kranz", "schrank", "anzug", "ausflug", "ansatz", "wunsch", "fluch", "sack",
    "strauß", "schoss", "schuss", "grund", "traum", "raum", "brauch",
}

# Kleine Ausnahmeklasse "nackter" Neutra mit unregelmaessigem -en statt des erwartbaren -e/-er.
NEUT_EN_PLURAL = {"ohr", "bett", "hemd"}

# Maskulina auf -er/-el/-en (Nullplural-Klasse per Default), die abweichend TROTZDEM Umlaut nehmen.
NULL_PLURAL_UMLAUT_EXCEPTIONS = {
    "vater", "bruder", "garten", "mangel", "apfel", "boden", "mantel", "nagel", "ofen",
    "vogel", "faden", "schaden", "graben", "hafen", "kasten", "laden", "magen", "sattel",
    "schnabel",
}

# Substantive auf Vokal (bzw. bekannte Fremdwortmuster), die den Lehnwort-Plural -s nehmen.
S_PLURAL_VOWEL_ENDINGS = ("a", "i", "o", "u", "y")
# Native Diphthonge, die zufaellig auf denselben Buchstaben enden wie die Lehnwort-Endungen oben (au/eu
# enden auf "u", ei endet auf "i") - MUESSEN ausgeschlossen werden, sonst "Frau"->"Fraus"/"Ei"->"Eis"
# statt der korrekten nativen Formen "Frauen"/"Eier".
NATIVE_DIPHTHONGS = ("au", "ei", "eu", "äu")


def _matches_root(lw, roots):
    """Prueft, ob 'lw' auf eine der kuratierten Wurzeln endet - nicht nur exakte Gleichheit. Deutsche
    Nomen sind ueberwiegend Komposita ("Widerspruch" = wider+Spruch, "Nebenfluss" = neben+Fluss) und
    erben das Pluralverhalten (inkl. Umlaut) vom letzten Bestandteil. _apply_umlaut() sucht ohnehin vom
    Wortende her nach dem letzten umlautfaehigen Vokal, trifft also automatisch den richtigen innerhalb
    der Wurzel, nicht faelschlich einen fruehen Vokal im vorangestellten Kompositumsteil."""
    return any(lw.endswith(root) for root in roots)


def _apply_umlaut(word):
    """Ersetzt den letzten umlautfaehigen Vokal (a/o/u, bzw. den Digraph 'au') im Wort durch seine
    Umlautform. Prueft 'au' vor Einzelvokalen, da es sonst faelschlich als 'a' erkannt wuerde."""
    lw = word.lower()
    for i in range(len(lw) - 1, -1, -1):
        if lw[i : i + 2] == "au":
            repl = "Äu" if word[i].isupper() else "äu"
            return word[:i] + repl + word[i + 2 :]
    for i in range(len(lw) - 1, -1, -1):
        if lw[i] in "aou":
            repl = {"a": "ä", "o": "ö", "u": "ü"}[lw[i]]
            if word[i].isupper():
                repl = repl.upper()
            return word[:i] + repl + word[i + 1 :]
    return word


def plural(noun, genus):
    """noun: Nominativ-Singular-Form (dict.tsv-Schreibweise). genus: 'f'|'m'|'n'.
    Gibt die Nominativ-Plural-Form zurueck (Nom=Akk=Gen Plural sind im Deutschen immer identisch;
    nur der Dativ Plural bekommt zusaetzlich ein -n, siehe dativ_plural())."""
    lw = noun.lower()

    # Schwache Maskulina: Plural = dieselbe -n/-en-Form wie der Singular-Oblique (Kunde/Kunden,
    # Journalist/Journalisten) - Zusammenfall ist im Deutschen so vorgesehen, kein Sonderfall.
    if genus == "m" and _is_weak_masculine(lw):
        return _weak_plural(noun, lw)

    if lw.endswith("chen") or lw.endswith("lein"):
        return noun  # Diminutive: immer Nullplural.

    if genus == "f" and lw.endswith(FEM_IN_SUFFIX) and len(lw) > 3:
        return noun + "nen"

    if genus == "n" and lw.endswith("um") and len(lw) > 3:
        return noun[:-2] + "en"

    if lw.endswith("nis"):
        return noun + "se"

    if lw and lw[-1] in S_PLURAL_VOWEL_ENDINGS and lw[-2:] not in NATIVE_DIPHTHONGS:
        return noun + "s"

    if genus == "f" and lw.endswith(FEM_PRODUCTIVE_SUFFIXES):
        # "ie"/"age"/"ade" enden bereits auf -e, brauchen also nur +n (Familie->Familien), nicht +en
        # (waere Familieen) UND nicht unveraendert (waere Familie) - frueherer Fehler hier liess das Wort
        # bei -e-Endung komplett unveraendert statt +n anzuhaengen.
        return noun + "n" if lw.endswith("e") else noun + "en"

    # Feminine Nullplural-mit-Umlaut-Ausnahmen (Mutter/Tochter) muessen VOR der generischen -er/-el/-en-
    # Nullplural-Regel geprueft werden, sonst greift dort faelschlich der umlautlose Default (Mutter->Mutter
    # statt Muetter), da "mutter" zufaellig auch auf "-er" endet.
    if genus == "f" and _matches_root(lw, FEM_UMLAUT_NULL):
        return _apply_umlaut(noun)

    # "-iel" ist meist kein echtes derivatives -el-Suffix (Beispiel = bei+Spiel, Ziel, Spiel selbst),
    # sondern zufaelliges Wortende - diese Woerter folgen NICHT dem Nullplural-Muster von Onkel/Mangel/
    # Segel (echte -el-Woerter), sondern nehmen regulaer -e (Beispiele, Ziele, Spiele).
    if lw.endswith(("er", "el", "en")) and not lw.endswith("iel"):
        if _matches_root(lw, NULL_PLURAL_UMLAUT_EXCEPTIONS):
            return _apply_umlaut(noun)
        return noun  # Nullplural-Default fuer diese Endungsklasse.

    if genus == "f":
        if _matches_root(lw, FEM_UMLAUT_E):
            return _apply_umlaut(noun) + "e"
        if lw.endswith("e"):
            return noun + "n"
        return noun + "en"

    # Nicht-feminine Nomen auf unbetontes -e (selten, v.a. Neutra wie "Auge", "Ende", "Interesse") folgen
    # demselben Muster wie feminine -e-Nomen: nur +n, nicht der generische +e-Default (der sonst faelschlich
    # ein doppeltes -e ergaebe, "Auge"->"Augee").
    if lw.endswith("e"):
        return noun + "n"

    # "Nackte" Maskulina/Neutra: Standardfall -e (mit/ohne Umlaut je nach Ausnahmetabelle), Sonderfall -er,
    # kleine Ausnahmeklasse mit unregelmaessigem -en (Ohr/Bett/Hemd).
    if _matches_root(lw, ER_PLURAL_UMLAUT):
        return _apply_umlaut(noun) + "er"
    if _matches_root(lw, ER_PLURAL_NO_UMLAUT):
        return noun + "er"
    if _matches_root(lw, E_PLURAL_UMLAUT_MASC):
        return _apply_umlaut(noun) + "e"
    if _matches_root(lw, NEUT_EN_PLURAL):
        return noun + "en"
    return noun + "e"


def _weak_plural(noun, lw):
    # "Herr" ist wie im Singular-Oblique unregelmaessig, aber ANDERS: Plural ist "Herren" (nicht "Herrn"
    # wie im Singular-Oblique - die beiden Formen fallen bei diesem einen Wort ausnahmsweise NICHT
    # zusammen, waehrend sie es bei allen anderen schwachen Maskulina tun).
    if lw.endswith("herr"):
        return noun + "en"
    return noun + _oblique_ending(lw)


def dativ_plural(nom_plural):
    """Dativ Plural bekommt im modernen Deutsch ein zusaetzliches -n, AUSSER die Form endet bereits auf
    -n oder -s (Frauen bleibt Frauen, Autos bleibt Autos)."""
    if nom_plural.endswith(("n", "s")):
        return nom_plural
    return nom_plural + "n"


if __name__ == "__main__":
    tests = [
        ("Frau", "f"), ("Tag", "m"), ("Sohn", "m"), ("Kind", "n"), ("Buch", "n"), ("Haus", "n"),
        ("Auto", "n"), ("Kunde", "m"), ("Herr", "m"), ("Journalist", "m"), ("Lehrer", "m"),
        ("Vater", "m"), ("Mutter", "f"), ("Mädchen", "n"), ("Freiheit", "f"), ("Lehrerin", "f"),
        ("Museum", "n"), ("Verständnis", "n"), ("Straße", "f"), ("Baum", "m"), ("Mann", "m"),
    ]
    for noun, genus in tests:
        p = plural(noun, genus)
        print(f"{noun:12s} [{genus}] Plural={p:15s} Dativ-Plural={dativ_plural(p)}")
