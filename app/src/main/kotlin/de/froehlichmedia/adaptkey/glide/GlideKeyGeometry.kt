// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.glide

/**
 * The rendered letter-key centres a glide stroke is scored against (D-516, plan §4.2: "Tastenzentren
 * kommen aus dem gerenderten Layout"). Pure data: the R2 view wiring builds it from
 * `AdaptKeyboardView`'s own rendered key bounds; tests build it from a synthetic row grid. Only
 * characters that are actual letter keys of the active layout belong here - a candidate whose folded
 * spelling contains a character without a centre is not typeable as one stroke and is skipped.
 */
class GlideKeyGeometry(private val centers: Map<Char, GlidePoint>) {

    /**
     * The smallest distance between any two key centres - the natural scale of this geometry. Used
     * to keep endpoint tolerances and point caps resolution-independent (they scale with the
     * layout's own key size rather than a fixed pixel figure).
     */
    val minKeyPitch: Double = run {
        var minimum = Double.MAX_VALUE
        val values = centers.values.toList()
        for (i in values.indices) {
            for (j in i + 1 until values.size) {
                val d = values[i].distanceTo(values[j])
                if (d < minimum) {
                    minimum = d
                }
            }
        }
        minimum
    }

    /** The centre of letter key [c], or null when the layout has no such letter key. */
    fun centerOf(c: Char): GlidePoint? = centers[c]

    /**
     * The letter key whose centre is closest to [p]. Never null when the geometry is non-empty -
     * callers that need "the point is actually near a key" apply their own distance bound against
     * [minKeyPitch] (see [GlideRecognizer]'s endpoint prefilter).
     */
    fun nearestKey(p: GlidePoint): Char? {
        var best: Char? = null
        var bestDistance = Double.MAX_VALUE
        for ((c, center) in centers) {
            val d = p.distanceTo(center)
            if (d < bestDistance) {
                bestDistance = d
                best = c
            }
        }
        return best
    }

    companion object {

        /**
         * Builds a left-aligned grid from [rows] (top to bottom, letters left to right) with fixed
         * [keyWidth]/[keyHeight] cells. The recognizer only ever compares against paths built from
         * the same map, so the simplified left-aligned geometry is exactly as valid as the real
         * rendered layout - and it is what the synthetic-trace tests walk.
         */
        fun fromRows(rows: List<String>, keyWidth: Double, keyHeight: Double): GlideKeyGeometry {
            val centers = LinkedHashMap<Char, GlidePoint>()
            rows.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { columnIndex, c ->
                    if (c !in centers) {
                        centers[c] = GlidePoint(
                            (columnIndex + 0.5) * keyWidth,
                            (rowIndex + 0.5) * keyHeight
                        )
                    }
                }
            }
            return GlideKeyGeometry(centers)
        }
    }
}
