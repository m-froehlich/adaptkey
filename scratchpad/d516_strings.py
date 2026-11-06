# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Froehlich Media
#
# D-516/G-09 (R2) string pass: glide typing - the d516 switch title/summary, the d19 summary's
# glide hint sentence (appended), and the D-89 feature-catalog entry f39, for the English reference
# plus all 31 translation directories (per the standing D-478 rule; drafts). Idempotent per value.
# Run from the repo root: python scratchpad/d516_strings.py

from pathlib import Path
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")

RES = Path("app/src/main/res")

# locale -> (glide title, glide summary, d19 hint sentence, f39 title, f39 desc)
STRINGS = {
    "values": (
        "Glide typing",
        "Draw a word in one continuous stroke across the letters to type it. Off by default; while on, the letters page switches pages only via the ?123 key.",
        "With glide typing on, the letters page switches pages only via the ?123 key.",
        "Glide typing",
        "Optional: draw a word in one continuous stroke across the letters, and the best match is typed for you - with dictionary, diacritic folding and personal context. Off by default.",
    ),
    "values-de": (
        "Gesteeneingabe (Glide-Typing)",
        "Ein Wort in einem durchgehenden Strich über die Buchstaben ziehen, um es zu schreiben. Standardmäßig aus; bei eingeschalteter Gesteeneingabe schaltet die Buchstaben-Seite nur noch über die ?123-Taste.",
        "Bei aktiver Gesteeneingabe (Glide-Typing) schaltet auf der Buchstaben-Seite nur die ?123-Taste.",
        "Gesteeneingabe (Glide-Typing)",
        "Optional: Ein Wort in einem durchgehenden Strich über die Buchstaben ziehen - der beste Treffer wird geschrieben, mit Wörterbuch, Umlaut-Folding und persönlichem Kontext. Standardmäßig aus.",
    ),
    "values-el": (
        "Χειρονομική πληκτρολόγηση (glide typing)",
        "Σχεδιάστε μια λέξη με ένα συνεχές τόξο πάνω από τα γράμματα για να τη γράψετε. Απενεργοποιημένη από προεπιλογή· όταν είναι ενεργή, η σελίδα γραμμάτων αλλάζει μόνο μέσω του πλήκτρου ?123.",
        "Με ενεργή τη χειρονομική πληκτρολόγηση, η σελίδα γραμμάτων αλλάζει μόνο μέσω του πλήκτρου ?123.",
        "Χειρονομική πληκτρολόγηση (glide typing)",
        "Προαιρετικό: σχεδιάστε μια λέξη με ένα συνεχές τόξο πάνω από τα γράμματα - η καλύτερη αντιστοίχιση γράφεται για εσάς, με λεξικό, αναδίπλωση διακριτικών και προσωπικό πλαίσιο. Απενεργοποιημένη από προεπιλογή.",
    ),
    "values-fr": (
        "Saisie gestuelle (glide typing)",
        "Tracez un mot en un seul trait continu sur les lettres pour l'écrire. Désactivée par défaut ; lorsqu'elle est active, la page des lettres ne change que via la touche ?123.",
        "Avec la saisie gestuelle active, la page des lettres ne change que via la touche ?123.",
        "Saisie gestuelle (glide typing)",
        "Optionnel : tracez un mot en un seul trait continu sur les lettres - la meilleure correspondance est tapée pour vous, avec dictionnaire, repli des diacritiques et contexte personnel. Désactivée par défaut.",
    ),
    "values-es": (
        "Escritura gestual (glide typing)",
        "Dibuja una palabra en un solo trazo continuo sobre las letras para escribirla. Desactivada por defecto; cuando está activada, la página de letras solo cambia mediante la tecla ?123.",
        "Con la escritura gestual activada, la página de letras solo cambia mediante la tecla ?123.",
        "Escritura gestual (glide typing)",
        "Opcional: dibuja una palabra en un solo trazo continuo sobre las letras y se escribe la mejor coincidencia, con diccionario, plegado de diacríticos y contexto personal. Desactivada por defecto.",
    ),
    "values-it": (
        "Scrittura gestuale (glide typing)",
        "Disegna una parola con un unico tratto continuo sulle lettere per scriverla. Disattivata per impostazione predefinita; quando attiva, la pagina delle lettere cambia solo tramite il tasto ?123.",
        "Con la scrittura gestuale attiva, la pagina delle lettere cambia solo tramite il tasto ?123.",
        "Scrittura gestuale (glide typing)",
        "Opzionale: disegna una parola con un unico tratto continuo sulle lettere - la corrispondenza migliore viene scritta per te, con dizionario, riporto dei diacritici e contesto personale. Disattivata per impostazione predefinita.",
    ),
    "values-pt": (
        "Escrita por gestos (glide typing)",
        "Desenhe uma palavra num único traço contínuo sobre as letras para a escrever. Desativado por predefinição; quando ativo, a página de letras só muda através da tecla ?123.",
        "Com a escrita por gestos ativa, a página de letras só muda através da tecla ?123.",
        "Escrita por gestos (glide typing)",
        "Opcional: desenhe uma palavra num único traço contínuo sobre as letras - a melhor correspondência é escrita por si, com dicionário, repliegue de diacríticos e contexto pessoal. Desativado por predefinição.",
    ),
    "values-nl": (
        "Glide-typen (veegtypen)",
        "Trek een woord in één doorlopende lijn over de letters om het te typen. Standaard uit; wanneer ingeschakeld, wisselt de letterspagina alleen via de ?123-toets.",
        "Met glide-typen ingeschakeld wisselt de letterspagina alleen via de ?123-toets.",
        "Glide-typen (veegtypen)",
        "Optioneel: trek een woord in één doorlopende lijn over de letters - de beste match wordt voor je getypt, met woordenboek, diakrieten-samenvoeging en persoonlijke context. Standaard uit.",
    ),
    "values-pl": (
        "Pisanie gestami (glide typing)",
        "Przesuń palcem po literach jednym ciągiem, aby napisać słowo. Domyślnie wyłączone; gdy włączone, strona liter zmienia się tylko klawiszem ?123.",
        "Gdy pisanie gestami jest włączone, strona liter zmienia się tylko klawiszem ?123.",
        "Pisanie gestami (glide typing)",
        "Opcjonalnie: przesuń palcem po literach jednym ciągiem - najlepsze dopasowanie zostanie wpisane za ciebie, ze słownikiem, odwzorowaniem diakrytyków i osobistym kontekstem. Domyślnie wyłączone.",
    ),
    "values-tr": (
        "Kaydırarak yazma (glide typing)",
        "Bir kelimeyi harflerin üzerinde kesintisiz bir çizgiyle çizin ve yazılsın. Varsayılan olarak kapalı; açıkken harf sayfası yalnızca ?123 tuşuyla değişir.",
        "Kaydırarak yazma açıkken harf sayfası yalnızca ?123 tuşuyla değişir.",
        "Kaydırarak yazma (glide typing)",
        "İsteğe bağlı: bir kelimeyi harflerin üzerinde kesintisiz bir çizgiyle çizin - en iyi eşleşme sizin adınıza yazılır; sözlük, diakritik eşleme ve kişisel bağlam dahil. Varsayılan olarak kapalı.",
    ),
    "values-sv": (
        "Glide-typing (svepskrivning)",
        "Rita ett ord i ett sammanhängande stråk över bokstäverna för att skriva det. Av som standard; när det är på byter bokstavssidan sida endast via ?123-tangenten.",
        "Med glide-typing på byter bokstavssidan sida endast via ?123-tangenten.",
        "Glide-typing (svepskrivning)",
        "Valfritt: rita ett ord i ett sammanhängande stråk över bokstäverna - bästa träffen skrivs åt dig, med ordbok, diakritisk sammanfogning och personlig kontext. Av som standard.",
    ),
    "values-nb": (
        "Glide-tasting (sveveskriving)",
        "Tegn et ord i én sammenhengende bevegelse over bokstavene for å skrive det. Av som standard; når på, bytter bokstavsiden kun via ?123-tasten.",
        "Med glide-tasting på bytter bokstavsiden kun via ?123-tasten.",
        "Glide-tasting (sveveskriving)",
        "Valgfritt: tegn et ord i én sammenhengende bevegelse over bokstavene - best treff skrives for deg, med ordbok, diakritisk sammenslåing og personlig kontekst. Av som standard.",
    ),
    "values-da": (
        "Glide-indtastning (strøgskrivning)",
        "Tegn et ord i én sammenhængende bevægelse over bogstaverne for at skrive det. Fra som standard; når til, skifter bogstavssiden kun via ?123-tasten.",
        "Med glide-indtastning til skifter bogstavssiden kun via ?123-tasten.",
        "Glide-indtastning (strøgskrivning)",
        "Valgfrit: tegn et ord i én sammenhængende bevægelse over bogstaverne - det bedste match skrives for dig, med ordbog, diakritisk sammenfoldning og personlig kontekst. Fra som standard.",
    ),
    "values-fi": (
        "Liukukirjoitus (glide typing)",
        "Piirrä sana yhtenäisenä viivana kirjainten yli kirjoittaaksesi sen. Oletuksena pois; kun käytössä, kirjainsivu vaihtuu vain ?123-näppäimellä.",
        "Kun liukukirjoitus on käytössä, kirjainsivu vaihtuu vain ?123-näppäimellä.",
        "Liukukirjoitus (glide typing)",
        "Valinnainen: piirrä sana yhtenäisenä viivana kirjainten yli - paras osuma kirjoitetaan puolestasi, sanakirja, diakriittien taitto ja henkilökohtainen konteksti mukaan lukien. Oletuksena pois.",
    ),
    "values-cs": (
        "Tažení prstem (glide typing)",
        "Nakreslete slovo jedním souvislým tahem přes písmena a bude napsáno. Ve výchozím stavu vypnuto; při zapnutí se stránka písmen přepíná pouze klávesou ?123.",
        "Když je tažení prstem zapnuté, stránka písmen se přepíná pouze klávesou ?123.",
        "Tažení prstem (glide typing)",
        "Volitelné: nakreslete slovo jedním souvislým tahem přes písmena - nejlepší shoda se napíše za vás, včetně slovníku, skládání diakritiky a osobního kontextu. Ve výchozím stavu vypnuto.",
    ),
    "values-sk": (
        "Ťahanie prstom (glide typing)",
        "Nakreslite slovo jedným súvislým ťahom cez písmená a bude napísané. Predvolene vypnuté; pri zapnutí sa stránka písmen prepína iba klávesou ?123.",
        "Keď je ťahanie prstom zapnuté, stránka písmen sa prepína iba klávesou ?123.",
        "Ťahanie prstom (glide typing)",
        "Voliteľné: nakreslite slovo jedným súvislým ťahom cez písmená - najlepšia zhoda sa napíše za vás, vrátane slovníka, skladania diakritiky a osobného kontextu. Predvolene vypnuté.",
    ),
    "values-hu": (
        "Csúszó gépelés (glide typing)",
        "Rajzolj egy szót egy folyamatos vonallal a betűk fölött, és beíródik. Alapból ki; bekapcsolva a betűoldal csak a ?123 gombbal vált.",
        "Csúszó gépelés mellett a betűoldal csak a ?123 gombbal vált.",
        "Csúszó gépelés (glide typing)",
        "Opcionális: rajzolj egy szót egy folyamatos vonallal a betűk fölött - a legjobb találat beíródik helyetted, szótárral, ékezet-hajtogatással és személyes kontextussal. Alapból ki.",
    ),
    "values-ro": (
        "Tastare prin glisare (glide typing)",
        "Desenează un cuvânt într-o singură linie continuă peste litere pentru a-l scrie. Dezactivat în mod implicit; când este activ, pagina de litere comută doar prin tasta ?123.",
        "Cu tastarea prin glisare activă, pagina de litere comută doar prin tasta ?123.",
        "Tastare prin glisare (glide typing)",
        "Opțional: desenează un cuvânt într-o singură linie continuă peste litere - cea mai bună potrivire se scrie pentru tine, cu dicționar, replierea diacriticelor și context personal. Dezactivat în mod implicit.",
    ),
    "values-hr": (
        "Klizno tipkanje (glide typing)",
        "Nacrtaj riječ jednim povezanim pokretom preko slova i bit će upisana. Zadano isključeno; kad je uključeno, stranica slova mijenja se samo tipkom ?123.",
        "Kad je klizno tipkanje uključeno, stranica slova mijenja se samo tipkom ?123.",
        "Klizno tipkanje (glide typing)",
        "Neobavezno: nacrtaj riječ jednim povezanim pokretom preko slova - najbolja podudarnost upisuje se umjesto tebe, uz rječnik, preslikavanje dijakritika i osobni kontekst. Zadano isključeno.",
    ),
    "values-bs": (
        "Klizno kucanje (glide typing)",
        "Nacrtaj riječ jednim povezanim pokretom preko slova i biće upisana. Podrazumijevano isključeno; kad je uključeno, stranica slova mijenja se samo tipkom ?123.",
        "Kad je klizno kucanje uključeno, stranica slova mijenja se samo tipkom ?123.",
        "Klizno kucanje (glide typing)",
        "Neobavezno: nacrtaj riječ jednim povezanim pokretom preko slova - najbolja podudarnost upisuje se umjesto tebe, uz rječnik, preslikavanje dijakritika i lični kontekst. Podrazumijevano isključeno.",
    ),
    "values-sr": (
        "Клизно куцање (glide typing)",
        "Нацртај реч једним повезаним покретом преко слова и биће уписана. Подразумевано искључено; кад је укључено, страна слова мења се само тастером ?123.",
        "Кад је клизно куцање укључено, страна слова мења се само тастером ?123.",
        "Клизно куцање (glide typing)",
        "Опционо: нацртај реч једним повезаним покретом преко слова - најбоље поклапање уписује се уместо тебе, уз речник, пресликавање дијакритика и лични контекст. Подразумевано искључено.",
    ),
    "values-et": (
        "Libisev kirjutamine (glide typing)",
        "Joonista sõna ühe katkematu joonega tähtede peale ja see kirjutatakse. Vaikimisi väljas; kui sees, vahetub tähtede leht ainult klahviga ?123.",
        "Kui libisev kirjutamine on sees, vahetub tähtede leht ainult klahviga ?123.",
        "Libisev kirjutamine (glide typing)",
        "Valikuline: joonista sõna ühe katkematu joonega tähtede peale - parim vaste kirjutatakse sinu eest, koos sõnaraamatu, diakriitika-teisenduse ja isikliku kontekstiga. Vaikimisi väljas.",
    ),
    "values-lv": (
        "Slīdošā rakstīšana (glide typing)",
        "Zīmē vārdu ar vienu nepārtrauktu līniju pār burtiem, un tas tiks uzrakstīts. Pēc noklusējuma izslēgts; kad ieslēgts, burtu lapa mainās tikai ar taustiņu ?123.",
        "Kad slīdošā rakstīšana ir ieslēgta, burtu lapa mainās tikai ar taustiņu ?123.",
        "Slīdošā rakstīšana (glide typing)",
        "Neobligāti: zīmē vārdu ar vienu nepārtrauktu līniju pār burtiem - labākā atbilstība tiek uzrakstīta jūsu vietā, ar vārdnīcu, diakritisko zīmju savienošanu un personīgo kontekstu. Pēc noklusējuma izslēgts.",
    ),
    "values-lt": (
        "Slydantis rašymas (glide typing)",
        "Nupiešk žodį viena nenutrūkstama linija per raides ir jis bus parašytas. Pagal nutylėjimą išjungta; įjungus raidžių puslapis keičiasi tik klavišu ?123.",
        "Kai slydantis rašymas įjungtas, raidžių puslapis keičiasi tik klavišu ?123.",
        "Slydantis rašymas (glide typing)",
        "Nebūtina: nupiešk žodį viena nenutrūkstama linija per raides - geriausias atitikmušas parašomas už tave, su žodynu, diakritinių ženklų sudengimu ir asmeniniu kontekstu. Pagal nutylėjimą išjungta.",
    ),
    "values-id": (
        "Mengetik geser (glide typing)",
        "Gambar sebuah kata dalam satu garisan menerus di atas huruf untuk mengetiknya. Dimatikan secara bawaan; saat aktif, halaman huruf hanya beralih melalui tombol ?123.",
        "Saat mengetik geser aktif, halaman huruf hanya beralih melalui tombol ?123.",
        "Mengetik geser (glide typing)",
        "Opsional: gambar sebuah kata dalam satu garisan menerus di atas huruf - padanan terbaik diketik untukmu, dengan kamus, pelipatan diakritik dan konteks pribadi. Dimatikan secara bawaan.",
    ),
    "values-ms": (
        "Taipan luncur (glide typing)",
        "Lukis satu perkataan dalam satu garisan berterusan merentas huruf untuk menaipnya. Dimatikan secara lalai; semasa aktif, halaman huruf hanya bertukar melalui kekunci ?123.",
        "Semasa taipan luncur aktif, halaman huruf hanya bertukar melalui kekunci ?123.",
        "Taipan luncur (glide typing)",
        "Pilihan: lukis satu perkataan dalam satu garisan berterusan merentas huruf - padanan terbaik ditaip untuk anda, dengan kamus, pelipatan diakritik dan konteks peribadi. Dimatikan secara lalai.",
    ),
    "values-sw": (
        "Kuchapisha kwa kusogeza (glide typing)",
        "Chora neno kwa mstari mmoja unaoendelea juu ya herufi ili uandike. Imezimwa kwa chaguo-msingi; ikiwa imewashwa, ukurasa wa herufi hubadilika kwa kitufe cha ?123 pekee.",
        "Kuchapisha kwa kusogeza kikiwa kimewashwa, ukurasa wa herufi hubadilika kwa kitufe cha ?123 pekee.",
        "Kuchapisha kwa kusogeza (glide typing)",
        "Hiari: chora neno kwa mstari mmoja unaoendelea juu ya herufi - maelezano bora huandikwa kwa niaba yako, pamoja na kamusi, kuunganisha alama za sauti na muktadha wako binafsi. Imezimwa kwa chaguo-msingi.",
    ),
    "values-fil": (
        "Pag-slide sa pag-type (glide typing)",
        "Guhit ang salita sa isang tuloy-tuloy na linya sa mga titik para i-type ito. Nakasara bilang default; kapag naka-on, ang pahina ng mga titik ay nagpapalit lamang sa ?123 key.",
        "Kapag naka-on ang pag-slide sa pag-type, ang pahina ng mga titik ay nagpapalit lamang sa ?123 key.",
        "Pag-slide sa pag-type (glide typing)",
        "Opsyonal: guhit ang salita sa isang tuloy-tuloy na linya sa mga titik - ang pinakamahusay na tugma ay mai-type para sa iyo, kasama ang diksyunaryo, pag-tiklop ng mga diakritiko at personal na konteksto. Nakasara bilang default.",
    ),
    "values-ru": (
        "Непрерывный ввод (glide typing)",
        "Проведите слово одной непрерывной линией по буквам, чтобы его набрать. По умолчанию выключено; когда включено, страница букв переключается только клавишей ?123.",
        "При включённом непрерывном вводе страница букв переключается только клавишей ?123.",
        "Непрерывный ввод (glide typing)",
        "Необязательно: проведите слово одной непрерывной линией по буквам — лучшее совпадение будет набрано за вас, с учётом словаря, диакритического схлопывания и личного контекста. По умолчанию выключено.",
    ),
    "values-uk": (
        "Неперервний ввід (glide typing)",
        "Накресліть слово однією суцільною лінією по літерах, щоб набрати його. Типово вимкнено; коли увімкнено, сторінка літер перемикається лише клавішею ?123.",
        "При увімкненому неперервному вводі сторінка літер перемикається лише клавішею ?123.",
        "Неперервний ввід (glide typing)",
        "Необов'язково: накресліть слово однією суцільною лінією по літерах — найкращий збіг буде набрано за вас, із словником, діакритичним згортанням та особистим контекстом. Типово вимкнено.",
    ),
    "values-az": (
        "Sürüşərək yazma (glide typing)",
        "Sözu hərflər üzərində fasiləsiz bir xəttlə çizin və yazılsın. Standart olaraq bağlı; açıq olduqda hərf səhifəsi yalnız ?123 düyməsi ilə dəyişir.",
        "Sürüşərək yazma açıq olduqda hərf səhifəsi yalnız ?123 düyməsi ilə dəyişir.",
        "Sürüşərək yazma (glide typing)",
        "İstəyə bağlı: sözu hərflər üzərində fasiləsiz bir xəttlə çizin - ən yaxşı uyğunluq sizin əvəzinizə yazılır; lüğət, diakritik uyğunlaşdırma və şəxsi kontekst daxil olmaqla. Standart olaraq bağlı.",
    ),
    "values-uz": (
        "Sirishtirib yozish (glide typing)",
        "So'zni harflar ustidan uzluksiz bir chiziq bilan chizing va yoziladi. Standart bo'yicha o'chirilgan; yoqilganda harflar sahifasi faqat ?123 tugmasi orqali almashadi.",
        "Sirishtirib yozish yoqilganda harflar sahifasi faqat ?123 tugmasi orqali almashadi.",
        "Sirishtirib yozish (glide typing)",
        "Ixtiyoriy: so'zni harflar ustidan uzluksiz bir chiziq bilan chizing - eng yaxshi moslik siz uchun yoziladi; lug'at, diakritik moslashtirish va shaxsiy kontekst bilan. Standart bo'yicha o'chirilgan.",
    ),
}


def escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


for locale, (title, summary, d19_hint, f39_title, f39_desc) in STRINGS.items():
    path = RES / locale / "strings.xml"
    text = path.read_text(encoding="utf-8")

    # 1. the d19 summary hint sentence - appended to the existing value (§5/§8.5: summary hint,
    #    no graying-out, no flag copying).
    pattern = re.compile(r'(<string name="d19_surface_swipe_summary">)(.*?)(</string>)', re.S)
    match = pattern.search(text)
    assert match, f"{locale}: d19_surface_swipe_summary not found"
    if d19_hint[:30] in match.group(2):
        print(f"{locale}: d19 hint already present, skipping")
    else:
        replacement = match.group(1) + match.group(2).rstrip() + " " + escape(d19_hint) + match.group(3)
        text = text[: match.start()] + replacement + text[match.end():]
        print(f"{locale}: d19 hint appended")

    # 2. the d516 switch strings and the f39 catalog entry.
    additions = ""
    if "d516_glide_typing_title" not in text:
        additions += (
            f'    <string name="d516_glide_typing_title">{escape(title)}</string>\n'
            f'    <string name="d516_glide_typing_summary">{escape(summary)}</string>\n'
        )
    if "d89_f39_title" not in text:
        additions += (
            f'    <string name="d89_f39_title">{escape(f39_title)}</string>\n'
            f'    <string name="d89_f39_desc">{escape(f39_desc)}</string>\n'
        )
    if additions:
        assert "</resources>" in text, f"{locale}: no closing resources tag"
        text = text.replace("</resources>", additions + "</resources>")
        print(f"{locale}: d516/f39 strings added")
    path.write_text(text, encoding="utf-8", newline="\n")

print("done")
