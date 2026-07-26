// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

/**
 * A language the on-device detector (A-03) can recognise. [UNKNOWN] is returned when the context is
 * too short or too ambiguous to decide - the caller then falls back to its default behaviour.
 *
 * D-280: [endonym] is the single source of truth for this language's display name, shared by every
 * settings screen with a language selector (`BlacklistActivity`, `LearnedWordsActivity`,
 * `LanguagePacksActivity`) and [de.froehlichmedia.adaptkey.AdaptKeyService]'s own G-01 space-bar label -
 * declared per-entry (not a separate lookup function) so the language-contribution guide's checklist for
 * adding a new language reduces to one line here, impossible to forget in a way a compiler would miss.
 *
 * @property code the short profile code used in the bundled `language_profiles.tsv` asset
 * @property endonym this language's own name for itself, as shown to the user
 */
enum class Language(val code: String, val endonym: String) {
    GERMAN("de", "Deutsch"),
    ENGLISH("en", "English"),
    GREEK("el", "Ελληνικά"),
    FRENCH("fr", "Français"),
    SPANISH("es", "Español"),
    ITALIAN("it", "Italiano"),
    DUTCH("nl", "Nederlands"),
    PORTUGUESE("pt", "Português"),
    UNKNOWN("??", "?");
    
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
