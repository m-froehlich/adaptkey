# -*- coding: utf-8 -*-
# D-574 (M7, english-dictionary-quality plan) REVIEW-LIST GENERATOR - does not modify anything.
#
# The noun-only force-cap class after M11: rows tagged exactly NOUN,PROPER_NOUN. Every one of them
# force-capitalises its typed lowercase form via section-6 rules 3/4 (isNounOnly: every tag in
# {NOUN, PROPER_NOUN}) - the plan's "china vase -> China vase" wrongness for polysemes and ordinary
# proper-noun leaks, mixed with the demonyms whose force-cap is CORRECT (always capital in English).
#
# Decided policy (a), per the plan: Re-Tagging nach D-441 with a small explicit exception list.
#   RE-TAG NOUN,PROPER_NOUN -> NOUN,OTHER for every row whose word is NOT on the always-capital
#   list (rule 5 ambiguous afterwards - typed lowercase stays, typed capital is preserved by rule 1;
#   the NOUN signal for A-05's split gate survives, exactly D-441's convention).
#   EXCEPTION (untouched, keeps force-cap): the curated always-capital list - demonyms/language/
#   nationality/religious adjectives and their plural forms, where the force-cap IS the desired
#   English behaviour ("american south" -> "American").
# No bare-NOUN rows exist (gate), so this class is the whole noun-only population.

import io

EN = "app/src/main/assets/en/dict.tsv"
OUT = "scratchpad/d574_m7_review.tsv"

ALWAYS_CAPITAL = {
    # demonyms / nationality & language adjectives
    "American", "English", "European", "German", "Greek", "Roman", "Soviet", "Jewish", "Latin",
    "Chinese", "Spanish", "Italian", "Dutch", "Portuguese", "Brazilian", "Mexican", "Canadian",
    "Australian", "Indian", "Japanese", "Russian", "Polish", "Turkish", "Swedish", "Norwegian",
    "Danish", "Finnish", "Icelandic", "Irish", "Scottish", "Welsh", "African", "Asian", "Arab",
    "Arabic", "Islamic", "Muslim", "Catholic", "Protestant", "Buddhist", "Hindu", "Sikh",
    "Celtic", "Nordic", "Slavic", "Germanic", "Norman", "Saxon", "Viking", "Aztec", "Maya",
    "Inca", "Korean", "Vietnamese", "Thai", "Indonesian", "Malaysian", "Filipino", "Pakistani",
    "Bangladeshi", "Israeli", "Palestinian", "Egyptian", "Moroccan", "Algerian", "Tunisian",
    "Libyan", "Sudanese", "Ethiopian", "Kenyan", "Nigerian", "Ghanaian", "Zimbabwean", "Zambian",
    "Ugandan", "Tanzanian", "Somali", "Cuban", "Jamaican", "Haitian", "Dominican", "Chilean",
    "Argentine", "Argentinian", "Colombian", "Peruvian", "Venezuelan", "Bolivian", "Ecuadorian",
    "Paraguayan", "Uruguayan", "Albanian", "Croatian", "Serbian", "Bosnian", "Slovenian",
    "Slovak", "Czech", "Hungarian", "Bulgarian", "Romanian", "Moldovan", "Ukrainian",
    "Belarusian", "Lithuanian", "Latvian", "Estonian", "Kazakh", "Mongolian", "Nepalese",
    "Nepali", "Burmese", "Cambodian", "Laotian", "Slovene", "Macedonian", "Montenegrin",
    "Cypriot", "Maltese", "Belgian", "Swiss", "Austrian", "Azerbaijani", "Luxembourger",
    "Kurdish", "Flemish", "Coptic", "Turkic", "Semitic", "Frankish", "Cornish", "Gaelic",
    "Norse", "Hellenistic", "Vedic", "Napoleonic", "Elizabethan", "Victorian", "Edwardian",
    "Georgian", "Renaissance", "Gothic", "Baroque", "Byzantine", "Persian", "Armenian",
    # religious names / holy books (always capital in English)
    "Christianity", "Islam", "Judaism", "Hinduism", "Mormon", "Christ", "Quran", "Talmud",
    "Yahweh", "Hebrew", "Bible", "Koran",
    # historical-political terms always capital in English usage
    "Nazi",
}
# Deliberately NOT on the list (a real lowercase reading exists or the lowercase form is standard):
# Ottoman (the furniture), Sir (Dear sir), Reich, China (the porcelain), Turkey (the bird),
# United (gone since M11), plus every proper-noun leak row - they re-tag to NOUN,OTHER and fall to
# rule 5, where typed lowercase stays and typed capital is preserved by rule 1.


def main():
    rows = []
    with io.open(EN, "r", encoding="utf-8", newline="") as f:
        for idx, line in enumerate(f, 1):
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 3 and parts[2] == "NOUN,PROPER_NOUN":
                rows.append((idx, parts[0], int(parts[1])))

    keep = [r for r in rows if r[1] in ALWAYS_CAPITAL or r[1].rstrip("s") in ALWAYS_CAPITAL
            or (r[1].endswith("es") and r[1][:-2] in ALWAYS_CAPITAL)]
    retag = [r for r in rows if r not in keep]

    print(f"NOUN,PROPER_NOUN rows: {len(rows)} | exception (stays force-cap): {len(keep)} "
          f"| re-tag to NOUN,OTHER: {len(retag)}")
    print("exception rows:", ", ".join(w for _, w, _ in sorted(keep, key=lambda r: -r[2])))

    with io.open(OUT, "w", encoding="utf-8", newline="\n") as out:
        out.write("line#\tword\tfreq\tdecision\n")
        for idx, word, freq in sorted(keep, key=lambda r: -r[2]):
            out.write(f"{idx}\t{word}\t{freq}\tKEEP-forcecap\n")
        for idx, word, freq in sorted(retag, key=lambda r: -r[2]):
            out.write(f"{idx}\t{word}\t{freq}\tRETAG-NOUN,OTHER\n")
    print(f"written -> {OUT}")


if __name__ == "__main__":
    main()
