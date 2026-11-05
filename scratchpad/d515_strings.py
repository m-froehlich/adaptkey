# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Froehlich Media
#
# D-515 string pass: appends the apostrophe-key switch's title/summary to every locale's strings.xml
# (the English reference plus all 31 translation directories, per the standing D-478 rule; drafts).
# Idempotent: a file that already carries either name is skipped. Run from the repo root.

from pathlib import Path
import sys

sys.stdout.reconfigure(encoding="utf-8")

RES = Path("app/src/main/res")

STRINGS = {
    "values": (
        "Apostrophe key",
        "Show the apostrophe key (right of l / m) on non-English keyboards as well. Off: English keyboards only.",
    ),
    "values-de": (
        "Apostrophe-Taste",
        "Apostrophe-Taste (rechts neben l bzw. m) auch in nicht-englischen Tastaturen anzeigen. Aus: nur englische Tastaturen.",
    ),
    "values-el": (
        "Πλήκτρο απόστρου",
        "Εμφάνιση του πλήκτρου απόστρου (δεξιά του l/m) και σε μη αγγλικά πληκτρολόγια. Ανενεργό: μόνο αγγλικά πληκτρολόγια.",
    ),
    "values-fr": (
        "Touche apostrophe",
        "Afficher la touche apostrophe (à droite de l/m) aussi sur les claviers non anglais. Désactivé : claviers anglais uniquement.",
    ),
    "values-es": (
        "Tecla de apóstrofo",
        "Mostrar la tecla de apóstrofo (a la derecha de l/m) también en teclados no ingleses. Desactivado: solo teclados ingleses.",
    ),
    "values-it": (
        "Tasto apostrofo",
        "Mostra il tasto apostrofo (a destra di l/m) anche sulle tastiere non inglesi. Disattivato: solo tastiere inglesi.",
    ),
    "values-pt": (
        "Tecla de apóstrofo",
        "Mostrar a tecla de apóstrofo (à direita de l/m) também em teclados não ingleses. Desativado: apenas teclados ingleses.",
    ),
    "values-nl": (
        "Apostroftoets",
        "Toon de apostroftoets (rechts van l/m) ook op niet-Engelse toetsenborden. Uit: alleen Engelse toetsenborden.",
    ),
    "values-pl": (
        "Klawisz apostrofu",
        "Pokaż klawisz apostrofu (na prawo od l/m) także na klawiaturach nieangielskich. Wyłączone: tylko klawiatury angielskie.",
    ),
    "values-tr": (
        "Kesme işareti tuşu",
        "Kesme işareti tuşunu (l/m'nin sağında) İngilizce olmayan klavyelerde de göster. Kapalı: yalnızca İngilizce klavyeler.",
    ),
    "values-sv": (
        "Apostroftangent",
        "Visa apostroftangenten (till höger om l/m) även på icke-engelska tangentbord. Av: endast engelska tangentbord.",
    ),
    "values-nb": (
        "Apostroftast",
        "Vis apostroftasten (til høyre for l/m) også på ikke-engelske tastatur. Av: kun engelske tastatur.",
    ),
    "values-da": (
        "Apostroftast",
        "Vis apostroftasten (til højre for l/m) også på ikke-engelske tastaturer. Fra: kun engelske tastaturer.",
    ),
    "values-fi": (
        "Heittomerkkinäppäin",
        "Näytä heittomerkkinäppäin (l/m:n oikealla puolella) myös ei-enganninkielisillä näppäimistöillä. Pois käytöstä: vain englanninkieliset näppäimistöt.",
    ),
    "values-cs": (
        "Klávesa apostrofu",
        "Zobrazit klávesu apostrofu (vpravo od l/m) i na neanglických klávesnicích. Vypnuto: pouze anglické klávesnice.",
    ),
    "values-sk": (
        "Klávesa apostrofu",
        "Zobraziť klávesu apostrofu (vpravo od l/m) aj na neanglických klávesniciach. Vypnuté: iba anglické klávesnice.",
    ),
    "values-hu": (
        "Aposztróf billentyű",
        "Az aposztróf billentyű (az l/m jobb oldalán) megjelenítése a nem angol billentyűzeteken is. Ki: csak angol billentyűzeteken.",
    ),
    "values-ro": (
        "Tasta apostrof",
        "Afișează tasta apostrof (la dreapta lui l/m) și pe tastaturile neengleze. Dezactivat: doar tastaturi engleze.",
    ),
    "values-hr": (
        "Tipka apostrofa",
        "Prikaži tipku apostrofa (desno od l/m) i na neengleskim tipkovnicama. Isključeno: samo engleske tipkovnice.",
    ),
    "values-bs": (
        "Tipka apostrofa",
        "Prikaži tipku apostrofa (desno od l/m) i na neengleskim tastaturama. Isključeno: samo engleske tastature.",
    ),
    "values-sr": (
        "Тастер апострофа",
        "Прикажи тастер апострофа (десно од l/m) и на неенглеским тастатурама. Искључено: само енглеске тастатуре.",
    ),
    "values-et": (
        "Apostrofi klahv",
        "Kuva apostrofi klahv (l/m-st paremal) ka mitteingliskeelsetel klaviatuuridel. Väljas: ainult ingliskeelsed klaviatuurid.",
    ),
    "values-lv": (
        "Apostrofa taustiņš",
        "Rādīt apostrofa taustiņu (pa labi no l/m) arī neangļu tastatūrās. Izslēgts: tikai angļu tastatūras.",
    ),
    "values-lt": (
        "Apostrofo klavišas",
        "Rodyti apostrofo klavišą (dešinėje nuo l/m) ir ne anglų kalbos klaviatūrose. Išjungta: tik anglų klaviatūros.",
    ),
    "values-id": (
        "Tombol apostrof",
        "Tampilkan tombol apostrof (di kanan l/m) juga pada papan ketik non-Inggris. Mati: hanya papan ketik Inggris.",
    ),
    "values-ms": (
        "Kekunci apostrof",
        "Paparkan kekunci apostrof (di kanan l/m) pada papan kekunci bukan Inggeris juga. Mati: papan kekunci Inggeris sahaja.",
    ),
    "values-sw": (
        "Kitufe cha apostrof",
        "Onyesha kitufe cha apostrof (kulia kwa l/m) kwenye kibodi zisizo za Kiingereza pia. Zima: kibodi za Kiingereza pekee.",
    ),
    "values-fil": (
        "Apostrope key",
        "Ipakita ang apostrope key (sa kanan ng l/m) sa mga keyboard na hindi Ingles. Naka-off: mga keyboard na Ingles lang.",
    ),
    "values-ru": (
        "Клавиша апострофа",
        "Показывать клавишу апострофа (справа от l/m) и на неанглийских клавиатурах. Выкл.: только английские клавиатуры.",
    ),
    "values-uk": (
        "Клавіша апострофа",
        "Показувати клавішу апострофа (праворуч від l/m) і на неанглійських клавіатурах. Вимк.: лише англійські клавіатури.",
    ),
    "values-az": (
        "Apostrof düyməsi",
        "Apostrof düyməsini (l/m-dən sağda) ingiliscə olmayan klaviaturalarda da göstər. Bağlı: yalnız ingiliscə klaviaturalar.",
    ),
    "values-uz": (
        "Apostrof tugmasi",
        "Apostrof tugmasini (l/m dan oʻngda) inglizcha boʻlmagan klaviaturalarda ham koʻrsatish. Oʻchirilgan: faqat inglizcha klaviaturalar.",
    ),
}


def escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


for locale, (title, summary) in STRINGS.items():
    path = RES / locale / "strings.xml"
    text = path.read_text(encoding="utf-8")
    if "d515_apostrophe_key_title" in text:
        print(f"{locale}: already present, skipping")
        continue
    assert "</resources>" in text, f"{locale}: no closing resources tag"
    block = (
        f'    <string name="d515_apostrophe_key_title">{escape(title)}</string>\n'
        f'    <string name="d515_apostrophe_key_summary">{escape(summary)}</string>\n'
    )
    text = text.replace("</resources>", block + "</resources>")
    path.write_text(text, encoding="utf-8", newline="\n")
    print(f"{locale}: added d515 title/summary")

print("done")
