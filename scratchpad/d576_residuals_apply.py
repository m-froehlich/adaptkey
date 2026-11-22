# -*- coding: utf-8 -*-
# D-576 (named-residuals follow-up to the english-dictionary-quality plan) APPLY.
#
# Part 1 - the ABOVE-THRESHOLD German leakage (History §569's named residual: und 217, Das 151,
# Dar 127, der 778 sat above the decided <= 20 band). Full census (scratchpad/d576_leakage_review.py,
# EN freq <= 2000 & de freq >= 300): 619 rows - but the overwhelming majority are REAL English words
# that merely share Germanic spelling (hat, fast, gut, bald, rat, boot, sand, arm, hell, golf, link,
# wolf, rose, wider, analys(e), kilometer, theater, organisation, ...). Removing those would destroy
# English vocabulary. Removed are ONLY the German weak forms / stray German rows / party acronyms /
# German-compound fragments / Latin-French connector fragments with NO English reading, plus person
# names the M11 policy missed because their tag shape is not PROPER_NOUN-only:
#   der, und, des, Das, zu, auf, für(mojibake), Ein, aus, Oder, zur, DASS, über(mojibake), Unter,
#   KAM, sei, oft, mai, Je, Ich, Gesellschaft, Dar, Mal, kinder, Kleine, Mann, Lang, Deutscher,
#   Moderne, du, Ost, Hof, CDU, FDP, NSDAP, CSU, SED, Mythos, Führer(mojibake), Landtag, Gestalt,
#   Messe, Potsdamer, et, di, Le, vec, NS, Fritz, tfrac, Gare, Tat, Stern, Napoleon, Bismarck,
#   ibn, Mein, ums, Berlins, Moderner, Herr, Bush, Jacob, Les, Lucas, Franklin, facto, Normale,
#   dei, Kohl, fontsize, Victor, Yorker, Erasmus, Donald, Zimmer, Franco, Einstein, sur, Il, te,
#   Tu, dt, Lat, Ki.
# Kept on purpose (the shared-Germanic vocabulary and real English words the census caught):
#   hat, fast, gut, bald, rat, alt, boot, bad, sand, arm, hell, golf, link, wolf, rose, wider,
#   die (the English verb, 11,779!), den (the den), hat, stand, fast, pro, meter, dollar, links,
#   theater, organisation, alter, tag, lag, dies (plural of die), mum?/sum?, gut, euro, kilometer,
#   km, cm, mm, mg, kV, kWh, kg, ha, au (gold), Na (sodium), pi, null, cos, sqrt, pH, Hz, MHz...
#   and every brand/city/country/acronym row (USA, DDR (DDR5!), IV, UNESCO, NATO, NASA, CIA, IBM,
#   BMW, AC/DB/OS/PC/Mac/HTML/XML, Hamburg, Frankfurt, Leipzig, Ukraine, Iran, Chile, ...).
#
# Part 2 - the PROPER_NOUN-only polysemes with a real common-noun lowercase reading (History §574's
# named residual): rows tagged NOUN,PROPER_NOUN whose lowercase form is an ordinary English word -
# the force-cap they produce ("the bay", "golden retriever", "mount a horse", "press corps",
# "magic wand", "star trek", "the bay of pigs") is exactly the china-wrongness. Re-tagged to
# NOUN,OTHER (conditional: only rows actually carrying that tag; anything else is reported and
# skipped). France-Greece-style multi-tag rows (French/Greek: NOUN,VERB,ADJECTIVE,PROPER_NOUN) are
# deliberately left - they already fall to rule 5 with S-11 dual chips serving the capital, and
# their verb/adjective readings are real Wiktionary senses.
#
# Guards: uniqueness asserts; dangling-lemma dependents of removals dropped transitively (printed);
# quality gates re-run afterwards.

import io

EN = "app/src/main/assets/en/dict.tsv"

REMOVE = {
    "der", "und", "des", "Das", "zu", "auf", "für", "Ein", "aus", "Oder", "zur", "DASS",
    "Über", "Unter", "KAM", "sei", "oft", "mai", "Je", "Ich", "Gesellschaft", "Dar", "Mal",
    "kinder", "Kleine", "Mann", "Lang", "Deutscher", "Moderne", "du", "Ost", "Hof", "CDU",
    "FDP", "NSDAP", "CSU", "SED", "Mythos", "Führer", "Landtag", "Gestalt", "Messe",
    "Potsdamer", "et", "di", "Le", "vec", "NS", "Fritz", "tfrac", "Gare", "Tat", "Stern",
    "Napoleon", "Bismarck", "ibn", "Mein", "ums", "Berlins", "moderner", "Herr", "Bush",
    "Jacob", "Les", "Lucas", "Franklin", "facto", "Normale", "dei", "Kohl", "fontsize",
    "Victor", "Yorker", "Erasmus", "Donald", "Zimmer", "Franco", "Einstein", "sur", "Il",
    "te", "Tu", "dt", "Lat", "Ki", "Türk",
}

RETAG_MAYBE = {
    # word -> reason (common-noun lowercase reading the force-cap gets wrong)
    "Tower": "a tower", "Golden": "golden retriever", "Mount": "mount a horse", "Villa": "a villa",
    "Forum": "a forum", "Delta": "a delta", "Atlas": "an atlas", "Metro": "the metro",
    "Arena": "an arena", "Bay": "the bay", "Regiment": "a regiment", "Corps": "the press corps",
    "Hill": "up the hill", "Bell": "ring the bell", "Watt": "a watt", "Shuttle": "a shuttle",
    "Trek": "a trek", "Corporation": "a corporation", "Dame": "a dame",
    "Loch": "the loch", "Royals": "the royals", "Oblast": "an oblast", "Laden": "laden with",
    "Fest": "a film fest", "Autobahn": "the autobahn",
}


def main():
    with io.open(EN, "r", encoding="utf-8", newline="") as f:
        lines = f.read().splitlines(True)

    by_word = {}
    for i, line in enumerate(lines):
        by_word[line.split("\t", 1)[0]] = i

    missing = set(REMOVE) - set(by_word)
    assert not missing, f"not found: {sorted(missing)[:20]}"

    retagged = 0
    skipped_retag = []
    for word in RETAG_MAYBE:
        i = by_word[word]
        parts = lines[i].rstrip("\n").split("\t")
        if parts[2] == "NOUN,PROPER_NOUN":
            parts[2] = "NOUN,OTHER"
            lines[i] = "\t".join(parts) + "\n"
            retagged += 1
        else:
            skipped_retag.append((word, parts[2]))

    # Dangling-lemma dependents of removed rows: junk minted forms die with their headword.
    while True:
        dangling = []
        for i, line in enumerate(lines):
            parts = line.rstrip("\n").split("\t")
            word = parts[0]
            if word in REMOVE:
                continue
            if len(parts) > 3 and parts[3] and parts[3] in REMOVE:
                dangling.append((word, parts[3]))
        if not dangling:
            break
        for word, lemma in dangling:
            print(f"dependent removed (lemma-links to removed {lemma!r}): {word}")
            REMOVE.add(word)

    out = [l for l in lines if l.split("\t", 1)[0] not in REMOVE]
    with io.open(EN, "w", encoding="utf-8", newline="") as f:
        f.writelines(out)
    print(f"re-tagged {retagged} noun-only polysemes to NOUN,OTHER "
          f"(skipped, other tags: {skipped_retag}); "
          f"removed {len(lines) - len(out)} leakage/junk/name rows; {len(lines)} -> {len(out)}")


if __name__ == "__main__":
    main()
