package de.froehlichmedia.adaptkey.touch

import kotlin.math.abs

/**
 * A key's pixel bounds, free of Android types so the ambiguity geometry can be unit-tested.
 *
 * @property id the key id (matches [OffsetModel] keys, e.g. {@code "c:b"})
 * @property char the key's character, or null for the space bar
 * @property left left edge in view pixels
 * @property top top edge in view pixels
 * @property right right edge in view pixels
 * @property bottom bottom edge in view pixels
 */
data class KeyBox(
    val id: String,
    val char: Char?,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    
    /** @return the horizontal centre of the box */
    val centerX: Float
        get() = (left + right) / 2f
    
    /** @return the box height */
    val height: Float
        get() = bottom - top
    
    /** @return half the box width */
    val halfWidth: Float
        get() = (right - left) / 2f
}

/** The space/letter confusion classification of a single tap (T-05). */
enum class TapAmbiguity {
    
    /** The tap is unambiguous. */
    NONE,
    
    /** A letter tapped in its lower edge band - the committed letter may be an intended space (A-05). */
    SPACE_AMBIGUOUS,
    
    /** A space tapped in its upper edge band - the committed space may be an intended letter (A-06). */
    LETTER_AMBIGUOUS
}

/**
 * The result of classifying a tap: its ambiguity kind and, for [TapAmbiguity.LETTER_AMBIGUOUS], the
 * nearest letter inferred from the tap's x-coordinate.
 *
 * @property kind the ambiguity classification
 * @property inferredChar the inferred letter for a letter-ambiguous space, otherwise null
 */
data class AmbiguityResult(
    val kind: TapAmbiguity,
    val inferredChar: Char? = null
)

/**
 * Classifies a bottom-row tap into the two T-05 ambiguity bands.
 *
 * The boundary between the space bar and the bottom letter row ({@code c v b n m}) is a high-risk zone
 * for swapped space/letter input, aggravated by the narrower space bar (L-02). A tap in the lower edge
 * band of one of those letters is flagged [TapAmbiguity.SPACE_AMBIGUOUS] (the letter may be an intended
 * space, cf. A-05); a tap in the upper edge band of the space bar is flagged
 * [TapAmbiguity.LETTER_AMBIGUOUS] with the nearest letter inferred from the tap's x-coordinate via the
 * personal offset model (T-03, cf. A-06).
 *
 * The band depth is a fraction of the key height, so it scales with the configured key proportions
 * (C-01); when an [OffsetModel] is supplied it both refines the band edge (by the key's learned
 * vertical mean) and compensates the nearest-letter inference (by each candidate's learned horizontal
 * mean). The class is Android-free and unit-tested.
 *
 * @property bandFraction the edge-band depth as a fraction of the key height (0..1)
 */
class AmbiguityBands(private val bandFraction: Float = DEFAULT_BAND_FRACTION) {
    
    init {
        require(bandFraction in 0f..1f) { "bandFraction must be in 0..1" }
    }
    
    /**
     * Classifies a tap given the resolved key and the relevant bottom-row geometry.
     *
     * @param resolvedId the id of the key the tap resolved to
     * @param x the raw tap x (T-01 ACTION_DOWN)
     * @param y the raw tap y (T-01 ACTION_DOWN)
     * @param bottomLetters the bounds of the bottom letter keys above the space bar ({@code c v b n m})
     * @param space the bounds of the space bar, or null when it is not present
     * @param model the personal offset model for band/inference refinement, or null
     * @return the ambiguity classification for this tap
     */
    fun classify(
        resolvedId: String,
        x: Float,
        y: Float,
        bottomLetters: List<KeyBox>,
        space: KeyBox?,
        model: OffsetModel? = null
    ): AmbiguityResult {
        val letter = bottomLetters.firstOrNull { it.id == resolvedId }
        if (letter != null) {
            val band = letter.height * bandFraction
            val threshold = letter.bottom - band + verticalShift(model, letter, band)
            val kind = if (y > threshold) TapAmbiguity.SPACE_AMBIGUOUS else TapAmbiguity.NONE
            return AmbiguityResult(kind)
        }
        if (space != null && space.id == resolvedId) {
            val band = space.height * bandFraction
            val threshold = space.top + band + verticalShift(model, space, band)
            if (y < threshold) {
                return AmbiguityResult(TapAmbiguity.LETTER_AMBIGUOUS, nearestLetter(x, bottomLetters, model))
            }
            return AmbiguityResult(TapAmbiguity.NONE)
        }
        return AmbiguityResult(TapAmbiguity.NONE)
    }
    
    /**
     * Infers the bottom-row letter nearest to a tap's x-coordinate, compensated by each candidate's
     * learned horizontal offset (T-03) when a model is supplied.
     *
     * @param x the raw tap x
     * @param bottomLetters the candidate letter keys
     * @param model the personal offset model, or null for pure geometry
     * @return the inferred letter, or null when there are no candidates
     */
    fun nearestLetter(x: Float, bottomLetters: List<KeyBox>, model: OffsetModel? = null): Char? {
        return bottomLetters.minByOrNull { box ->
            val compensatedCenter = box.centerX + horizontalShift(model, box)
            abs(x - compensatedCenter)
        }?.char
    }
    
    private fun horizontalShift(model: OffsetModel?, box: KeyBox): Float {
        if (model == null) {
            return 0f
        }
        val cap = box.halfWidth.toDouble()
        return model.cappedMeanOffset(box.id, cap, cap).first.toFloat()
    }
    
    private fun verticalShift(model: OffsetModel?, box: KeyBox, band: Float): Float {
        if (model == null) {
            return 0f
        }
        val cap = band.toDouble()
        return model.cappedMeanOffset(box.id, cap, cap).second.toFloat()
    }
    
    companion object {
        
        /** Default edge-band depth: a quarter of the key height. */
        const val DEFAULT_BAND_FRACTION = 0.25f
    }
}
