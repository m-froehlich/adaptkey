// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-411: [DictionaryStore.learnedFrequencyOf]'s own result - a learned word's raw reinforcement count and
 * when it was last touched, kept apart from [entryOf]'s merged bundled+learned view (see that method's own
 * KDoc for why).
 *
 * @property frequency the word's own reinforcement count since promotion (never the bundled contribution)
 * @property lastTouched epoch millis of the most recent write to this entry
 */
data class LearnedFrequency(val frequency: Long, val lastTouched: Long)
