// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

/**
 * The positional and field context used to decide a token's capitalisation (§6).
 *
 * @property explicitFirstUpper the user explicitly typed/shifted the first letter as uppercase;
 *           an explicit uppercase is never lowercased (hierarchy rule 1)
 * @property sentenceStart the token starts a sentence (field start, or after `.`/`!`/`?` + space) - D-405:
 *           kept on this class and still populated by every caller, but deliberately no longer read inside
 *           [CapitalisationEngine.capitalise] itself; sentence-start capitalisation is applied live instead
 *           (see [ShiftGrace]), never as a commit-time override. Retained here so a future, narrower,
 *           context-aware reintroduction (should live arming ever prove insufficient on its own) stays a
 *           small, local change rather than needing this whole plumbing rebuilt from scratch.
 * @property capsMode the editor-mandated capitalisation, ranking directly below explicit input
 * @property afterHyphen the token is the segment following a hyphen (B-02: lowercase unless proper)
 * @property previousHyphenSegment D-373: the hyphen chain's own previous segment (real casing preserved),
 *           or null when [afterHyphen] is false or no segment precedes the hyphen at all - see
 *           [de.froehlichmedia.adaptkey.capitalisation.SentenceBoundary.previousHyphenSegment]'s own KDoc
 * @property previousHyphenSegmentAtSentenceStart D-373: whether [previousHyphenSegment] was itself at a
 *           sentence start - see [CapitalisationEngine.previousSegmentPropagates] for how this and
 *           [previousHyphenSegment] together decide whether capitalisation propagates across the hyphen
 */
data class CapitalisationContext(
    val explicitFirstUpper: Boolean,
    val sentenceStart: Boolean,
    val capsMode: CapsMode,
    val afterHyphen: Boolean,
    val previousHyphenSegment: String? = null,
    val previousHyphenSegmentAtSentenceStart: Boolean = false
)
