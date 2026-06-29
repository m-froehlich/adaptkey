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
                data[id] = OffsetModel.Stat(
                    count = array.getLong(0),
                    meanDx = array.getDouble(1),
                    meanDy = array.getDouble(2),
                    m2Dx = array.getDouble(3),
                    m2Dy = array.getDouble(4)
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
            obj.put(id, array)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STATS, obj.toString())
            .apply()
    }
}
