// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-488: *how* a word came to be on the blacklist, stored persistently alongside the entry itself
 * ([BlacklistEntry.origin]). Blacklisting is rare, and until now every write path was silent - the user
 * could find a completely ordinary word on the blacklist with no way to answer "when, how, why". This
 * enum is the persistent answer: every write site records its own value, the Blacklist editor displays
 * it, and it travels through backups ([de.froehlichmedia.adaptkey.backup.BackupBundle]).
 */
enum class BlacklistOrigin {
    
    /** D-488: confirmed via the D-488 chip after a G-04 drag-to-trash of a bundled word. */
    GESTURE_G04,
    
    /** D-488: confirmed via the D-488 chip - a recurrence (D-177) of a word previously provisionally
     * forgotten (G-04 drag or the Learned Words editor's "Vergessen"). */
    ESCALATION,
    
    /** D-488: confirmed via the D-488 chip after the W-03 "Gelernt: X" chip's deep "Verbieten" zone. */
    W03_FORBID,
    
    /** Typed by hand into the Blacklist editor (C-05) - an explicit user action, no chip involved. */
    EDITOR_MANUAL,
    
    /** Restored from a backup (D-278). A v2 backup preserves the original origin; a v1 backup (which
     * carried words only) imports under this value. */
    BACKUP_IMPORT,
    
    /** Seeded by the app itself - the D-278/D-176 confusables list, re-seeded idempotently on every
     * service start. Provenance of a re-seed never overwrites an existing row's own values. */
    BUNDLED_SEED,
    
    /** A pre-D-488 entry (no provenance was recorded when it was written), or an entry whose stored
     * origin value could not be parsed. Displayed as "unknown (from an earlier version)". */
    UNKNOWN
}

/**
 * D-488: one blacklist row with its own provenance - the word (as stored, lower-cased key form),
 * its category, and the [BlacklistOrigin] plus timestamp answering "when, how and why is this here".
 * Read-side only; writes go through [DictionaryStore.blacklist] with the same facts.
 *
 * @property word the blacklisted word, in the store's lower-cased key form
 * @property category the blacklist category (USER vs BUNDLED)
 * @property origin how the entry came to be (see [BlacklistOrigin])
 * @property addedAtMillis when the entry was written (epoch millis), or 0 when unknown
 *        (pre-D-488 rows, old backups)
 */
data class BlacklistEntry(
    val word: String,
    val category: BlacklistCategory,
    val origin: BlacklistOrigin,
    val addedAtMillis: Long
)
