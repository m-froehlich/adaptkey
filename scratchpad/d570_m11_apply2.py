# -*- coding: utf-8 -*-
# D-570 (M11) FOLLOW-UP: targeted removal inside the NOUN,PROPER_NOUN class.
#
# The plan's class-1 definition is "person names WITHOUT a generic reading" - that is the
# PROPER_NOUN-only sweep (already applied: 26,822 rows). The Wiktionary merge additionally minted
# marginal generic readings for a few dozen top names inside NOUN,PROPER_NOUN (a "john" = slang for
# toilet/client, "georges" = archaic coins, "henrys" = SI inductance units); their name reading
# dominates typing by orders of magnitude and §4-7 decided names go "fully out, including the tops",
# naming John explicitly. This script removes exactly those reviewed rows. Kept on purpose - real
# generic readings, which are M7's re-tagging territory: Christian ("a Christian"), Jack (car
# jack), Frank (frank), Ford (the brand), Kaiser (the title), Washington/Hamilton (cities),
# Casanova, plus every demonym/geography row of the class (American, English, China, York-free
# ... wait: York goes - it is a reviewed splitter fragment, see REMOVE below).
#
# Removed (40): John, David, George, Henry (the SI-unit sense is an accepted loss), Thomas, Louis,
# Alexander, Richard, Joseph, Edward, Albert, Joe, Leo, Hans, Benjamin, Gregory, Simon, Hermann,
# Jesse, Schmidt, Hitler, Stalin, Johnson, Scott, Adams, Campbell, Allen, Thompson, Collins,
# Parker, Morgan, Walker, Turner, Gibson, Phillips, Edwards, Bailey, Jenkins, Mozart, York.

import io

EN = "app/src/main/assets/en/dict.tsv"

REMOVE = {
    "John", "David", "George", "Henry", "Thomas", "Louis", "Alexander", "Richard", "Joseph",
    "Edward", "Albert", "Joe", "Leo", "Hans", "Benjamin", "Gregory", "Simon", "Hermann", "Jesse",
    "Schmidt", "Hitler", "Stalin", "Johnson", "Scott", "Adams", "Campbell", "Allen", "Thompson",
    "Collins", "Parker", "Morgan", "Walker", "Turner", "Gibson", "Phillips", "Edwards", "Bailey",
    "Jenkins", "Mozart", "York",
}

KEEP_CHECK = {
    "Christian", "Jack", "Frank", "Ford", "Kaiser", "Washington", "Hamilton", "Casanova",
}


def main():
    with io.open(EN, "r", encoding="utf-8", newline="") as f:
        lines = f.read().splitlines(True)

    by_word = {}
    for i, line in enumerate(lines):
        by_word[line.split("\t", 1)[0]] = i

    missing = REMOVE - set(by_word)
    assert not missing, f"not found: {sorted(missing)}"
    for word in REMOVE:
        parts = lines[by_word[word]].rstrip("\n").split("\t")
        assert parts[2] == "NOUN,PROPER_NOUN", f"unexpected tags for {word}: {parts[2]}"
    for word in KEEP_CHECK:
        assert word in by_word and word not in REMOVE, word

    # No survivor may lemma-link into the removal set (transitive; junk minted senses print).
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
    print(f"removed {len(lines) - len(out)} rows; {len(lines)} -> {len(out)}")


if __name__ == "__main__":
    main()
