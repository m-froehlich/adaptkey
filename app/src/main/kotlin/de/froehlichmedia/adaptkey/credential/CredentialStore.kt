// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.credential

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * D-142: persists learned login-field values (usernames/email addresses) as a compact JSON array in its
 * own private [android.content.SharedPreferences] file, mirroring the existing store pattern (e.g.
 * [de.froehlichmedia.adaptkey.emoji.RecentEmojiStore]) - deliberately its own file, entirely separate
 * from the ordinary word dictionary and every other store, since this one holds real identifying data
 * the user should be able to reason about (and clear) on its own.
 */
object CredentialStore {
    
    private const val PREFS = "adaptkey_credentials"
    private const val KEY_ENTRIES = "entries"
    private const val FIELD_VALUE = "value"
    private const val FIELD_KIND = "kind"
    private const val FIELD_FREQUENCY = "frequency"
    
    /**
     * @param context any valid context (the input method service)
     * @return every learned credential entry, or empty when nothing is stored
     */
    fun all(context: Context): List<CredentialEntry> {
        val json = prefs(context).getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                val kind = runCatching { LoginFieldKind.valueOf(obj.getString(FIELD_KIND)) }.getOrNull()
                    ?: return@mapNotNull null
                CredentialEntry(obj.getString(FIELD_VALUE), kind, obj.getLong(FIELD_FREQUENCY))
            }
        }.getOrDefault(emptyList())
    }
    
    /**
     * Learns one observation of [value] immediately - no D-37-style promotion threshold, since deliberate
     * credential input (typing a real username/email into a recognised login field) is precise by
     * construction, not the kind of one-off typo D-37's threshold exists to guard against.
     *
     * Identity is case-insensitive (re-typing the same address in different casing reinforces the one
     * saved entry instead of creating a duplicate); on a repeat observation, [kind] and the stored casing
     * always take the latest values - so a value first learned as a bare username and later typed with an
     * `@` becomes an EMAIL entry going forward, rather than being stuck as USERNAME forever.
     *
     * @param context any valid context (the input method service)
     * @param value the credential value exactly as typed
     * @param kind [LoginFieldKind.USERNAME] or [LoginFieldKind.EMAIL] - [LoginFieldKind.PASSWORD] and
     *        [LoginFieldKind.NONE] are silently ignored, never stored
     */
    fun learn(context: Context, value: String, kind: LoginFieldKind) {
        if (value.isBlank() || kind == LoginFieldKind.NONE || kind == LoginFieldKind.PASSWORD) {
            return
        }
        val entries = all(context).toMutableList()
        val index = entries.indexOfFirst { it.value.equals(value, ignoreCase = true) }
        if (index >= 0) {
            val existing = entries[index]
            entries[index] = existing.copy(value = value, kind = kind, frequency = existing.frequency + 1L)
        } else {
            entries.add(CredentialEntry(value, kind, 1L))
        }
        save(context, entries)
    }
    
    /**
     * Deletes every learned credential entry (a settings action, mirroring the existing T-03/dictionary
     * reset flow) - the whole point of a separate store is that this data can be wiped on its own, without
     * touching anything else the keyboard has learned.
     *
     * @param context any valid context (the input method service)
     */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_ENTRIES).apply()
    }
    
    /**
     * D-180: removes one learned credential entry outright, regardless of its accumulated frequency -
     * the settings screen's own per-entry removal, complementing [clear]'s delete-everything action.
     *
     * @param context any valid context (the input method service)
     * @param value the credential value to remove (case-insensitive, matching [learn]'s own identity rule)
     */
    fun forget(context: Context, value: String) {
        val entries = all(context).filterNot { it.value.equals(value, ignoreCase = true) }
        save(context, entries)
    }
    
    /**
     * @param context any valid context (the input method service)
     * @return true when no credential has been learned yet
     */
    fun isEmpty(context: Context): Boolean {
        return all(context).isEmpty()
    }
    
    private fun save(context: Context, entries: List<CredentialEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put(FIELD_VALUE, entry.value)
            obj.put(FIELD_KIND, entry.kind.name)
            obj.put(FIELD_FREQUENCY, entry.frequency)
            array.put(obj)
        }
        prefs(context).edit().putString(KEY_ENTRIES, array.toString()).apply()
    }
    
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
