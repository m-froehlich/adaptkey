package de.froehlichmedia.adaptkey.emoji

import android.content.Context
import org.json.JSONArray

/**
 * Persists the recent-emoji list (L-03) across sessions as a compact JSON array in
 * {@link android.content.SharedPreferences}, mirroring the existing store pattern (e.g.
 * {@code touch.OffsetStore}).
 */
object RecentEmojiStore {
    
    private const val PREFS = "adaptkey_emoji"
    private const val KEY_RECENTS = "recents"
    
    /**
     * @param context any valid context (the input method service)
     * @return the persisted recent-emoji list (most recent first), or empty when nothing is stored
     */
    fun load(context: Context): List<String> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_RECENTS, null)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList())
    }
    
    /**
     * @param context any valid context (the input method service)
     * @param recents the recent-emoji list to persist (most recent first)
     */
    fun save(context: Context, recents: List<String>) {
        val array = JSONArray()
        recents.forEach { array.put(it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENTS, array.toString())
            .apply()
    }
}
