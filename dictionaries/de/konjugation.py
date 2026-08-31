# -*- coding: utf-8 -*-
# Phase 2 (Wortfamilien-Projekt): Verb-Konjugationsgenerator.
# Erzeugt Praesens, Praeteritum, Partizip II und Imperativ fuer ein Verb (Basis oder praefigiert),
# unter Nutzung der Stammformentabelle fuer starke/unregelmaessige Basisverben.

from stammformen import (
    STAMMFORMEN_BY_INFINITIV,
    AUX_MODAL_FORMS,
    WEAK_IRREGULAR_PARTIZIP,
    WEAK_FALSE_PREFIX_EXCEPTIONS,
    WEAK_NATIVE_IEREN_EXCEPTIONS,
)
from praefix_overrides import PRAEFIX_VERB_OVERRIDES

# A-05-Praefixliste (spec.md §7): untrennbare Verbpraefixe - kein "ge-"-Infix im Partizip II,
# und im Praesens/Praeteritum bleibt das Praefix am Verb (keine Satzklammer-Trennung).
INSEPARABLE_PREFIXES = ("ver", "zer", "ent", "emp", "be", "ge", "miss", "er", "un", "ur", "wider")

# Bekannte trennbare Praefixe, produktiv genutzt fuer die Komposition (nicht erschoepfend,
# aber deckt die haeufigsten Faelle ab). "ge-" Infix steht NACH dem Praefix: aufstehen -> aufgestanden.
# durch/um/über/unter/voll/hinter/wieder sind bewusst NICHT enthalten - spec.md A-05 haelt fest, dass diese
# Praefixe je nach Verb sowohl trennbar (umfahren = anfahren, "faehrt um") als auch untrennbar (umfahren =
# im Bogen ausweichen, "umfaehrt") sein koennen; ohne Pro-Verb-Wissen waere jede mechanische Komposition
# fuer diese Gruppe ein Muenzwurf (bestaetigt am Testfall "uebertragen": Partizip haette faelschlich
# "uebergetragen" statt "uebertragen" ergeben). Bleibt als bekannte Luecke, bis eine kuratierte Ausnahmeliste
# pro betroffenem Verb existiert - kein blindes Raten.
SEPARABLE_PREFIXES = (
    "auf", "aus", "ein", "an", "ab", "mit", "nach", "vor", "zu", "zurück", "weg",
    "fort", "her", "hin", "los",
    "bei", "empor", "fest", "frei", "gegen", "heim", "nieder", "statt", "teil", "weiter", "zusammen",
    "hoch", "hervor", "einher", "entgegen", "zurecht", "voraus", "herunter", "herauf", "heraus",
    "hinein", "herein", "hinaus", "hinunter", "hinauf", "davon", "dahin", "vorbei", "vorüber", "voran",
    "fern", "auseinander", "durcheinander", "wahr", "heran", "zuvor", "aufeinander",
    "hintereinander", "übereinander", "untereinander", "nebenher", "vorher",
)

# Praefixe mit uneindeutiger Trennbarkeit (durch/um/ueber/unter/voll/hinter/wieder) - bewusst weder in
# INSEPARABLE_PREFIXES noch SEPARABLE_PREFIXES, damit strip_prefix() sie erst gar nicht erkennt, statt sie
# falsch zu klassifizieren.
AMBIGUOUS_PREFIXES = ("durch", "um", "über", "unter", "voll", "hinter", "wieder")

# Fuer die blinde Praefix-Erkennung in conjugate_weak() (ohne Basiswort-Validierung, siehe dort) wird "un"
# bewusst ausgeschlossen: "unter" beginnt selbst mit "un", was sonst z.B. "unternehmen" faelschlich als
# "un"-praefigiert erkennen wuerde, obwohl das eigentliche (mehrdeutige) Praefix "unter" ist.
WEAK_PREFIX_CHECK = tuple(p for p in INSEPARABLE_PREFIXES if p != "un")


def strip_prefix(word, known_bases):
    """Zerlegt ein Wort in (praefix, basis) falls die Basis in known_bases (Infinitivform) liegt.
    Bevorzugt die laengste erkennbare Basis (vermeidet Fehltrennungen wie 'ver'+'stehen' vs 'v'+'erstehen')."""
    lw = word.lower()
    for base in sorted(known_bases, key=len, reverse=True):
        if lw == base:
            return "", base
        if lw.endswith(base) and len(lw) > len(base):
            prefix = lw[: -len(base)]
            if prefix in INSEPARABLE_PREFIXES or prefix in SEPARABLE_PREFIXES:
                return prefix, base
    return None, None


def is_inseparable(prefix):
    return prefix in INSEPARABLE_PREFIXES


def _needs_epenthesis(stem):
    """Stamm endet auf Dental (d/t) oder bestimmten Konsonantenclustern -> epenthetisches -e-
    vor -st/-t noetig (findest/findet, nicht findst/findt; ordnest/ordnet, nicht ordnst/ordnt)."""
    return stem.endswith(("d", "t", "chn", "ffn", "dm", "gn", "tm", "dn", "kn"))


def _ends_in_sibilant(stem):
    """Stamm endet auf s/ss/ß/z/x -> 2. Person Singular Praesens faellt mit der 3. Person zusammen
    (du isst = er isst, du laesst = er laesst), da eine zusaetzliche -st-Endung eine im Deutschen
    vermiedene Dreifach-Sibilant-Folge erzeugen wuerde (nicht "issst", "laessst")."""
    return stem.endswith(("s", "ß", "z", "x"))


def conjugate_strong(infinitiv, prefix="", inseparable=None):
    """Erzeugt alle Formen fuer ein (ggf. praefigiertes) starkes/unregelmaessiges Verb.
    infinitiv muss ein Basisverb aus STAMMFORMEN sein."""
    if infinitiv in AUX_MODAL_FORMS and not prefix:
        # sein/haben/werden/Modalverben/wissen: vollstaendig hartkodiert, siehe stammformen.py.
        base = AUX_MODAL_FORMS[infinitiv]
        forms = {
            "praesens": list(base["praesens"]),
            "praeteritum": list(base["praeteritum"]),
            "partizip2": base["partizip2"],
            "imperativ": list(base["imperativ"]) if base["imperativ"] else None,
        }
        return forms

    entry = STAMMFORMEN_BY_INFINITIV.get(infinitiv)
    if entry is None:
        return None
    _, p3, pret, part, gemischt = entry

    stem_e = infinitiv[:-2]  # z.B. "gehen" -> "geh"
    pres_epenthesis = _needs_epenthesis(stem_e)
    sibilant_stem = _ends_in_sibilant(stem_e)

    # Praesens: nur 2./3. Person Singular koennen vom Stamm abweichen (Ablaut); Rest regelmaessig vom Infinitivstamm.
    if p3:
        # p3 ist bereits die volle 3.-Person-Form des Basisverbs, z.B. "gibt" fuer "geben".
        p3_form = p3
    else:
        p3_form = stem_e + ("et" if pres_epenthesis else "t")

    if sibilant_stem:
        # du isst/laesst/wächst = er isst/laesst/wächst (identische Form, keine zusaetzliche -st-Endung).
        p2_form = p3_form
    elif p3:
        if stem_e.endswith(("t", "d")) and p3[-1] == stem_e[-1]:
            # Stammauslaut ist selbst schon der Dental und bleibt in p3 unveraendert erhalten
            # (halten -> haelt, gelten -> gilt, raten -> raet) - das -t in p3 ist Teil des Stamms,
            # keine separate Endung, also NICHT vor -st entfernen (haeltst, nicht haelst).
            p2_form = p3 + "st"
        else:
            # p3 endet auf einen ANDEREN Konsonanten als der Stamm (laden -> lädt: Stamm endet auf
            # "d", p3 auf "t") - das -t in p3 ist hier die eigentliche Endung und muss vor -st entfernt
            # werden (laedst, nicht laedtst).
            p2_form = p3[:-1] + "st" if p3.endswith("t") else p3 + "st"
    else:
        p2_form = stem_e + ("est" if pres_epenthesis else "st")
    p1_form = stem_e + "e"
    p4_form = infinitiv  # wir
    p5_form = stem_e + ("et" if pres_epenthesis else "t")  # ihr
    p6_form = infinitiv  # sie

    pret_stem = pret
    pret_p1 = pret_stem
    pret_p3 = pret_stem
    pret_epenthesis = _needs_epenthesis(pret_stem)
    # Modernes Standarddeutsch bevorzugt in der 2. Pers. Sg. Praeteritum durchgehend die epenthesenlose
    # Form (bandst/standst/littst/rissst->risst, NICHT bandest/standest/littest/rissest) - die Formen mit
    # eingeschobenem -e- sind laut Referenzkonjugation (verbformen.de) als "Verwendung veraltet" markiert,
    # auch bei Dental-Auslaut. Bei Sibilant-Auslaut (s/ss/ß/z/x) faellt die 2. Pers. Sg. dabei zusaetzlich
    # mit der 2. Pers. Pl. zusammen (beide nur Stamm+"t", z.B. "aßt" fuer du UND ihr) - dieselbe Haplologie
    # wie im Praesens (du isst = er isst), hier aber 2.Sg.=2.Pl. statt 2.Sg.=3.Sg.
    if pret_stem.endswith("e"):
        pret_p2 = pret_stem + "st"
        pret_p4 = pret_stem + "n"
        pret_p5 = pret_stem + "t"
        pret_p6 = pret_stem + "n"
    elif _ends_in_sibilant(pret_stem):
        pret_p2 = pret_stem + "t"
        pret_p4 = pret_stem + "en"
        pret_p5 = pret_stem + "t"
        pret_p6 = pret_stem + "en"
    else:
        pret_p2 = pret_stem + "st"
        pret_p4 = pret_stem + "en"
        pret_p5 = pret_stem + ("et" if pret_epenthesis else "t")
        pret_p6 = pret_stem + "en"

    partizip = part

    # Imperativ: du-Form = Praesensstamm, ggf. mit e/i(e)-Ablaut (aber NICHT a->ä/au->äu/o->ö-Ablaut,
    # das ist ein Praesens-only-Phaenomen). Pruefung ueber das gesamte Wort, nicht nur den Anfang -
    # der Ablaut sitzt im Stamm, nicht zwingend am Wortanfang bei praefigierten Verben.
    if p3 and not any(v in p3 for v in ("ä", "ö")):
        if stem_e.endswith(("t", "d")):
            imp_du = p3  # Stammauslaut-Dental ist Teil des Stamms, nicht abtrennen (gilt, nicht gil)
        else:
            imp_du = p3[:-1] if p3.endswith("t") else p3
    else:
        imp_du = stem_e + "e" if pres_epenthesis else stem_e
    imp_ihr = p5_form

    forms = {
        "praesens": [p1_form, p2_form, p3_form, p4_form, p5_form, p6_form],
        "praeteritum": [pret_p1, pret_p2, pret_p3, pret_p4, pret_p5, pret_p6],
        "partizip2": partizip,
        "imperativ": [imp_du, imp_ihr],
    }

    if prefix:
        forms = _apply_prefix(forms, prefix, inseparable=inseparable)
    return forms


def conjugate_weak(infinitiv, prefix="", inseparable=None):
    """Erzeugt alle Formen fuer ein regelmaessiges (schwaches) Verb - kein Ablaut, rein produktives Muster.
    Im Gegensatz zu conjugate_strong() wird keine Stammformentabelle gebraucht: jeder Infinitiv kann direkt
    konjugiert werden. 'prefix' muss der Aufrufer bereits validiert haben (z.B. via strip_prefix() gegen
    echte, im Woerterbuch belegte Basisverben) - diese Funktion raet nicht selbst, ob ein Wortanfang ein
    echtes Praefix ist (Gegenbeispiel: "erben" beginnt zufaellig mit "er", aber "ben" ist kein Verb - eine
    hartkodierte Praefixerkennung wuerde hier faelschlich "erbt" statt korrekt "geerbt" erzeugen)."""
    if not infinitiv.endswith("n"):
        return None
    is_eln = infinitiv.endswith("eln")
    is_ern = infinitiv.endswith("ern") and not is_eln
    if is_eln or is_ern:
        stem = infinitiv[:-1]  # nur "n" entfernen: laecheln -> laechel, wandern -> wander
    elif infinitiv.endswith("en"):
        stem = infinitiv[:-2]  # "en" entfernen: spielen -> spiel
    else:
        return None

    epenthesis = _needs_epenthesis(stem)
    sibilant = _ends_in_sibilant(stem)

    # -eln-Verben tilgen das Stamm-e vor dem -l in der 1. Pers. Sg. und im Imperativ (ich laechle, nicht
    # ich laechele) - produktive moderne Standardform; -ern-Verben (wandern) tilgen NICHT (ich wandere).
    p1_stem = stem[:-2] + "le" if is_eln else stem + "e"

    p3_form = stem + ("et" if epenthesis else "t")
    # Sibilant-Kollaps wie bei starken Verben: du reist = er reist, nicht "reisst".
    p2_form = p3_form if sibilant else stem + ("est" if epenthesis else "st")
    p4_form = infinitiv
    p5_form = p3_form
    p6_form = infinitiv

    # Praeteritum-Tempusmarker "-te-"/"-ete-" ist hier PFLICHT (nicht optional wie die Ablaut-Epenthese bei
    # starken Verben) - "wartte" ist unaussprechlich/falsch, nur "wartete" ist korrekt.
    pret_stem = stem + ("ete" if epenthesis else "te")
    pret_p1 = pret_stem
    pret_p2 = pret_stem + "st"
    pret_p3 = pret_stem
    pret_p4 = pret_stem + "n"
    pret_p5 = pret_stem + "t"
    pret_p6 = pret_stem + "n"

    # -ieren-Verben (unbetonte Erstsilbe, meist Fremd-/Lehnwoerter) bekommen nie ein ge-Praefix im Partizip
    # II: "studiert", nicht "gestudiert".
    # Beginnt der Infinitiv SELBST mit einem unbetonten Praefix (be-/ge-/ent-/emp-/er-/ver-/zer-/miss-/...),
    # entfaellt "ge-" immer - unabhaengig vom uebergebenen 'prefix'-Parameter, denn diese Regel ist rein
    # phonologisch (unbetonte Erstsilbe) und gilt unabhaengig davon, ob die Basis compositionell als
    # "wirkliches" eigenstaendiges Wort erkannt wurde. Wichtig fuer doppelt praefigierte Faelle wie
    # "zusammengehoeren" (Basis "gehoeren" beginnt selbst mit "ge-") - ohne diese Prüfung wuerde die Basis
    # faelschlich "gegehört" liefern und nach Praefix-Komposition "zusammengegehört" statt "zusammengehört".
    if infinitiv in WEAK_IRREGULAR_PARTIZIP and not prefix:
        partizip = WEAK_IRREGULAR_PARTIZIP[infinitiv]
    elif infinitiv.endswith("ieren") and infinitiv not in WEAK_NATIVE_IEREN_EXCEPTIONS:
        partizip = stem + "t"
    elif infinitiv.startswith(WEAK_PREFIX_CHECK) and infinitiv not in WEAK_FALSE_PREFIX_EXCEPTIONS:
        partizip = stem + ("et" if epenthesis else "t")
    else:
        partizip = "ge" + stem + ("et" if epenthesis else "t")

    # -eln-Verben: elidierte Form bevorzugt (handle!, laechle! - Wiktionary listet dies vor der
    # unelidierten Variante). -ern-Verben: NICHT elidierte Form bevorzugt (wandere!, feiere! - hier listet
    # Wiktionary die -e-Form zuerst, umgekehrt zu -eln).
    if is_eln:
        imp_du = p1_stem
    elif is_ern:
        imp_du = stem + "e"
    else:
        imp_du = stem + "e" if epenthesis else stem
    imp_ihr = p5_form

    forms = {
        "praesens": [p1_stem, p2_form, p3_form, p4_form, p5_form, p6_form],
        "praeteritum": [pret_p1, pret_p2, pret_p3, pret_p4, pret_p5, pret_p6],
        "partizip2": partizip,
        "imperativ": [imp_du, imp_ihr],
    }
    if prefix:
        forms = _apply_prefix(forms, prefix, inseparable=inseparable)
    return forms


def _apply_prefix(forms, prefix, inseparable=None):
    """inseparable=None laesst is_inseparable(prefix) entscheiden (Standardfall: Praefix stammt aus
    INSEPARABLE_PREFIXES/SEPARABLE_PREFIXES). Fuer die mehrdeutigen Praefixe (durch/um/ueber/unter/voll/
    hinter/wieder) uebergibt der Aufrufer die Entscheidung explizit aus PRAEFIX_VERB_OVERRIDES, da diese
    Praefixe in keiner der beiden Listen stehen und is_inseparable() sie sonst faelschlich als trennbar
    einstufen wuerde."""
    if inseparable is None:
        inseparable = is_inseparable(prefix)
    out = {"praesens": [], "praeteritum": [], "partizip2": None, "imperativ": []}
    base_part = forms["partizip2"]
    # In beiden Faellen gilt: base_part beginnt nur dann mit "ge", wenn die Basis TATSAECHLICH die echte
    # Partizip-Markierung "ge-" traegt (regulaerer unprafigierter Fall, oder zufaellig ge-anlautende
    # Faelle wie "gehoeren"/"geizen" - dort hat conjugate_weak() das bereits korrekt entschieden). Beginnt
    # base_part NICHT mit "ge" (z.B. weil die Basis selbst schon untrennbar praefigiert ist wie "belebt"
    # aus "beleben", oder ein -ieren-Verb wie "studiert"), darf beim Zusammensetzen KEIN "ge" erfunden
    # werden - weder untrennbar noch trennbar (sonst "wiedergebelebt" statt korrekt "wiederbelebt").
    if inseparable:
        # Praefix bleibt fest am Verb, kein ge-Infix.
        out["praesens"] = [prefix + f for f in forms["praesens"]]
        out["praeteritum"] = [prefix + f for f in forms["praeteritum"]]
        out["partizip2"] = prefix + (base_part[2:] if base_part.startswith("ge") else base_part)
        out["imperativ"] = [prefix + f for f in forms["imperativ"]]
    else:
        # Trennbares Praefix: in dieser Tabelle nur die NICHT satzklammer-getrennte (fusionierte)
        # Schreibweise erzeugt - z.B. "aufgestanden" (Partizip), "aufzustehen" (Infinitiv mit zu) NICHT
        # abgedeckt; Praesens/Praeteritum als eigene Woerter im Haupt-/Nebensatz sind ohnehin nur
        # fusioniert relevant fuer das Wörterbuch (Nebensatz-Wortstellung: "dass er aufsteht").
        out["praesens"] = [prefix + f for f in forms["praesens"]]
        out["praeteritum"] = [prefix + f for f in forms["praeteritum"]]
        if base_part.startswith("ge"):
            out["partizip2"] = prefix + "ge" + base_part[2:]
        else:
            out["partizip2"] = prefix + base_part
        out["imperativ"] = [prefix + f for f in forms["imperativ"]]
    return out


def _check_ambiguous_prefix(lw, base_candidates):
    """Prueft, ob 'lw' aus einem der mehrdeutigen Praefixe (durch/um/ueber/unter/voll/hinter/wieder) plus
    einer bekannten Basis besteht UND dieses konkrete (Praefix,Basis)-Paar in der kuratierten
    PRAEFIX_VERB_OVERRIDES-Tabelle einzeln entschieden wurde. Nur dann wird komponiert - ohne Eintrag
    bleibt die Komposition gesperrt (kein Raten bei echter Mehrdeutigkeit wie "uebertragen"/"umfahren")."""
    for pre in AMBIGUOUS_PREFIXES:
        if lw.startswith(pre) and len(lw) > len(pre) + 2:
            rest = lw[len(pre):]
            decision = PRAEFIX_VERB_OVERRIDES.get((pre, rest))
            if decision and rest in base_candidates:
                return pre, rest, decision == "untrennbar"
    return None, None, None


# Seltene Doppel-Praefix-Faelle innerhalb der wieder-Gruppe (aeusseres "wieder" + inneres trennbares
# Praefix): die einstufige Komposition reicht hier nicht, da die "ge"-Position zwischen beiden Praefixen
# liegt (wiederaufgenommen, nicht wiedergeaufnehmt). Format: wort -> (innere_basis, inneres_praefix).
WIEDER_DOUBLE_PREFIX = {
    "wiederaufnehmen": ("nehmen", "auf"),
    "wiederaufbauen": ("bauen", "auf"),
    "wiederherstellen": ("stellen", "her"),
}


def classify_and_conjugate(infinitiv, known_weak_bases):
    """Vollstaendige Klassifikation+Konjugation fuer einen Woerterbuch-Infinitiv: erkennt Praefixe sowohl
    fuer starke (via STAMMFORMEN) als auch schwache (via 'known_weak_bases' - vom Aufrufer aus echten
    VERB-getaggten dict.tsv-Eintraegen zu befuellen) Basisverben, damit "ge-" bei praefigierten schwachen
    Verben korrekt platziert wird (abgeholt, nicht geabholt; eingewirkt, nicht geeinwirkt). Prueft danach
    zusaetzlich die kuratierte PRAEFIX_VERB_OVERRIDES-Tabelle fuer die mehrdeutigen Praefixe. Ohne erkanntes
    Praefix wird das ganze Wort als unprafigierte Basis konjugiert (konservativer Default, vgl. "erben").
    Gibt (formen_dict, ist_stark) zurueck, oder (None, None) wenn keine Konjugation moeglich ist."""
    lw = infinitiv.lower()
    if lw in WIEDER_DOUBLE_PREFIX:
        inner_base, inner_prefix = WIEDER_DOUBLE_PREFIX[lw]
        is_strong = inner_base in STAMMFORMEN_BY_INFINITIV
        inner_forms = (
            conjugate_strong(inner_base, inner_prefix, inseparable=False)
            if is_strong
            else conjugate_weak(inner_base, inner_prefix, inseparable=False)
        )
        return _apply_prefix(inner_forms, "wieder", inseparable=False), is_strong

    if lw in STAMMFORMEN_BY_INFINITIV or lw in AUX_MODAL_FORMS:
        return conjugate_strong(lw), True

    pre, base = strip_prefix(lw, STAMMFORMEN_BY_INFINITIV.keys())
    if base:
        return conjugate_strong(base, pre), True

    pre, base, insep = _check_ambiguous_prefix(lw, STAMMFORMEN_BY_INFINITIV.keys())
    if base:
        return conjugate_strong(base, pre, inseparable=insep), True

    weak_bases = known_weak_bases - {lw}
    pre, base = strip_prefix(lw, weak_bases)
    if base:
        return conjugate_weak(base, pre), False

    pre, base, insep = _check_ambiguous_prefix(lw, weak_bases)
    if base:
        return conjugate_weak(base, pre, inseparable=insep), False

    return conjugate_weak(lw), False


if __name__ == "__main__":
    for inf in ("gehen", "stehen", "binden", "scheinen", "finden"):
        print(inf, conjugate_strong(inf))
