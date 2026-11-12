# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Froehlich Media
#
# D-531 string pass: the page-navigation reform. The letters page's combined key is the `!?#`
# special-characters-page toggle now (was the `?123` number-layer toggle), the special-characters
# page's own key is the `?123` number-layer toggle (was `ABC`), and the number layer's `ABC` returns
# to letters. The D-507 setting's title/summary/mode label and every string naming the letters page's
# key (the D-19 summary incl. its D-516 glide hint, the D-516 summary, the D-89 f08 entry) are
# updated for the English reference plus all 31 translation directories (per the standing D-478
# rule; drafts). Idempotent per value. Run from the repo root: python scratchpad/d531_strings.py

from pathlib import Path
import sys

sys.stdout.reconfigure(encoding="utf-8")

RES = Path("app/src/main/res")

# locale -> (d507_combined_key_summary, d19_surface_swipe_summary)
REWRITES = {
    "values": (
        "What the combined key in the bottom row does: jump to the special-characters page (!?#), open the emoji panel, or nothing - the key disappears and its slot stays empty (D-507).",
        "Swipe left or right across the keyboard to switch between the letters page and the symbol pages. When off, only the page keys (!?# / ?123 / ABC) switch. With glide typing on, the letters page switches pages only via the !?# key.",
    ),
    "values-de": (
        "Was die kombinierte Taste in der unteren Reihe macht: zur Sonderzeichen-Seite springen (!?#), das Emoji-Panel öffnen oder nichts - die Taste verschwindet, ihr Platz bleibt leer (D-507).",
        "Nach links oder rechts über die Tastatur wischen wechselt zwischen der Buchstaben-Seite und den Symbol-Seiten. Wenn aus, wechseln nur die Seiten-Tasten (!?# / ?123 / ABC). Bei aktiver Gesteeneingabe (Glide-Typing) schaltet auf der Buchstaben-Seite nur die !?#-Taste.",
    ),
    "values-el": (
        "Τι κάνει το συνδυασμένο πλήκτρο στην κάτω σειρά: μετάβαση στη σελίδα ειδικών χαρακτήρων (!?#), άνοιγμα του πίνακα emoji ή τίποτα - το πλήκτρο εξαφανίζεται και η θέση του μένει κενή (D-507).",
        "Σαρώστε αριστερά ή δεξιά στο πληκτρολόγιο για εναλλαγή μεταξύ της σελίδας γραμμάτων και των σελίδων συμβόλων. Όταν είναι απενεργοποιημένο, εναλλάσσουν μόνο τα πλήκτρα σελίδων (!?# / ?123 / ABC). Με ενεργή τη χειρονομική πληκτρολόγηση, η σελίδα γραμμάτων αλλάζει μόνο μέσω του πλήκτρου !?#.",
    ),
    "values-fr": (
        "Ce que fait la touche combinée de la rangée du bas : aller à la page des caractères spéciaux (!?#), ouvrir le panneau d\\'émojis, ou rien - la touche disparaît et son emplacement reste vide (D-507).",
        "Balayez vers la gauche ou la droite sur le clavier pour passer de la page des lettres aux pages de symboles. Si désactivé, seules les touches de page (!?# / ?123 / ABC) permettent de changer. Avec la saisie gestuelle active, la page des lettres ne change que via la touche !?#.",
    ),
    "values-es": (
        "Qué hace la tecla combinada de la fila inferior: ir a la página de caracteres especiales (!?#), abrir el panel de emojis o nada - la tecla desaparece y su hueco queda vacío (D-507).",
        "Desliza a la izquierda o a la derecha sobre el teclado para cambiar entre la página de letras y las páginas de símbolos. Si está desactivado, solo cambian las teclas de página (!?# / ?123 / ABC). Con la escritura gestual activada, la página de letras solo cambia mediante la tecla !?#.",
    ),
    "values-it": (
        "Cosa fa il tasto combinato nella riga inferiore: passare alla pagina dei caratteri speciali (!?#), aprire il pannello delle emoji o niente - il tasto scompare e il suo spazio resta vuoto (D-507).",
        "Scorri a sinistra o a destra sulla tastiera per passare dalla pagina delle lettere alle pagine dei simboli. Se disattivato, cambiano solo i tasti di pagina (!?# / ?123 / ABC). Con la scrittura gestuale attiva, la pagina delle lettere cambia solo tramite il tasto !?#.",
    ),
    "values-pt": (
        "O que a tecla combinada da fileira inferior faz: ir para a página de caracteres especiais (!?#), abrir o painel de emojis ou nada - a tecla desaparece e o seu espaço fica vazio (D-507).",
        "Deslize para a esquerda ou direita no teclado para alternar entre a página de letras e as páginas de símbolos. Quando desativado, apenas as teclas de página (!?# / ?123 / ABC) alternam. Com a escrita por gestos ativa, a página de letras só muda através da tecla !?#.",
    ),
    "values-nl": (
        "Wat de gecombineerde toets in de onderste rij doet: naar de pagina met speciale tekens gaan (!?#), het emoji-paneel openen of niets - de toets verdwijnt en zijn plek blijft leeg (D-507).",
        "Veeg naar links of rechts over het toetsenbord om te wisselen tussen de letterpagina en de symboolpagina\\'s. Indien uit, wisselen alleen de paginatoetsen (!?# / ?123 / ABC). Met glide-typen ingeschakeld wisselt de letterspagina alleen via de !?#-toets.",
    ),
    "values-pl": (
        "Co robi klawisz kombinowany w dolnym rzędzie: przejście na stronę znaków specjalnych (!?#), otwieranie panelu emoji albo nic - klawisz znika, a jego miejsce pozostaje puste (D-507).",
        "Przesuń w lewo lub w prawo po klawiaturze, aby przełączać między stroną liter a stronami symboli. Po wyłączeniu przełączają tylko klawisze stron (!?# / ?123 / ABC). Gdy pisanie gestami jest włączone, strona liter zmienia się tylko klawiszem !?#.",
    ),
    "values-tr": (
        "Alt sıradaki birleşik tuşun yaptığı şey: özel karakterler sayfasına geçmek (!?#), emoji panelini açmak ya da hiçbir şey - tuş kaybolur ve yeri boş kalır (D-507).",
        "Harf sayfası ile simge sayfaları arasında geçiş yapmak için klavyede sola veya sağa kaydırın. Kapalıyken yalnızca sayfa tuşları (!?# / ?123 / ABC) geçiş yapar. Kaydırarak yazma açıkken harf sayfası yalnızca !?# tuşuyla değişir.",
    ),
    "values-sv": (
        "Vad den kombinerade tangenten i nedersta raden gör: gå till sidan med specialtecken (!?#), öppna emoji-panelen eller ingenting - tangenten försvinner och dess plats förblir tom (D-507).",
        "Svep åt vänster eller höger över tangentbordet för att växla mellan bokstavssidan och symbolsidorna. När av växlar endast sidotangenterna (!?# / ?123 / ABC). Med glide-typing på byter bokstavssidan sida endast via !?#-tangenten.",
    ),
    "values-nb": (
        "Hva den kombinerte tasten i nederste rad gjør: gå til siden med spesialtegn (!?#), åpne emoji-panelet eller ingenting - tasten forsvinner og plassen står tom (D-507).",
        "Sveip til venstre eller høyre over tastaturet for å bytte mellom bokstavsiden og symbolsidene. Når av, bytter bare sidetastene (!?# / ?123 / ABC). Med glide-tasting på bytter bokstavsiden kun via !?#-tasten.",
    ),
    "values-da": (
        "Hvad den kombinerede tast i nederste række gør: gå til siden med specialtegn (!?#), åbne emoji-panelet eller intet - tasten forsvinder, og pladsen forbliver tom (D-507).",
        "Stryg til venstre eller højre hen over tastaturet for at skifte mellem bogstavssiden og symbolsiderne. Når slået fra, skifter kun sidetasterne (!?# / ?123 / ABC). Med glide-indtastning til skifter bogstavssiden kun via !?#-tasten.",
    ),
    "values-fi": (
        "Mitä alimmalla rivillä oleva yhdistetty näppäin tekee: siirtyminen erikoismerkkien sivulle (!?#), emoji-paneelin avaaminen tai ei mitään - näppäin katoaa ja sen paikka jää tyhjäksi (D-507).",
        "Pyyhkäise näppäimistön yli vasemmalle tai oikealle vaihtaaksesi kirjainsivun ja symbolisivujen välillä. Kun pois päältä, vain sivunäppäimet (!?# / ?123 / ABC) vaihtavat. Kun liukukirjoitus on käytössä, kirjainsivu vaihtuu vain !?#-näppäimellä.",
    ),
    "values-cs": (
        "Co dělá kombinovaná klávesa v dolní řadě: přechod na stránku speciálních znaků (!?#), otevření panelu emoji nebo nic - klávesa zmizí a její místo zůstane prázdné (D-507).",
        "Prejetím doleva nebo doprava přes klávesnici přepnete mezi stránkou písmen a stránkami symbolů. Když je vypnuto, přepínají pouze klávesy stránek (!?# / ?123 / ABC). Když je tažení prstem zapnuté, stránka písmen se přepíná pouze klávesou !?#.",
    ),
    "values-sk": (
        "Čo robí kombinovaná klávesa v dolnom rade: prechod na stránku špeciálnych znakov (!?#), otvorenie panela emoji alebo nič - klávesa zmizne a jej miesto zostane prázdne (D-507).",
        "Potiahnutím doľava alebo doprava cez klávesnicu prepnete medzi stránkou písmen a stránkami symbolov. Keď je vypnuté, prepínajú iba klávesy stránok (!?# / ?123 / ABC). Keď je ťahanie prstom zapnuté, stránka písmen sa prepína iba klávesou !?#.",
    ),
    "values-hu": (
        "Mit tesz az alsó sorban lévő kombinált billentyű: ugrás a speciális karakterek oldalára (!?#), az emoji-panel megnyitása vagy semmi - a billentyű eltűnik, a helye üresen marad (D-507).",
        "Csúsztasson balra vagy jobbra a billentyűzeten a betűoldal és a szimbólumoldalak közötti váltáshoz. Kikapcsolva csak az oldalgombok (!?# / ?123 / ABC) váltanak. Csúszó gépelés mellett a betűoldal csak a !?# gombbal vált.",
    ),
    "values-ro": (
        "Ce face tasta combinată din rândul de jos: trecerea la pagina de caractere speciale (!?#), deschiderea panoului de emoji sau nimic - tasta dispare, iar locul ei rămâne gol (D-507).",
        "Glisați la stânga sau la dreapta pe tastatură pentru a comuta între pagina de litere și paginile de simboluri. Când este dezactivat, comută doar tastele de pagină (!?# / ?123 / ABC). Cu tastarea prin glisare activă, pagina de litere comută doar prin tasta !?#.",
    ),
    "values-hr": (
        "Što kombinirana tipka u donjem redu radi: prelazak na stranicu posebnih znakova (!?#), otvaranje ploče emojija ili ništa - tipka nestane, a njezino mjesto ostaje prazno (D-507).",
        "Prevucite lijevo ili desno preko tipkovnice za prelazak između stranice sa slovima i stranica simbola. Kada je isključeno, prebacuju samo tipke stranica (!?# / ?123 / ABC). Kad je klizno tipkanje uključeno, stranica slova mijenja se samo tipkom !?#.",
    ),
    "values-bs": (
        "Šta kombinovani taster u donjem redu radi: prelazak na stranicu posebnih znakova (!?#), otvaranje panela emojija ili ništa - taster nestane, a njegovo mjesto ostane prazno (D-507).",
        "Prevucite lijevo ili desno preko tastature za prelazak između stranice sa slovima i stranica simbola. Kada je isključeno, prebacuju samo tipke stranica (!?# / ?123 / ABC). Kad je klizno kucanje uključeno, stranica slova mijenja se samo tipkom !?#.",
    ),
    "values-sr": (
        "Шта комбиновани тастер у доњем реду ради: прелазак на страницу посебних знакова (!?#), отварање панела емођија или ништа - тастер нестане, а његово место остане празно (D-507).",
        "Превуците лево или десно преко тастатуре за прелазак између стране са словима и страна симбола. Када је искључено, пребацују само тастери страница (!?# / ?123 / ABC). Кад је клизно куцање укључено, страна слова мења се само тастером !?#.",
    ),
    "values-et": (
        "Mida teeb alumisel real olev kombineeritud klahv: üleminek erimärkide lehele (!?#), emoji-paneeli avamine või mitte midagi - klahv kaob ja selle koht jääb tühjaks (D-507).",
        "Pühi üle klaviatuuri vasakule või paremale, et vahetada tähtede lehe ja sümbolilehtede vahel. Kui väljas, vahetavad ainult leheklahvid (!?# / ?123 / ABC). Kui libisev kirjutamine on sees, vahetub tähtede leht ainult klahviga !?#.",
    ),
    "values-lv": (
        "Ko apvienotais taustiņš apakšējā rindā dara: pāreja uz īpašo zīmju lapu (!?#), emocijzīmju paneļa atvēršana vai nekas - taustiņš pazūd un tā vieta paliek tukša (D-507).",
        "Velciet pa kreisi vai pa labi pāri tastatūrai, lai pārslēgtos starp burtu lapu un simbolu lapām. Ja izslēgts, pārslēdz tikai lapu taustiņi (!?# / ?123 / ABC). Kad slīdošā rakstīšana ir ieslēgta, burtu lapa mainās tikai ar taustiņu !?#.",
    ),
    "values-lt": (
        "Ką kombinuotas klavišas apatinėje eilėje daro: perjungimas į specialiųjų simbolių puslapį (!?#), emoji skydelio atvėrimas arba nieko - klavišas išnyksta, o jo vieta lieka tuščia (D-507).",
        "Braukite kairėn arba dešinėn per klaviatūrą, kad perjungtumėte raidžių puslapį ir simbolių puslapius. Išjungus perjungia tik puslapių klavišai (!?# / ?123 / ABC). Kai slydantis rašymas įjungtas, raidžių puslapis keičiasi tik klavišu !?#.",
    ),
    "values-id": (
        "Apa yang dilakukan tombol gabungan di baris bawah: beralih ke halaman karakter khusus (!?#), membuka panel emoji, atau tidak sama sekali - tombolnya hilang dan tempatnya tetap kosong (D-507).",
        "Geser ke kiri atau kanan di keyboard untuk beralih antara halaman huruf dan halaman simbol. Jika nonaktif, hanya tombol halaman (!?# / ?123 / ABC) yang beralih. Saat mengetik geser aktif, halaman huruf hanya beralih melalui tombol !?#.",
    ),
    "values-ms": (
        "Apa yang kekunci gabungan dalam baris bawah buat: bertukar ke halaman aksara khas (!?#), membuka panel emoji, atau tiada - kekunci itu hilang dan tempatnya kekal kosong (D-507).",
        "Leret ke kiri atau kanan pada papan kekunci untuk bertukar antara halaman huruf dan halaman simbol. Apabila dimatikan, hanya kekunci halaman (!?# / ?123 / ABC) yang menukar. Semasa taipan luncur aktif, halaman huruf hanya bertukar melalui kekunci !?#.",
    ),
    "values-sw": (
        "Kitufe cha pamoja kwenye safu ya chini kinafanya nini: kwenda ukurasa wa alama maalum (!?#), kufungua paneli ya emoji, au hakuna - kitufe kinatoweka na nafasi yake inabaki wazi (D-507).",
        "Telezesha kushoto au kulia kwenye kibodi ili kubadilisha kati ya ukurasa wa herufi na kurasa za alama. Ikizimwa, vitufe vya kurasa pekee ndivyo hubadilisha (!?# / ?123 / ABC). Kuchapisha kwa kusogeza kikiwa kimewashwa, ukurasa wa herufi hubadilika kwa kitufe cha !?# pekee.",
    ),
    "values-fil": (
        "Ano ang ginagawa ng pinagsamang pindutan sa pinakailalim na hanay: paglipat sa pahina ng mga espesyal na karakter (!?#), pagbukas ng emoji panel, o wala - mawawala ang pindutan at mananatiling bakante ang puwang nito (D-507).",
        "Mag-swipe pakaliwa o pakanan sa keyboard para lumipat sa pagitan ng pahina ng mga titik at ng mga pahina ng simbolo. Kapag naka-off, ang mga page key lang (!?# / ?123 / ABC) ang lumilipat. Kapag naka-on ang pag-slide sa pag-type, ang pahina ng mga titik ay nagpapalit lamang sa !?# key.",
    ),
    "values-ru": (
        "Что делает комбинированная клавиша в нижнем ряду: переход на страницу специальных символов (!?#), открытие панели эмодзи или ничего - клавиша исчезает, а её место остаётся пустым (D-507).",
        "Проведите пальцем влево или вправо по клавиатуре, чтобы переключаться между страницей букв и страницами символов. Когда выключено, переключают только клавиши страниц (!?# / ?123 / ABC). При включённом непрерывном вводе страница букв переключается только клавишей !?#.",
    ),
    "values-uk": (
        "Що робить комбінована клавіша в нижньому ряду: перехід на сторінку спеціальних символів (!?#), відкриття панелі емодзі або нічого - клавіша зникає, а її місце залишається порожнім (D-507).",
        "Проведіть пальцем вліво або вправо по клавіатурі, щоб перемикатися між сторінкою літер і сторінками символів. Коли вимкнено, перемикають лише клавіші сторінок (!?# / ?123 / ABC). При увімкненому неперервному вводі сторінка літер перемикається лише клавішею !?#.",
    ),
    "values-az": (
        "Aşağı sıradakı birləşdirilmiş düymə nə edir: xüsusi işarələr səhifəsinə keçid (!?#), emoji panelını açmaq və ya heç nə - düymə yox olur və yeri boş qalır (D-507).",
        "Hərflər səhifəsi ilə simvol səhifələri arasında keçmək üçün klaviatura üzərində sola və ya sağa sürüşdürün. Söndürüləndə yalnız səhifə düymələri (!?# / ?123 / ABC) keçid edir. Sürüşərək yazma açık olduqda hərf səhifəsi yalnız !?# düyməsi ilə dəyişir.",
    ),
    "values-uz": (
        "Pastki qatordagi birlashtirilgan tugma nima qiladi: maxsus belgilar sahifasiga o\\'tish (!?#), emoji panelini ochish yoki hech narsa - tugma yo\\'qoladi va o\\'rni bo\\'sh qaladi (D-507).",
        "Harflar sahifasi va belgilar sahifalari orasida o\\'tish uchun klaviatura bo\\'ylab chapga yoki o\\'ngga suring. O\\'chirilganda faqat sahifa tugmalari (!?# / ?123 / ABC) almashtiradi. Sirishtirib yozish yoqilganda harflar sahifasi faqat !?# tugmasi orqali almashadi.",
    ),
}

# Keys whose only change is the key's own name: "?123" -> "!?#" (and the escaped "\?123" variant
# some locales use at string start).
KEY_NAME_ONLY = (
    "d507_combined_key_title",
    "d507_mode_symbols",
    "d516_glide_typing_summary",
    "d89_f08_desc",
)


def replace_string_value(text: str, name: str, new_value: str, locale: str) -> str:
    """Replaces the exact value of one <string name="..."> entry, idempotently."""
    marker = f'<string name="{name}">'
    start = text.find(marker)
    assert start >= 0, f"{locale}: {name} not found"
    value_start = start + len(marker)
    end = text.find("</string>", value_start)
    assert end >= 0, f"{locale}: {name} not closed"
    if text[value_start:end] == new_value:
        print(f"{locale}: {name} already up to date, skipping")
        return text
    return text[:value_start] + new_value + text[end:]


for locale, (d507_summary, d19_summary) in REWRITES.items():
    path = RES / locale / "strings.xml"
    text = path.read_text(encoding="utf-8")

    text = replace_string_value(text, "d507_combined_key_summary", d507_summary, locale)
    text = replace_string_value(text, "d19_surface_swipe_summary", d19_summary, locale)

    for name in KEY_NAME_ONLY:
        marker = f'<string name="{name}">'
        start = text.find(marker)
        assert start >= 0, f"{locale}: {name} not found"
        value_start = start + len(marker)
        end = text.find("</string>", value_start)
        value = text[value_start:end]
        updated = value.replace("\\?123", "!?#").replace("?123", "!?#")
        if updated != value:
            text = text[:value_start] + updated + text[end:]
            print(f"{locale}: {name} key name updated")
        else:
            print(f"{locale}: {name} already up to date, skipping")

    path.write_text(text, encoding="utf-8", newline="\n")

print("done")
