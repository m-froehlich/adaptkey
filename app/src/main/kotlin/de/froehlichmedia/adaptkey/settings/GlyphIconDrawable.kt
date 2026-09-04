// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat

/**
 * D-361-followup: a single Unicode symbol character rendered as a monochrome icon, for the settings
 * screen's own sub-screen entry points (Calibration, Language packs, Blacklist, ...) - the user's own
 * explicit call: a proper glyph/character-based icon needs no bundled image asset (and so no separate
 * licence to source and credit, unlike a bitmap/vector icon set would), matching this app's own established
 * "no unnecessary asset pipeline" precedent (the extra row's own 🧲/📋 buttons are plain characters too).
 * Tinted to [android.R.attr.textColorSecondary] to match the muted, monochrome look Android's own Settings
 * icons use - deliberately not participating in [setTint]/[android.graphics.drawable.Drawable.DrawableContainer]-
 * style tint lists, since every call site here is a fixed, standalone icon, never swapped at runtime.
 *
 * @param glyph the single character (or short character sequence, e.g. one code point plus a variation
 *        selector) to draw, centred within this drawable's own bounds
 * @param sizeDp the drawable's own intrinsic width/height, in dp - still comfortably under the icon-frame's
 *        own real AndroidX default cap (`image_frame.xml`'s `maxWidth`/`maxHeight="48dp"`, confirmed
 *        directly in the §382/§383 investigation), so no ad-hoc host-side scaling is needed. D-361-followup
 *        (v2): widened from the original 24dp - reported as reading too small/thin on a real device once the
 *        first glyph set (thin-stroke Mathematical Operators/Arrows characters) shipped.
 */
class GlyphIconDrawable(context: Context, private val glyph: String, sizeDp: Float = 28f) : Drawable() {
    
    private val sizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeDp, context.resources.displayMetrics).toInt()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ResourcesCompat.getColor(context.resources, android.R.color.darker_gray, context.theme)
        val secondary = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.textColorSecondary, secondary, true)) {
            color = runCatching { ResourcesCompat.getColor(context.resources, secondary.resourceId, context.theme) }
                .getOrDefault(color)
        }
        // D-361-followup (v2): widened from 0.75f alongside sizeDp - the same "too small" report.
        textSize = sizePx * 0.9f
        textAlign = Paint.Align.CENTER
    }
    private val textBounds = Rect().also { paint.getTextBounds(glyph, 0, glyph.length, it) }
    
    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY() - textBounds.exactCenterY()
        canvas.drawText(glyph, cx, cy, paint)
    }
    
    override fun getIntrinsicWidth(): Int = sizePx
    override fun getIntrinsicHeight(): Int = sizePx
    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }
    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }
    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
