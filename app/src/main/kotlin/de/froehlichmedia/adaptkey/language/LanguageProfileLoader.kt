package de.froehlichmedia.adaptkey.language

import android.content.Context

/**
 * Loads the bundled `language_profiles.tsv` asset (A-03) and hands it to the pure
 * [LanguageProfileParser]. Android-only glue; left to instrumented tests.
 */
object LanguageProfileLoader {
    
    private const val ASSET_NAME = "language_profiles.tsv"
    
    /**
     * @param context any valid context (the input method service)
     * @return a classifier over the parsed profiles; over an empty profile set (so every result is
     *         [Language.UNKNOWN], a safe no-op guard) when the asset is missing or unreadable
     */
    fun loadClassifier(context: Context): LanguageClassifier {
        val profiles = runCatching {
            val raw = context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
            LanguageProfileParser.parse(raw)
        }.getOrDefault(emptyMap())
        return LanguageClassifier(profiles)
    }
}
