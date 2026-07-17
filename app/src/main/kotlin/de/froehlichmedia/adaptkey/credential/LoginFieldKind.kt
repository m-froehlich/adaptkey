// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.credential

/**
 * D-142: what kind of login-relevant field is currently focused, as classified by [LoginFieldDetector].
 * Drives both what gets learned (only [USERNAME] / [EMAIL] input, never [PASSWORD]) and what the
 * suggestion bar shows (the credential list instead of the ordinary dictionary for [USERNAME] / [EMAIL];
 * nothing at all for [PASSWORD]).
 */
enum class LoginFieldKind {
    
    /** An ordinary field - the ordinary dictionary/suggestion pipeline applies unchanged. */
    NONE,
    
    /** A username-style identifier field. */
    USERNAME,
    
    /** An email-address field. */
    EMAIL,
    
    /**
     * A password field. Deliberately never learned from and never suggested into - a password is not an
     * identifier the user would want offered back later, and storing it would be a real privacy risk this
     * feature has no reason to take on.
     */
    PASSWORD
}
