# One-off script: rule-based Portuguese noun/adjective PLURAL generation, closing part of the D-445
# Wortfamilien gap (Portuguese's own native Wiktionary edition documents almost no regular noun/adjective
# inflection - see extract_wiktionary.py's own module docstring). Deliberately scoped to only the rule
# classes that are genuinely mechanical and low-risk; every genuinely ambiguous class is explicitly skipped
# rather than guessed at, per direct user instruction ("nur das aufbauen, das du ohne großes Fehlerpotential
# machen kannst - Müll müssen wir nicht künstlich aufbauen").
#
# RULES APPLIED (all mechanical, suffix-only string operations - never touch anything but the final 1-2
# characters, so a rule can never be wrong about the rest of the word):
#   - ends in an unaccented or accented vowel (a e i o u á à â ã é ê í ó ô õ ú) -> + s
#   - ends in -m -> -ns                                  (fully regular, no known exceptions)
#   - ends in -r -> + es                                 (fully regular for consonant-final words)
#   - ends in -z -> + es, UNLESS preceded by "ui"/"ai"    (juiz/raiz-style hiatus needs an accent this rule
#                                                          does not attempt - skipped rather than guessed)
#   - ends in -al/-el/-ol/-ul -> -ais/-éis/-óis/-uis      (drop the l, add the ending; é/ó are the only
#                                                          mechanical accent insertions this rule makes,
#                                                          always on the fixed replaced vowel, never
#                                                          elsewhere in the word), UNLESS the word already
#                                                          contains another accented vowel earlier (a
#                                                          proparoxytone signal, e.g. cônsul - the -is/-eis
#                                                          rule does not apply to these; skipped)
#
# EXPLICITLY NOT ATTEMPTED, real known ambiguity, left for a future native-reviewed round rather than
# guessed:
#   - words ending in -s (paroxytone loanwords like "lápis"/"vírus"/"ônibus" are invariable; oxytone words
#     like "mês"/"país" take -es - not reliably distinguishable by simple spelling rule, especially for
#     accented Latin loanwords that keep a non-final stress accent)
#   - words ending in -il (oxytone "-is" vs. paroxytone "-eis" - the same class of stress ambiguity)
#   - words ending in -ão (three genuinely competing patterns - -ões/-ães/-ãos - with real, contested
#     dialectal variation even in native reference grammars for some members; a curated exception list was
#     considered and deliberately not built this round, since several candidate members could not be
#     confirmed with full confidence without a native speaker)
#   - adjective gender-pair generation (-o/-a) - kaikki's own Portuguese entries carry no reliable
#     per-word gender signal for adjectives (unlike nouns, which do carry `masculine`/`feminine` in some
#     cases), so there is no safe way to know which adjectives even take the -o/-a alternation at all
#
# Calibration: the frequency ratio for a genuinely NEW generated word is not guessed - it is measured
# empirically, the same way merge_wiktionary.py already calibrates Wortfamilien ratios: for every word this
# rule would apply to, check whether the predicted plural ALREADY exists as its own real dict.tsv entry (an
# independent corpus attestation of both forms) and take the median of those real ratios.

import re
from pathlib import Path

BASE = Path(__file__).parent
DICT_PATH = BASE / "dict.tsv"

ACCENTED_VOWELS = set("áàâãéêíóôõú")
PLAIN_VOWELS = set("aeiou")
ALL_VOWELS = ACCENTED_VOWELS | PLAIN_VOWELS


def load_dict(path):
    rows = []
    by_lower = {}
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            parts = line.split("\t")
            word = parts[0]
            freq = int(parts[1])
            pos = set(parts[2].split(",")) if len(parts) > 2 and parts[2] else set()
            lemma = parts[3] if len(parts) > 3 else ""
            row = {"word": word, "freq": freq, "pos": pos, "lemma": lemma}
            rows.append(row)
            by_lower[word.lower()] = row
    return rows, by_lower


def matching_row(word, by_lower):
    row = by_lower.get(word.lower())
    if row is None or row["word"] != word:
        return None
    return row


def predict_plural(word: str):
    """Returns the predicted plural for `word`, or None if no safe rule applies (see module docstring for
    the full list of rules applied and classes deliberately skipped)."""
    lower = word.lower()
    if len(lower) < 2:
        return None

    if lower.endswith(("ao", "ão")):
        return None  # three-way ambiguous, deliberately skipped
    if lower.endswith("il"):
        return None  # oxytone/paroxytone stress ambiguity, deliberately skipped
    if lower.endswith("s"):
        return None  # invariable-vs-oxytone ambiguity, deliberately skipped

    if lower[-1] in ALL_VOWELS:
        return word + "s"

    if lower.endswith("m"):
        return word[:-1] + "ns"

    if lower.endswith("r"):
        return word + "es"

    if lower.endswith("z"):
        if lower.endswith("uiz") or lower.endswith("aiz"):
            return None  # hiatus-accent risk (juiz/raiz-style), deliberately skipped
        return word + "es"

    stem_has_earlier_accent = any(ch in ACCENTED_VOWELS for ch in lower[:-2])
    if lower.endswith("al") and not stem_has_earlier_accent:
        return word[:-2] + "ais"
    if lower.endswith("el") and not stem_has_earlier_accent:
        return word[:-2] + "éis"
    if lower.endswith("ol") and not stem_has_earlier_accent:
        return word[:-2] + "óis"
    if lower.endswith("ul") and not stem_has_earlier_accent:
        return word[:-2] + "uis"

    return None  # no safe rule matches - default is to skip, never guess


def restore_case(form, model_word):
    if model_word[:1].isupper() and form[:1].islower():
        return form[0].upper() + form[1:]
    return form


def main():
    rows, by_lower = load_dict(DICT_PATH)
    original_count = len(rows)

    eligible = [r for r in rows if r["pos"] & {"NOUN", "ADJECTIVE"} and "PROPER_NOUN" not in r["pos"]]

    # Calibration: measure real ratios wherever the predicted plural already exists as its own dict.tsv entry.
    ratios = []
    for row in eligible:
        predicted = predict_plural(row["word"])
        if predicted is None or predicted.lower() == row["word"].lower():
            continue
        plural_row = matching_row(predicted, by_lower)
        if plural_row is not None and plural_row["freq"] > 0 and row["freq"] > 0:
            ratios.append(plural_row["freq"] / row["freq"])
    import statistics
    ratio = statistics.median(ratios) if ratios else 0.2
    print(f"Calibration: plural ratio={ratio:.4f} from {len(ratios)} already-matched real pairs")

    new_rows = []
    stats = {"eligible": len(eligible), "no_rule": 0, "linked_existing": 0, "generated": 0, "skipped_collision": 0}
    for row in eligible:
        predicted = predict_plural(row["word"])
        if predicted is None or predicted.lower() == row["word"].lower():
            stats["no_rule"] += 1
            continue
        existing = matching_row(predicted, by_lower)
        if existing is not None:
            if not existing["lemma"]:
                existing["lemma"] = row["word"]
            stats["linked_existing"] += 1
            continue
        if predicted.lower() in by_lower:
            stats["skipped_collision"] += 1
            continue
        cased = restore_case(predicted, row["word"])
        new_freq = max(1, round(row["freq"] * ratio))
        new_row = {"word": cased, "freq": new_freq, "pos": set(row["pos"]), "lemma": row["word"]}
        if new_row["pos"] == {"NOUN"}:
            new_row["pos"].add("OTHER")
        new_rows.append(new_row)
        by_lower[predicted.lower()] = new_row
        stats["generated"] += 1

    print(f"Stats: {stats}")
    print(f"dict.tsv rows: {original_count} -> {original_count + len(new_rows)} (+{len(new_rows)})")

    bare_noun_check = sum(1 for r in rows if r["pos"] == {"NOUN"}) + sum(1 for r in new_rows if r["pos"] == {"NOUN"})
    print(f"Bare-NOUN safety check (must be 0): {bare_noun_check}")

    with DICT_PATH.open("wb") as out:
        for row in rows:
            pos_str = ",".join(sorted(row["pos"], key=lambda t: ["NOUN", "VERB", "ADJECTIVE", "PREPOSITION", "PROPER_NOUN", "OTHER"].index(t) if t in ["NOUN", "VERB", "ADJECTIVE", "PREPOSITION", "PROPER_NOUN", "OTHER"] else 99))
            line = f"{row['word']}\t{row['freq']}\t{pos_str}"
            if row["lemma"]:
                line += f"\t{row['lemma']}"
            out.write((line + "\n").encode("utf-8"))
        for row in new_rows:
            pos_str = ",".join(sorted(row["pos"], key=lambda t: ["NOUN", "VERB", "ADJECTIVE", "PREPOSITION", "PROPER_NOUN", "OTHER"].index(t) if t in ["NOUN", "VERB", "ADJECTIVE", "PREPOSITION", "PROPER_NOUN", "OTHER"] else 99))
            line = f"{row['word']}\t{row['freq']}\t{pos_str}\t{row['lemma']}"
            out.write((line + "\n").encode("utf-8"))

    print(f"Written -> {DICT_PATH}")


if __name__ == "__main__":
    main()
