Bitte mache dich mit dem Projekt vertraut.
Lies /doc/books/AdaptKey-Spec.md und /doc/books/AdaptKey-Progress.md ganz;
**/doc/books/AdaptKey-History.md nie durchlesen – nur gezielt:** erst per Suche lokalisieren (D-/§-Nummer, Stichwort, oder `## §`-Überschriften hinten für die neuesten Einträge), dann nur die getroffenen Abschnitte. Ohne konkretes Thema: nur die letzten 2–3 §-Einträge.

Read rules/version-bump.md

Committe jeden in sich sinnvoll abgeschlossenen Schritt für sich.

# Autonomie / Pushen
- Standard: Der Nutzer pusht immer selber (Bestätigung für ihn, dass alles angekommen ist; er sieht,
  was noch zum Pushen rumliegt). Der Assistent committet, pusht aber nicht von sich aus.
- Ausnahme F-Droid-Prozess: Sagt der Nutzer "Bitte schiebe den aktuellen Stand nach F-Droid"
  (oder ruft /fdroid-release auf), läuft der komplette Prozess selbstständig nach
  .kilo/command/fdroid-release.md - dort ist Pushen und Taggen ausdrücklich Teil des Prozesses.
- Dem Nutzer bleiben nur die Schritte, die ein fremdes Login erfordern (aktuell: GitLab-Kommentare
  an fdroiddata und Pipeline-Re-Runs dort).
- Versionen immer als versionName nennen ("1.2.93"). Der interne versionCode ist für den Nutzer
  irrelevant und gehört nicht in Berichte - nur wo die F-Droid-Infrastruktur ihn wörtlich braucht
  (Monitor-Log-URL, APK-Dateiname, Metadatenfeld).

Dieses Projekt ist kein Java-Eclipse-Projekt. Daher ignoriere eclipse-lsp und verwende File-Tools.
