// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * D-401-followup: works out which line the cursor gesture's own moving end currently sits on, from the
 * three pieces of text an `InputConnection` can actually hand over.
 *
 * **Why this is not simply "scan backwards from the caret".** `InputConnection` has no notion of "the
 * caret" while a selection exists - it has a selection, and its two read calls are anchored to that
 * selection's *ends*: `getTextBeforeCursor()` returns the text before the selection's **start**, and
 * `getTextAfterCursor()` the text after its **end**. The gesture's moving end is only one of those two, so
 * on the side where the selection sits between the moving end and the text, the selection's own content
 * ([selectedText]) is the missing piece that has to be stitched back in. Extracted here as pure logic
 * precisely because getting that stitching wrong is not hypothetical: a device log showed Stage 2 freezing
 * after its very first move, because the previous version searched *only* the selection for a line break
 * and gave up entirely when it found none - which is the normal case, since selections rarely span lines.
 */
object CursorLineBounds {
    
    /**
     * The offsets of the first and last position on the line containing [position].
     *
     * A line break inside [selectedText] counts exactly like one in the surrounding text; a line with no
     * break on a given side simply extends to the edge of whatever text was supplied, which is the read
     * window's own limit rather than a real document boundary. That is deliberate and harmless here: the
     * result is only ever used to clamp a drag, and no realistic line reaches the far end of the window.
     *
     * @param position the moving end's own absolute offset - the caret in Stage 1, the dragged end of the
     *        selection in Stage 2
     * @param textBeforeSelection the text immediately before the selection's start
     * @param selectedText the selection's own text; empty when the selection is collapsed, in which case
     *        [movingEndIsSelectionEnd] makes no difference
     * @param textAfterSelection the text immediately after the selection's end
     * @param movingEndIsSelectionEnd whether [position] is the selection's end (the drag went forwards)
     *        rather than its start (the drag went backwards)
     * @return the line's own first and last addressable offset; the two are equal on an empty line
     */
    fun of(
        position: Int,
        textBeforeSelection: String,
        selectedText: String,
        textAfterSelection: String,
        movingEndIsSelectionEnd: Boolean
    ): IntRange {
        require(position >= 0) { "position must not be negative: $position" }
        val before = if (movingEndIsSelectionEnd) textBeforeSelection + selectedText else textBeforeSelection
        val after = if (movingEndIsSelectionEnd) textAfterSelection else selectedText + textAfterSelection
        val start = position - (before.length - (before.lastIndexOf('\n') + 1))
        val newlineAfter = after.indexOf('\n')
        val end = position + if (newlineAfter == -1) after.length else newlineAfter
        return start..end
    }
}
