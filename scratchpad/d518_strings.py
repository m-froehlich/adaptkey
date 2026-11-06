# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Froehlich Media
#
# D-518 string pass: the "Was AdaptKey alles kann" page's trailing note (d89_footer_note) - a
# deliberate afterword stating that, since the Alt-Belegungs-Reform, the letters page makes
# practically all relevant characters directly available, so everyday typing never needs the
# special-character pages. English reference plus all 31 translation directories (per the standing
# D-478 rule; drafts). Idempotent. Run from the repo root: python scratchpad/d518_strings.py

from pathlib import Path
import sys

sys.stdout.reconfigure(encoding="utf-8")

RES = Path("app/src/main/res")

STRINGS = {
    "values": "Note: since the alternative key-assignment reform, the letters page makes practically all relevant characters directly available - dual-corner keys, the digits row with its own symbols, and the long-press popups included - so everyday typing never needs a trip to the special-character pages.",
    "values-de": "Nachtrag: Seit der Alt-Belegungs-Reform macht die Buchstaben-Seite praktisch alle relevanten Zeichen direkt verfügbar - Doppel-Ecken-Tasten, die Ziffernleiste samt ihrer Symbole und die Langdruck-Popups eingeschlossen. Für den Alltag ist kein Zugriff auf die Sonderzeichen-Seiten notwendig.",
    "values-el": "Σημείωση: Μετά τη μεταρρύθμιση της εναλλακτικής διάταξης πλήκτρων, η σελίδα γραμμάτων διαθέτει σχεδόν όλους τους σχετικούς χαρακτήρες απευθείας - πλήκτρα με διπλές γωνίες, τη σειρά ψηφίων με τα σύμβολά της και τα αναδυόμενα παρατεταμένου πατήματος συμπεριλαμβανομένων - έτσι η καθημερινή πληκτρολόγηση δεν χρειάζεται μετάβαση στις σελίδες ειδικών χαρακτήρων.",
    "values-fr": "Remarque : depuis la réforme de l'attribution alternative des touches, la page des lettres rend pratiquement tous les caractères pertinents directement disponibles - touches à double coin, rangée de chiffres avec ses propres symboles et popups d'appui long compris - la frappe quotidienne n'a jamais besoin d'un détour par les pages de caractères spéciaux.",
    "values-es": "Nota: desde la reforma de asignación alternativa de teclas, la página de letras pone prácticamente todos los caracteres relevantes directamente disponibles - teclas de doble esquina, la fila de dígitos con sus propios símbolos y las ventanas emergentes de pulsación larga incluidas - el tecleo diario nunca necesita un pase por las páginas de caracteres especiales.",
    "values-it": "Nota: dalla riforma dell'assegnazione alternativa dei tasti, la pagina delle lettere rende praticamente tutti i caratteri rilevanti direttamente disponibili - tasti con doppio angolo, riga delle cifre con i propri simboli e popup di pressione prolungata inclusi - la digitazione quotidiana non ha mai bisogno di un passaggio dalle pagine dei caratteri speciali.",
    "values-pt": "Nota: desde a reforma da atribuição alternativa de teclas, a página de letras torna praticamente todos os caracteres relevantes diretamente disponíveis - teclas de canto duplo, fila de dígitos com os seus próprios símbolos e popups de premição longa incluídos - a escrita quotidiana nunca precisa de uma visita às páginas de caracteres especiais.",
    "values-nl": "Let op: sinds de alternatieve-toetsindelingshervorming biedt de letterspagina vrijwel alle relevante tekens direct - dubbelhoektoetsen, de cijferrij met eigen symbolen en de ingedrukt-houd-popups inbegrepen - alledaags typen heeft nooit een omweg naar de specialetekenspagina's nodig.",
    "values-pl": "Uwaga: od reformy alternatywnego przypisania klawiszy strona liter udostępnia praktycznie wszystkie istotne znaki bezpośrednio - klawisze z podwójnym narożnikiem, rząd cyfr z własnymi symbolami i wyskakujące okna długiego przytrzymania włącznie - codzienne pisanie nigdy nie potrzebuje wycieczki na strony znaków specjalnych.",
    "values-tr": "Not: alternatif tuş ataması reformundan bu yana harf sayfası ilgili karakterlerin neredeyse tamamını doğrudan sunar - çift köşeli tuşlar, kendi sembolleriyle rakam sırası ve uzun basma pencereleri dahil - günlük yazma için özel karakter sayfalarına uğramak hiç gerekmez.",
    "values-sv": "Obs: sedan den alternativa tangenttilldelningsreformen erbjuder bokstavssidan praktiskt taget alla relevanta tecken direkt - dubbelhörnstangenter, sifferaden med egna symboler och långtryckspopups inräknade - vardagligt skrivande behöver aldrig en omväg till specialteckensidorna.",
    "values-nb": "Merk: siden den alternative tastendreformsreformen tilbyr bokstavssiden praktisk talt alle relevante tegn direkte - dobbelthjørnetaster, sifferaden med egne symboler og langtrykksvinduer inkludert - hverdagens skriving trenger aldri en omvei til spesialtegn-sidene.",
    "values-da": "Bemærk: siden den alternative tastetildelingsreform tilbyder bogstavssiden praktisk talt alle relevante tegn direkte - dobbelthjørnetaster, cifferækken med egne symboler og langtrykspopups inkluderet - hverdagsskrivning behøver aldrig en omvej til specialtegn-siderne.",
    "values-fi": "Huom: vaihtoehtoisen näppäinjärjestelyn uudistuksen jälkeen kirjainsivu tarjoaa lähes kaikki oleelliset merkit suoraan - kaksoiskulmanäppäimet, numerorivi omine symboleineen ja pitkän painalluksen ponnahdusikkunat mukaan lukien - jokapäiväinen kirjoittaminen ei koskaan tarvitse käyntiä erikoismerkkisivuilla.",
    "values-cs": "Poznámka: od reformy alternativního přiřazení kláves stránka písmen zpřístupňuje prakticky všechny relevantní znaky přímo - klávesy s dvojitým rohem, řádek číslic s vlastními symboly a vyskakovací okna dlouhého stisku včetně - běžné psaní nikdy nepotřebuje výlet na stránky speciálních znaků.",
    "values-sk": "Poznámka: od reformy alternatívneho priradenia kláves stránka písmen sprístupňuje prakticky všetky relevantné znaky priamo - klávesy s dvojitým rohom, rad číslic s vlastnými symbolmi a vyskakovacie okná dlhého stlačenia vrátane - bežné písanie nikdy nepotrebuje výlet na stránky špeciálnych znakov.",
    "values-hu": "Megjegyzés: az alternatív billentyűhozzárendelés reformja óta a betűoldal a releváns karakterek gyakorlatilag mindegyikét közvetlenül elérhetővé teszi - duplasarkú billentyűk, a saját szimbólumokkal rendelkező numsor és a hosszú nyomás felugró ablakai is - a mindennapi gépelésnek soha nincs szüksége kitérésre a speciális karakterek oldalaira.",
    "values-ro": "Notă: de la reforma atribuirii alternative a tastelor, pagina de litere face practic toate caracterele relevante direct disponibile - taste cu colț dublu, rândul de cifre cu propriile simboluri și ferestrele pop-up de apăsare lungă incluse - tastarea zilnică nu are nevoie niciodată de o vizită la paginile de caractere speciale.",
    "values-hr": "Napomena: od reforme alternativne raspodjele tipki stranica slova čini praktički sve relevantne znakove izravno dostupnima - tipke s dvostrukim kutom, red znamenki s vlastitim simbolima i skočni prozori dugog pritiska uključeni - svakodnevno tipkanje nikada ne treba posjet stranicama posebnih znakova.",
    "values-bs": "Napomena: od reforme alternativne raspodjele tipki stranica slova čini praktički sve relevantne znakove direktno dostupnima - tipke s dvostrukim uglom, red brojeva sa svojim simbolima i iskačući prozori dugog pritiska uključeni - svakodnevno kucanje nikada ne treba posjet stranicama posebnih znakova.",
    "values-sr": "Напомена: од реформе алтернативне расподеле тастера страна слова чини практично све релевантне знакове директно доступним - тастери са двоструким углом, ред бројева са сопственим симболима и искакаћи прозори дугог притиска укључени - свакодневно куцање никада не треба посета странама посебних знакова.",
    "values-et": "Märkus: alternatiivse klahvijaotuse reformi järel pakub tähtede leht peaaegu kõiki olulisi märke otse - topelt-nurgaklahvid, numbrite rida oma sümbolitega ja pikalt vajutamise hüpikaknad kaasa arvatud - igapäevane tippimine ei vaja kunagi külastust erimärkide lehtedele.",
    "values-lv": "Piezīme: kopš alternatīvo taustiņu piešķiršanas reformas burtu lapa padara gandrīz visus būtiskos simbolus tieši pieejamus - dubultā stūra taustiņi, ciparu rinda ar savējiem simboliem un ilgas nospiešanas uznirstošie logi ieskaitot - ikdienas rakstīšanai nekad nav vajadzīgs apmeklējums īpašo simbolu lapām.",
    "values-lt": "Pastaba: nuo alternatyvios klaviatūros paskirstymo reformos raidžių puslapis padaro beveik visus svarbius simbolius tiesiogiai prieinamus - dvigubo kampo klavišai, skaitmenų eilė su savais simboliais ir ilgo paspaudimo iškylantieji langai įskaitant - kasdieniam rašymui niekada nereikia užsukti į specialiųjų simbolių puslapius.",
    "values-id": "Catatan: sejak reformasi penugasan tombol alternatif, halaman huruf menyediakan hampir semua karakter relevan secara langsung - tombol sudut ganda, barisan digit dengan simbolnya sendiri, dan jendela pop tekanan panjang termasuk - mengetik sehari-hari tidak pernah perlu mampir ke halaman karakter khusus.",
    "values-ms": "Nota: sejak pembaharuan penugasan kekunci alternatif, halaman huruf menyediakan hampir semua aksara relevan secara terus - kekunci sudut berganda, baris digit dengan simbolnya sendiri dan tetingkap pop tekanan panjang termasuk - menaip harian tidak pernah perlu singgah ke halaman aksara khas.",
    "values-sw": "Kumbuka: tangu mageuzi ya mgawanyo mbadala wa vitufe, ukurasa wa herufi unatoa vibonyezo vinavyohusika vya kawaida moja kwa moja - vitufe vya kona mbili, safu ya nambari pamoja na ishara zake na vichenjaji vya kubonyeza kwa muda vikiwemo - kuandika ya kila siku hauhitaji kwenda kurasa za vibonyezo maalum kabisa.",
    "values-fil": "Paalala: mula nang reporma sa alternatibong pagtatalaga ng mga key, ginagawang direktang available ng pahina ng mga titik ang praktikal na lahat ng mahahalagang karakter - mga key na dobleng sulok, ang hilera ng mga digit na may sariling mga simbolo, at mga pop-up ng mahabang pagpindot kasama - hindi kailanman kailangan ng pang-araw-araw na pag-type na daanan ang mga pahina ng mga espesyal na karakter.",
    "values-ru": "Примечание: с реформы альтернативного распределения клавиш буквенная страница делает практически все нужные символы напрямую доступными - клавиши с двойными углами, ряд цифр с собственными символами и всплывающие окна долгого нажатия включительно - повседневному набору никогда не нужен заход на страницы специальных символов.",
    "values-uk": "Примітка: з реформи альтернативного розподілу клавіш сторінка літер робить практично всі потрібні символи безпосередньо доступними - клавіші з подвійними кутами, ряд цифр із власними символами та спливні вікна довгого натискання включно - повсякденному набору ніколи не потрібен захід на сторінки спеціальних символів.",
    "values-az": "Qeyd: alternativ düymə bölgüsü islahatından bəri hərf səhifəsi demək olar bütün müvafiq simvolları birbaşa əlçatan edir - ikiqutbu düymələr, öz simvolları ilə rəqəm sırası və uzun basma pəncərələri daxil olmaqla - gündəlik yazma üçün xüsusi simvol səhifələrinə müraciət heç vaxt lazım deyil.",
    "values-uz": "Eslatma: alternativ tugma taqsimoti islohotidan beri harflar sahifasi deyarli barcha tegishli belgilarni to'g'ridan-to'g'ri mavjud qiladi - ikki burchakli tugmalar, o'z belgilari bilan raqamlar qatori va uzoq bosish oynalari ham shu jumladan - kundalik yozish uchun maxsus belgilar sahifalariga murojaat qilish hech qachon kerak emas.",
}


def escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


for locale, note in STRINGS.items():
    path = RES / locale / "strings.xml"
    text = path.read_text(encoding="utf-8")
    if "d89_footer_note" in text:
        print(f"{locale}: already present, skipping")
        continue
    assert "</resources>" in text, f"{locale}: no closing resources tag"
    block = f'    <string name="d89_footer_note">{escape(note)}</string>\n'
    text = text.replace("</resources>", block + "</resources>")
    path.write_text(text, encoding="utf-8", newline="\n")
    print(f"{locale}: d89_footer_note added")

print("done")
