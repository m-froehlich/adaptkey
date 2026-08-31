# -*- coding: utf-8 -*-
# Phase 1 (Wortfamilien-Projekt): Stammformentabelle fuer starke/unregelmaessige deutsche Basisverben.
# Format je Eintrag: (infinitiv, praesens_3sg_stammwechsel_oder_None, praeteritum_stamm, partizip_ii, gemischt)
# "gemischt" = Verb hat Vokalwechsel, aber dentales (-t) Praeteritum/Partizip statt -en (z.B. brennen/brannte/gebrannt)
# Praefigierte Ableitungen (verstehen, aufstehen, ...) werden NICHT einzeln gelistet - die werden bei der
# Anwendung durch Komposition aus Praefix + dieser Basisform gebildet (nutzt die A-05-Praefixliste).
# Dauerhaft in dieser Datei gehalten (nicht mehr wegwerfbar) - wird von declension/conjugation-Skripten importiert.

STAMMFORMEN = [
    ("sein", "ist", "war", "gewesen", False),
    ("haben", "hat", "hatte", "gehabt", False),
    ("werden", "wird", "wurde", "geworden", False),
    ("können", "kann", "konnte", "gekonnt", False),
    ("müssen", "muss", "musste", "gemusst", False),
    ("dürfen", "darf", "durfte", "gedurft", False),
    ("sollen", None, "sollte", "gesollt", False),
    ("wollen", "will", "wollte", "gewollt", False),
    ("mögen", "mag", "mochte", "gemocht", False),
    ("wissen", "weiß", "wusste", "gewusst", False),

    ("bleiben", None, "blieb", "geblieben", False),
    ("schreiben", None, "schrieb", "geschrieben", False),
    ("treiben", None, "trieb", "getrieben", False),
    ("steigen", None, "stieg", "gestiegen", False),
    ("schweigen", None, "schwieg", "geschwiegen", False),
    ("scheiden", None, "schied", "geschieden", False),
    ("leiden", None, "litt", "gelitten", False),
    ("schneiden", None, "schnitt", "geschnitten", False),
    ("reiten", None, "ritt", "geritten", False),
    ("streiten", None, "stritt", "gestritten", False),
    ("greifen", None, "griff", "gegriffen", False),
    ("pfeifen", None, "pfiff", "gepfiffen", False),
    ("reißen", None, "riss", "gerissen", False),
    ("beißen", None, "biss", "gebissen", False),
    ("gleiten", None, "glitt", "geglitten", False),
    ("scheinen", None, "schien", "geschienen", False),
    ("meiden", None, "mied", "gemieden", False),
    ("leihen", None, "lieh", "geliehen", False),
    ("verzeihen", None, "verzieh", "verziehen", False),
    ("weisen", None, "wies", "gewiesen", False),
    ("schreien", None, "schrie", "geschrien", False),

    ("fliegen", None, "flog", "geflogen", False),
    ("fliehen", None, "floh", "geflohen", False),
    ("fließen", None, "floss", "geflossen", False),
    ("frieren", None, "fror", "gefroren", False),
    ("verlieren", None, "verlor", "verloren", False),
    ("wiegen", None, "wog", "gewogen", False),
    ("ziehen", None, "zog", "gezogen", False),
    ("biegen", None, "bog", "gebogen", False),
    ("bieten", None, "bot", "geboten", False),
    ("schieben", None, "schob", "geschoben", False),
    ("schießen", None, "schoss", "geschossen", False),
    ("genießen", None, "genoss", "genossen", False),
    ("gießen", None, "goss", "gegossen", False),
    ("kriechen", None, "kroch", "gekrochen", False),
    ("riechen", None, "roch", "gerochen", False),
    ("schließen", None, "schloss", "geschlossen", False),
    ("lügen", None, "log", "gelogen", False),
    ("saugen", None, "sog", "gesogen", False),

    ("binden", None, "band", "gebunden", False),
    ("finden", None, "fand", "gefunden", False),
    ("singen", None, "sang", "gesungen", False),
    ("springen", None, "sprang", "gesprungen", False),
    ("trinken", None, "trank", "getrunken", False),
    ("beginnen", None, "begann", "begonnen", False),
    ("gewinnen", None, "gewann", "gewonnen", False),
    ("schwimmen", None, "schwamm", "geschwommen", False),
    ("klingen", None, "klang", "geklungen", False),
    ("gelingen", None, "gelang", "gelungen", False),
    ("verschwinden", None, "verschwand", "verschwunden", False),
    ("zwingen", None, "zwang", "gezwungen", False),
    ("dringen", None, "drang", "gedrungen", False),
    ("hängen", None, "hing", "gehangen", False),
    ("sinken", None, "sank", "gesunken", False),
    ("sinnen", None, "sann", "gesonnen", False),
    ("spinnen", None, "spann", "gesponnen", False),

    ("geben", "gibt", "gab", "gegeben", False),
    ("nehmen", "nimmt", "nahm", "genommen", False),
    ("sprechen", "spricht", "sprach", "gesprochen", False),
    ("treffen", "trifft", "traf", "getroffen", False),
    ("helfen", "hilft", "half", "geholfen", False),
    ("sterben", "stirbt", "starb", "gestorben", False),
    ("werfen", "wirft", "warf", "geworfen", False),
    ("brechen", "bricht", "brach", "gebrochen", False),
    ("stehlen", "stiehlt", "stahl", "gestohlen", False),
    ("empfehlen", "empfiehlt", "empfahl", "empfohlen", False),
    ("essen", "isst", "aß", "gegessen", False),
    ("messen", "misst", "maß", "gemessen", False),
    ("vergessen", "vergisst", "vergaß", "vergessen", False),
    ("fressen", "frisst", "fraß", "gefressen", False),
    ("lesen", "liest", "las", "gelesen", False),
    ("sehen", "sieht", "sah", "gesehen", False),
    ("geschehen", "geschieht", "geschah", "geschehen", False),

    ("fahren", "fährt", "fuhr", "gefahren", False),
    ("tragen", "trägt", "trug", "getragen", False),
    ("schlagen", "schlägt", "schlug", "geschlagen", False),
    ("wachsen", "wächst", "wuchs", "gewachsen", False),
    ("waschen", "wäscht", "wusch", "gewaschen", False),
    ("laden", "lädt", "lud", "geladen", False),
    ("graben", "gräbt", "grub", "gegraben", False),
    ("backen", "bäckt", "backte", "gebacken", False),
    ("fangen", "fängt", "fing", "gefangen", False),
    ("empfangen", "empfängt", "empfing", "empfangen", False),

    ("laufen", "läuft", "lief", "gelaufen", False),

    ("halten", "hält", "hielt", "gehalten", False),
    ("lassen", "lässt", "ließ", "gelassen", False),
    ("raten", "rät", "riet", "geraten", False),
    ("schlafen", "schläft", "schlief", "geschlafen", False),
    ("blasen", "bläst", "blies", "geblasen", False),
    ("rufen", None, "rief", "gerufen", False),
    ("stoßen", "stößt", "stieß", "gestoßen", False),
    ("heißen", None, "hieß", "geheißen", False),

    ("gehen", None, "ging", "gegangen", False),
    ("stehen", None, "stand", "gestanden", False),
    ("liegen", None, "lag", "gelegen", False),
    ("sitzen", None, "saß", "gesessen", False),
    ("bitten", None, "bat", "gebeten", False),
    ("kommen", None, "kam", "gekommen", False),

    ("gelten", "gilt", "galt", "gegolten", False),
    ("erhalten", "erhält", "erhielt", "erhalten", False),
    ("verlassen", "verlässt", "verließ", "verlassen", False),
    ("erschrecken", "erschrickt", "erschrak", "erschrocken", False),
    ("entscheiden", None, "entschied", "entschieden", False),

    # Nachtrag: im Woerterbuchkorpus belegte starke Verben, die in der urspruenglichen 121er-Auswahl
    # fehlten (Fund beim Abgleich der Wortfamilien-Anwendung gegen echte dict.tsv-Eintraege - siehe
    # AdaptKey-Plan-Wortfamilien.md). "bewegen" (ueberwiegend schwache Alltagsbedeutung "sich bewegen")
    # und "triefen" (moderne Standardform ist schwach "triefte/getrieft"; die starke Nebenform wuerde im
    # Partizip mit "getroffen" von treffen kollidieren) sind bewusst NICHT aufgenommen.
    ("schmelzen", "schmilzt", "schmolz", "geschmolzen", False),
    ("verderben", "verdirbt", "verdarb", "verdorben", False),
    ("gedeihen", None, "gedieh", "gediehen", False),
    ("weichen", None, "wich", "gewichen", False),
    ("schwellen", "schwillt", "schwoll", "geschwollen", False),
    ("erlöschen", "erlischt", "erlosch", "erloschen", False),
    ("bergen", "birgt", "barg", "geborgen", False),
    ("gleichen", None, "glich", "geglichen", False),
    ("streichen", None, "strich", "gestrichen", False),
    ("schleichen", None, "schlich", "geschlichen", False),
    ("preisen", None, "pries", "gepriesen", False),
    ("erwägen", None, "erwog", "erwogen", False),
    ("winden", None, "wand", "gewunden", False),
    ("schinden", None, "schund", "geschunden", False),
    ("sprießen", None, "spross", "gesprossen", False),
    ("reiben", None, "rieb", "gerieben", False),
    ("hauen", None, "hieb", "gehauen", False),
    ("stechen", "sticht", "stach", "gestochen", False),
    ("schwören", None, "schwor", "geschworen", False),
    ("schleifen", None, "schliff", "geschliffen", False),
    ("treten", "tritt", "trat", "getreten", False),
    ("werben", "wirbt", "warb", "geworben", False),
    ("fallen", "fällt", "fiel", "gefallen", False),
    ("ringen", None, "rang", "gerungen", False),
    ("schreiten", None, "schritt", "geschritten", False),
    ("gebären", "gebärt", "gebar", "geboren", False),
    ("schlingen", None, "schlang", "geschlungen", False),
    ("misslingen", None, "misslang", "misslungen", False),
    ("heben", None, "hob", "gehoben", False),
    ("empfinden", None, "empfand", "empfunden", False),
    ("schleißen", None, "schliss", "geschlissen", False),
    ("trügen", None, "trog", "getrogen", False),

    ("kennen", None, "kannte", "gekannt", True),
    ("nennen", None, "nannte", "genannt", True),
    ("rennen", None, "rannte", "gerannt", True),
    ("brennen", None, "brannte", "gebrannt", True),
    ("senden", None, "sandte", "gesandt", True),
    ("wenden", None, "wandte", "gewandt", True),
    ("bringen", None, "brachte", "gebracht", True),
    ("denken", None, "dachte", "gedacht", True),
]

# Deduplizieren (falls ein Verb in mehreren Klassenlisten notiert wurde)
_seen = {}
for _entry in STAMMFORMEN:
    _seen[_entry[0]] = _entry
STAMMFORMEN = list(_seen.values())

STAMMFORMEN_BY_INFINITIV = {e[0]: e for e in STAMMFORMEN}

# sein/haben/werden und die Praeteritopraesentia (koennen/muessen/duerfen/sollen/wollen/moegen/wissen) sind
# historisch eine eigene, hochgradig unregelmaessige Verbklasse: 1. Person Singular Praesens = 3. Person
# Singular (kein "-e"), teils suppletive Formen (sein: bin/bist/ist/sind/seid/sind), teils Einzel-
# unregelmaessigkeiten (werden: wirst statt wirdst). Mechanische Ableitung aus einem Stamm scheitert hier
# grundsaetzlich - deshalb vollstaendig hartkodiert statt algorithmisch erzeugt.
AUX_MODAL_FORMS = {
    "sein": {
        "praesens": ["bin", "bist", "ist", "sind", "seid", "sind"],
        "praeteritum": ["war", "warst", "war", "waren", "wart", "waren"],
        "partizip2": "gewesen",
        "imperativ": ["sei", "seid"],
    },
    "haben": {
        "praesens": ["habe", "hast", "hat", "haben", "habt", "haben"],
        "praeteritum": ["hatte", "hattest", "hatte", "hatten", "hattet", "hatten"],
        "partizip2": "gehabt",
        "imperativ": ["hab", "habt"],
    },
    "werden": {
        "praesens": ["werde", "wirst", "wird", "werden", "werdet", "werden"],
        "praeteritum": ["wurde", "wurdest", "wurde", "wurden", "wurdet", "wurden"],
        "partizip2": "geworden",
        "imperativ": ["werde", "werdet"],
    },
    "können": {
        "praesens": ["kann", "kannst", "kann", "können", "könnt", "können"],
        "praeteritum": ["konnte", "konntest", "konnte", "konnten", "konntet", "konnten"],
        "partizip2": "gekonnt",
        "imperativ": None,
    },
    "müssen": {
        "praesens": ["muss", "musst", "muss", "müssen", "müsst", "müssen"],
        "praeteritum": ["musste", "musstest", "musste", "mussten", "musstet", "mussten"],
        "partizip2": "gemusst",
        "imperativ": None,
    },
    "dürfen": {
        "praesens": ["darf", "darfst", "darf", "dürfen", "dürft", "dürfen"],
        "praeteritum": ["durfte", "durftest", "durfte", "durften", "durftet", "durften"],
        "partizip2": "gedurft",
        "imperativ": None,
    },
    "sollen": {
        "praesens": ["soll", "sollst", "soll", "sollen", "sollt", "sollen"],
        "praeteritum": ["sollte", "solltest", "sollte", "sollten", "solltet", "sollten"],
        "partizip2": "gesollt",
        "imperativ": None,
    },
    "wollen": {
        "praesens": ["will", "willst", "will", "wollen", "wollt", "wollen"],
        "praeteritum": ["wollte", "wolltest", "wollte", "wollten", "wolltet", "wollten"],
        "partizip2": "gewollt",
        "imperativ": ["wolle", "wollt"],
    },
    "mögen": {
        "praesens": ["mag", "magst", "mag", "mögen", "mögt", "mögen"],
        "praeteritum": ["mochte", "mochtest", "mochte", "mochten", "mochtet", "mochten"],
        "partizip2": "gemocht",
        "imperativ": None,
    },
    "wissen": {
        "praesens": ["weiß", "weißt", "weiß", "wissen", "wisst", "wissen"],
        "praeteritum": ["wusste", "wusstest", "wusste", "wussten", "wusstet", "wussten"],
        "partizip2": "gewusst",
        "imperativ": ["wisse", "wisst"],
    },
}

# Schwache Verben mit einer einzelnen unregelmaessigen Ausnahme im Partizip II (Praesens und Praeteritum
# sind ganz regulaer schwach - "mahlte", "spaltete" - nur das Partizip hat eine alte, stark aussehende
# Form ohne -t bewahrt). Zu klein/uneinheitlich fuer eine eigene Konjugationsklasse, deshalb als gezielte
# Partizip-Ausnahme neben conjugate_weak() gefuehrt statt als vollstaendiger STAMMFORMEN-Eintrag.
WEAK_IRREGULAR_PARTIZIP = {
    "mahlen": "gemahlen",
    "spalten": "gespalten",
}

# Schwache Verben, deren Anfang zufaellig wie ein unbetontes Praefix (be-/ge-/er-/...) aussieht, wo es aber
# tatsaechlich Teil des einsilbigen Wortstamms ist - brauchen trotzdem "ge-" im Partizip II (geerbt, nicht
# erbt; geerntet, nicht erntet). Kuratierte Ausnahmeliste zur allgemeinen Praefix-Erkennungsregel in
# conjugate_weak(), die sonst faelschlich "ge-" unterdruecken wuerde.
WEAK_FALSE_PREFIX_EXCEPTIONS = {
    "erben", "ernten", "geizen", "bellen", "beben", "beten",
    "entern", "beugen", "geiseln", "geißeln",
}

# Bekannte Konjunktiv-/Sonderformen von sein/werden, die zufaellig auf -en enden und im Woerterbuch
# vorkommen, aber keine eigenen Infinitive sind (vgl. "worden" = Passiv-Partizip von werden).
NONBASE_INFLECTED_FORMS = {"worden", "seien", "wären", "würden", "täten", "hätten"}

# Native deutsche Verben, deren Stamm zufaellig auf "-ier" endet (schmier-en, zier-en) und daher wie ein
# unbetontes Fremdwort-Verb auf -ieren aussieht (stud-ieren, exist-ieren) - brauchen aber trotzdem "ge-" im
# Partizip II (geschmiert, nicht schmiert; geziert, nicht ziert), da "-ier-" hier Teil des nativen Stamms
# ist, nicht die Lehnverb-Endung.
WEAK_NATIVE_IEREN_EXCEPTIONS = {"schmieren", "zieren", "gieren", "stieren"}

if __name__ == "__main__":
    print(f"Anzahl Basisverben: {len(STAMMFORMEN)}")
