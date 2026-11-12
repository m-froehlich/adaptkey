// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.glide

/**
 * The glide stroke's pure state machine (D-517, plan §4.1): the view feeds raw move points, this
 * class answers the three questions the touch layer has - is this stroke a glide yet (the claim),
 * should a recognition update be computed now (the throttle), and is the stroke still live. The view
 * keeps every UI/commit side: it owns the touch stream, the trail drawing and the listener calls;
 * this class owns only the classification and the stroke geometry.
 *
 * Claim rule (plan §4.1.2, all constants hardcoded starting points for R3's device calibration -
 * deliberately NOT coupled to `d508_field_swipe_scale`, which calibrates a different question): a
 * stroke claims as glide once it has passed at least [MIN_DISTINCT_KEYS] different letter keys in
 * sufficient proximity ([PASSING_RADIUS_FACTOR] of the layout's own minimum key pitch), its path
 * clearly exceeds the origin key's own width ([MIN_PATH_FACTOR]), and it has lasted longer than
 * [MIN_CLAIM_MS] - the last gate separating glide from the fastest legitimate two-key tap sweep.
 * Before the claim the stroke is an ordinary tap candidate: all existing key behaviour (long-press
 * popup arming, T-01 tap resolution) stays untouched, and a stroke that never claims falls back to
 * the existing tap/swipe resolution at release exactly as today.
 *
 * The second-finger rule (D-495) is enforced by the view, which kills the stroke via [cancel]; the
 * conflict matrix this classification implements (D-19 page-swipe swallow, G-03 dominance gate,
 * D-380 smear protection change, popup suppression after the claim) lives at the view's own dispatch
 * points and consults [claimed] there - this class deliberately knows nothing about those gestures.
 *
 * Pure by design (no Android imports): the geometry comes in as a [GlideKeyGeometry] value per
 * stroke, times come in as plain millis. Unit-tested directly; the view stays in the project's
 * accepted untested Android-glue layer.
 */
class GlideGestureController {

    /** What a move asks the view to do next. */
    enum class Decision {

        /** Nothing to do yet - the stroke has not claimed and no update is due. */
        NONE,

        /** The stroke just claimed as glide this move (exactly once per stroke). */
        CLAIMED,

        /** The stroke is claimed and an (already throttled) recognition update is due. */
        UPDATE
    }

    private var geometry: GlideKeyGeometry? = null
    private var originKeyWidthPx = 0.0
    private var startTimeMs = 0L
    private var lastUpdateTimeMs = 0L
    private var lastEventTimeMs = 0L
    private val strokePoints = ArrayList<GlidePoint>()
    private var lastStrokeSummary = ""

    /** Whether a stroke is live at all (begun, not yet released/cancelled). */
    var hasStroke: Boolean = false
        private set

    /** Whether the live stroke has claimed as glide. */
    var claimed: Boolean = false
        private set

    /**
     * Begins a potential glide stroke. Only the view decides eligibility (letters surface, letter
     * key, the feature and field gates) - this method just records the stroke's origin.
     */
    fun begin(
        geometry: GlideKeyGeometry,
        originKeyWidthPx: Float,
        x: Float,
        y: Float,
        timeMs: Long
    ) {
        this.geometry = geometry
        this.originKeyWidthPx = originKeyWidthPx.toDouble()
        strokePoints.clear()
        strokePoints.add(GlidePoint(x.toDouble(), y.toDouble()))
        startTimeMs = timeMs
        lastUpdateTimeMs = 0L
        lastEventTimeMs = timeMs
        hasStroke = true
        claimed = false
    }

    /**
     * Feeds one move. Returns [Decision.CLAIMED] exactly once, at the claim; [Decision.UPDATE] at
     * most once per [UPDATE_THROTTLE_MS] afterwards; [Decision.NONE] otherwise.
     */
    fun move(x: Float, y: Float, timeMs: Long): Decision {
        if (!hasStroke) {
            return Decision.NONE
        }
        strokePoints.add(GlidePoint(x.toDouble(), y.toDouble()))
        lastEventTimeMs = timeMs
        if (!claimed && satisfiesClaimRule(timeMs)) {
            claimed = true
            lastUpdateTimeMs = timeMs
            return Decision.CLAIMED
        }
        if (claimed && timeMs - lastUpdateTimeMs >= UPDATE_THROTTLE_MS) {
            lastUpdateTimeMs = timeMs
            return Decision.UPDATE
        }
        return Decision.NONE
    }

    /** Ends the stroke (release or cancel side); the view reads [claimed] before calling this. */
    fun end() {
        if (hasStroke) {
            lastStrokeSummary = describeStroke()
        }
        hasStroke = false
        claimed = false
        geometry = null
        strokePoints.clear()
    }

    /** The stroke's raw points, for the recognizer. Empty when no stroke is live. */
    val points: List<GlidePoint>
        get() = strokePoints

    /**
     * D-534: one locale-free diagnostic line about the stroke - raw point count, path length in px,
     * path length in origin-key widths (integer hundredths, `pathWidths=142` meaning 1.42), how many
     * distinct letter keys came within the passing radius, and the elapsed duration in ms. Exactly
     * the facts a community repro needs to say WHY a stroke never claimed (which of the three claim
     * gates failed) or how far a committed one actually travelled. Survives [end] - an ended stroke
     * reports its own final summary - and returns "" when no stroke was ever begun since the last
     * clear.
     */
    fun strokeSummary(): String {
        if (hasStroke) {
            return describeStroke()
        }
        return lastStrokeSummary
    }

    private fun describeStroke(): String {
        var pathLength = 0.0
        for (i in 1 until strokePoints.size) {
            pathLength += strokePoints[i - 1].distanceTo(strokePoints[i])
        }
        var distinct = 0
        geometry?.let { layout ->
            val passed = HashSet<Char>()
            for (point in strokePoints) {
                val key = layout.nearestKey(point) ?: continue
                val center = layout.centerOf(key) ?: continue
                if (point.distanceTo(center) <= PASSING_RADIUS_FACTOR * layout.minKeyPitch) {
                    passed.add(key)
                }
            }
            distinct = passed.size
        }
        val pathWidths = if (originKeyWidthPx > 0.0) (pathLength / originKeyWidthPx * 100).toInt() else 0
        return "points=${strokePoints.size} pathPx=${pathLength.toInt()} pathWidths=$pathWidths " +
            "distinctKeys=$distinct durationMs=${(lastEventTimeMs - startTimeMs).coerceAtLeast(0)}"
    }

    /**
     * The letter sequence the stroke visited, for the unknown-word fallback (plan §8.3: when no
     * dictionary candidate exists, the recognized letter sequence is inserted as a word). Walks the
     * speed-invariantly resampled stroke and appends a letter exactly when a stroke point sits
     * clearly inside one key's neighbourhood - near its centre ([PASSING_RADIUS_FACTOR] of the
     * minimum key pitch) *and* unambiguously nearer to it than to any other key
     * ([DOMINANCE_MARGIN]); a point midway between two centres names neither (picking either would
     * be arbitrary), and consecutive repeats collapse (a dwell over one key is one letter). A
     * stroke that sweeps straight along a row does therefore read as the sequence of key centres it
     * actually crossed - that is what "the recognized letter sequence" means geometrically. Empty
     * when no stroke is live or no key came near enough.
     */
    fun inferredLetterSequence(): String {
        val layout = geometry ?: return ""
        val resampled = GlideTrace.resample(strokePoints)?.points ?: strokePoints
        val builder = StringBuilder()
        var last: Char? = null
        for (point in resampled) {
            var nearest: Char? = null
            var nearestDistance = Double.MAX_VALUE
            var secondDistance = Double.MAX_VALUE
            for ((key, center) in layout.centers) {
                val d = point.distanceTo(center)
                when {
                    d < nearestDistance -> {
                        secondDistance = nearestDistance
                        nearestDistance = d
                        nearest = key
                    }
                    d < secondDistance -> secondDistance = d
                }
            }
            val key = nearest ?: continue
            if (nearestDistance > PASSING_RADIUS_FACTOR * layout.minKeyPitch ||
                secondDistance - nearestDistance <= DOMINANCE_MARGIN * layout.minKeyPitch
            ) {
                continue
            }
            if (key != last) {
                builder.append(key)
                last = key
            }
        }
        return builder.toString()
    }

    /**
     * The plan's three-part claim rule, evaluated against the stroke as it stands. Cheap: the
     * distinct-key pass count is recomputed per move over the raw points (a stroke has tens of
     * points and the geometry tens of keys).
     */
    private fun satisfiesClaimRule(timeMs: Long): Boolean {
        val layout = geometry ?: return false
        var pathLength = 0.0
        for (i in 1 until strokePoints.size) {
            pathLength += strokePoints[i - 1].distanceTo(strokePoints[i])
        }
        if (pathLength <= originKeyWidthPx * MIN_PATH_FACTOR) {
            return false
        }
        val passed = HashSet<Char>()
        for (point in strokePoints) {
            val key = layout.nearestKey(point) ?: continue
            val center = layout.centerOf(key) ?: continue
            if (point.distanceTo(center) <= PASSING_RADIUS_FACTOR * layout.minKeyPitch) {
                passed.add(key)
                if (passed.size >= MIN_DISTINCT_KEYS) {
                    break
                }
            }
        }
        return passed.size >= MIN_DISTINCT_KEYS && timeMs - startTimeMs >= MIN_CLAIM_MS
    }

    companion object {

        /** Distinct letter keys that must come near the stroke before it claims (plan §4.1.2). */
        const val MIN_DISTINCT_KEYS = 2

        /** "In ausreichender Nähe": within this fraction of the layout's own minimum key pitch. */
        const val PASSING_RADIUS_FACTOR = 0.5

        /**
         * The inference's unambiguity margin (see [inferredLetterSequence]): a point must be this
         * much nearer to its nearest key than to the runner-up before that key is appended.
         */
        const val DOMINANCE_MARGIN = 0.25

        /** "Die Tastenbreite clearly übersteigt": the path must clear this multiple of the origin key's width. */
        const val MIN_PATH_FACTOR = 1.5

        /**
         * Minimum stroke duration in ms, separating glide from the fastest legitimate tap sweep
         * between two keys. Considered starting point, R3-calibrated.
         */
        const val MIN_CLAIM_MS = 120L

        /**
         * Recognition update throttle (plan §3: "gedrosselt (~30–50 ms)"). One live update per
         * throttle window; the release always computes its own definitive pass.
         */
        const val UPDATE_THROTTLE_MS = 40L
    }
}
