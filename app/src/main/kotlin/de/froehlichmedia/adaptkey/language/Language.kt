package de.froehlichmedia.adaptkey.language

/**
 * A language the on-device detector (A-03) can recognise. [UNKNOWN] is returned when the context is
 * too short or too ambiguous to decide - the caller then falls back to its default behaviour.
 *
 * @property code the short profile code used in the bundled `language_profiles.tsv` asset
 */
enum class Language(val code: String) {
    GERMAN("de"),
    ENGLISH("en"),
    GREEK("el"),
    FRENCH("fr"),
    SPANISH("es"),
    ITALIAN("it"),
    DUTCH("nl"),
    PORTUGUESE("pt"),
    UNKNOWN("??");
    
    companion object {
        
        /**
         * @param code a profile code such as `de`
         * @return the matching language, or null when no language uses that code
         */
        fun fromCode(code: String): Language? {
            return entries.firstOrNull { it.code == code }
        }
    }
}
