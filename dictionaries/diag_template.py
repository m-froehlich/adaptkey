# Reusable diagnostic script: usage `python diag_template.py <jsonl.gz> <lang_code>`
# Prints pos distribution, verb form_of ratio, noun/adj forms richness, multi-word-form shape samples -
# everything needed to decide the extract_wiktionary.py design for a new language before writing it for real.
import gzip
import json
import sys
from collections import Counter

path = sys.argv[1]
lang_code = sys.argv[2]

pos_counts = Counter()
verb_total = 0
verb_formof = 0
noun_lemma_total = 0
noun_with_real = 0
adj_lemma_total = 0
adj_with_real = 0
noun_tag_vocab = Counter()
adj_tag_vocab = Counter()
space_forms_total = 0
samples = []

with gzip.open(path, "rt", encoding="utf-8") as f:
    for line in f:
        try:
            e = json.loads(line)
        except Exception:
            continue
        if e.get("lang_code") != lang_code:
            continue
        pos = e.get("pos")
        pos_counts[pos] += 1
        has_formof = any(s.get("form_of") for s in e.get("senses", []))
        if pos == "verb":
            verb_total += 1
            if has_formof:
                verb_formof += 1
        for form in e.get("forms", []):
            fv = form.get("form", "")
            if any(c.isspace() for c in fv):
                space_forms_total += 1
        if not has_formof and pos == "noun":
            noun_lemma_total += 1
            has_real = any(f.get("tags") and f.get("tags") != ["canonical"] for f in e.get("forms", []))
            if has_real:
                noun_with_real += 1
                for f in e.get("forms", []):
                    for t in f.get("tags", []):
                        noun_tag_vocab[t] += 1
                if len(samples) < 4:
                    samples.append(("NOUN", e.get("word"), e.get("forms")))
        if not has_formof and pos == "adj":
            adj_lemma_total += 1
            has_real = any(f.get("tags") and f.get("tags") != ["canonical"] for f in e.get("forms", []))
            if has_real:
                adj_with_real += 1
                for f in e.get("forms", []):
                    for t in f.get("tags", []):
                        adj_tag_vocab[t] += 1
                if len(samples) < 8:
                    samples.append(("ADJ", e.get("word"), e.get("forms")))
        if not has_formof and pos == "verb" and len(samples) < 12 and e.get("forms"):
            samples.append(("VERB", e.get("word"), e.get("forms")[:15]))

print("pos distribution:", pos_counts.most_common(25))
print("verb total", verb_total, "form_of", verb_formof)
print("noun lemma total", noun_lemma_total, "with real forms", noun_with_real)
print("adj lemma total", adj_lemma_total, "with real forms", adj_with_real)
print("space-containing forms total:", space_forms_total)
print("noun tag vocab:", noun_tag_vocab.most_common(25))
print("adj tag vocab:", adj_tag_vocab.most_common(25))
with open("diag_out.json", "w", encoding="utf-8") as out:
    json.dump(samples, out, ensure_ascii=False, indent=1)
print("wrote", len(samples), "samples to diag_out.json")
