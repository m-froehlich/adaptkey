// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.credential

/**
 * D-142: classifies a text field as login-relevant from the signal `EditorInfo` actually exposes for this,
 * so credential-style input can be learned/suggested differently from ordinary text. Deliberately takes
 * plain values rather than `android.view.inputmethod.EditorInfo`/`android.text.InputType` directly, so this
 * stays JVM-unit-testable without any Android dependency, matching this project's usual split between pure
 * policy and Android glue.
 *
 * **Verified against the real Android SDK, not assumed**: `EditorInfo` has no `autofillHints` field at all
 * (confirmed via `javap` against the actual `android-35` `android.jar` before writing this) - autofill
 * hints live on the target app's own `View`, which an IME never has a reference to. `InputType`'s variation
 * bits are therefore the *only* reliable signal available at all, and they distinguish email and password
 * fields cleanly - but have no distinct variation for "username" whatsoever, so [classify] can never
 * produce [LoginFieldKind.USERNAME] on its own. [hasWeakUsernameSignal] offers a second, explicitly
 * unreliable signal for that case (an app-supplied hint string, when present at all) - used only to
 * proactively nudge the user toward the manual credential-mode toggle, never to switch suggestion
 * behaviour by itself (see `AdaptKeyService`).
 */
object LoginFieldDetector {
    
    // android.text.InputType's variation constants, as plain Int literals (the same values javac/kotlinc
    // would inline from the real constants) - keeps this class free of any android.* import, so it needs
    // no Robolectric shadow to unit-test.
    private const val VARIATION_EMAIL_ADDRESS = 0x00000021
    private const val VARIATION_WEB_EMAIL_ADDRESS = 0x000000d1
    private const val VARIATION_PASSWORD = 0x00000081
    private const val VARIATION_VISIBLE_PASSWORD = 0x00000091
    private const val VARIATION_WEB_PASSWORD = 0x000000e1
    
    /**
     * @param inputTypeVariation `EditorInfo.inputType and InputType.TYPE_MASK_VARIATION`
     * @return [LoginFieldKind.EMAIL] / [LoginFieldKind.PASSWORD] when reliably signalled by the variation,
     *         [LoginFieldKind.NONE] otherwise (never [LoginFieldKind.USERNAME] - see [hasWeakUsernameSignal])
     */
    fun classify(inputTypeVariation: Int): LoginFieldKind {
        return when (inputTypeVariation) {
            VARIATION_PASSWORD, VARIATION_VISIBLE_PASSWORD, VARIATION_WEB_PASSWORD -> LoginFieldKind.PASSWORD
            VARIATION_EMAIL_ADDRESS, VARIATION_WEB_EMAIL_ADDRESS -> LoginFieldKind.EMAIL
            else -> LoginFieldKind.NONE
        }
    }
    
    /**
     * A deliberately weak, best-effort signal that the field *might* be a username-style identifier field:
     * whether its app-supplied hint text or field name contains a recognisable keyword. Neither field is
     * reliably populated by real apps, and a hit here is never trusted enough to switch suggestion
     * behaviour on its own - it only prompts the user (a settings-row nudge) to confirm via the manual
     * toggle. Deliberately not exhaustive across every language - covers the languages this project already
     * ships dictionaries for (German, English) plus a couple of the most common others.
     *
     * @param hintText `EditorInfo.hintText`, or null when the field declares none
     * @param fieldName `EditorInfo.fieldName`, or null when the field declares none
     * @return true when a recognisable username-style keyword was found
     */
    fun hasWeakUsernameSignal(hintText: CharSequence?, fieldName: String?): Boolean {
        val haystack = listOfNotNull(hintText?.toString(), fieldName).joinToString(" ").lowercase()
        if (haystack.isBlank()) {
            return false
        }
        return USERNAME_KEYWORDS.any { haystack.contains(it) }
    }
    
    private val USERNAME_KEYWORDS = listOf(
        "username", "user name", "user id", "userid", "login", "account name",
        "benutzername", "benutzer", "anmeldename", "kontoname",
        "nom d'utilisateur", "nombre de usuario"
    )
}
