# -*- coding: utf-8 -*-
# Phase 3 (Wortfamilien-Projekt): Genus-Heuristik aus Artikel-Kookkurrenz in bigram.tsv.
# Kein Genus-Feld existiert im Woerterbuchschema (verifiziert in WordEntry.kt/PartOfSpeech.kt) - diese
# Heuristik liefert das Genus NUR als Zwischenergebnis fuer den Deklinationsgenerator, schreibt es nicht
# zurueck ins Schema (siehe AdaptKey-Plan-Wortfamilien.md Phase 3).
#
# bigram.tsv-Format: "prev\twort\tanzahl", durchgehend kleingeschrieben (auch Substantive).
#
# Empirisch validierte Regeln (Stichprobe, siehe Plan-Dokument):
# - "eine"/"einer" -> IMMER feminin (Indefinitartikel hat in diesen Formen nur die feminine Flexion).
# - "das" (ohne "eine"/"einer") -> neutrales Signal, aber nur verlaesslich wenn es klar dominiert.
# - "ein"/"einem"/"eines" -> maskulin ODER neutrum (Indefinitartikel ist in diesen Formen fuer beide
#   gleich) - nur durch Abwesenheit eines starken "das"-Signals von neutrum unterscheidbar (Ausschlussschluss,
#   keine positive maskuline Evidenz).
# - "der" NIEMALS allein vertrauen: markiert sowohl maskulin Nominativ Singular als auch feminin
#   Genitiv/Dativ Singular ("der Frau" ist feminin, nicht maskulin) - empirisch bestaetigter Fallstrick.
# - "die" NIEMALS allein vertrauen: feminin Singular UND Plural aller Genera gleichzeitig.

FEMININE_SIGNALS = ("eine", "einer")
MASC_NEUT_SIGNALS = ("ein", "einem", "eines")
NEUT_SIGNAL = "das"

# Mindestanzahl Belege, bevor ein Genus-Urteil ueberhaupt versucht wird - vermeidet Rauschen bei sehr
# seltenen Woertern mit nur 1-2 zufaelligen Bigramm-Treffern.
MIN_EVIDENCE = 3


def load_bigrams(path="bigram.tsv"):
    """Liest bigram.tsv in ein dict {(prev, wort): anzahl}."""
    counts = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) == 3:
                prev, wort, cnt = parts
                try:
                    counts[(prev, wort)] = int(cnt)
                except ValueError:
                    continue
    return counts



# "das X" ist bei echten maskulinen Nomen empirisch NIE belegt (Stichprobe von 16 bekannten maskulinen
# Nomen quer durch die Frequenzbaender: durchgehend 0 Treffer fuer "das X"). Ein reiner Anteilsschwellwert
# (z.B. "neut muss >=20% des Gesamtbelegs sein") schlaegt bei haeufigen, aber typischerweise indefinit
# gebrauchten Neutra fehl (Beispiel "Beispiel": "das Beispiel"=118, aber "ein Beispiel"=1047 wegen der
# Redewendung "zum Beispiel" - Anteil nur 10%, trotzdem eindeutig neutral). Da "das X" bei Maskulina auf
# Null faellt, reicht schon eine kleine ABSOLUTE Mindestanzahl als verlaessliches Neutrum-Signal, unabhaengig
# davon, wie gross der (durch die Buendelung dreier Formen natuerlicherweise groessere) mn-Wert ist.
NEUT_MIN_COUNT = 3


def guess_genus(noun, bigrams):
    """Schaetzt das Genus eines Substantivs (dict.tsv-Schreibweise, z.B. 'Frau') aus Artikel-Kookkurrenz.
    Gibt 'f' | 'm' | 'n' | None zurueck (None = keine ausreichende Evidenz)."""
    lw = noun.lower()
    fem = sum(bigrams.get((sig, lw), 0) for sig in FEMININE_SIGNALS)
    mn = sum(bigrams.get((sig, lw), 0) for sig in MASC_NEUT_SIGNALS)
    neut = bigrams.get((NEUT_SIGNAL, lw), 0)

    total = fem + mn + neut
    if total < MIN_EVIDENCE:
        return None

    if fem > 0 and fem >= mn and fem >= neut:
        return "f"
    if neut >= NEUT_MIN_COUNT:
        return "n"
    if mn > 0:
        return "m"
    return None


if __name__ == "__main__":
    bigrams = load_bigrams()
    tests = ["Frau", "Stadt", "Zeit", "Universität", "Mann", "Tisch", "Haus", "Kind", "Buch"]
    for w in tests:
        print(w, guess_genus(w, bigrams))
