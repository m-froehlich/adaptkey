# D-555: insert the bottom-row emoji key's two strings into values/ + all 31 locale strings.xml
# files, directly after the d507_combined_key_summary anchor (same insertion pattern as
# d515_strings.py). English (values/) is the reference; de is maintainer-curated; every other
# locale is an AI draft per the D-478 precedent (UiLocaleConsistencyTest enforces presence).
import io
import os

RES = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")

# locale dir name -> (title, summary). Apostrophes must be XML-escaped (\' - Android raw strings).
STRINGS = {
    "values": (
        "Emoji key",
        "Show an emoji key (😊) directly right of the special-characters key. Comma and full stop "
        "give up their extra width above a letter\\'s width to make room. Off: bottom row as before.",
    ),
    "values-de": (
        "Emoji-Taste",
        "Emoji-Taste (😊) direkt rechts neben der Sonderzeichen-Taste anzeigen. Komma und Punkt "
        "geben dafür ihre Verbreiterung über der Buchstabenbreite ab. Aus: untere Reihe wie bisher.",
    ),
    "values-az": (
        "Emoji düyməsi",
        "Xüsusi simvollar düyməsinin birbaşa sağında emoji düyməsini göstər. Vergül və nöqtə bunun "
        "üçün hərf enindən yuxarı olan əlavə enlərini verir. Bağlı: alt sıra əvvəlki kimi.",
    ),
    "values-bs": (
        "Emoji tipka",
        "Prikaži emoji tipku (odmah desno od tipke specijalnih znakova). Zarez i tačka za to daju "
        "svoju širinu iznad širine slova. Isključeno: donji red kao prije.",
    ),
    "values-cs": (
        "Klávesa emoji",
        "Zobrazit klávesu emoji (přímo vpravo od klávesy speciálních znaků). Čárka a tečka za to "
        "věnují svou šíři nad šíří písmene. Vypnuto: spodní řádek jako dříve.",
    ),
    "values-da": (
        "Emoji-tast",
        "Vis en emoji-tast (lige til højre for specialtegnstasten). Komma og punktum afgiver deres "
        "ekstra bredde over bogstavbredde til formålet. Fra: nederste række som før.",
    ),
    "values-el": (
        "Πλήκτρο emoji",
        "Εμφάνιση πλήκτρου emoji (αμέσως δεξιά του πλήκτρου ειδικών χαρακτήρων). Το κόμμα και η "
        "τελεία παραχωρούν το επιπλέον πλάτος τους πάνω από το πλάτος γράμματος. Ανενεργό: κάτω "
        "σειρά όπως πριν.",
    ),
    "values-es": (
        "Tecla de emojis",
        "Mostrar una tecla de emojis (justo a la derecha de la tecla de caracteres especiales). La "
        "coma y el punto ceden su anchura extra por encima de la anchura de una letra. Desactivado: "
        "fila inferior como antes.",
    ),
    "values-et": (
        "Emoji-klahv",
        "Kuva emoji-klahv (kohe erisümbolite klahvist paremal). Koma ja punkt annavad selleks oma "
        "lisalaiuse tähelaiusest üle. Väljas: alumine rida nagu enne.",
    ),
    "values-fi": (
        "Emoji-näppäin",
        "Näytä emoji-näppäin (heti erikoismerkkinäppäimen oikealla puolella). Pilkku ja piste "
        "luovuttavat sitä varten ylimääräisen leveytensä kirjaimen leveyden yltä. Pois käytöstä: "
        "alarivi kuten ennen.",
    ),
    "values-fil": (
        "Emoji key",
        "Ipakita ang emoji key (kanan agad ng special-characters key). Ang kuwit at tuldok ay "
        "nagbibigay ng kanilang dagdag na lapad sa itaas ng lapad ng titik. Naka-off: ilalim na "
        "hanay gaya ng dati.",
    ),
    "values-fr": (
        "Touche emoji",
        "Afficher une touche emoji (juste à droite de la touche des caractères spéciaux). La "
        "virgule et le point cèdent leur largeur supplémentaire au-dessus de la largeur d\\'une "
        "lettre. Désactivé : rangée du bas comme avant.",
    ),
    "values-hr": (
        "Tipka emoji",
        "Prikaži tipku emoji (odmah desno od tipke posebnih znakova). Zarez i točka za to daju "
        "svoju dodatnu širinu iznad širine slova. Isključeno: donji red kao prije.",
    ),
    "values-hu": (
        "Emoji billentyű",
        "Emoji billentyű megjelenítése (közvetlenül a speciális karakterek billentyűje jobbján). A "
        "vessző és a pont ehhez leadja a betűszélesség feletti többletszélességét. Ki: alsó sor, "
        "mint korábban.",
    ),
    "values-id": (
        "Tombol emoji",
        "Tampilkan tombol emoji (tepat di kanan tombol karakter khusus). Koma dan titik "
        "melepaskan lebar ekstra mereka di atas lebar huruf. Mati: baris bawah seperti sebelumnya.",
    ),
    "values-it": (
        "Tasto emoji",
        "Mostra un tasto emoji (subito a destra del tasto dei caratteri speciali). Virgola e punto "
        "cedono la loro larghezza extra sopra la larghezza di una lettera. Disattivato: riga "
        "inferiore come prima.",
    ),
    "values-lt": (
        "Emoji klavišas",
        "Rodyti emoji klavišą (tiesiogiai dešinėje nuo specialiųjų simbolių klavišo). Kablelis ir "
        "taškas tam atiduoda savo papildomą plotį virš raidės pločio. Išjungta: apatinė eilutė kaip "
        "anksčiau.",
    ),
    "values-lv": (
        "Emocijzīmju taustiņš",
        "Rādīt emocijzīmju taustiņu (tieši pa labi no īpašo simbolu taustiņa). Komats un punkts tam "
        "atdod savu papildu platumu virs burtu platuma. Izslēgts: apakšējā rinda kā iepriekš.",
    ),
    "values-ms": (
        "Kekunci emoji",
        "Paparkan kekunci emoji (terus di kanan kekunci aksara khas). Koma dan titik menyerahkan "
        "lebar tambahan mereka melebihi lebar huruf. Mati: baris bawah seperti sebelum.",
    ),
    "values-nb": (
        "Emoji-tast",
        "Vis en emoji-tast (rett til høyre for spesialtegnstasten). Komma og punktum gir fra seg "
        "ekstrabredden over bokstavbredden til formålet. Av: nederste rad som før.",
    ),
    "values-nl": (
        "Emoji-toets",
        "Toon een emoji-toets (direct rechts van de speciale-tekens-toets). Komma en punt geven "
        "hun extra breedte boven de letterbreedte af. Uit: onderste rij als voorheen.",
    ),
    "values-pl": (
        "Klawisz emoji",
        "Pokaż klawisz emoji (bezpośrednio na prawo od klawisza znaków specjalnych). Przecinek i "
        "kropka oddają na to swoją nadwyżkę szerokości ponad szerokość litery. Wyłączone: dolny "
        "rząd jak wcześniej.",
    ),
    "values-pt": (
        "Tecla de emoji",
        "Mostrar uma tecla de emoji (logo à direita da tecla de caracteres especiais). A vírgula e "
        "o ponto cedem a sua largura extra acima da largura de uma letra. Desativado: linha "
        "inferior como antes.",
    ),
    "values-ro": (
        "Tastă emoji",
        "Afișează o tastă emoji (imediat la dreapta tastei caracterelor speciale). Virgula și "
        "punctul cedează lățimea lor suplimentară peste lățimea unei litere. Dezactivat: rândul de "
        "jos ca înainte.",
    ),
    "values-ru": (
        "Клавиша эмодзи",
        "Показывать клавишу эмодзи (сразу справа от клавиши специальных символов). Запятая и точка "
        "отдают для этого свою лишнюю ширину сверх ширины буквы. Выкл.: нижний ряд как раньше.",
    ),
    "values-sk": (
        "Klávesa emoji",
        "Zobraziť klávesu emoji (priamo vpravo od klávesy špeciálnych znakov). Čiarka a bodka za "
        "to venujú svoju šírku nad šírkou písmena. Vypnuté: spodný riadok ako predtým.",
    ),
    "values-sr": (
        "Тастер емодзија",
        "Прикажи тастер емодзија (одмах десно од тастера специјалних знакова). Зарез и тачка за то "
        "дају свој вишак ширине изнад ширине слова. Искључено: доњи ред као раније.",
    ),
    "values-sv": (
        "Emoji-tangent",
        "Visa en emoji-tangent (direkt till höger om specialteckenstangenten). Komma och punkt "
        "avstår från sin extra bredd över bokstavsbredd. Av: undre raden som förut.",
    ),
    "values-sw": (
        "Kitufe cha emoji",
        "Onyesha kitufe cha emoji (kulia mara moja kwa kitufe cha herufi maalum). Koma na nukta "
        "zinatoa upana wao wa ziada zaidi ya upana wa herufi. Zima: safu ya chini kama kawaida.",
    ),
    "values-tr": (
        "Emoji tuşu",
        "Emoji tuşunu (özel karakterler tuşunun hemen sağında) göster. Virgül ve nokta bunun için "
        "harf genişliği üzerindeki fazla genişliklerini verir. Kapalı: alt sıra önceki gibi.",
    ),
    "values-uk": (
        "Клавіша емодзі",
        "Показувати клавішу емодзі (одразу праворуч від клавіші спеціальних символів). Кома й "
        "крапка віддають для цього свій надлишок ширини понад шириною літери. Вимк.: нижній ряд як "
        "раніше.",
    ),
    "values-uz": (
        "Emoji tugmasi",
        "Emoji tugmasini (maxsus belgilar tugmasidan oʻngda) koʻrsatish. Vergul va nuqta buning "
        "uchun harf kengligidan oshiq kengligini beradi. Oʻchirilgan: pastki qator avvalgidek.",
    ),
}

ANCHOR = 'name="d507_combined_key_summary"'
LINE_END = "</string>"

inserted = 0
for locale, (title, summary) in STRINGS.items():
    path = os.path.join(RES, locale, "strings.xml")
    with io.open(path, "r", encoding="utf-8", newline="") as fh:
        content = fh.read()
    if 'name="d555_emoji_key_title"' in content:
        print("skip (already present):", locale)
        continue
    # Normalize to the file's own dominant EOL (the repo hook keeps them uniform either way).
    crlf = "\r\n" in content
    lines = [ln.rstrip("\r") for ln in content.split("\n")]
    out = []
    done = False
    for line in lines:
        out.append(line)
        if not done and ANCHOR in line and line.rstrip().endswith(LINE_END):
            indent = line[: len(line) - len(line.lstrip())]
            out.append('{}<string name="d555_emoji_key_title">{}</string>'.format(indent, title))
            out.append('{}<string name="d555_emoji_key_summary">{}</string>'.format(indent, summary))
            done = True
    if not done:
        raise SystemExit("anchor not found in " + path)
    joined = "\r\n" if crlf else "\n"
    with io.open(path, "w", encoding="utf-8", newline="") as fh:
        fh.write(joined.join(out) + joined if content.endswith(("\n", "\r")) else joined.join(out))
    inserted += 1

print("inserted into", inserted, "files")
