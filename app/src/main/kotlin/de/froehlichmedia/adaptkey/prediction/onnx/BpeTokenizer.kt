package de.froehlichmedia.adaptkey.prediction.onnx

import java.util.regex.Pattern

/**
 * Pure byte-level BPE tokenizer (§9), matching the SmolLM2 / GPT-2 tokenisation pipeline:
 * an `individual_digits` digit split, the GPT-2 pre-tokenisation regex, byte-level encoding
 * ([ByteLevel]) and rank-ordered BPE merges.
 *
 * It takes the vocabulary, the rank-ordered merges and the special tokens as plain data, so the whole
 * algorithm is Android-free and unit-testable — including a parity test against golden vectors produced
 * by the reference tokenizer. The Android layer only supplies the parsed data from `tokenizer.json`.
 *
 * @property specialTokens special-token content to id (e.g. `<|im_end|>`); matched verbatim in the
 *           input before pre-tokenisation and skipped by [decode] unless asked to keep them
 */
class BpeTokenizer(
    private val tokenToId: Map<String, Int>,
    merges: List<Pair<String, String>>,
    val specialTokens: Map<String, Int> = emptyMap()
) {
    
    private val mergeRank: Map<Pair<String, String>, Int> = buildMap {
        merges.forEachIndexed { index, pair -> putIfAbsent(pair, index) }
    }
    private val idToToken: Map<Int, String> = tokenToId.entries.associate { (token, id) -> id to token }
    private val specialIds: Set<Int> = specialTokens.values.toHashSet()
    private val specialsByLengthDesc: List<String> = specialTokens.keys.sortedByDescending { it.length }
    
    /** The number of entries in the vocabulary. */
    val vocabSize: Int get() = tokenToId.size
    
    /**
     * @param content the special-token content
     * @return its id, or null when [content] is not a known special token
     */
    fun specialTokenId(content: String): Int? = specialTokens[content]
    
    /**
     * Encodes text into token ids.
     *
     * @param text the text to encode
     * @return the token ids, in order
     */
    fun encode(text: String): IntArray {
        val ids = ArrayList<Int>()
        for (segment in splitOnSpecials(text)) {
            val specialId = specialTokens[segment]
            if (specialId != null) {
                ids.add(specialId)
                continue
            }
            for (piece in preTokenize(segment)) {
                for (symbol in bpe(piece)) {
                    val id = tokenToId[symbol] ?: continue
                    ids.add(id)
                }
            }
        }
        return ids.toIntArray()
    }
    
    /**
     * Decodes token ids back to text.
     *
     * @param ids the token ids
     * @param skipSpecial when true (default), special tokens are omitted from the output
     * @return the decoded text
     */
    fun decode(ids: IntArray, skipSpecial: Boolean = true): String {
        val sb = StringBuilder()
        for (id in ids) {
            if (skipSpecial && id in specialIds) {
                continue
            }
            sb.append(idToToken[id] ?: continue)
        }
        return ByteLevel.decode(sb.toString())
    }
    
    /**
     * Splits [text] into segments, isolating any special-token content (longest match first) from the
     * surrounding normal text. Normal segments are encoded via BPE; special segments map to their id.
     */
    private fun splitOnSpecials(text: String): List<String> {
        if (specialsByLengthDesc.isEmpty()) {
            return listOf(text)
        }
        val out = ArrayList<String>()
        val buffer = StringBuilder()
        var i = 0
        outer@ while (i < text.length) {
            for (special in specialsByLengthDesc) {
                if (special.isNotEmpty() && text.startsWith(special, i)) {
                    if (buffer.isNotEmpty()) {
                        out.add(buffer.toString())
                        buffer.setLength(0)
                    }
                    out.add(special)
                    i += special.length
                    continue@outer
                }
            }
            buffer.append(text[i])
            i++
        }
        if (buffer.isNotEmpty()) {
            out.add(buffer.toString())
        }
        return out
    }
    
    /**
     * Pre-tokenises a normal segment: an `individual_digits` split, then the GPT-2 regex, then
     * byte-level encoding of each resulting piece.
     */
    private fun preTokenize(text: String): List<String> {
        val pieces = ArrayList<String>()
        for (digitPiece in splitDigits(text)) {
            val matcher = GPT2_PATTERN.matcher(digitPiece)
            while (matcher.find()) {
                pieces.add(ByteLevel.encode(matcher.group()))
            }
        }
        return pieces
    }
    
    /**
     * Splits on digit boundaries, isolating each numeric character (the `individual_digits` behaviour).
     */
    private fun splitDigits(text: String): List<String> {
        val out = ArrayList<String>()
        val run = StringBuilder()
        for (ch in text) {
            if (isNumeric(ch)) {
                if (run.isNotEmpty()) {
                    out.add(run.toString())
                    run.setLength(0)
                }
                out.add(ch.toString())
            } else {
                run.append(ch)
            }
        }
        if (run.isNotEmpty()) {
            out.add(run.toString())
        }
        return out
    }
    
    /**
     * Applies rank-ordered BPE merges to one byte-level piece (the reference GPT-2 algorithm: each round
     * merges every occurrence of the lowest-rank adjacent pair).
     */
    private fun bpe(piece: String): List<String> {
        if (piece.isEmpty()) {
            return emptyList()
        }
        var word = ArrayList<String>(piece.length)
        for (ch in piece) {
            word.add(ch.toString())
        }
        if (word.size < 2) {
            return word
        }
        while (true) {
            var bestRank = Int.MAX_VALUE
            var bestPair: Pair<String, String>? = null
            for (i in 0 until word.size - 1) {
                val pair = Pair(word[i], word[i + 1])
                val rank = mergeRank[pair] ?: continue
                if (rank < bestRank) {
                    bestRank = rank
                    bestPair = pair
                }
            }
            val bigram = bestPair ?: break
            val first = bigram.first
            val second = bigram.second
            val merged = ArrayList<String>(word.size)
            var i = 0
            while (i < word.size) {
                val next = indexOfFrom(word, first, i)
                if (next < 0) {
                    for (k in i until word.size) {
                        merged.add(word[k])
                    }
                    break
                }
                for (k in i until next) {
                    merged.add(word[k])
                }
                if (next < word.size - 1 && word[next + 1] == second) {
                    merged.add(first + second)
                    i = next + 2
                } else {
                    merged.add(word[next])
                    i = next + 1
                }
            }
            word = merged
            if (word.size == 1) {
                break
            }
        }
        return word
    }
    
    private fun indexOfFrom(list: List<String>, target: String, from: Int): Int {
        for (i in from until list.size) {
            if (list[i] == target) {
                return i
            }
        }
        return -1
    }
    
    private fun isNumeric(ch: Char): Boolean {
        return when (Character.getType(ch)) {
            Character.DECIMAL_DIGIT_NUMBER.toInt(), Character.LETTER_NUMBER.toInt(), Character.OTHER_NUMBER.toInt() -> true
            else -> false
        }
    }
    
    companion object {
        
        // The GPT-2 pre-tokenisation regex; UNICODE_CHARACTER_CLASS makes \s / \p{N} Unicode-aware.
        private val GPT2_PATTERN: Pattern = Pattern.compile(
            "'s|'t|'re|'ve|'m|'ll|'d| ?\\p{L}+| ?\\p{N}+| ?[^\\s\\p{L}\\p{N}]+|\\s+(?!\\S)|\\s+",
            Pattern.UNICODE_CHARACTER_CLASS
        )
    }
}
