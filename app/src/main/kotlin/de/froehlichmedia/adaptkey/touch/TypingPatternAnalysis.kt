package de.froehlichmedia.adaptkey.touch

/**
 * Bridges the personal offset model (T-03) and the typing-pattern classifier (T-04).
 *
 * It turns the raw per-key statistics plus the keyboard geometry into the normalised [KeySample]s the
 * classifier consumes, then runs the classification. Kept Android-free so the whole T-04 pipeline can
 * be unit-tested on the JVM; the caller supplies the geometry from the laid-out keyboard view.
 */
object TypingPatternAnalysis {
    
    /**
     * Builds the normalised per-key evidence from the model and the char-key geometry.
     *
     * @param model the personal offset model
     * @param geometry the char keys' geometry (centre and half-size), typically from the keyboard view
     * @param keyboardWidth the total keyboard width in pixels, used to normalise the x positions
     * @return one [KeySample] per trained key, or an empty list when [keyboardWidth] is non-positive
     */
    fun samples(
        model: OffsetModel,
        geometry: List<OffsetModel.Candidate>,
        keyboardWidth: Float
    ): List<KeySample> {
        if (keyboardWidth <= 0f) {
            return emptyList()
        }
        val result = ArrayList<KeySample>()
        for (candidate in geometry) {
            val stat = model.statFor(candidate.id) ?: continue
            if (stat.count == 0L) {
                continue
            }
            val lateralFraction = if (candidate.halfWidth > 0f) stat.meanDx / candidate.halfWidth else 0.0
            val verticalFraction = if (candidate.halfHeight > 0f) stat.meanDy / candidate.halfHeight else 0.0
            val contactArea = model.meanContactArea(candidate.id)
            val normalizedX = (candidate.centerX / keyboardWidth).toDouble()
            result.add(KeySample(normalizedX, lateralFraction, verticalFraction, contactArea, stat.count))
        }
        return result
    }
    
    /**
     * Convenience: builds the samples and classifies them in one call.
     *
     * @param model the personal offset model
     * @param geometry the char keys' geometry
     * @param keyboardWidth the total keyboard width in pixels
     * @param classifier the classifier to use; defaults to the standard thresholds
     * @return the detected typing pattern
     */
    fun classify(
        model: OffsetModel,
        geometry: List<OffsetModel.Candidate>,
        keyboardWidth: Float,
        classifier: TypingPatternClassifier = TypingPatternClassifier()
    ): TypingPattern {
        return classifier.classify(samples(model, geometry, keyboardWidth))
    }
}
