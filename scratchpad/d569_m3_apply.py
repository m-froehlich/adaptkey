# -*- coding: utf-8 -*-
# D-569 (M3) APPLY: remove the reviewed DE<->EN veto victims from en/dict.tsv.
#
# Derivation is mechanical: every candidate from scratchpad/d569_m3_candidates.txt (EN freq <= 20
# whose lowercase is a de/dict.tsv word with de freq >= 300; 380 rows) MINUS the reviewed whitelist
# below gets removed. Nothing outside the candidate list is ever touched.
#
# WHITELIST (the decided §4-2 "Homograph-Whitelist"; kept on purpose - genuine English words, for
# which the D-106 veto's suppression is its CORRECT behaviour when an English loanword is typed
# inside German text; these are not Rauschen):
#   Real English words / borrowed English readings: gab, sag, spiel (row "Spiel"), mutter (the
#     English verb, row "mutter"), rad, bier, krone, zar (row "Zar"), alb (row "Alb"), ort
#     (row "ORT"), Hanse, Koran, Casanova, Praxis (the dictionary's only representation of English
#     "praxis"), Grad/Grads (English informal "grad", only representation), Ersatz (row "Ersatz"),
#     Lieder (the English music term), Sten (the Sten gun).
#   Genuine English inflections: bares, barer (of bare), golds (Olympic golds), autos, hamburgers,
#     Frankfurters, Henrys.
#   Real English acronyms/abbreviations: MFs, EPOs, GG, JH, SEP, KAP, BIP, HSV, Apr, Wm.
#   English veterinary/nautical terms: orf (sheep disease, row "orf"), Gar (the gar fish).
#   English-verbatim German place names (English texts use these exact names; M11 keep-class
#     "cities/regions" consistent): Zug, Saarland, Saale, Neckar, Lippe, Minden, Fulda, Duisburg,
#     Koblenz, Bielefeld, Schwerin, Siegen, Erlangen, Schweinfurt, Konstanz, Altenburg, Tempelhof,
#     Neustadt, Taunus.
#
# Everything else is German corpus leakage (or mojibake of it) with no English reading - the exact
# class that deadened the D-106 veto: eine/ist/sich/nach/bei/durch/nicht/sind/sie/zum/wie/aber/dir,
# German-only proper spellings (Schweiz, Kuba, Italien, ...), German admin compounds (Landkreis,
# Regierungsbezirk, Bahnhof, ...), and all encoding-damaged rows (Gro▀e, M³nchen, Lõnder, R÷mer, km▓,
# m▓, ─mter, Beitrõge, ...).

import io

EN = "app/src/main/assets/en/dict.tsv"
CANDIDATES = "scratchpad/d569_m3_candidates.txt"

WHITELIST = {
    # real English words / borrowed English readings
    "gab", "sag", "Spiel", "mutter", "rad", "bier", "krone", "Zar", "Alb", "ORT", "Hanse",
    "Koran", "Casanova", "Praxis", "Grad", "Grads", "Ersatz", "Lieder", "Sten",
    # genuine English inflections
    "bares", "barer", "golds", "autos", "hamburgers", "Frankfurters", "Henrys",
    # real English acronyms / abbreviations
    "MFs", "EPOs", "GG", "JH", "SEP", "KAP", "BIP", "HSV", "Apr", "Wm",
    # English-verbatim place names
    "Zug", "Saarland", "Saale", "Neckar", "Lippe", "Minden", "Fulda", "Duisburg", "Koblenz",
    "Bielefeld", "Schwerin", "Siegen", "Erlangen", "Schweinfurt", "Konstanz", "Altenburg",
    "Tempelhof", "Neustadt", "Taunus",
    # English veterinary / nautical terms
    "orf", "Gar",
}


def load_candidates():
    words = []
    with io.open(CANDIDATES, "r", encoding="utf-8", newline="") as f:
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 2 and parts[0] != "word" and parts[1] not in ("ENfreq",):
                try:
                    int(parts[1])
                except ValueError:
                    continue
                words.append(parts[0])
    return words


def main():
    candidates = load_candidates()
    assert len(candidates) == 380, f"expected 380 candidates, found {len(candidates)}"

    unknown_keep = WHITELIST - set(candidates)
    assert not unknown_keep, f"whitelist entries not in candidate list: {sorted(unknown_keep)}"

    remove = [w for w in candidates if w not in WHITELIST]
    print(f"{len(candidates)} candidates, {len(WHITELIST)} whitelisted, {len(remove)} to remove")

    with io.open(EN, "r", encoding="utf-8", newline="") as f:
        lines = f.read().splitlines(True)

    remove_set = set(remove)
    hits = {}
    for i, line in enumerate(lines):
        word = line.split("\t", 1)[0]
        if word in remove_set:
            hits.setdefault(word, []).append(i)

    missing = remove_set - set(hits)
    dupes = {w: len(n) for w, n in hits.items() if len(n) > 1}
    assert not missing, f"not found in dict: {sorted(missing)}"
    assert not dupes, f"duplicated: {dupes}"

    # No surviving row may lemma-link into the removal set - such dependents are by construction the
    # same artefact family (generated inflections / mojibake genitives of the removed German
    # leakage); they join the removal set transitively and are printed for the record.
    while True:
        extra = {}
        for line in lines:
            word = line.split("\t", 1)[0]
            if word in remove_set:
                continue
            parts = line.rstrip("\n").split("\t")
            if len(parts) > 3 and parts[3] in remove_set:
                extra[word] = parts[3]
        if not extra:
            break
        for word, lemma in sorted(extra.items()):
            print(f"dependent (lemma-links to removed {lemma!r}): {word}")
        remove_set.update(extra)

    kept = [l for l in lines if l.split("\t", 1)[0] not in remove_set]
    with io.open(EN, "w", encoding="utf-8", newline="") as f:
        f.writelines(kept)
    print(f"removed {len(lines) - len(kept)} rows; {len(kept)} remain")


if __name__ == "__main__":
    main()
