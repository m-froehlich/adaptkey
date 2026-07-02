package de.froehlichmedia.adaptkey.prediction.onnx

/**
 * Minimal, pure parser for a flat `vocab.json` — a single JSON object mapping token strings to integer
 * ids, e.g. `{"the": 260, "Ġworld": 905, ...}` (§9).
 *
 * A byte-level BPE `vocab.json` is exactly this shape, so a tiny purpose-built reader avoids pulling in a
 * JSON dependency and keeps the parsing pure and unit-testable (the Android layer only reads the file
 * text). It handles the JSON string escapes that appear in real vocabularies (`\"`, `\\`, `\uXXXX`, …)
 * and raw UTF-8 keys (the byte-level characters like `Ġ`).
 */
object VocabJson {
    
    /**
     * Parses a flat `{"token": id, ...}` object.
     *
     * @param text the vocab.json contents
     * @return the token→id map, preserving file order
     * @throws IllegalArgumentException when the text is not a flat string→int JSON object
     */
    fun parse(text: String): Map<String, Int> {
        val map = LinkedHashMap<String, Int>()
        var i = skipWhitespace(text, 0)
        require(i < text.length && text[i] == '{') { "expected '{' at $i" }
        i = skipWhitespace(text, i + 1)
        if (i < text.length && text[i] == '}') {
            return map
        }
        while (true) {
            i = skipWhitespace(text, i)
            require(i < text.length && text[i] == '"') { "expected key string at $i" }
            val key = readString(text, i)
            i = skipWhitespace(text, key.next)
            require(i < text.length && text[i] == ':') { "expected ':' at $i" }
            i = skipWhitespace(text, i + 1)
            val value = readInt(text, i)
            map[key.value] = value.value
            i = skipWhitespace(text, value.next)
            require(i < text.length) { "unterminated object" }
            val ch = text[i]
            i++
            if (ch == '}') {
                break
            }
            require(ch == ',') { "expected ',' or '}' at ${i - 1}" }
        }
        return map
    }
    
    private fun skipWhitespace(text: String, from: Int): Int {
        var i = from
        while (i < text.length && text[i].isWhitespace()) {
            i++
        }
        return i
    }
    
    private fun readInt(text: String, from: Int): Parsed<Int> {
        var i = from
        if (i < text.length && text[i] == '-') {
            i++
        }
        val start = i
        while (i < text.length && text[i].isDigit()) {
            i++
        }
        require(i > start) { "expected integer at $from" }
        return Parsed(text.substring(from, i).toInt(), i)
    }
    
    private fun readString(text: String, from: Int): Parsed<String> {
        val sb = StringBuilder()
        var i = from + 1
        while (i < text.length) {
            when (val c = text[i]) {
                '"' -> return Parsed(sb.toString(), i + 1)
                '\\' -> {
                    i++
                    require(i < text.length) { "dangling escape at ${i - 1}" }
                    when (val e = text[i]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append(0x0C.toChar())
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = text.substring(i + 1, i + 5)
                            sb.append(hex.toInt(16).toChar())
                            i += 4
                        }
                        
                        else -> sb.append(e)
                    }
                    i++
                }
                
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        throw IllegalArgumentException("unterminated string from $from")
    }
    
    private class Parsed<T>(val value: T, val next: Int)
}
