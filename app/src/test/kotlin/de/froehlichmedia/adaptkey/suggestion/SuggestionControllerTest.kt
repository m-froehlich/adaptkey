package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the suggestion-bar policy (S-01 … S-06).
 */
class SuggestionControllerTest {
    
    private fun controller(max: Int = 8): SuggestionController {
        return SuggestionController(SuggestionConfig(maxSuggestions = max))
    }
    
    private fun words(controller: SuggestionController): List<String> {
        return controller.displayed().map { it.word }
    }
    
    @Test
    fun `S-01 first population is sorted by descending score`() {
        val controller = controller()
        controller.update("a", listOf(Suggestion("a1", 1.0), Suggestion("a2", 3.0), Suggestion("a3", 2.0)), null)
        
        assertEquals(listOf("a2", "a3", "a1"), words(controller))
    }
    
    @Test
    fun `S-01 caps the number of suggestions`() {
        val controller = controller(max = 2)
        controller.update("a", listOf(Suggestion("a1", 1.0), Suggestion("a2", 2.0), Suggestion("a3", 3.0)), null)
        
        assertEquals(listOf("a3", "a2"), words(controller))
    }
    
    @Test
    fun `S-02 never offers the word exactly as typed`() {
        val controller = controller()
        controller.update("der", listOf(Suggestion("der", 100.0), Suggestion("derby", 5.0)), null)
        
        assertEquals(listOf("derby"), words(controller))
    }
    
    @Test
    fun `S-03 keeps visible positions and appends new candidates at the right`() {
        val controller = controller()
        controller.update("ab", listOf(Suggestion("abc", 5.0), Suggestion("abd", 3.0)), null)
        assertEquals(listOf("abc", "abd"), words(controller))
        
        // 'abe' has the highest score but must be appended at the right, not reordered to the front.
        controller.update("ab", listOf(Suggestion("abd", 3.0), Suggestion("abc", 5.0), Suggestion("abe", 10.0)), null)
        assertEquals(listOf("abc", "abd", "abe"), words(controller))
    }
    
    @Test
    fun `S-04 resort reorders strictly by descending score`() {
        val controller = controller()
        controller.update("ab", listOf(Suggestion("abc", 5.0), Suggestion("abd", 3.0)), null)
        controller.update("ab", listOf(Suggestion("abc", 5.0), Suggestion("abd", 3.0), Suggestion("abe", 10.0)), null)
        
        controller.resort()
        assertEquals(listOf("abe", "abc", "abd"), words(controller))
    }
    
    @Test
    fun `S-06 pins the verbatim chip and its replacement at the far left`() {
        val controller = controller()
        controller.update("gu", listOf(Suggestion("gut", 5.0), Suggestion("gute", 3.0)), "gut")
        
        val items = controller.displayed()
        assertEquals(SuggestionController.Kind.VERBATIM, items[0].kind)
        assertEquals("gu", items[0].word)
        assertEquals("\"gu\"", items[0].text)
        assertEquals(listOf("gu", "gut", "gute"), items.map { it.word })
    }
    
    @Test
    fun `S-06 verbatim survives further typing and stays first`() {
        val controller = controller()
        controller.update("gu", listOf(Suggestion("gut", 5.0)), "gut")
        controller.update("gu", listOf(Suggestion("gute", 9.0), Suggestion("gut", 5.0)), "gut")
        
        assertEquals(SuggestionController.Kind.VERBATIM, controller.displayed().first().kind)
    }
    
    @Test
    fun `pending autocorrect equal to the input produces no verbatim chip`() {
        val controller = controller()
        controller.update("gut", listOf(Suggestion("gute", 3.0)), "gut")
        
        assertTrue(controller.displayed().none { it.kind == SuggestionController.Kind.VERBATIM })
    }
    
    @Test
    fun `declineAutocorrect removes the verbatim chip`() {
        val controller = controller()
        controller.update("gu", listOf(Suggestion("gut", 5.0)), "gut")
        
        controller.declineAutocorrect()
        assertTrue(controller.displayed().none { it.kind == SuggestionController.Kind.VERBATIM })
    }
    
    @Test
    fun `clear resets all state`() {
        val controller = controller()
        controller.update("ab", listOf(Suggestion("abc", 5.0)), "abx")
        
        controller.clear()
        assertTrue(controller.displayed().isEmpty())
    }
}
