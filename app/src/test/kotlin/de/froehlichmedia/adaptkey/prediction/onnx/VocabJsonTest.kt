package de.froehlichmedia.adaptkey.prediction.onnx

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the flat vocab.json parser.
 */
class VocabJsonTest {
    
    @Test
    fun `parses a simple object`() {
        val map = VocabJson.parse("""{"a": 0, "b": 1, "c": 2}""")
        assertEquals(mapOf("a" to 0, "b" to 1, "c" to 2), map)
    }
    
    @Test
    fun `preserves file order`() {
        val map = VocabJson.parse("""{"z": 0, "a": 1, "m": 2}""")
        assertEquals(listOf("z", "a", "m"), map.keys.toList())
    }
    
    @Test
    fun `decodes string escapes in keys`() {
        // Keys: a literal quote, a backslash, and the byte-level space character via Ġ.
        val map = VocabJson.parse("""{"\"": 1, "\\": 2, "Ġ": 3}""")
        assertEquals(1, map["\""])
        assertEquals(2, map["\\"])
        assertEquals(3, map["Ġ"])
    }
    
    @Test
    fun `handles whitespace and an empty object`() {
        assertTrue(VocabJson.parse("  {  }  ").isEmpty())
        assertEquals(mapOf("x" to 5), VocabJson.parse("{\n  \"x\" : 5\n}"))
    }
    
    @Test
    fun `keeps raw utf-8 keys`() {
        assertEquals(mapOf("Ġwar" to 7), VocabJson.parse("""{"Ġwar": 7}"""))
    }
    
    @Test
    fun `rejects malformed input`() {
        assertThrows(IllegalArgumentException::class.java) { VocabJson.parse("""["a", "b"]""") }
        assertThrows(IllegalArgumentException::class.java) { VocabJson.parse("""{"a": }""") }
    }
}
