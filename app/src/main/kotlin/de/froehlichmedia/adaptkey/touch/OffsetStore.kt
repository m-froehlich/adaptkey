// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists an {@link OffsetModel}'s aggregated statistics across sessions (T-03).
 *
 * The full raw tap log described in the specification (one `(touch_x, touch_y, confirmed_key)`
 * tuple per event, in SQLite) arrives with the dictionary-infrastructure session. For now the
 * model's sufficient statistics - which fully determine the per-key Gaussian - are stored as a
 * compact JSON blob in {@link android.content.SharedPreferences}, surviving app restarts.
 */
object OffsetStore {
    
    private const val PREFS = "adaptkey_offset_model"
    private const val KEY_STATS = "stats"
    private const val KEY_PATTERN = "typing_pattern"
    
    /**
     * Loads the persisted model.
     *
     * @param context any valid context (the input method service)
     * @return a model populated from storage, or an empty model when nothing is stored or parsing fails
     */
    fun load(context: Context): OffsetModel {
        val model = OffsetModel()
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_STATS, null)
            ?: return model
        runCatching {
            val obj = JSONObject(json)
            val data = HashMap<String, OffsetModel.Stat>()
            for (id in obj.keys()) {
                val array = obj.getJSONArray(id)
                val count = array.getLong(0)
                data[id] = OffsetModel.Stat(
                    count = count,
                    meanDx = array.getDouble(1),
                    meanDy = array.getDouble(2),
                    m2Dx = array.getDouble(3),
                    m2Dy = array.getDouble(4),
                    // Contact-area stats (T-04) were added later; older blobs omit them.
                    sizeCount = if (array.length() > 5) array.getLong(5) else 0L,
                    meanSize = if (array.length() > 6) array.getDouble(6) else 0.0,
                    // D-159: weightSum was added later still - an older blob's every recorded sample was
                    // genuinely unweighted (full weight 1.0 each), so weightSum == count is the correct
                    // migration, not just a convenient placeholder.
                    weightSum = if (array.length() > 7) array.getDouble(7) else count.toDouble()
                )
            }
            model.restore(data)
        }
        return model
    }
    
    /**
     * Persists the model's current statistics.
     *
     * @param context any valid context (the input method service)
     * @param model the model to store
     */
    fun save(context: Context, model: OffsetModel) {
        val obj = JSONObject()
        for ((id, stat) in model.snapshot()) {
            val array = JSONArray()
            array.put(stat.count)
            array.put(stat.meanDx)
            array.put(stat.meanDy)
            array.put(stat.m2Dx)
            array.put(stat.m2Dy)
            array.put(stat.sizeCount)
            array.put(stat.meanSize)
            array.put(stat.weightSum)
            obj.put(id, array)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STATS, obj.toString())
            .apply()
    }
    
    /**
     * Persists the most recently detected typing pattern (T-04) so the settings screen, which has no
     * laid-out keyboard, can display it. Stored alongside the model because it is derived from it.
     *
     * @param context any valid context (the input method service)
     * @param pattern the detected pattern
     */
    fun saveDetectedPattern(context: Context, pattern: TypingPattern) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PATTERN, pattern.name)
            .apply()
    }
    
    /**
     * Loads the most recently detected typing pattern (T-04).
     *
     * @param context any valid context
     * @return the stored pattern, or [TypingPattern.UNKNOWN] when none is stored or it is unparseable
     */
    fun loadDetectedPattern(context: Context): TypingPattern {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PATTERN, null)
            ?: return TypingPattern.UNKNOWN
        return runCatching { TypingPattern.valueOf(name) }.getOrDefault(TypingPattern.UNKNOWN)
    }
}
