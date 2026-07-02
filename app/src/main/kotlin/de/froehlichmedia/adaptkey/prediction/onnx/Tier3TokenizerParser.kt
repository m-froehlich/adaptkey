package de.froehlichmedia.adaptkey.prediction.onnx

/**
 * Pure builder that assembles a [BpeTokenizer] from the raw `vocab.json` and `merges.txt` contents of a
 * byte-level BPE model (§9).
 *
 * Kept Android-free (it takes the file text, not a path) so it is unit-testable; the Android layer only
 * reads the files and hands the text in. The special-token *contents* are the fixed SmolLM2 set, but
 * their ids are taken from the supplied vocabulary, so the parser stays correct if the ids ever differ.
 */
object Tier3TokenizerParser {
    
    /** The SmolLM2 special-token contents (their ids are resolved from the vocabulary). */
    val SPECIAL_CONTENTS = listOf(
        "<|endoftext|>",
        "<|im_start|>",
        "<|im_end|>",
        "<repo_name>",
        "<reponame>",
        "<file_sep>",
        "<filename>",
        "<gh_stars>",
        "<issue_start>",
        "<issue_comment>",
        "<issue_closed>",
        "<jupyter_start>",
        "<jupyter_text>",
        "<jupyter_code>",
        "<jupyter_output>",
        "<jupyter_script>",
        "<empty_output>"
    )
    
    /**
     * Builds a tokenizer from the model files.
     *
     * @param vocabJson the `vocab.json` contents (token→id object)
     * @param mergesText the `merges.txt` contents (one `left right` merge per line, rank order)
     * @return the assembled [BpeTokenizer]
     */
    fun parse(vocabJson: String, mergesText: String): BpeTokenizer {
        val vocab = VocabJson.parse(vocabJson)
        val merges = parseMerges(mergesText)
        val specials = SPECIAL_CONTENTS.mapNotNull { content -> vocab[content]?.let { content to it } }.toMap()
        return BpeTokenizer(vocab, merges, specials)
    }
    
    /**
     * Parses `merges.txt`: one `left right` pair per line in rank order, skipping the optional
     * `#version` header and blank lines. Tolerates a trailing carriage return (CRLF files).
     *
     * @param text the merges.txt contents
     * @return the merges in rank order
     */
    fun parseMerges(text: String): List<Pair<String, String>> {
        val merges = ArrayList<Pair<String, String>>()
        for (raw in text.lineSequence()) {
            val line = raw.trimEnd('\r')
            if (line.isEmpty() || line.startsWith("#version")) {
                continue
            }
            val space = line.indexOf(' ')
            if (space <= 0 || space >= line.length - 1) {
                continue
            }
            merges.add(line.substring(0, space) to line.substring(space + 1))
        }
        return merges
    }
}
