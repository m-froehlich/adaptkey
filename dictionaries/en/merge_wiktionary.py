# -*- coding: utf-8 -*-
# Einmal-Skript: fasst die vier Wiktionary-Extrakte (Substantive/Verben/Adjektive/Praepositionen) mit dem
# bestehenden app/src/main/assets/en/dict.tsv zusammen. Scope-Disziplin identisch zum deutschen
# Wortfamilien-Projekt (AdaptKey-History.md §322): nur BESTEHENDE dict.tsv-Lemmata werden getaggt/
# vervollstaendigt, keine neue Vokabel-Menge wird hinzugezogen - ein Wiktionary-Wort ohne bereits
# vorhandenes dict.tsv-Lemma wird schlicht uebersprungen (bei ~233k extrahierten Substantiven gegen nur
# ~90k dict.tsv-Zeilen ist das der weitaus haeufigere Fall, genau wie beim Deutschen).
#
# Zwei Operationen pro Wortart:
#  1. TAGGING: das Lemma selbst bekommt die echte POS-Tag hinzugefuegt (OTHER faellt weg - fuer Englisch war
#     OTHER nie ein echtes linguistisches Signal, nur "unbekannt"; PROPER_NOUN bleibt bestehen, falls
#     vorhanden, und wird NICHT durch die neue Tag ersetzt - Praezedenzfall aus dem deutschen Projekt:
#     PROPER_NOUN wurde dort NEBEN NOUN vergeben, nie anstelle davon).
#  2. VERVOLLSTAENDIGUNG: fehlende Flexionsformen (Plural/Verbformen/Komparativ+Superlativ) werden
#     angehaengt, sofern die Form nicht bereits selbst als eigene Zeile existiert (Kollisionsregel: nie eine
#     bereits vorhandene Zeile ueberschreiben) - mit einer aus bereits-vorhandenen Formen-Paaren kalibrierten
#     Frequenz (Median-Verhaeltnis Form/Lemma je Wortart, exaktes Vorbild aus §322).
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


def restore_case(form, model_word):
    if model_word[:1].isupper() and form[:1].islower():
        return form[0].upper() + form[1:]
    return form


def matching_row(word, by_lower):
    # D-CASE-GUARD: dict.tsv's own case is "the most frequent surface form" - a genuinely common, lowercase
    # Wiktionary word should already be canonically lowercase in dict.tsv too. A row whose OWN casing differs
    # (e.g. dict.tsv's "Gan" vs Wiktionary's "gan") is real evidence it is a *different* word that only
    # collides case-insensitively, not the same one - found via a real bug: the archaic/dialectal verb "gan"
    # (attested past tense "went") case-insensitively matched an unrelated, already-present dict.tsv entry
    # "Gan" (freq 47, OTHER), which would have wrongly lemma-linked "went" to "Gan" instead of "go". Skipping
    # any case mismatch entirely (never attempting a "smart" resolution) is deliberately conservative - a
    # missed completion is a far safer failure than cross-contaminating two unrelated words.
    row = by_lower.get(word.lower())
    if row is None or row["word"] != word:
        return None
    return row


def calibrate_ratio(pairs, by_lower):
    # A form identical to its own lemma (invariant plurals like "sheep", irregular verbs like "hit" where
    # past=participle=base) is not a genuine inflected-form pair - including it would inject spurious
    # ratio=1.0 points and skew the median.
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


def process(kind, entries, by_lower, new_rows, ratio, tag, stats):
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
                # Case-insensitively occupied by an unrelated word (matching_row's own case check just
                # rejected it as a real match) - dict.tsv's own case-insensitive-uniqueness invariant means a
                # new row here would collide with that existing, different word. Skip rather than risk a
                # second bug of the same shape matching_row was added to fix.
                stats["skipped_case_collision"] += 1
                continue
            cased = restore_case(form, lemma_row["word"])
            new_freq = max(1, round(lemma_row["freq"] * ratio))
            new_row = {"word": cased, "freq": new_freq, "pos": {tag}, "lemma": lemma_row["word"]}
            new_rows.append(new_row)
            by_lower[form.lower()] = new_row
            stats["generated_forms"] += 1


def read_nouns(path):
    entries = []
    with open(path, encoding="utf-8") as f:
        next(f)
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 2:
                continue
            entries.append((parts[0], [parts[1]]))
    return entries


def read_verbs(path):
    entries = []
    with open(path, encoding="utf-8") as f:
        next(f)
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 5:
                continue
            entries.append((parts[0], [parts[1], parts[2], parts[3], parts[4]]))
    return entries


def read_adjectives(path):
    entries = []
    with open(path, encoding="utf-8") as f:
        next(f)
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 3:
                continue
            entries.append((parts[0], [parts[1], parts[2]]))
    return entries


def read_prepositions(path):
    words = []
    with open(path, encoding="utf-8") as f:
        next(f)
        for line in f:
            word = line.rstrip("\n")
            if word:
                words.append(word)
    return words


def main(dict_path, en_dir, out_path):
    rows, by_lower = load_dict(dict_path)
    original_count = len(rows)

    nouns = read_nouns(f"{en_dir}/wiktionary_nouns.tsv")
    verbs = read_verbs(f"{en_dir}/wiktionary_verbs.tsv")
    adjectives = read_adjectives(f"{en_dir}/wiktionary_adjectives.tsv")
    prepositions = read_prepositions(f"{en_dir}/wiktionary_prepositions.tsv")

    noun_ratio, noun_pairs_n = calibrate_ratio(nouns, by_lower)
    verb_ratio, verb_pairs_n = calibrate_ratio(verbs, by_lower)
    adj_ratio, adj_pairs_n = calibrate_ratio(adjectives, by_lower)
    print(f"Calibration: noun ratio={noun_ratio:.4f} (n={noun_pairs_n}), "
          f"verb ratio={verb_ratio:.4f} (n={verb_pairs_n}), "
          f"adjective ratio={adj_ratio:.4f} (n={adj_pairs_n})", file=sys.stderr)

    new_rows = []
    stats = {"tagged_lemmas": 0, "unmatched_lemmas": 0, "linked_existing_forms": 0, "generated_forms": 0, "skipped_case_collision": 0}

    process("noun", nouns, by_lower, new_rows, noun_ratio, "NOUN", stats)
    print(f"After nouns: {stats}", file=sys.stderr)
    stats_verb_start = dict(stats)
    process("verb", verbs, by_lower, new_rows, verb_ratio, "VERB", stats)
    print(f"After verbs (delta): tagged+{stats['tagged_lemmas']-stats_verb_start['tagged_lemmas']} "
          f"generated+{stats['generated_forms']-stats_verb_start['generated_forms']}", file=sys.stderr)
    stats_adj_start = dict(stats)
    process("adjective", adjectives, by_lower, new_rows, adj_ratio, "ADJECTIVE", stats)
    print(f"After adjectives (delta): tagged+{stats['tagged_lemmas']-stats_adj_start['tagged_lemmas']} "
          f"generated+{stats['generated_forms']-stats_adj_start['generated_forms']}", file=sys.stderr)

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
    dict_path = sys.argv[1]
    en_dir = sys.argv[2]
    out_path = sys.argv[3]
    main(dict_path, en_dir, out_path)
