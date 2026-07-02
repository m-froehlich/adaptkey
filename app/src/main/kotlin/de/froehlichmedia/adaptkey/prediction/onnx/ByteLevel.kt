package de.froehlichmedia.adaptkey.prediction.onnx

/**
 * The GPT-2 / byte-level reversible mapping between raw bytes (0..255) and printable Unicode characters
 * used by byte-level BPE tokenizers (§9).
 *
 * Every byte is mapped to a distinct printable character so a tokenizer can operate on text without ever
 * encountering control characters or whitespace directly (a space byte becomes `Ġ`, a newline `Ċ`, and
 * so on). The mapping is the exact one from the reference implementation, so it round-trips byte-for-byte
 * and is unit-testable independently of any model.
 */
object ByteLevel {
    
    private val byteToChar = CharArray(256)
    private val charToByte = HashMap<Char, Int>(512)
    
    init {
        val bs = ArrayList<Int>()
        for (b in 0x21..0x7E) {
            bs.add(b)
        }
        for (b in 0xA1..0xAC) {
            bs.add(b)
        }
        for (b in 0xAE..0xFF) {
            bs.add(b)
        }
        val cs = ArrayList<Int>(bs)
        val initial = bs.toHashSet()
        var n = 0
        for (b in 0..255) {
            if (b !in initial) {
                bs.add(b)
                cs.add(256 + n)
                n++
            }
        }
        for (i in bs.indices) {
            val ch = cs[i].toChar()
            byteToChar[bs[i]] = ch
            charToByte[ch] = bs[i]
        }
    }
    
    /**
     * Byte-encodes a text fragment: its UTF-8 bytes are each mapped to their printable character.
     *
     * @param text the raw text fragment
     * @return the byte-level string (one character per UTF-8 byte)
     */
    fun encode(text: String): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder(bytes.size)
        for (b in bytes) {
            sb.append(byteToChar[b.toInt() and 0xFF])
        }
        return sb.toString()
    }
    
    /**
     * Reverses [encode]: maps a byte-level string back to the original bytes. Characters that are not
     * part of the mapping (which valid tokenizer output never contains) are skipped defensively.
     *
     * @param text the byte-level string
     * @return the decoded UTF-8 text
     */
    fun decode(text: String): String {
        val bytes = ArrayList<Byte>(text.length)
        for (ch in text) {
            val b = charToByte[ch] ?: continue
            bytes.add(b.toByte())
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }
}
