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
 * produce [LoginFieldKind.USERNAME] on its own, and real apps do not always set the email variation even on
 * a field visibly labelled "E-Mail-Adresse" (confirmed on device - `finanzen.net zero`'s own email field did
 * not classify as EMAIL). [weakSignalKind] offers a second, explicitly unreliable signal for both cases (an
 * app-supplied hint string, when present at all) - used only to proactively nudge the user toward the manual
 * credential-mode toggle, never to switch suggestion behaviour by itself (see `AdaptKeyService`).
 *
 * **Bug fixed, found while building D-143's own analogous URI-variation check, not guessed**: the five
 * variation constants below previously included the `TYPE_CLASS_TEXT` bit baked in (e.g. `0x21` for EMAIL),
 * copied from how the *unmasked* `InputType` constants are commonly written combined with their class.
 * [classify]'s own contract, though, is a value already masked with `TYPE_MASK_VARIATION` (`0x0ff0`) - which
 * does *not* include the class bits (`0x000f`) at all, so the masked value `classify` actually ever receives
 * (verified via `javap` against `android-35`'s real `InputType.class`: `TYPE_MASK_VARIATION = 4080`,
 * `TYPE_TEXT_VARIATION_EMAIL_ADDRESS = 32` = `0x20`, not `0x21`) could never match these class-bit-inclusive
 * literals - `classify` silently returned [LoginFieldKind.NONE] for *every* real field, regardless of its
 * actual variation. This plausibly explains why even the "reliable" EMAIL detection never activated on
 * device (§85's `finanzen.net zero` report), not only the already-known "no variation exists for username at
 * all" limitation. Corrected to the bare (class-bit-free) variation values.
 */
object LoginFieldDetector {
    
    // android.text.InputType's variation constants, as plain Int literals (the same values javac/kotlinc
    // would inline from the real constants) - keeps this class free of any android.* import, so it needs
    // no Robolectric shadow to unit-test. Bare variation bits only (no TYPE_CLASS_TEXT baked in) - see the
    // class KDoc above for why that distinction matters here.
    private const val VARIATION_EMAIL_ADDRESS = 0x00000020
    private const val VARIATION_WEB_EMAIL_ADDRESS = 0x000000d0
    private const val VARIATION_PASSWORD = 0x00000080
    private const val VARIATION_VISIBLE_PASSWORD = 0x00000090
    private const val VARIATION_WEB_PASSWORD = 0x000000e0
    
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
     * A deliberately weak, best-effort signal for a field [classify] could not reliably place: whether its
     * app-supplied hint text or field name contains a recognisable EMAIL or USERNAME keyword. Neither
     * `EditorInfo` field is reliably populated by real apps in the first place, and a hit here is never
     * trusted enough to switch suggestion behaviour on its own - it only prompts the user (a settings-row
     * nudge) to confirm via the manual toggle. EMAIL keywords are checked first and win over USERNAME ones
     * on a field mentioning both (an email-labelled field is the more specific, more actionable read -
     * domain completion only ever applies to EMAIL). Deliberately not exhaustive across every language -
     * covers the languages this project already ships dictionaries for (German, English) plus a couple of
     * the most common others.
     *
     * @param hintText `EditorInfo.hintText`, or null when the field declares none
     * @param fieldName `EditorInfo.fieldName`, or null when the field declares none
     * @return [LoginFieldKind.EMAIL] / [LoginFieldKind.USERNAME] when a recognisable keyword was found,
     *         [LoginFieldKind.NONE] otherwise
     */
    fun weakSignalKind(hintText: CharSequence?, fieldName: String?): LoginFieldKind {
        val haystack = listOfNotNull(hintText?.toString(), fieldName).joinToString(" ").lowercase()
        if (haystack.isBlank()) {
            return LoginFieldKind.NONE
        }
        return when {
            EMAIL_KEYWORDS.any { haystack.contains(it) } -> LoginFieldKind.EMAIL
            USERNAME_KEYWORDS.any { haystack.contains(it) } -> LoginFieldKind.USERNAME
            else -> LoginFieldKind.NONE
        }
    }
    
    private val EMAIL_KEYWORDS = listOf(
        "email", "e-mail", "mailadresse",
        "διεύθυνση email", "correo electrónico", "adresse e-mail"
    )
    
    private val USERNAME_KEYWORDS = listOf(
        "username", "user name", "user id", "userid", "login", "account name",
        "benutzername", "benutzer", "anmeldename", "kontoname",
        "nom d'utilisateur", "nombre de usuario"
    )
}
