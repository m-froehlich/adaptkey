# AdaptKey — Plan: Dark Theme Follows the System (D-501, simplified)

**Status:** IMPLEMENTED in this round (v1.2.90-v1.2.92; see §7). Build + all unit tests green.
**Not device-confirmed** — the dark surfaces and the live day/night flip need a real device look
(§6's checklist); the on-device confirmation closes the round.

## 1. Motivation — the request, and the simplification

Original backlog entry (D-501, requested 2026-10-26): a Dark Mode with a three-way switch
(System Default / Light / Dark). Simplified by the user on 2026-10-28 before any code: **no switch**.
Reasoning: nobody wants dark keys while the system is light, or vice versa - the keyboard must simply
be dark when the system is dark. The three-way switch's entire hard part (how the service's view
context resolves a mode that may disagree with the system) therefore never gets built: with
follow-system, "which mode?" is answered natively by Android's `values-night/` resource qualifier.

Verified anchors (Progress' D-501 entry, re-confirmed in code before this plan):

- The settings Activities already run on `Theme.AdaptKey` =
  `Theme.Material3.DayNight.NoActionBar` - they follow the system's night mode today.
- Every self-drawn keyboard surface (`AdaptKeyboardView`, `SuggestionBarView`, `ExtraRowView`,
  `EmojiPanelView`, the long-press popup paints, the suggestion/credential chips built in
  `AdaptKeyService`) resolves its colours at view-construction time from `R.color.*` resources -
  21 tokens in `values/colors.xml`, no `values-night/` existed. No theme-attribute coupling at all.
- The only hardcoded view colours were the D-24 touch-model overlay's three paint literals in
  `AdaptKeyboardView` (now resources - see §4).
- D-136's `isAppearanceLightNavigationBars = true` in `onCreateInputView()` is unconditional and
  its comment states "always light" - it must follow the night mode once the background can be dark.

## 2. Agreed decisions

1. **No setting, no preference key, no strings.** Follow-system only. Nothing in the backup
   export/import, nothing for `UiLocaleConsistencyTest`.
2. The mode carrier is exclusively the resource qualifier: `values-night/colors.xml` overrides the
   existing colour names (never new names), so every existing call site follows without code change.
3. `SuggestionConfig.DEFAULT_HIGHLIGHT_COLOR` and the `Color.parseColor` sites (C-04/S-05
   highlight) stay theme-independent user data (D-247 precedent) - deliberately not night-aware.
4. `ic_launcher_background` stays the fixed brand colour.
5. D-247's independence rule survives: `suggestion_learned_text`/`suggestion_forget_background`
   keep their fixed green in both modes (only the text-chip colour lightens for contrast on dark).
6. D-337's distinction survives in dark: the Caps-Lock border (`#4285F4`) must stay visibly
   different from the pressed flash (`#A6C8FF`), the same separation the light palette has.
7. The D-136 fix is exactly the conditional, nothing more - no status-bar appearance change, no
   inset change (D-81/D-486 untouched).
8. No preemptive live-flip machinery: the framework `InputMethodService` re-creates the input view
   on a configuration change (and `AdaptKeyService.onConfigurationChanged` already calls `super`),
   so a mid-session system flip re-resolves every colour through the recreated views. A fallback is
   added only if a device ever shows stale colours - not before.

## 3. The dark palette

Gboard/Material-dark family, mirroring each light token's role:

| Token | Light | Night | Note |
|---|---|---|---|
| `keyboard_background` | `#D2D5DA` | `#202124` | Gboard-dark keyboard body |
| `key_background` | `#FFFFFF` | `#3C4043` | letter keys |
| `key_background_special` | `#E0E3E7` | `#2F3235` | special keys darker than letter keys, like light |
| `key_background_pressed` | `#A6C8FF` | `#A6C8FF` | unchanged - bright pale-blue flash reads on dark keys too |
| `key_text` | `#1A1C1E` | `#E8EAED` | |
| `key_hint` | `#7A7E83` | `#9AA0A6` | |
| `suggestion_bar_background` | `#E8EAED` | `#292A2D` | slightly lighter than the body, like light |
| `suggestion_text` | `#1A1C1E` | `#E8EAED` | |
| `suggestion_verbatim_text` | `#1565C0` | `#8AB4F8` | lightened accent blue |
| `suggestion_divider` | `#BFC3C9` | `#3C4043` | |
| `suggestion_trash_background` | `#D32F2F` | `#D32F2F` | unchanged - white-on-red reads identically |
| `suggestion_trash_text` | `#FFFFFF` | `#FFFFFF` | unchanged |
| `link_text` | `#1565C0` | `#8AB4F8` | |
| `caps_lock_border` | `#1565C0` | `#4285F4` | distinct from the pressed flash (D-337) |
| `suggestion_learned_text` | `#43A047` | `#81C995` | chip *text* lightens; the forget-chip fills stay green (D-247) |
| `suggestion_forget_background` | `#43A047` | `#43A047` | unchanged |
| `suggestion_forget_text` | `#FFFFFF` | `#FFFFFF` | unchanged |
| `suggestion_search_query_text` | `#7A7E83` | `#9AA0A6` | mirrors `key_hint`, as in light |
| `suggestion_loading_text` | `#F57C00` | `#FFB74D` | amber lightened for dark |
| `suggestion_blacklist_confirm_text` | `#C62828` | `#F28B82` | red lightened for dark (D-488 chip) |
| `language_pack_suggested_background` | `#E3F2FD` | `#1E3A5F` | dark blue row tint (D-385-followup) |
| `touch_model_fill` (new, D-24) | `#3300796B` | `#334DB6A2` | translucent teal, brightened at night |
| `touch_model_stroke` (new, D-24) | `#8800796B` | `#884DB6A2` | |
| `touch_model_dot` (new, D-24) | `#E0D32F2F` | `#E0EF5350` | expected-strike dot, brighter red at night |

## 4. The D-24 overlay literals become resources

The three paint literals move into `values/colors.xml` as `touch_model_fill` / `touch_model_stroke`
/ `touch_model_dot` with the byte-identical values (a no-behaviour-change refactor of its own
commit), and `AdaptKeyboardView`'s three paints resolve them like every other colour the view
draws. The night variants in §3 then cover the overlay with no further code.

## 5. D-136: the gesture-area controls follow the mode

`onCreateInputView()` sets `isAppearanceLightNavigationBars` from the creating configuration
(`resources.configuration.uiMode and UI_MODE_NIGHT_MASK`): dark controls on the light keyboard,
light controls on the dark one. The comment's "always light" premise is rewritten.

## 6. Device-confirmation checklist (both modes + the live flip)

Letters/symbol page 2/calculator, URL/email modes; long-press popups (single-hint, multi-cell,
reversed-at-edge); Caps-Lock border vs pressed flash; suggestion bar in all chip states (verbatim,
learned, search-query, loading, blacklist-confirm, forget-drag, trash overlay); extra row + emoji
panel + emoji search; D-24 overlay; swipe chevrons; settings screens incl. the LanguagePacks
suggested row; and D-136's pill/indicator contrast - once per mode, plus one live system flip while
the keyboard is open.

## 7. Implementation state

- v1.2.90 (586): D-24 literals → `touch_model_*` resources (refactor, no behaviour change).
- v1.2.91 (587): `values-night/colors.xml` with the palette above - the feature itself.
- v1.2.92 (588): D-136's conditional `isAppearanceLightNavigationBars`.
- Docs: spec §51, History §527, Progress D-501 → implemented (device confirmation open).
