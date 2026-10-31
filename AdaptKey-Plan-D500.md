# AdaptKey-Plan-D500.md

**Status:** CLOSED, device-confirmed by the user 2026-10-31 ("Funktioniert wunderbar") - the §5 checklist
passed on the device. Implemented as History §537, v1.2.97 / versionCode 593.
**Backlog item:** D-500 (AdaptKey-Progress.md, "Open TODOs"). Request: a "Suppress Auto-Caps" switch,
directly above C-10 in the same category. User decisions 2026-10-31: mirror the big keyboards' behaviour
at this point; the switch must stay a coarse, dumb switch that does not reach into the keyboard's fine
mechanics; nouns may also stay lowercase for a user who turns it off ("Mir ist es am Ende völlig egal,
wie das arbeitet").

---

## 1. Reference behaviour (the big keyboards, checked 2026-10-31)

- **Gboard:** Settings -> Text correction -> "Auto-capitalization", default **on**. Off: the keyboard no
  longer capitalises sentence starts. A plain boolean, no sub-options. Community finding (r/gboard
  "Any way to disable ALL auto-capitalizations?"): the toggle only covers sentence starts - users wanting
  completely-manual capitalisation cannot get it there; app caps flags and dictionary-cased replacements
  keep working. AdaptKey deliberately goes one step further (see §3) on the user's explicit allowance.
- **iOS:** Settings -> General -> Keyboard -> "Auto-Capitalisation", default **on**. Off: no automatic
  sentence capitals. No noun-capitalisation feature exists at all.
- **Samsung Keyboard:** "Automatische Groß-/Kleinschreibung", default **on** - same class.
- Common shape: ONE plain on/off switch inside the keyboard's own settings, default ON; manual Shift and
  Caps Lock always work; autocorrect and suggestions are separate switches. None of the big keyboards
  auto-capitalises German nouns (no such feature exists there). AdaptKey's §6 noun rule has no
  counterpart to mirror - "mirroring" therefore means: with the switch off, AdaptKey adds no capital the
  user did not type, which for AdaptKey includes the noun rule.

## 2. Agreed direction

1. Plain "Auto-capitalisation" switch, **default on** (mirrored polarity - the off state is what the
   community asks for), same position the user fixed: directly above `c10_comma_line_not_sentence_start`.
2. Dumb switch: no per-field-type, per-language or per-rule sub-options anywhere in its behaviour.
3. Nouns may stay lowercase under the off state (explicit user allowance - the §6 noun rule is gated).
4. Fine mechanics untouched: autocorrect, split/merge/fusion, chips, guards all keep their own switches
   and behaviour.

## 3. Definition - what the off state gates, mapped to the code

**Gated (two blunt sites):**

1. **Live Shift-arming** - `AdaptKeyService.armShiftForNextWord()`, the single choke point every arming
   path runs through (field start / `onStartInput`, every commit via `armShiftForNextWordUnlessOpener`
   (D-378), the caret re-derivation `rearmShiftForCaretMove` (D-313/D-455), and D-406's position half
   inside `applyShiftAfterDelete`). With the switch off, the derived arm is `false` instead of
   `ShiftGrace.autoArmAtWordStart(capsMode, sentenceStart)` - no capsMode distinction in the gate
   (deliberately dumb). This writes `shifted = false` exactly like today's mid-sentence derivation
   already does, so the D-491 (`shiftSetByManualPress`) / D-335 / D-373 / D-378 one-shot guards keep
   guarding the same writes unchanged; a manual Shift press or Caps Lock state can never be clobbered by
   anything new. D-406's other half (a deleted uppercase character keeps shift lit - restore, not
   proactive capitalisation) does NOT run through the gate and stays untouched.
2. **Commit-time linguistic caps** - `CapitalisationEngine.capitalise()` gains an
   `autoCapsEnabled: Boolean = true` parameter. When false: the `isNounOnly` force-capitalisation
   (§6 rules 3+4, D-461) does not fire, the `llmForcesUpper` rule-6 lift does not fire, and the
   D-457 acronym canonical restore ("llm" -> "LLM") does not fire. The parameter is passed from exactly
   ONE call site: `finalizeAndCommit`'s commit call (`AdaptKeyService` ~line 5334, re-locate before
   trusting). Result: "tisch" commits as "tisch", "llm" as "llm" - nothing is capitalised that the user
   did not type.

**Deliberately NOT gated (boundaries that already exist for other reasons):**

- **Editor mandates at commit** stay: `CapsMode.CHARACTERS` (all-caps field -> `uppercaseAll`),
  `CapsMode.WORDS` (name field force-upper), B-02's hyphen rules. §6 rule 2 is a protection ("never
  lowercase a field-required capital"), not proactive capitalisation - an app that demands capitals
  keeps getting them. The pure rule-1 symmetry (explicit upper/lowercase never changed) is untouched by
  construction.
- **Chip/preview casing** and every chip-commit path stay - they reproduce what the user explicitly
  tapped (display and commit must keep matching, D-440's pairing).
- **A-05 split / A-06 merge / D-391 fusion** casing stays - each has its own setting (C-21/C-25); the
  new switch does not reach into them.
- **Autocorrect (C-22)** stays - its own switch. A dictionary replacement may still carry the
  dictionary's own casing; that is correction, not capitalisation.
- **W-04 learned casing, S-11 dual-casing chips**, verbatim fields' own bypass (D-288) - untouched.
- **C-10 and C-07** become inert under the off state (no arming happens, so nothing is refined or
  guarded) - no code change, they stay as they are.
- **Turkish `CasingRules`** etc. map characters that are already being cased; nothing to gate.

## 4. Implementation steps

1. **Setting:** `d500_auto_capitalisation` (`SwitchPreferenceCompat`, `android:defaultValue="true"`),
   directly above C-10 in `settings_preferences.xml`; `SettingsStore` KEY + `DEF_AUTO_CAPITALISATION =
   true` -> `RawSettings` -> `SettingsMapper` -> `AdaptSettings.autoCapitalisation`; insert into
   `EXPORT_SETTINGS_KEY_ORDER` directly before `KEY_COMMA_LINE_NOT_SENTENCE_START` (screen order).
   Backup semantics: only stored keys export, so a never-toggled install follows the default - no
   migration, the D-490 pattern.
2. **Strings:** title EN "Auto-capitalisation" (de "Automatische Großschreibung") + summary, in EN +
   all 31 `values-xx` (AI drafts, script-assisted like d488/d507/d508; `UiLocaleConsistencyTest`
   enforces). Summary names what is covered: first word of sentences + words that exist only as
   nouns/proper nouns; off = exactly what you type; manual Shift/Caps Lock unaffected; autocorrect is
   its own setting.
3. **Arming gate:** `ShiftGrace.autoArmAtWordStart(capsMode, sentenceStart, autoCapsEnabled: Boolean =
   true)` - returns `false` when disabled (default keeps every existing caller/test byte-identical);
   the service passes the setting at the choke point. `isGuardedArm` needs no parameter: a guarded arm
   can only exist when something is armed.
4. **Engine gate:** `capitalise(word, context, llmForcesUpper = false, autoCapsEnabled = true)` - the
   three branches above conditional; only `finalizeAndCommit` passes the setting.
5. **Tests:** `SettingsMapperTest` (default on, stored off passes through), `SettingsStoreRoboTest`
   (default with no key written, stored false wins), `ShiftGraceTest` (off -> no arm in every capsMode),
   `CapitalisationEngineTest` (`autoCapsEnabled = false`: noun-only word stays lowercase, LLM lift
   ignored, acronym restore skipped; editor mandates still honoured); full suite + `:app:assembleRelease`.
6. **Spec:** the §20 settings table gains **C-28**; the capitalisation sections (§6 rules /
   Editor-Mandated Capitalisation, and §35's live-only note) gain the D-500 addendum.
7. **Docs + bump:** Progress D-500 stub, History §537, version bump as its own concluding commit.

## 5. Device checklist

- Default sanity: switch untouched -> behaviour byte-identical to today (sentence caps, noun caps).
- Switch off: field start lowercase; after `.`/`!`/`?` and Enter lowercase; caret moved to a sentence
  start leaves the text as typed; "tisch" commits "tisch"; "llm" stays "llm"; comma + newline (C-10
  territory) inert; manual Shift arms and capitalises; Caps Lock toggles; G-05 double-tap case flip
  intact; D-406 delete-restore intact; a name field (CAP_WORDS) still commits capitals; autocorrect and
  suggestion chips unchanged; C-07 grace guard causes no surprise (nothing armed).
- Switch back on: everything as before.

## 6. Out of scope

- Any sub-option (per-field, per-language, per-rule overrides) - deliberately not built.
- The tier-3 LLM rule 6 beyond sharing the same gate (inert without an installed model anyway).
- Revisiting C-10/C-07 mechanics.
