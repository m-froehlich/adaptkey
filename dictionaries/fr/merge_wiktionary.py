# -*- coding: utf-8 -*-
# Einmal-Skript: fasst die franzoesischen Wiktionary-Extrakte (aus extract_wiktionary.py, der nativen
# franzoesischen Wiktionnaire-Ausgabe) mit dem bestehenden dictionaries/fr/dict.tsv zusammen. Gleiche
# Scope-Disziplin, Kollisionsregel, Case-Match-Absicherung und Anhaenge-statt-Neusortieren-Konvention wie
# dictionaries/el/merge_wiktionary.py - siehe dessen eigenen Kommentar fuer die volle Begruendung jedes
# einzelnen Punkts (identisch uebernommen, nicht neu erfunden).
#
# ZWEI ECHTE ERWEITERUNGEN gegenueber dem deutschen/griechischen/englischen Vorbild, beide aus der Language
# Contribution Guide's eigenem, ueberarbeiteten Schritt 4 ("mandatory bare-noun safety check") und der
# gleichzeitig gefundenen bare-NOUN-Luecke in Englisch/Griechisch (siehe AdaptKey-History.md D-444):
#  1. EIGENNAMEN (wiktionary_propernouns.tsv) werden ebenfalls verarbeitet - PROPER_NOUN wird einer Zeile
#     nur zugefuegt, wenn ihre bisherige Tag-Menge leer oder exakt {OTHER} ist (Kollisionsregel: ein Wort,
#     das an anderer Stelle bereits als echtes Nomen/Verb/Adjektiv erkannt wurde, behaelt diese Lesart -
#     PROPER_NOUN wuerde sie sonst zwingend grossschreiben, siehe D-441s eigene pierre/jean-Geschichte).
#  2. NACH jeder Tag-Vergabe (add_tag) wird sofort geprueft, ob die resultierende Tag-Menge exakt {NOUN}
#     ist - wenn ja, wird OTHER direkt mitvergeben. Franzoesisch schreibt gewoehnliche Nomen nicht gross
#     (§6 Regel 3 ist NICHT Language-gated, siehe CapitalisationEngine/D-441-Addendum in AdaptKey-Spec.md) -
#     ohne diese Sicherung wuerde jedes Wort mit einer echten, und sei sie noch so seltenen/technischen,
#     Wiktionary-Nomen-Lesart zwangsgrossgeschrieben, exakt der Fehler, der bei Englisch/Griechisch
#     unentdeckt blieb (36.580 bzw. 46.608 betroffene Zeilen).
#
# dict.tsv bleibt frequenzsortiert (nicht alphabetisch) - neue Zeilen werden ANGEHAENGT, niemals neu
# sortiert, exaktes Vorbild §322/Wortfamilien.

import statistics
import sys

POS_ORDER = ["NOUN", "VERB", "ADJECTIVE", "PREPOSITION", "PROPER_NOUN", "OTHER"]


def pos_sort_key(tag):
    return POS_ORDER.index(tag) if tag in POS_ORDER else len(POS_ORDER)


def format_pos(tags):
    return ",".join(sorted(tags, key=pos_sort_key))


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


def add_tag(row, tag):
    if tag not in row["pos"]:
        row["pos"].discard("OTHER")
        row["pos"].add(tag)
    # Mandatory bare-noun safety (Guide step 4 / D-444): this language does not capitalise common nouns
    # (step 8's own decision, see the D-441 addendum) - a tag set that is now exactly {NOUN} must never be
    # left bare, or CapitalisationEngine's rule 3 force-capitalises it unconditionally on every real use.
    if row["pos"] == {"NOUN"}:
        row["pos"].add("OTHER")


def restore_case(form, model_word):
    if model_word[:1].isupper() and form[:1].islower():
        return form[0].upper() + form[1:]
    return form


def matching_row(word, by_lower):
    # See dictionaries/en/merge_wiktionary.py's own matching_row() KDoc for the full "went"/"Gan" bug story
    # this guards against - identical reasoning applies here.
    row = by_lower.get(word.lower())
    if row is None or row["word"] != word:
        return None
    return row


def calibrate_ratio(pairs, by_lower):
    ratios = []
    for lemma, forms in pairs:
        lemma_row = matching_row(lemma, by_lower)
        if lemma_row is None or lemma_row["freq"] <= 0:
            continue
        for form in forms:
            if not form or form.lower() == lemma.lower():
                continue
            form_row = matching_row(form, by_lower)
            if form_row is not None and form_row["freq"] > 0:
                ratios.append(form_row["freq"] / lemma_row["freq"])
    return statistics.median(ratios) if ratios else 0.2, len(ratios)


def process(entries, by_lower, new_rows, ratio, tag, stats):
    for lemma, forms in entries:
        lemma_row = matching_row(lemma, by_lower)
        if lemma_row is None:
            stats["unmatched_lemmas"] += 1
            continue
        add_tag(lemma_row, tag)
        stats["tagged_lemmas"] += 1
        for form in forms:
            if not form or form.lower() == lemma.lower():
                continue
            form_row = matching_row(form, by_lower)
            if form_row is not None:
                add_tag(form_row, tag)
                if not form_row["lemma"]:
                    form_row["lemma"] = lemma_row["word"]
                stats["linked_existing_forms"] += 1
                continue
            if form.lower() in by_lower:
                stats["skipped_case_collision"] += 1
                continue
            cased = restore_case(form, lemma_row["word"])
            new_freq = max(1, round(lemma_row["freq"] * ratio))
            new_row = {"word": cased, "freq": new_freq, "pos": {tag}, "lemma": lemma_row["word"]}
            if new_row["pos"] == {"NOUN"}:
                new_row["pos"].add("OTHER")
            new_rows.append(new_row)
            by_lower[form.lower()] = new_row
            stats["generated_forms"] += 1


def process_proper_nouns(words, by_lower, all_pos, stats):
    # D-444: the collision check reads `all_pos` (every pos kaikki documents for this exact word string,
    # not only noun/verb/adjective) rather than the dict.tsv row's own prior tag - a prior tag of bare
    # OTHER is not evidence of "no real collision", it is frequently just the generic "kept from the
    # Wikipedia-frequency pass, no POS resolved yet" default a closed-class word (pronoun, adverb, ...)
    # would carry, since PartOfSpeech has no category for those at all. Real, found example: French "les"
    # (a pronoun) also has a genuine but rare "name" (surname) Wiktionary entry - dict.tsv's own prior tag
    # for it was bare OTHER, which the row-based check let straight through; `all_pos["les"]` correctly
    # contains `{"pron", "name"}`, catching the real collision the row-based check missed.
    tagged = 0
    unmatched = 0
    skipped_collision = 0
    for word in words:
        pos_values = all_pos.get(word, {"name"})
        if pos_values - {"name"}:
            skipped_collision += 1
            continue
        row = matching_row(word, by_lower)
        if row is None:
            unmatched += 1
            continue
        row["pos"].discard("OTHER")
        row["pos"].add("PROPER_NOUN")
        tagged += 1
    stats["propernouns_tagged"] = tagged
    stats["propernouns_unmatched"] = unmatched
    stats["propernouns_skipped_collision"] = skipped_collision


def read_grouped(path):
    """word\tform, one row per form (an empty form field registers a forms-less lemma) - grouped back into
    (lemma, [forms...]) pairs, in file order."""
    grouped = {}
    order = []
    with open(path, encoding="utf-8") as f:
        next(f)
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 2:
                continue
            word, form = parts[0], parts[1]
            if word not in grouped:
                grouped[word] = []
                order.append(word)
            if form:
                grouped[word].append(form)
    return [(word, grouped[word]) for word in order]


def read_words(path):
    words = []
    with open(path, encoding="utf-8") as f:
        next(f)
        for line in f:
            word = line.rstrip("\n")
            if word:
                words.append(word)
    return words


def read_allpos(path):
    """word\tpos_values (comma-separated) -> {word: {pos, pos, ...}}, the full per-word pos vocabulary
    extract_wiktionary.py saw, used by process_proper_nouns's own collision check."""
    result = {}
    with open(path, encoding="utf-8") as f:
        next(f)
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 2 or not parts[1]:
                continue
            result[parts[0]] = set(parts[1].split(","))
    return result


def main(dict_path, lang_dir, out_path):
    rows, by_lower = load_dict(dict_path)
    original_count = len(rows)

    nouns = read_grouped(f"{lang_dir}/wiktionary_nouns.tsv")
    verbs = read_grouped(f"{lang_dir}/wiktionary_verbs.tsv")
    adjectives = read_grouped(f"{lang_dir}/wiktionary_adjectives.tsv")
    prepositions = read_words(f"{lang_dir}/wiktionary_prepositions.tsv")
    proper_nouns = read_words(f"{lang_dir}/wiktionary_propernouns.tsv")
    all_pos = read_allpos(f"{lang_dir}/wiktionary_allpos.tsv")

    noun_ratio, noun_pairs_n = calibrate_ratio(nouns, by_lower)
    verb_ratio, verb_pairs_n = calibrate_ratio(verbs, by_lower)
    adj_ratio, adj_pairs_n = calibrate_ratio(adjectives, by_lower)
    print(f"Calibration: noun ratio={noun_ratio:.4f} (n={noun_pairs_n}), "
          f"verb ratio={verb_ratio:.4f} (n={verb_pairs_n}), "
          f"adjective ratio={adj_ratio:.4f} (n={adj_pairs_n})", file=sys.stderr)

    new_rows = []
    stats = {"tagged_lemmas": 0, "unmatched_lemmas": 0, "linked_existing_forms": 0, "generated_forms": 0, "skipped_case_collision": 0}

    process(nouns, by_lower, new_rows, noun_ratio, "NOUN", stats)
    print(f"After nouns: {stats}", file=sys.stderr)
    s1 = dict(stats)
    process(verbs, by_lower, new_rows, verb_ratio, "VERB", stats)
    print(f"After verbs (delta): tagged+{stats['tagged_lemmas']-s1['tagged_lemmas']} generated+{stats['generated_forms']-s1['generated_forms']}", file=sys.stderr)
    s2 = dict(stats)
    process(adjectives, by_lower, new_rows, adj_ratio, "ADJECTIVE", stats)
    print(f"After adjectives (delta): tagged+{stats['tagged_lemmas']-s2['tagged_lemmas']} generated+{stats['generated_forms']-s2['generated_forms']}", file=sys.stderr)

    prep_tagged = 0
    prep_unmatched = 0
    for word in prepositions:
        row = matching_row(word, by_lower)
        if row is None:
            prep_unmatched += 1
            continue
        add_tag(row, "PREPOSITION")
        prep_tagged += 1

    process_proper_nouns(proper_nouns, by_lower, all_pos, stats)

    print(f"Prepositions: tagged={prep_tagged} unmatched={prep_unmatched}", file=sys.stderr)
    print(f"Proper nouns: {stats['propernouns_tagged']} tagged, {stats['propernouns_unmatched']} unmatched, "
          f"{stats['propernouns_skipped_collision']} skipped (real-word collision)", file=sys.stderr)
    print(f"TOTAL: {stats}", file=sys.stderr)
    print(f"dict.tsv rows: {original_count} -> {original_count + len(new_rows)} (+{len(new_rows)})", file=sys.stderr)

    bare_noun_check = sum(1 for r in rows if r["pos"] == {"NOUN"}) + sum(1 for r in new_rows if r["pos"] == {"NOUN"})
    print(f"Bare-NOUN safety check (must be 0): {bare_noun_check}", file=sys.stderr)

    with open(out_path, "wb") as out:
        for row in rows:
            pos_str = format_pos(row["pos"])
            line = f"{row['word']}\t{row['freq']}\t{pos_str}"
            if row["lemma"]:
                line += f"\t{row['lemma']}"
            out.write((line + "\n").encode("utf-8"))
        for row in new_rows:
            pos_str = format_pos(row["pos"])
            line = f"{row['word']}\t{row['freq']}\t{pos_str}\t{row['lemma']}"
            out.write((line + "\n").encode("utf-8"))

    print(f"Written -> {out_path}", file=sys.stderr)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
