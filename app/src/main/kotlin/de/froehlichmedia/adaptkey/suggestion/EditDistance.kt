package de.froehlichmedia.adaptkey.suggestion

/**
 * Small string-distance helpers shared by the suggestion providers.
 */
object EditDistance {
    
    /**
     * Checks whether two strings differ by at most one single-character edit.
     *
     * @param a the first string
     * @param b the second string
     * @return true if [a] and [b] are equal or differ by exactly one insert, delete, or substitution
     */
    fun atMostOne(a: String, b: String): Boolean {
        val lengthDiff = a.length - b.length
        if (lengthDiff < -1 || lengthDiff > 1) {
            return false
        }
        if (a.length == b.length) {
            var mismatches = 0
            for (i in a.indices) {
                if (a[i] != b[i] && ++mismatches > 1) {
                    return false
                }
            }
            return true
        }
        // Lengths differ by exactly one: check for a single insert/delete.
        val shorter = if (a.length < b.length) a else b
        val longer = if (a.length < b.length) b else a
        var i = 0
        var j = 0
        var edited = false
        while (i < shorter.length && j < longer.length) {
            if (shorter[i] == longer[j]) {
                i++
                j++
            } else {
                if (edited) {
                    return false
                }
                edited = true
                j++
            }
        }
        return true
    }
}
