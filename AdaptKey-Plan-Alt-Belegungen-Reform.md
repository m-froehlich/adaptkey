# AdaptKey — Plan: Alt-Belegungs-Reform (D-509 umbrella, phases D-509…D-514)

**Status:** DRAFT v3 (2026-11-05) — dritte Abstimmungsrunde eingearbeitet. Neu entschieden: `*` fest auf
s-links, `+` redundant auf der ersten Taste der untersten Reihe (Nutzer-Vorschlag, im Plan validiert),
ñ universell als rechte Ecke auf n (nicht nur es), Seite 2 bleibt wie sie ist, hu/pt Lead-Varianten
bleiben (Assistenten-Entscheidung), Release-Default-Regel bestätigt, Popup-Fluss als
"Tastaturmitte-Regel mit o-Ausnahme" präzisiert. Verbleibend: §8 (nur der formale Final-Nod auf den
+-Platz und zwei Umsetzungs-Device-Blicke).
**Backlog item:** six Progress stubs D-509…D-514 assigned at implementation start (see §9) — the plan
file stays the single umbrella document.

---

## 1. Current state (verified, line refs as of this draft — re-locate before trusting)

- **Number row** (`KeyboardLayout.NUMBER_HINTS`, KeyboardLayout.kt:33): the German-QWERTZ shifted
  symbols `! " § $ % & / ( ) =` on every layout. `numberKey()` (KeyboardLayout.kt:401) builds each
  digit's popup `[shifted, superscript]`, with `2` = `" ' ² ₂` (D-382/D-417) and `7` = `/ \ ⁷` (D-498).
  `NUMBER_SUPERSCRIPTS` (KeyboardLayout.kt:41) stays per the plan.
- **The number row is shared by EVERY layout**: `AzertyLayout` (AzertyLayout.kt:59) delegates to
  `KeyboardLayout.numberKey`, and per its own KDoc (KeyboardLayout.kt:397) so do Greek, Jcuken
  (Russian/Ukrainian), Serbian and Azerbaijani. DECIDED (round 2): the non-Latin layouts benefit too
  and receive the reformed row — no pinning; their digit-row tests get updated.
- **Letter hints**: `DEFAULT_LETTER_HINTS` (KeyboardLayout.kt:157) is the compiled-in German map
  (`q=@ e=€ h=# m=- n=+ d=° p=π x=× c=÷ v=/ b=* f=ƒ a=ä o=ö u=ü s=ß`). Per active language it is
  REPLACED (not merged) by the language's own full copy in `hints.tsv`
  (`AdaptKeyService.kt:1124` → `SettingsStore.kt:301` → `LanguageLetterHintsLoader`), so every shipped
  pack carries the same German symbol skeleton plus the language's own letters (D-281/D-497).
  DECIDED: switch to compiled-in universal BASE + per-language DELTA with UNION merge (round 3 fix,
  see §6).
- **English today** (`assets/en/hints.tsv`): `b=* c=÷ d=° e=€ f=ƒ h=# m=- n=+ p=π q=@ v=/ x=×` —
  no ä ö ü ß anywhere. CONFIRMED DESIRED (round 2): English keeps the German letters out entirely.
  The round-1 "triangle for hidden umlauts" idea is DROPPED.
- **Corner glyph**: exactly ONE hint, drawn top-right (`AdaptKeyboardView.drawKeys`,
  AdaptKeyboardView.kt:1128-1147); keys with ≥2 alternatives and no own hint draw the triangle
  `MORE_ALTERNATIVES_GLYPH = "◢"` (D-98, AdaptKeyboardView.kt:2756). Shifted state uppercases letter
  hints (`cornerHintDisplayTextFor`, AdaptKeyboardView.kt:1263) — this machinery also covers ñ/Ñ.
- **Popup**: `HorizontalLongPressPopup.rowLeft` centres the PRE-SELECTED cell above the finger (D-44);
  pre-selection is the key's own char when present, else index 0; near the right edge the whole order
  is reversed when clamping (D-282, AdaptKeyboardView.kt:1868-1872). POPUP FLOW (user, round 3): the
  popups of keys on the right keyboard half flow LEFT today (toward the keyboard centre) — this is
  the rule the o-key exception modifies (§2.11). One device look during implementation reconciles
  the flow rule with D-282's clamping reversal.
- **Letter popups are coupled to the DEFAULT hint by equality** (`letterKey`, KeyboardLayout.kt:345):
  `p`=π→Greek letters, `o`=ö→Ø, `a`=ä→æ å à, `e`=€→é è ê ë, `n`=+→ñ — only while the key still carries
  its default hint. RESOLVED: popups become explicit per-key data (corners + ordered extras).
- **Bottom row** (letters page): `x=× c=÷ v=/ b=* n=+ m=-`; comma popup `/ - , : ; _` (D-499), period
  popup `! . ?`. The plan removes ÷ / * + - from the letters; `×` stays on `x`.
- **Fallback when C-09 hides the number row**: `SymbolLayout` page 2 carries `CATCHALL_DIGITS`,
  `CATCHALL_NUMBER_SYMBOLS = !"§$%&/()=` (SymbolLayout.kt:101) and a catch-all hints row
  `# ' ^ ° - + × ÷ * ±` (SymbolLayout.kt:113). DECIDED (round 3, R2): this page STAYS AS IT IS —
  nobody reaches the special-characters page to long-press things; its space lets nearly everything
  sit directly on a key, which is already well solved. No mirroring of the new scheme.
- **Related invariants that must survive untouched** (from the spec read-through): the bottom-row
  letters themselves stay letters — T-05's ambiguity bands, A-10's connector-split and D-391's
  fusion connector set (`y x c v b n m`) key on the LETTERS, not on their corners, so corner
  additions on bottom-row keys change none of them. Greek's letters page never uses the Latin hint
  maps (own accent system), so only its digit row rides along (§2.12). D-380/D-437's smear
  protection already special-cases the o key's popup — the o-flow exception (§2.11) complements it.
- **Middle row width**: `KeyProportions.thirdRowLetterWeight(letterCount)` (KeyProportions.kt:76) is
  generic — a 10th (QWERTY/QWERTZ) or 11th (AZERTY) letter key just shrinks the neighbours; row total
  preserved. `Key.id` (`c:'`) is unique, so gesture/personal-offset data starts clean (T-03).

## 2. Agreed direction (Eckpunkte, round 3 state)

1. **Two corner symbols per key** become a general concept: one LEFT and one RIGHT corner glyph.
   **The RIGHT corner is always the default** (pre-selected popup cell + what a plain
   long-press-release commits), one rule for every layout; when only a LEFT corner exists, it becomes
   the default (round 3, R6 confirmed).
2. **Apostrophe key**: a dedicated `'` key right of `l` (QWERTY/QWERTZ) / right of `m` (AZERTY);
   left corner `"`, right corner `'` (default → release commits `'`). All Latin layouts get it.
3. **Number row — exactly per the user's assignment table** (left corners as single per-digit
   assignments, right corners = the US shifted row, middle slot only on 7):

   | Key | left corner | middle | right corner (default) | further alts |
   | --- | ----------- | ------ | ---------------------- | ------------ |
   | 1   | `~`         |        | `!`                    | ¹ |
   | 2   | `"`         |        | `@`                    | `'` ² ₂ |
   | 3   | `§`         |        | `#`                    | ³ |
   | 4   | —           |        | `$`                    | ⁴ |
   | 5   | `+`         |        | `%`                    | ⁵ |
   | 6   | `-`         |        | `^`                    | ⁶ |
   | 7   | `/`         | `÷`    | `&`                    | `\` ⁷ |
   | 8   | —           |        | `*`                    | ⁸ |
   | 9   | —           |        | `(`                    | ⁹ |
   | 0   | `=`         |        | `)`                    | ⁰ |

4. **US holy cows stay put as right corners** — `! @ # $ % ^ & * ( )` on `1…0`, exactly where US
   users expect them, and (per Eckpunkt 1) each is also the release-default of its digit.
5. **Letters page**: `@` stays on `q`, `#` stays on `h`, `π` on `p`, `ƒ` on `f`, `×` on `x` (all
   unchanged); `e=€` stays on the German layout. **d**: left `°`, right `$` (default). **n**: left
   `(`, right `ñ`; **m**: left `)`; **v**, **b**, **c** are freed entirely. `+ - / * ÷` leave the
   bottom row to their digit-row slots; `×` stays on `x`.
6. **ñ keeps its home on n — universally, not ES-only** (user decision, round 3): `ñ` becomes the
   RIGHT corner of `n` on every Latin language. The corner model absorbs the only nuance without
   reopening any contradiction: the three languages that own an n-letter keep it as the primary
   right corner — pl `ń`, cs `ň`, lv `ņ` — and ñ moves to their popup's first extra (straddle
   `[ ( | ñ | ń ]`: left corner above-left, ñ in the middle, the language letter above-right and
   still the release-default). For everyone else `n = [ ( | ñ ]`, so ñ gains corner visibility and
   becomes the release-default. The es oddity `l=ñ` dies (l freed in the es delta). Shifted display
   `Ñ` is covered by the existing letter-hint uppercasing.
7. **`*` as the LEFT corner on s — FIXED** (user, round 3: "Das ist fest"): `s = [ * | ß ]` on German,
   `s = [ * | š/ś ]` on the languages whose own letter owns s (cs hr bs pl sk lt lv), `s = [ * | — ]`
   on English (left corner becomes the default, R6). The one operator rescued from C-09's
   hidden-number-row loss, because footnotes/wildcards use it outside formulas. P stays untouched
   (π + 5 Greek extras = already the heaviest letter key).
8. **`+` redundantly on the first key of the bottom row** (user proposal, round 3 — validated, see
   §5): the geometry's first third-row key — `y` on QWERTZ, `z` on QWERTY, `w` on AZERTY — carries
   `+` as its LEFT corner, in the base map per geometry-letter (y/z/w are exactly the letters that
   swap rows between the geometries, so the SLOT is screen-stable for everyone while the LETTER is
   the stable per-language assignment). Language letters on those keys (cs `ý`/`ž`, fr `è`, …) keep
   the right corner per the standing rule; `+` as left corner never displaces them. Bonus: freeing
   fr `w` retires one of the deliberate French scatter-workaround hints (§519) — `è` remains fully
   reachable on e's own popup.
9. **Superscript/subscript digit alts stay** (column "further alts" in Eckpunkt 3; `2`/`7` specials
   keep their existing extra cells).
10. **Umlaut policy**: German layout keeps ä ö ü ß visible as (right-)corners; English keeps them
    out ENTIRELY (status quo of `en/hints.tsv`); every other Latin language keeps its own letters as
    corners per its pack (verified correct today, §5). No hiding mechanism, no triangle-for-umlauts —
    the per-language data model already implements the rule.
11. **Popup geometry and flow**: left-corner char sits roughly above the key's left edge, right-corner
    char above the right edge; with both corners present, cells order `[left, extras…, right]` — a
    centred straddle for exactly 2-3 cells; >3 cells adapt position and order (`2` = `" ' ² ₂ @`,
    `7` = `/ ÷ \ ⁷ &`); pre-selected = the right-corner cell. General FLOW RULE (user, round 3):
    popups flow toward the keyboard centre — left-half keys' extras extend right, right-half keys'
    extras extend left. **Exception: the o key flows RIGHT** — on the German layout `[ö, Ø]` with ö
    above the key and Ø extending rightward, away from the left index finger's smear path when
    sliding out of the popup (the user's exact complaint; complements D-380/D-437's smear protection,
    which already special-cases the o key). Scandinavian layouts have the order flipped by their own
    data anyway (da/nb lead `ø`, sv/fi lead `ö` — §5). D-282 edge clamping still wins over corner
    fidelity.
12. **All layouts move together**: every Latin layout adopts the reformed base map (language letters
    stay primary corner + first in order), AND the non-Latin layouts (Greek, Cyrillic, Azerbaijani)
    receive the reformed digit row too — user decision (round 2), reversing round 1's pinning idea.

## 3. Target letters page (base map, deltas per §5/§6)

| Key | corners after reform (left \| right) | notes |
| --- | ------------------------------------ | ----- |
| q   | — \| `@`                             | unchanged; release `@` |
| e   | — \| `€` (DE)                        | extras é è ê ë flow right (unchanged order) |
| d   | `°` \| `$`                           | NEW; release changes ° → $ |
| f   | — \| `ƒ`                             | unchanged |
| h   | — \| `#`                             | unchanged |
| p   | — \| `π`                             | Greek extras unchanged, flow left (right-half key) |
| x   | — \| `×`                             | unchanged |
| a   | — \| `ä` (DE) / language letter      | extras æ å à flow right (unchanged order) |
| o   | — \| `ö` (DE) / `ø` (da nb) / `ö` (sv fi) | Ø extra flows RIGHT (the exception, §2.11) |
| u   | — \| `ü` (DE)                        | unchanged |
| s   | `*` \| `ß` (DE) / `š ś` (cs hr bs pl sk lt lv) / — (en) | NEW left corner |
| n   | `(` \| `ñ` (universal; pl ń / cs ň / lv ņ win the corner, ñ → first extra) | NEW; release ñ (→ ń/ň/ņ there) |
| m   | `)` \| —                             | NEW; release ) (left corner is the default, R6) |
| y z w | `+` \| (language letter where one exists) | NEW — the first third-row key per geometry (§2.8) |
| v b c | freed (no corners, no popups)       | their `/ * ÷` live on the digit row now |
| l (new key) | `"` \| `'`                    | release commits `'`; `"` reachable by slide-left |

Rule inherited from D-497, extended to corners: a language's own letter, when present, is the RIGHT
corner (primary, release-default) and first in popup order; base symbols append (left corner + popup
extras), flowing per §2.11.

## 3b. Release-default consequences on the GERMAN layout (on record — decided, not re-asked)

Long-press-release today vs after the reform (DE): digits `2 "→@`, `3 §→#`, `6 &→^`, `7 /→&`,
`8 (→*`, `9 )→(`, `0 =→)` (1/4/5 unchanged); letters `d °→$`, `n +→ñ`, `m -→)`; `v` and `b` lose
their long-press entirely; `y` and `s(en: z)` GAIN a long-press (→ `+`, there was none before);
`s ß→ß`, `o ö→ö`, `q @`, `h #`, `e €`, `a ä`, `u ü`, `p π`, `x ×`, `f ƒ` unchanged. Slide-left now
delivers the German survivors `" § =` and the math singles `+ - / °` on digits/letters. This is the
deliberate cost of "right corner = default, one rule for all".

## 4. Contradictions from round 1 — resolution status

- **W1 (shared number row) — RESOLVED, opposite of round 1's proposal**: non-Latin layouts ARE
  rebuilt with the reform; no pinning, no builder flag; their digit-row tests change.
- **W2 (C-09 hides `+ * ÷`) — RESOLVED**: accepted as intended; `*` rescued via s-left; `+` is
  additionally rescued via the new bottom-row slot (§2.8); only `÷` remains digit-row-only
  (`/ -` survive in the comma popup, `! ?` in the period popup, `×` on x).
- **W3 (ñ's home) — RESOLVED**: ñ stays on n as right corner, universally (§2.6).
- **W4 (hint-equality popups) — RESOLVED structurally**: popups become explicit per-key data.
- **W5/W6 (EN umlauts; which language drives visibility) — RESOLVED by simplification**: EN keeps no
  German letters at all; no cross-language hiding rule is needed — per-language data is the policy.
- **W7 (release default vs corners) — RESOLVED**: right corner is always the default; consequences
  recorded in §3b.
- **W8 (packs go stale) — RESOLVED**: universal base + per-language deltas with UNION merge (§6);
  every pack rewritten, version bumps, `LanguageLetterHintsDataTest` extended with §5's table.
- **W9 (D-282 clamping) — kept as implementation note**: edge keys (`0`, `'`, `p`) clamp; corner
  fidelity yields, same spirit as D-282; reconciled with the new flow rule during D-510.
- **W10 (×÷ split) — accepted**: `×` stays on x, `÷` lives on 7-middle.
- **W11 (AZERTY anchor) — unchanged**: `'` lands right of `m`; middle row grows to 11 keys.

## 5. Analysis run: language packs vs the reform (verified from all 31 shipped hints.tsv)

- **Scandinavian primary-in-corner rule: already satisfied today, stays.** sv `a=ä,å` / `o=ö`;
  fi `a=ä,å` / `o=ö`; da `a=å,æ` / `o=ø`; nb `a=å,æ` / `o=ø` — each language's own letter leads and
  becomes the right corner (release-default); order flips vs German automatically.
- **Turkish** `i=ı o=ö u=ü c=ç g=ğ s=ş` — all correct corners. **Azerbaijani** has its own dedicated
  layout with ə ı ğ ş ç ö ü as primaries (§519) — its LETTERS stay; only its digit row adopts the
  reform (§2.12).
- **hu/pt lead variants — DECIDED (round 3, assistant's call)**: keep `o=ó,ö,ő` / `u=ú,ü,ű` and
  `a=á,…` / `o=ó,õ,ô` exactly as §519 fixed them (frequency-based lead; the pt device look is still
  pending there anyway). Flipping is a one-character data change later if a native speaker objects.
- **Bottom-row-first-key collisions (§2.8)**: the slot letters carry language letters in several
  packs — cs `y=ý` and `z=ž`, pl `z=ż,ź`, hr/bs/lt/sk `z=ž`, ro/da/nb/fi/sv/et/hu/it/ms/sw/tl
  `z=¶`-style symbols, tr/nl `z=±`, fr `w=è` (one of §519's deliberate scatter hints), id/sw/tl
  `y=¶`-style. All resolve the same way: language letter/symbol → right corner (release-default,
  today's release preserved), `+` takes the left corner. fr `w=è` may instead be retired in the fr
  delta (è already lives on e's popup) — the cleanup side effect, executed in the fr delta.
- **s-owners (for the new `*` left corner)**: cs/hr/bs/sk/lt/lv `š` (+pl `ś`) keep s's right corner;
  German keeps `ß`; everyone else gets an empty right corner.
- **n-owners (for the universal ñ right corner)**: pl `ń`, cs `ň`, lv `ņ` win the corner; ñ drops to
  their first popup extra — visible, reachable, no contradiction.
- **Collisions the base+delta union merge resolves elsewhere** (language letter first, base symbols
  appended): `c` (cs/hr/bs `č,ć`, fr/tr `ç`, es `¿`, da/fi/nb/sv `§` — base ÷ is gone from c anyway),
  `s` (above), `e` (fr `é,è,ê,ë` — fr has NO € today; merged order `é è ê ë €`), `a` (cs `á`,
  pt `á,ã,â,à`, ro `ă,â`), `l` (Nordic `l=~` entries are explicitly dropped in those deltas — ~
  moved to 1-left; es `l=ñ` dies per §2.6; delta format needs a removal marker), plus the currency
  shorthands (`da/nb/sv/fi t=kr`, `hu t=Ft`, `pl t=zł`, `tr t=₺`, …) which are pure deltas and stay
  untouched.
- **English**: stays as-is on the letters page minus the reform deltas (d `$`, n `(`+`ñ`, m `)`,
  s `*`-left, z `+`-left, new `'` key); it gains NO umlauts (decided).

## 6. Target data/UI model

- `Key` gains a corner pair (`leftHint`/`rightHint`; the old `hint` remains as the derived
  right-corner during migration). Drawing: left corner at `left + 6dp`, right at `right - 6dp`
  (same baseline); triangle rule unchanged but corner-aware; shifted uppercasing per corner.
- Popups are explicit data: `[leftCorner, extras…, rightCorner]` when both corners exist (straddle,
  pre-selected = right), flow per the centre rule (§2.11) with the o-key right-flow exception;
  single corner = today's single-hint behaviour (direct commit, no popup). >3 cells: today's centred
  mechanism with adapted order; D-282 clamp wins at edges. Under this model EVERY existing
  letters-page popup order and default is reproduced unchanged (`a [ä, æ, å, à]`,
  `e [€, é, è, ê, ë]`, `p [π, α, β, γ, δ, λ, ω]`, `o [ö, Ø]` flow-right) — the only order/default
  changes are the digits and d/n/m/s/y-z-w (§3b).
- **Merge semantics (fixed in round 3)**: UNION, not replace — compiled-in universal base + per-
  language deltas; a delta's letter(s) become that language's right corner + first popup entries,
  base symbols append after them (left corner + extras). This is what "alle lateinischen Layouts
  übernehmen die deutsche Alt-Belegung" means operationally, and it matches §5's collision table.
  Deltas can also REMOVE (empty/removal marker — e.g. the Nordic `l=~` retirements); the pack format
  extension (D-497 follow-up) needs to express: corner pair, extras, removal.
- Corner defaults: right corner if present; if only a left corner exists, it is the default
  (confirmed, R6).
- Hints data stays active-language driven (`AdaptKeyService.kt:1124`) — with EN keeping no umlauts
  and every other language carrying its own letters, the visibility policy is purely the data.

## 7. Implementation steps (draft)

1. **D-509 — Corner model + drawing (A):** `Key` corner pair, both corners drawn, right-corner =
   default rule (left-corner fallback), triangle rule, `cornerHintDisplayTextFor` extension, no
   behaviour change yet (every existing key still right-corner-only).
2. **D-510 — Popup geometry + flow (A):** corner-anchored `HorizontalLongPressPopup` positioning
   (straddle / centre-flow / o-exception variants), adapted order >3, D-282 reconciliation,
   unit-tested geometry.
3. **D-511 — Number row (B):** dual-corner `NUMBER_HINTS` per Eckpunkt 3, ÷ middle on 7, superscripts
   and `2`/`7` specials kept; shared by ALL layouts (§2.12 — Greek/Jcuken/Serbian/Azerbaijani
   digit-row tests updated to the new shared row).
4. **D-512 — Letters page (C):** base-map changes (d °/$, n ( + universal ñ-right, m ), s *-left,
   y/z/w +-left, v/b/c freed), `'` key on QWERTY/QWERTZ/AZERTY, bottom-row operator removal, popup
   couplings → explicit data, o-key flow exception.
5. **D-513 — Hints data (D):** base+delta union merge, all 31 packs rewritten as deltas (+ en asset),
   corner-pair/removal pack format (D-497 follow-up), version bumps, `LanguageLetterHintsDataTest`
   extended with §5's table; pl/cs/lv n-entries (ń ň ņ corner, ñ extra); fr `w=è` retirement; Nordic
   `l=~` removals.
6. **D-514 — Fallback page + docs (E):** page 2 explicitly untouched (R2 decision recorded), full
   suite, `:app:assembleRelease`, spec addenda (L-05/L-06 + §24/D-336 addenda, D-44 flow note,
   D-497 format note), Progress stubs + History entries.

## 8. Remaining open

- **O1 — formal nod on the `+` slot** (§2.8): the plan validates the user's own proposal (slot is
  screen-stable across QWERTZ/QWERTY/AZERTY; language letters keep the right corner; fr scatter
  cleanup rides along). Alternatives considered and not preferred: keeping `+` as a middle popup
  cell on n (loses corner visibility), or `u`/`i` (language-letter collisions everywhere). Await
  the final "ja" — then implementation starts with D-509.
- **O2 — device looks during implementation**: (a) reconcile the popup flow rule with D-282's
  clamping and verify the o-exception on device (round 3's R5); (b) the §3b release-default shifts
  get a deliberate device pass (digits, d, n, m, s, y/z/w).

## 9. Device checklist (draft)

- Both corners drawn and legible in portrait + landscape, light + dark; shifted uppercasing intact
  (ä→Ä as corner, ñ→Ñ universally, ń/ň/ņ on pl/cs/lv).
- Popup alignment: corners above their glyph edges for 2-3-cell popups; centre-flow on right-half
  keys; o flows right (`[ö, Ø]`); >3-cell digit popups sensible; `0`/`p`/`'` edge clamping behaves.
- Release defaults: digits commit the US row (`2`→`@` … `0`→`)`), d→`$`, n→`ñ` (→ ń/ň/ņ on
  pl/cs/lv), m→`)`, `'`→`'`, s(en)→`*`, y/z/w(en:z)→`+`; DE slide-left reaches `" § = + - / °`;
  language letters' releases unchanged where they own the key.
- DE shows ä ö ü ß corners; EN shows none of them; sv/fi/da/nb keep their own letters as corners;
  tr/az letters intact; es `l` carries no ñ any more.
- C-09 hidden number row: `*` via s, `+` via the bottom-row slot, `÷` via page 2 (accepted);
  comma/period popups unchanged; page 2 itself unchanged (R2).
- URL/email modes unchanged; AZERTY `'` right of m; all packs parse, all-packs walk green.

## 10. Out of scope

- Greek/Cyrillic/Azerbaijani LETTER geometry and hint deltas (their digit row does move, §2.12);
  the calculator page; C-01 proportions (the 10th/11th middle-row key is absorbed by the existing
  letter-weight formula); the T-03 personal offset model (the new `'` key starts fresh); any
  user-facing hint editor (D-281/D-301 removed it); page 2's content (R2: stays).
