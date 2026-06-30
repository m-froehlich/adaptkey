package de.froehlichmedia.adaptkey.touch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure T-05 ambiguity-band geometry: lower-band letter taps, upper-band space taps,
 * nearest-letter inference, and offset-model refinement.
 */
class AmbiguityBandsTest {
    
    // c v b n m laid out left to right, each 100 wide and 50 tall; the space bar sits directly below.
    private val letters = listOf(
        KeyBox("c:c", 'c', 0f, 200f, 100f, 250f),
        KeyBox("c:v", 'v', 100f, 200f, 200f, 250f),
        KeyBox("c:b", 'b', 200f, 200f, 300f, 250f),
        KeyBox("c:n", 'n', 300f, 200f, 400f, 250f),
        KeyBox("c:m", 'm', 400f, 200f, 500f, 250f)
    )
    private val space = KeyBox("SPACE", null, 100f, 250f, 400f, 300f)
    private val bands = AmbiguityBands()
    
    @Test
    fun `a tap in the lower band of a bottom letter is space-ambiguous`() {
        // band = 50 * 0.25 = 12.5; lower-band threshold = 250 - 12.5 = 237.5.
        val result = bands.classify("c:b", x = 250f, y = 245f, bottomLetters = letters, space = space)
        assertEquals(TapAmbiguity.SPACE_AMBIGUOUS, result.kind)
    }
    
    @Test
    fun `a tap in the upper part of a bottom letter is unambiguous`() {
        val result = bands.classify("c:b", x = 250f, y = 210f, bottomLetters = letters, space = space)
        assertEquals(TapAmbiguity.NONE, result.kind)
    }
    
    @Test
    fun `a tap in the upper band of the space bar is letter-ambiguous with the nearest letter`() {
        // space band threshold = 250 + 12.5 = 262.5; x = 240 is nearest to b (centre 250).
        val result = bands.classify("SPACE", x = 240f, y = 255f, bottomLetters = letters, space = space)
        assertEquals(TapAmbiguity.LETTER_AMBIGUOUS, result.kind)
        assertEquals('b', result.inferredChar)
    }
    
    @Test
    fun `a tap in the lower part of the space bar is unambiguous`() {
        val result = bands.classify("SPACE", x = 240f, y = 290f, bottomLetters = letters, space = space)
        assertEquals(TapAmbiguity.NONE, result.kind)
        assertNull(result.inferredChar)
    }
    
    @Test
    fun `a tap on any other key is unambiguous`() {
        val result = bands.classify("c:q", x = 250f, y = 245f, bottomLetters = letters, space = space)
        assertEquals(TapAmbiguity.NONE, result.kind)
    }
    
    @Test
    fun `nearest-letter inference picks the closest centre by x`() {
        assertEquals('c', bands.nearestLetter(10f, letters))
        assertEquals('v', bands.nearestLetter(140f, letters))
        assertEquals('m', bands.nearestLetter(460f, letters))
    }
    
    @Test
    fun `the offset model shifts nearest-letter inference toward the learned mean`() {
        // n learns a systematic leftward offset; its compensated centre moves from 350 toward 300.
        val model = OffsetModel()
        model.record("c:n", centerX = 350f, centerY = 225f, x = 270f, y = 225f)
        
        assertEquals('b', bands.nearestLetter(290f, letters, null))
        assertEquals('n', bands.nearestLetter(290f, letters, model))
    }
    
    @Test
    fun `the offset model shifts the lower band by the learned vertical mean`() {
        // b learns a downward offset; the lower-band threshold moves from 237.5 toward 250, so a tap
        // at y = 240 that was space-ambiguous without the model becomes unambiguous with it.
        val model = OffsetModel()
        model.record("c:b", centerX = 250f, centerY = 225f, x = 250f, y = 255f)
        
        assertEquals(TapAmbiguity.SPACE_AMBIGUOUS, bands.classify("c:b", 250f, 240f, letters, space, null).kind)
        assertEquals(TapAmbiguity.NONE, bands.classify("c:b", 250f, 240f, letters, space, model).kind)
    }
    
    @Test
    fun `an invalid band fraction is rejected`() {
        assertThrows(IllegalArgumentException::class.java, { AmbiguityBands(1.5f) })
    }
}
