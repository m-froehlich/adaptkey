# Plan: Vollständige Wortfamilien für das deutsche Wörterbuch (D-404 Tier 1)

**Status:** ABGESCHLOSSEN (§322, v1.0.75) - siehe `AdaptKey-Progress.md` für den vollständigen Verlauf und
die finalen Zahlen. Die Methodik wich am Ende vom ursprünglich hier skizzierten reinen Regelansatz ab
(Pivot zu Wiktionary als Primärquelle, siehe §322) - dieses Dokument bleibt als Ausgangspunkt/Kontext
erhalten, ist aber nicht mehr der aktuelle Stand.
**Kontext:** Erweiterung von D-404 (siehe `AdaptKey-Progress.md`, Open TODOs) - Ziel ist ein Wörterbuch, in
dem ein fehlendes Wort so gut wie nie mehr daran liegt, dass es "nur" eine Flexionsform eines bereits
bekannten Verbs oder Substantivs ist. Baut auf dem D-412-`lemma`-Feld (bundled-only, siehe Spec §38) und den
beiden bereits abgeschlossenen Vorprojekten auf:

- Verb-`OTHER`→`VERB`-Retagging (§306-§319, 9 Runden, 10.925 Kandidaten einzeln geprüft)
- Nomen-Lemma-Verlinkung (§320-§321, ~20.024 mechanische Kandidaten geprüft, 14.976 verlinkt)

Beide Vorprojekte haben **bestehende** Zeilen geprüft/verlinkt. Dieser Plan ist qualitativ anders: es sollen
**neue, bisher fehlende** Flexionsformen erzeugt werden - für praktisch das gesamte Nomen- und Verb-
Vokabular. Geschätzte Kandidatenmenge: 300.000+ Formen - eine Größenordnung über den Vorprojekten.

## Ausgangsfrage des Nutzers

> "Ist es realistisch, für alle Verben und Substantive die Wortfamilien zu vervollständigen? [...] a) für
> jedes Verb die Grundform zu finden [...] alle vier Fälle im Singular und Plural und Präsens, Präteritum
> und Perfekt plus Imperativ Singular/Plural zu bilden (wissen), zu prüfen, ob diese bereits vorhanden sind
> und ggf. einfügen [...] b) für jedes Substantiv die Grundform zu finden, dafür alle vier Fälle im Singular
> und Plural zu bilden [...] und, falls zum Nominativ unterschiedlich und noch nicht enthalten, einfügen"

## Geklärte Punkte (Diskussion vom 2026-08-30)

1. **Methodik:** Bei dieser Größenordnung ist Einzelprüfung jeder Form (wie in den beiden Vorprojekten)
   nicht mehr praktikabel. **Entscheidung: regelbasierte Generierung + kuratierte Ausnahmetabellen für
   unregelmäßige Fälle + Stichproben-Verifikation statt Vollprüfung.** Geringeres Kontrollniveau als bisher,
   aber der einzige realistische Weg bei dieser Menge.
2. **"Perfekt" bei Verben:** Nur das Partizip II wird eingetragen (z. B. "gewusst") - eine echte
   Perfekt-Form ("hat gewusst") ist zwei Wörter und passt nicht ins bestehende Ein-Wort-pro-Zeile-Schema.
3. **"Alle vier Fälle" bei Verben:** war ein Versehen (Copy-Paste aus Punkt b) - bei Verben zählt nur die
   Aufzählung Präsens/Präteritum/Perfekt(=Partizip II)/Imperativ, keine Kasus.

## Formen-Katalog

**Verben** - pro Lemma:
- Präsens: ich/du/er-sie-es/wir/ihr/sie (6 Formen)
- Präteritum: ich/du/er/wir/ihr/sie (6 Formen)
- Partizip II (1 Form)
- Imperativ: Singular (du-Form) + Plural (ihr-Form) (2 Formen)
- → ~15 Slots pro Verb, mehrere davon oft bereits durch bestehende Einträge abgedeckt (z. B.
  wir/sie-Präsens = Infinitiv).

**Substantive** - pro Lemma:
- Nominativ/Genitiv/Dativ/Akkusativ × Singular/Plural → 8 Slots, mit erheblicher Formengleichheit je nach
  Deklinationsklasse (z. B. Nom=Akk=Dat Singular bei starken Nomen, Nom=Akk=Gen Plural durchgängig).

## Technischer Befund (verifiziert im Code, nicht angenommen)

- `WordEntry.kt` / `PartOfSpeech.kt` geprüft: **es existiert aktuell kein Genus-Feld** im Schema. Das ist
  eine echte Voraussetzung für die Nomen-Deklination (Pluralklasse und Deklinationsmuster hängen strukturell
  am Genus) - kein Nice-to-have, sondern ein Blocker für Phase 3/4.
- `RegularVerbInflection.kt` existiert bereits (schützt bekannte Wörter vor Fehlkorrektur durch Erkennen
  plausibler schwacher Flexionsformen), aber nur zum *Schützen*, nicht zum *Generieren*. Die dort verwendete
  Endungsliste ist ein guter Ausgangspunkt für den Verb-Regelmotor.
- A-05 (Spec §7) enthält bereits eine kuratierte Liste trennbarer/untrennbarer Präfixe
  (`ver-/zer-/ent-/emp-/be-/ge-/miss-/er-/un-/ur-/wider-` = untrennbar) - wiederverwendbar für die
  ge-Infix-Platzierung im Partizip II.
- §309 hat bereits empirisch belegt, dass Präfixsuche bei starken Verben strukturell versagt (z. B.
  `gehen`/`gegangen` teilen kein gemeinsames Präfix) - eine handkuratierte Stammformentabelle ist
  unumgänglich, keine Abkürzung möglich.
- Das Nutzerbeispiel "wissen" ist selbst unregelmäßig (weiß/wusste/gewusst, Präteritopräsens-Klasse) -
  bestätigt, dass die Stammformentabelle eine echte Voraussetzung ist, kein Randfall.

## Phasenplan

| Phase | Inhalt | Schreibt dict.tsv? |
|---|---|---|
| **0 - Vermessung & Prototyp** | Echte Lemma-Zahlen ermitteln (Nomen/Verben ohne bereits verlinkte Flexionen, nicht die bloße Zeilenzahl); Genus-Heuristik (Artikel-Kookkurrenz aus dem Wikipedia-Korpus, analog zum bestehenden POS-aus-Großschreibung-Trick) an einer Stichprobe validieren; Regelmotor-Prototyp (Verben *und* Nomen) an je 200 Zufalls-Lemmata gegen die echte Datenbank laufen lassen, Trefferquote/Fehlerquote messen | Nein |
| **1 - Starke-Verben-Stammformentabelle** | ~150-300 Verben kuratieren (Infinitiv, Präteritumstamm, Partizip-II-Stamm, Präsens-Ablaut wo nötig) - ohnehin bereits offener TODO unabhängig von diesem Projekt | Nein |
| **2 - Verb-Generator + Anwendung** | Regelmotor für schwache Verben (Endungen, Epenthese-e nach d/t/chn/ffn/gn, Stamm auf -s/-ß/-z/-x, trennbare/untrennbare Präfixe + ge-Infix-Platzierung) + Stammformentabelle für starke Verben; bandweise Anwendung mit automatischer Kollisionserkennung + Frequenzformel statt Handkalibrierung; Stichprobenverifikation statt Einzelprüfung | Ja |
| **3 - Genus-Datenpipeline** | Neues optionales `gender`-Feld (bundled-only, analog zu `lemma`/D-412) falls die Heuristik aus Phase 0 trägt; sonst Alternative (externe Lexikondaten, neuer Lizenz-/Abhängigkeitscheck) evaluieren | Schema-Änderung, noch keine Nomen-Generierung |
| **4 - Nomen-Generator + Anwendung** | Regelmotor je Deklinationsklasse (stark m/n, schwach m mit durchgängiger n-Deklination, gemischt, feminin ohne Singular-Flexion) + Pluralklassen (-e/-¨e/-er/-¨er/-(e)n/-s/Nullplural); Ausnahmetabelle für nicht-regelhafte Fälle; gleiche Kollisions-/Frequenz-/Stichproben-Infrastruktur wie Phase 2 | Ja |
| **5 - Abschluss** | `lemma` auf alle neu generierten Formen setzen (fällt beim Generieren praktisch kostenlos als Nebenprodukt ab), Pack-Rebuild, Versionsbump, vollständiger Testlauf, Spec-/Progress-Abschlusseintrag | Ja |

## Aufwandseinschätzung

Phase 0/1 sind vom Umfang her je eine Design-/Prototyping-Sitzung. Phase 2 und 4 sind jeweils mindestens so
groß wie das komplette bisherige Nomen-Lemma-Projekt (§320/321, ~29 Runden) - vermutlich größer, weil jetzt
neue Zeilen geschrieben statt nur bestehende verlinkt werden - aber durch den Regelmotor-Ansatz in
überschaubaren Bandrunden statt monatelanger Einzelprüfung machbar. Gesamtprojekt: mehrere Wochen mit vielen
Einzelrunden, aber der reine Engineering-Anteil (Regelmotor, Tabellen, Kollisionslogik) ist einmalig und im
Vergleich zur reinen Datenmenge klein.

## Nächster Schritt, wenn dieser Plan aufgegriffen wird

Start mit **Phase 0** (reine Vermessung + Prototyp, kein Schreibzugriff auf `dict.tsv`, geringes Risiko,
lässt sich jederzeit unterbrechen).
