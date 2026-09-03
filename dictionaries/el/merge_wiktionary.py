# -*- coding: utf-8 -*-
# Einmal-Skript: fasst die vier griechischen Wiktionary-Extrakte mit dem bestehenden
# dictionaries/el/dict.tsv zusammen. Gleiche Scope-Disziplin, Kollisionsregel, Case-Match-Absicherung und
# Anhaenge-statt-Neusortieren-Konvention wie dictionaries/en/merge_wiktionary.py - siehe dessen eigenen
# Kommentar fuer die volle Begruendung jedes einzelnen Punkts (identisch uebernommen, nicht neu erfunden).
#
# EINZIGER struktureller Unterschied: liest word\tform (eine Zeile pro Form, beliebig viele Formen pro
# Lemma) statt fester Spalten pro Wortart - siehe extract_wiktionary.py's eigenen Kommentar fuer die
# Begruendung (Griechisch hat zu viele Kombinationen, um sie einzeln zu benennen).

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
            new_rows.append(new_row)
            by_lower[form.lower()] = new_row
            stats["generated_forms"] += 1


def read_grouped(path):
    """word\tform, one row per form - grouped back into (lemma, [forms...]) pairs, in file order."""
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
            grouped[word].append(form)
    return [(word, grouped[word]) for word in order]


def read_prepositions(path):
    words = []
    with open(path, encoding="utf-8") as f:
        next(f)
        for line in f:
            word = line.rstrip("\n")
            if word:
                words.append(word)
    return words


def main(dict_path, lang_dir, out_path):
    rows, by_lower = load_dict(dict_path)
    original_count = len(rows)

    nouns = read_grouped(f"{lang_dir}/wiktionary_nouns.tsv")
    verbs = read_grouped(f"{lang_dir}/wiktionary_verbs.tsv")
    adjectives = read_grouped(f"{lang_dir}/wiktionary_adjectives.tsv")
    prepositions = read_prepositions(f"{lang_dir}/wiktionary_prepositions.tsv")

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

    print(f"Prepositions: tagged={prep_tagged} unmatched={prep_unmatched}", file=sys.stderr)
    print(f"TOTAL: {stats}", file=sys.stderr)
    print(f"dict.tsv rows: {original_count} -> {original_count + len(new_rows)} (+{len(new_rows)})", file=sys.stderr)

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
