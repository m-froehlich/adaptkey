// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, real view inflation) for [AdaptKeyboardView]'s D-554 swipe-open number-row
 * reveal (plan `swipe-up-number-row.md`) - the geometry assertions need a real context (resources for
 * the dp dimensions, Paint setup), so they run under Robolectric. The reveal must be, by construction,
 * indistinguishable from a C-09-on letters surface: same row count (same measured height, D-55
 * spacing included, since [AdaptKeyboardView.extraSpacingPx] keys off the built rows), same char keys
 * (the shared L-06 number-row builder), no effect on the symbol pages (which ignore C-09 either way),
 * and a clean return to the C-09-off state once the reveal closes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NumberRowRevealRoboTest {
    
    private fun newView(): AdaptKeyboardView = AdaptKeyboardView(RuntimeEnvironment.getApplication())
    
    /**
     * Measures with a real width and lays out, so [AdaptKeyboardView.layoutKeys] fills the key rects
     * ([AdaptKeyboardView.charKeyGeometry] reads them) exactly like a real keyboard presentation would.
     *
     * @return the view's measured height after the layout pass
     */
    private fun layout(view: AdaptKeyboardView): Int {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(TEST_WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, TEST_WIDTH_PX, view.measuredHeight)
        return view.measuredHeight
    }
    
    private fun charKeyCount(view: AdaptKeyboardView): Int = view.charKeyGeometry().size
    
    @Test
    fun `D-554 the reveal adds the number row to the C-09-off letters surface`() {
        val view = newView()
        view.showNumberRow = false
        val hiddenHeight = layout(view)
        val hiddenCharKeys = charKeyCount(view)
        
        view.numberRowRevealed = true
        val revealedHeight = layout(view)
        val revealedCharKeys = charKeyCount(view)
        
        // One more row's worth of height, and the ten digits join the letters as char keys.
        assertTrue(revealedHeight > hiddenHeight)
        assertEquals(hiddenCharKeys + 10, revealedCharKeys)
    }
    
    @Test
    fun `D-554 the revealed row is indistinguishable from the C-09-on row`() {
        val c09On = newView().apply { showNumberRow = true }
        val revealed = newView().apply {
            showNumberRow = false
            numberRowRevealed = true
        }
        assertEquals(layout(c09On), layout(revealed))
        assertEquals(charKeyCount(c09On), charKeyCount(revealed))
    }
    
    @Test
    fun `D-554 the reveal is redundant while C-09 already shows the row`() {
        val view = newView()
        view.showNumberRow = true
        val c09Height = layout(view)
        val c09CharKeys = charKeyCount(view)
        
        view.numberRowRevealed = true
        
        assertEquals(c09Height, layout(view))
        assertEquals(c09CharKeys, charKeyCount(view))
    }
    
    @Test
    fun `D-554 the symbol pages ignore the reveal like they ignore C-09`() {
        val view = newView()
        view.surface = InputSurface.SYMBOLS
        view.symbolPage = 1
        val withoutReveal = layout(view)
        
        view.numberRowRevealed = true
        
        assertEquals(withoutReveal, layout(view))
    }
    
    @Test
    fun `D-554 closing the reveal restores the exact C-09-off state`() {
        val view = newView()
        view.showNumberRow = false
        val hiddenHeight = layout(view)
        val hiddenCharKeys = charKeyCount(view)
        
        view.numberRowRevealed = true
        layout(view)
        view.numberRowRevealed = false
        
        assertEquals(hiddenHeight, layout(view))
        assertEquals(hiddenCharKeys, charKeyCount(view))
    }
    
    companion object {
        
        /** A realistic phone-portrait keyboard width; only needs to be non-zero for [layout] to key the rows. */
        private const val TEST_WIDTH_PX = 1080
    }
}
