// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import de.froehlichmedia.adaptkey.keyboard.KeyProportions
import de.froehlichmedia.adaptkey.keyboard.KeyboardLayout
import de.froehlichmedia.adaptkey.prediction.LlmActivationThreshold
import de.froehlichmedia.adaptkey.suggestion.SuggestionConfig

/**
 * Fully resolved and validated keyboard configuration (C-01 … C-10).
 *
 * This is the bundle the running keyboard consumes: it is produced from the raw, persisted values by
 * [SettingsMapper], which clamps every value into the spec ranges (§10) so a corrupt or out-of-range
 * stored value can never violate the data-class init contracts of [KeyProportions] / [SuggestionConfig].
 * C-05 (the blacklist) lives in the SQLite dictionary, not here.
 *
 * @property keyProportions the key-proportion configuration (C-01)
 * @property suggestionConfig the suggestion-bar configuration (C-02 / C-03 / C-04)
 * @property showNumberRow whether the persistent number row is shown (C-09)
 * @property hintsEnabled whether the letter corner hints are drawn (C-08)
 * @property letterHints the per-letter secondary-symbol map (C-08)
 * @property shiftGraceWindowMs the shift grace window against surprising field capitalisation (C-07,
 *           0-500 ms); persisted only, the consuming logic does not exist yet
 * @property commaLineNotSentenceStart whether the content line after a comma-terminated line is not a
 *           sentence start (§6, e-mail salutation; C-10, default on)
 * @property llmActivationThreshold the C-06 tier-1 confidence below which the mini-LLM (tier 3) is
 *           consulted (§9); drives the tier-3 orchestration, inert while the backend is the no-op stub
 * @property keySoundEnabled whether a click sound plays on each key press (D-05, default off)
 * @property keyHapticsEnabled whether a short vibration fires on each key press (D-06, default off)
 * @property extraSpaceBelowNumberRowDp extra vertical spacing (dp) inserted below the number row (D-55,
 *           0-25, default 7), to cut mis-taps between the digits and the top letter row
 * @property extraSpaceAboveSpaceRowDp extra vertical spacing (dp) inserted above the space/enter row
 *           (D-55, 0-25, default 7), to cut accidental Enter presses
 * @property symbolKeyEnabled whether the combined ?123 key offers its numeric/symbol function (D-59,
 *           default on); with this off the combined key disappears (slot reserved). §51: no longer also
 *           governs the §48 settings row's emoji button - that button is unconditionally visible.
 * @property tier3Enabled D-126: whether the tier-3 mini-LLM may run at all, independent of whether a model
 *           file happens to be installed (default on, so installing a model is the only step needed by
 *           default) - lets a model stay imported while inference is switched off, e.g. to save
 *           battery/latency without having to delete and later re-import the file.
 * @property diagnosticLogEnabled D-139/D-110: whether the in-memory diagnostic log records while typing
 *           (default off - it captures raw composing/committed text). A rolling 5-minute window
 *           ([de.froehlichmedia.adaptkey.diagnostics.DiagnosticLog]), never written to disk, viewable and
 *           shareable from Settings - an alternative to `adb logcat` that needs no PC/USB tether.
 * @property pendingBlacklistExpiryDays D-177: how long a word stays provisionally forgotten
 *           ([de.froehlichmedia.adaptkey.dictionary.DictionaryStore.markPendingBlacklist]) before the
 *           mark simply expires unused, rather than ever becoming a real, permanent blacklist entry -
 *           default 7 days. Exposed as a setting mainly so the mechanism itself is discoverable, not
 *           because frequent retuning is expected once a value has proven itself.
 * @property neverRecordCredentials D-190: whether [de.froehlichmedia.adaptkey.AdaptKeyService.captureCredentialIfLoginField]
 *           is disabled outright (default off, i.e. recording stays on) - never affects PASSWORD fields,
 *           which are never recorded regardless of this setting; only gates whether a recognised
 *           username/email value gets saved for later suggestion. Already-saved values keep being
 *           suggested either way - this only stops new ones from being added.
 * @property contactsSuggestionsEnabled D-191: whether the address book's own email addresses are merged
 *           into the EMAIL-field credential suggestion list - default **off**, opt-in only. The user's
 *           stated preference, not a live permission check by itself; every actual read still re-checks
 *           `READ_CONTACTS` at the point of use (defence in depth against a permission revoked externally
 *           after this was turned on). Contacts are read fresh at suggestion time and never persisted -
 *           storing them locally would be redundant with the address book itself.
 */
data class AdaptSettings(
    val keyProportions: KeyProportions = KeyProportions.DEFAULT,
    val suggestionConfig: SuggestionConfig = SuggestionConfig(),
    val showNumberRow: Boolean = true,
    val hintsEnabled: Boolean = true,
    val letterHints: Map<Char, String> = KeyboardLayout.DEFAULT_LETTER_HINTS,
    val shiftGraceWindowMs: Long = DEFAULT_SHIFT_GRACE_WINDOW_MS,
    val commaLineNotSentenceStart: Boolean = true,
    val llmActivationThreshold: LlmActivationThreshold = LlmActivationThreshold.DEFAULT,
    val keySoundEnabled: Boolean = false,
    val keyHapticsEnabled: Boolean = false,
    val longPressDelayMs: Long = DEFAULT_LONGPRESS_DELAY_MS,
    val extraSpaceBelowNumberRowDp: Int = DEFAULT_EXTRA_SPACING_DP,
    val extraSpaceAboveSpaceRowDp: Int = DEFAULT_EXTRA_SPACING_DP,
    val symbolKeyEnabled: Boolean = true,
    val tier3Enabled: Boolean = true,
    val diagnosticLogEnabled: Boolean = false,
    val pendingBlacklistExpiryDays: Int = DEFAULT_PENDING_BLACKLIST_EXPIRY_DAYS,
    val neverRecordCredentials: Boolean = false,
    val contactsSuggestionsEnabled: Boolean = false
) {
    
    companion object {
        
        /** Default shift grace window (C-07, 300 ms per §10). */
        const val DEFAULT_SHIFT_GRACE_WINDOW_MS = 300L
        
        /** Default long-press delay (D-32, ~20 % below the typical system timeout). */
        const val DEFAULT_LONGPRESS_DELAY_MS = 320L
        
        /** Default extra row spacing in dp (D-55). */
        const val DEFAULT_EXTRA_SPACING_DP = 7
        
        /** Default D-177 pending-blacklist expiry, in days - the user's own proposed starting value. */
        const val DEFAULT_PENDING_BLACKLIST_EXPIRY_DAYS = 7
        
        /** The all-defaults configuration, equivalent to a freshly installed app. */
        val DEFAULT = AdaptSettings()
    }
}
