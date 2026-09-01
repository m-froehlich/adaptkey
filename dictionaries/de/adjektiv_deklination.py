# -*- coding: utf-8 -*-
# Phase 2 (Adjektiv-Projekt, siehe AdaptKey-Plan-Adjektive.md): Regel-Fallback fuer Adjektiv-Lemmata OHNE
# jede Wiktionary-Formenangabe (weder volle Flexionstabelle noch blosse Grad-Staemme). Wird nur fuer eine
# kleine Restmenge gebraucht - siehe Plan-Dokument fuer die tatsaechliche Groessenordnung.
#
# Alle Regeln unten sind gegen echte, aus wiktionary_adjektive.tsv extrahierte Belegformen verifiziert
# (schoen/gut/hoch/nah/dunkel/edel/teuer/bunt/frisch/rund/laut/klug), nicht aus dem Gedaechtnis uebernommen:
# - Die Fall/Numerus/Deklinationstyp/Genus-Endungstabelle ist ueber alle drei Grade identisch (aus "schoen"
#   verifiziert: Positiv-, Komparativ- und Superlativ-Endungen sind byteidentisch).
# - e-Elision (Stamm endet auf unbetontes -el/-er): gilt fuer den Positiv-Stamm selbst VOR jeder vokalisch
#   beginnenden Endung ("dunkel"+"e" -> "dunkle", nicht "dunkele") UND fuer die Komparativ-Stamm-Bildung
#   ("dunkel"+"er" -> "dunkler"). Gilt AUSDRUECKLICH NICHT vor der Superlativ-Endung "-st"/"-est" - belegt an
#   "dunkelste"/"edelste" (nicht "dunklste"/"edlste"), obwohl "edel"+"er" im Komparativ sehr wohl zu "edler"
#   elidiert.
# - Sibilant-/Dental-Erweiterung beim Superlativ (-est statt -st): nach s/sz/z/x/sch/d/t - belegt an
#   "heiss"->"heisseste", "kurz"->"kuerzeste", "bunt"->"bunteste", "rund"->"rundeste", "frisch"->
#   "frischeste" (Wiktionary listet dort zusaetzlich auch die kuerzere Nebenform "frischste" - beide gueltig,
#   dieses Modul erzeugt bewusst nur die -est-Form). "gross" ist eine bestaetigte ECHTE Ausnahme
#   ("groesste", nicht "groessteste") - betrifft aber nur die geschlossene Umlaut-Klasse haeufiger Woerter,
#   die ohnehin ueber die volle Wiktionary-Flexionstabelle abgedeckt sind, nie ueber diesen Fallback.
#
# Bewusst NICHT abgedeckt (gleiche Scoping-Entscheidung wie bei den starken Verben, §322): die geschlossene
# Umlaut-Klasse (alt/jung/arm/kalt/warm/stark/scharf/lang/kurz/hart/klug/gross/hoch/...) wird nur uebernommen,
# wenn Wiktionary sie belegt - dieses Modul raet niemals einen Umlaut.

ENDUNGEN = {
    ("nominative", "sg", "strong", "m"): "er", ("nominative", "sg", "strong", "f"): "e",
    ("nominative", "sg", "strong", "n"): "es", ("nominative", "pl", "strong", ""): "e",
    ("genitive", "sg", "strong", "m"): "en", ("genitive", "sg", "strong", "f"): "er",
    ("genitive", "sg", "strong", "n"): "en", ("genitive", "pl", "strong", ""): "er",
    ("dative", "sg", "strong", "m"): "em", ("dative", "sg", "strong", "f"): "er",
    ("dative", "sg", "strong", "n"): "em", ("dative", "pl", "strong", ""): "en",
    ("accusative", "sg", "strong", "m"): "en", ("accusative", "sg", "strong", "f"): "e",
    ("accusative", "sg", "strong", "n"): "es", ("accusative", "pl", "strong", ""): "e",

    ("nominative", "sg", "weak", "m"): "e", ("nominative", "sg", "weak", "f"): "e",
    ("nominative", "sg", "weak", "n"): "e", ("nominative", "pl", "weak", ""): "en",
    ("genitive", "sg", "weak", "m"): "en", ("genitive", "sg", "weak", "f"): "en",
    ("genitive", "sg", "weak", "n"): "en", ("genitive", "pl", "weak", ""): "en",
    ("dative", "sg", "weak", "m"): "en", ("dative", "sg", "weak", "f"): "en",
    ("dative", "sg", "weak", "n"): "en", ("dative", "pl", "weak", ""): "en",
    ("accusative", "sg", "weak", "m"): "en", ("accusative", "sg", "weak", "f"): "e",
    ("accusative", "sg", "weak", "n"): "e", ("accusative", "pl", "weak", ""): "en",

    ("nominative", "sg", "mixed", "m"): "er", ("nominative", "sg", "mixed", "f"): "e",
    ("nominative", "sg", "mixed", "n"): "es", ("nominative", "pl", "mixed", ""): "en",
    ("genitive", "sg", "mixed", "m"): "en", ("genitive", "sg", "mixed", "f"): "en",
    ("genitive", "sg", "mixed", "n"): "en", ("genitive", "pl", "mixed", ""): "en",
    ("dative", "sg", "mixed", "m"): "en", ("dative", "sg", "mixed", "f"): "en",
    ("dative", "sg", "mixed", "n"): "en", ("dative", "pl", "mixed", ""): "en",
    ("accusative", "sg", "mixed", "m"): "en", ("accusative", "sg", "mixed", "f"): "e",
    ("accusative", "sg", "mixed", "n"): "es", ("accusative", "pl", "mixed", ""): "en",
}

SUPERLATIV_EST_TRIGGER = ("s", "ß", "z", "x", "d", "t")


def _elide(stamm):
    """Zieht das unbetonte 'e' vor auslautendem l/r ab, falls vorhanden - sonst unveraendert."""
    if len(stamm) >= 3 and stamm[-2:] in ("el", "er"):
        return stamm[:-2] + stamm[-1]
    return stamm


def komparativ_stamm(positiv):
    return _elide(positiv) + "er"


def superlativ_stamm(positiv):
    if positiv.endswith("sch") or positiv.endswith(SUPERLATIV_EST_TRIGGER):
        return positiv + "est"
    return positiv + "st"


def dekliniere(stamm, ist_positiv_stamm):
    """Liefert alle 32 deklinierten Formen (Fall x Numerus x Deklinationstyp x Genus) fuer einen
    Grad-Stamm als dict {(fall, numerus, deklinationstyp, genus): form}. [ist_positiv_stamm] steuert, ob vor
    einer vokalisch beginnenden Endung noch elidiert werden darf (nur beim Positiv-Stamm selbst - ein
    bereits gebildeter Komparativ-/Superlativ-Stamm braucht das nicht mehr, siehe Modul-Kommentar)."""
    result = {}
    for (fall, numerus, dekl, genus), endung in ENDUNGEN.items():
        effektiver_stamm = _elide(stamm) if ist_positiv_stamm else stamm
        result[(fall, numerus, dekl, genus)] = effektiver_stamm + endung
    return result


def alle_formen(positiv):
    """Liefert alle drei Grad-Staemme plus ihre komplette Deklination fuer ein regelmaessiges Adjektiv, das
    in Wiktionary weder Flexionstabelle noch Grad-Staemme hat. Rueckgabe: dict
    {"positiv": {...}, "komparativ": {...}, "superlativ": {...}}, jeweils wie [dekliniere]."""
    komp = komparativ_stamm(positiv)
    sup = superlativ_stamm(positiv)
    return {
        "positiv": dekliniere(positiv, ist_positiv_stamm=True),
        "komparativ": dekliniere(komp, ist_positiv_stamm=False),
        "superlativ": dekliniere(sup, ist_positiv_stamm=False),
    }
