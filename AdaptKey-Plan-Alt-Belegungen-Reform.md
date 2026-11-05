# AdaptKey — Plan: Alt-Belegungs-Reform (D-509 umbrella, phases D-509…D-514)

**Status: READY — ausführungsbereit** (2026-11-05, nach fünf Abstimmungs-/Beobachtungsrunden).
Nicht gestartet; der Start erfolgt auf ausdrückliches Go hin (Empfehlung: eigener Chat, Start-Prompt
unten in §11). Diese Datei ist in sich geschlossen: `.kilo/command/start-prompt.md` + dieses Dokument
reichen einer frischen Session zur Umsetzung.
**Backlog item:** six Progress stubs D-509…D-514 assigned at implementation start (§7) — this file
stays the single umbrella document.

Runden-Log: (1) Nutzerplan strukturiert; (2) W1–W8 entschieden (Nicht-Latein steigen um, C-09-Verlust
akzeptiert, ñ-Frage offen, EN ohne Umlaute bestätigt, Rechts-Ecke = Default, Base+Delta); (3) `*`
fest auf s-links, `+`-Frage gestellt, ñ universell, Seite 2 bleibt, hu/pt-Leads beim Assistenten,
R4-Buchhaltung erklärt, R6 bestätigt, Popup-Fluss als Mitte-Regel mit o-Ausnahme formuliert;
(4) `+` entschieden (linke Ecke, y/z/w-Slot), Device-Blicke vorgezogen; (5) Device-Beobachtungen
geliefert — der Popup-„Fluss" ist root-caused als D-282-Klemm-Umdrehen (o UND p) bzw. D-54
(Einzelzellen), Baseline §3b device-bestätigt. Damit ist alles offen Gewesene geschlossen.

---

## 1. Current state (verified; line refs as of this draft — re-locate before trusting)

- **Number row** (`KeyboardLayout.NUMBER_HINTS`, KeyboardLayout.kt:33): the German-QWERTZ shifted
  symbols `! " § $ % & / ( ) =` on every layout. `numberKey()` (KeyboardLayout.kt:401) builds each
  digit's popup `[shifted, superscript]`, with `2` = `" ' ² ₂` (D-382/D-417) and `7` = `/ \ ⁷` (D-498).
  `NUMBER_SUPERSCRIPTS` (KeyboardLayout.kt:41) stays.
- **The number row is shared by EVERY layout**: `AzertyLayout` (AzertyLayout.kt:59) delegates to
  `KeyboardLayout.numberKey`, and per its own KDoc (KeyboardLayout.kt:397) so do Greek, Jcuken
  (Russian/Ukrainian), Serbian and Azerbaijani. DECIDED: the non-Latin layouts benefit too and
  receive the reformed row — no pinning; their digit-row tests get updated.
- **Letter hints**: `DEFAULT_LETTER_HINTS` (KeyboardLayout.kt:157) is the compiled-in German map
  (`q=@ e=€ h=# m=- n=+ d=° p=π x=× c=÷ v=/ b=* f=ƒ a=ä o=ö u=ü s=ß`). Per active language it is
  REPLACED (not merged) by the language's own full copy in `hints.tsv`
  (`AdaptKeyService.kt:1124` → `SettingsStore.kt:301` → `LanguageLetterHintsLoader`). DECIDED:
  switch to compiled-in universal BASE + per-language DELTA with UNION merge (§6).
- **English today** (`assets/en/hints.tsv`): `b=* c=÷ d=° e=€ f=ƒ h=# m=- n=+ p=π q=@ v=/ x=×` —
  no ä ö ü ß anywhere. CONFIRMED DESIRED: English keeps the German letters out entirely. The
  round-1 "triangle for hidden umlauts" idea is DROPPED.
- **Corner glyph**: exactly ONE hint, drawn top-right (`AdaptKeyboardView.drawKeys`,
  AdaptKeyboardView.kt:1128-1147); keys with ≥2 alternatives and no own hint draw the triangle
  `MORE_ALTERNATIVES_GLYPH = "◢"` (D-98, AdaptKeyboardView.kt:2756). Shifted state uppercases letter
  hints (`cornerHintDisplayTextFor`, AdaptKeyboardView.kt:1263) — covers ñ/Ñ, ń/Ń etc.
- **Popup geometry — RESOLVED by device observation (2026-11-05) and fully explained in code**:
  - Multi-cell popups are centred over the PRE-SELECTED cell (D-44:
    `HorizontalLongPressPopup.rowLeft`, pre-selection = the key's own char when present, else index
    0; AdaptKeyboardView.kt:1898-1914), extras extending right in list order.
  - When the row would run out of room on the right, D-282 REVERSES the whole order
    (AdaptKeyboardView.kt:1868-1872) so the pre-selected cell stays under the finger and the extras
    extend LEFT. **Device-confirmed firing on both**: o (2 cells — tiny overflow → `[Ø, ö]`, ö over
    the key, Ø LEFT of it, exactly the reported smear problem) and p (7 cells — π right-aligned over
    the p key, Greek letters growing left, observed as "so soll es sein").
  - Single-cell previews are nudged a few pixels toward the keyboard centre (D-54,
    AdaptKeyboardView.kt:1862-1867) — device-confirmed: u's ü shifted left (right half), s's ß
    shifted right (left half), both "so soll es sein".
  - A left-half multi-cell key (a: ä æ å à) is centred with extras growing right — device-confirmed.
  - The reform's new rule (§2.11) therefore REPLACES D-282's unconditional reversal with a
    drift-gated clamp; D-54 and D-44's centring stay.
- **Letter popups are coupled to the DEFAULT hint by equality** (`letterKey`, KeyboardLayout.kt:345):
  `p`=π→Greek letters, `o`=ö→Ø, `a`=ä→æ å à, `e`=€→é è ê ë, `n`=+→ñ — only while the key still
  carries its default hint. RESOLVED: popups become explicit per-key data.
- **Bottom row** (letters page): `x=× c=÷ v=/ b=* n=+ m=-`; comma popup `/ - , : ; _` (D-499),
  period popup `! . ?`. The reform removes ÷ / * + - from the letters; `×` stays on `x`.
- **Fallback when C-09 hides the number row**: `SymbolLayout` page 2 (`CATCHALL_DIGITS`,
  `CATCHALL_NUMBER_SYMBOLS = !"§$%&/()=` SymbolLayout.kt:101, catch-all hints row `# ' ^ ° - + × ÷ * ±`
  SymbolLayout.kt:113). DECIDED: this page STAYS AS IT IS — nobody reaches the special-characters
  page to long-press things; its space lets nearly everything sit directly on a key.
- **Related invariants that must survive untouched**: the bottom-row LETTERS stay letters — T-05's
  ambiguity bands, A-10's connector-split and D-391's fusion connector set (`y x c v b n m`) key on
  the letters, not their corners. Greek's letters page never uses the Latin hint maps (own accent
  system) — only its digit row rides along (§2.12). D-380/D-437's smear protection stays as is; the
  drift-gated clamp complements it on the o key.
- **Middle row width**: `KeyProportions.thirdRowLetterWeight(letterCount)` (KeyProportions.kt:76) is
  generic — a 10th (QWERTY/QWERTZ) or 11th (AZERTY) letter key just shrinks the neighbours; row
  total preserved. `Key.id` (`c:'`) is unique, so gesture/personal-offset data starts clean (T-03).

## 2. Agreed direction (Eckpunkte, final)

1. **Two corner symbols per key** become a general concept: one LEFT and one RIGHT corner glyph.
   **The RIGHT corner is always the default** (pre-selected popup cell + what a plain
   long-press-release commits), one rule for every layout; when only a LEFT corner exists, it
   becomes the default (confirmed).
2. **Apostrophe key**: a dedicated `'` key right of `l` (QWERTY/QWERTZ) / right of `m` (AZERTY);
   left corner `"`, right corner `'` (default → release commits `'`). All Latin layouts get it.
3. **Number row — exactly per the user's assignment table**:

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

4. **US holy cows stay put as right corners** — `! @ # $ % ^ & * ( )` on `1…0`, and each is also
   the release-default of its digit.
5. **Letters page**: `@` stays on `q`, `#` stays on `h`, `π` on `p`, `ƒ` on `f`, `×` on `x`;
   `e=€` stays on the German layout. **d**: left `°`, right `$` (default). **n**: left `(`, right
   `ñ`; **m**: left `)`; **v**, **b**, **c** are freed entirely. `+ - / * ÷` leave the bottom row
   to their digit-row slots; `×` stays on `x`.
6. **ñ keeps its home on n — universally** (user decision): `ñ` becomes the RIGHT corner of `n` on
   every Latin language. The corner model absorbs the one nuance: the three languages owning an
   n-letter keep it as the primary right corner — pl `ń`, cs `ň`, lv `ņ` — and ñ moves to their
   popup's first extra (straddle `[ ( | ñ | ń ]`: ñ middle, language letter above-right and still
   the release-default). For everyone else `n = [ ( | ñ ]`. The es oddity `l=ñ` dies. Shifted
   display `Ñ` is covered by the existing uppercasing.
7. **`*` as the LEFT corner on s — FIXED**: `s = [ * | ß ]` on German, `s = [ * | š/ś ]` on the
   languages owning s (cs hr bs pl sk lt lv), `s = [ * | — ]` on English (left corner becomes the
   default). The one operator rescued from C-09's hidden-number-row loss (footnotes/wildcards).
   P stays untouched (π + 5 Greek extras = already the heaviest letter key).
8. **`+` redundantly on the first key of the bottom row** — DECIDED (round 4): LEFT corner, so it
   sits consistently with the other math operators (5/6/7-left); via the R6 fallback it is still
   the release-default wherever the right corner is empty (de/en: release `+`). The geometry's
   first third-row key — `y` on QWERTZ, `z` on QWERTY, `w` on AZERTY — is screen-stable as a SLOT
   across all geometries while the letter is the per-language assignment. Language letters on those
   keys (cs `ý`/`ž`, pl `ż,ź`, fr `è`, …) keep the right corner per the standing rule; `+` as left
   corner never displaces them. Bonus: freeing fr `w` retires one of §519's deliberate French
   scatter hints (`è` stays fully reachable on e's own popup).
9. **Superscript/subscript digit alts stay** (§2.3 "further alts"; `2`/`7` specials keep their
   existing extra cells).
10. **Umlaut policy**: German layout keeps ä ö ü ß visible as (right-)corners; English keeps them
    out ENTIRELY; every other Latin language keeps its own letters as corners per its pack
    (verified correct, §5). No hiding mechanism, no triangle-for-umlauts — the per-language data
    model already implements the rule.
11. **Popup geometry and flow — final design** (round 5, replaces round 3's "centre-flow with
    o-exception"; the observation showed the o-case is D-282's reversal, so the fix is a general
    rule, not a special case):
    1. Corner-aligned straddle for exactly 2-3-cell popups on dual-corner keys: left-corner char
       above the key's left edge, right-corner char above the right edge; extras in between
       (`2` = `" ' ² ₂ @`, `7` = `/ ÷ \ ⁷ &`); pre-selected = the right-corner cell.
    2. Otherwise: pre-selected cell centred over the key, extras extending right in list order
       (D-44 unchanged — a: `[ä, æ, å, à]`, e: `[€, é, è, ê, ë]`, p: `[π, α, β, γ, δ, λ, ω]`).
    3. **Drift-gated clamp replaces D-282's unconditional reversal**: when the row overflows the
       screen edge, shift it left by exactly the missing pixels as long as the PRE-SELECTED cell's
       centre stays within the PRESSED key's own horizontal bounds. This yields the o fix the user
       asked for: `[ö, Ø]` with ö over the key (a few px left, within the key) and **Ø to the
       RIGHT of ö** — away from the left index finger's smear path.
    4. Only when the pre-selected cell would visibly LEAVE the pressed key does the order reverse
       (D-282's own purpose kept): p keeps its observed, user-approved shape — π over the p key,
       Greek letters growing left.
    5. Single-cell previews keep D-54's centre nudge (u left-confirmed, s right-confirmed).
    Edge cases: D-282's URL-protocol popups and any pack-data popup ride the same rule; the right
    corner being the pre-selected cell means corner-aligned popups satisfy rule 3 trivially.
12. **All layouts move together**: every Latin layout adopts the reformed base map (language letters
    stay primary corner + first in order), AND the non-Latin layouts (Greek, Cyrillic, Azerbaijani)
    receive the reformed digit row too.

## 3. Target letters page (base map, deltas per §5/§6)

| Key | corners after reform (left \| right) | notes |
| --- | ------------------------------------ | ----- |
| q   | — \| `@`                             | unchanged; release `@` |
| e   | — \| `€` (DE)                        | extras é è ê ë flow right (unchanged order) |
| d   | `°` \| `$`                           | NEW; release changes ° → $ |
| f   | — \| `ƒ`                             | unchanged |
| h   | — \| `#`                             | unchanged |
| p   | — \| `π`                             | Greek extras unchanged; popup keeps its observed shape (§2.11.4) |
| x   | — \| `×`                             | unchanged |
| a   | — \| `ä` (DE) / language letter      | extras æ å à grow right (unchanged order) |
| o   | — \| `ö` (DE) / `ø` (da nb) / `ö` (sv fi) | Ø extra grows RIGHT via the drift-gated clamp (§2.11.3) |
| u   | — \| `ü` (DE)                        | unchanged |
| s   | `*` \| `ß` (DE) / `š ś` (cs hr bs pl sk lt lv) / — (en) | NEW left corner |
| n   | `(` \| `ñ` (universal; pl ń / cs ň / lv ņ win the corner, ñ → first extra) | NEW; release ñ (→ ń/ň/ņ there) |
| m   | `)` \| —                             | NEW; release ) (left corner is the default) |
| y z w | `+` \| (language letter where one exists) | NEW — the first third-row key per geometry (§2.8) |
| v b c | freed (no corners, no popups)       | their `/ * ÷` live on the digit row now |
| l (new key) | `"` \| `'`                    | release commits `'`; `"` reachable by slide-left |

Rule inherited from D-497, extended to corners: a language's own letter, when present, is the RIGHT
corner (primary, release-default) and first in popup order; base symbols append (left corner + popup
extras).

## 3b. Release-default shift table (GERMAN layout; "before" side device-verified 2026-11-05)

Long-press-release today vs after the reform (DE): digits `2 "→@`, `3 §→#`, `6 &→^`, `7 /→&`,
`8 (→*`, `9 )→(`, `0 =→)` (1/4/5 unchanged); letters `d °→$`, `n +→ñ`, `m -→)`; `v` and `b` lose
their long-press entirely; `y` GAINS a long-press (→ `+`, there was none before; en: `z` the same);
`s ß→ß`, `o ö→ö`, `q @`, `h #`, `e €`, `a ä`, `u ü`, `p π`, `x ×`, `f ƒ` unchanged. Slide-left now
delivers the German survivors `" § =` and the math singles `+ - / °`. The deliberate cost of
"right corner = default, one rule for all". Baseline confirmed by the user on device 2026-11-05
("alles bestätigt": 2→", 3→§, 6→&, 7→/, 8→(, 9→(, 0→=, d→°, n→+, m→-, s→ß, o→ö, y→nothing).

## 4. Contradictions from round 1 — resolution status

- **W1 (shared number row) — RESOLVED**: non-Latin layouts ARE rebuilt with the reform; no pinning.
- **W2 (C-09 hides `+ * ÷`) — RESOLVED**: accepted; `*` rescued via s-left, `+` additionally via the
  bottom-row slot; only `÷` remains digit-row-only (`/ -` survive in the comma popup, `! ?` in the
  period popup, `×` on x).
- **W3 (ñ's home) — RESOLVED**: ñ stays on n as right corner, universally (§2.6).
- **W4 (hint-equality popups) — RESOLVED structurally**: popups become explicit per-key data.
- **W5/W6 (EN umlauts; visibility driver) — RESOLVED by simplification**: EN keeps no German letters;
  per-language data is the whole policy.
- **W7 (release default vs corners) — RESOLVED**: right corner is always the default; §3b.
- **W8 (packs go stale) — RESOLVED**: universal base + per-language deltas with UNION merge (§6).
- **W9 (D-282 clamping) — RESOLVED by the drift-gated clamp (§2.11.3/4)**: the reversal survives
  only as the last resort (pre-selected cell leaving the key); o falls to the plain clamp, p keeps
  its reversal.
- **W10 (×÷ split) — accepted**: `×` stays on x, `÷` lives on 7-middle.
- **W11 (AZERTY anchor) — unchanged**: `'` lands right of `m`; middle row grows to 11 keys.

## 5. Analysis run: language packs vs the reform (verified from all 31 shipped hints.tsv)

- **Scandinavian primary-in-corner rule: already satisfied today, stays.** sv `a=ä,å` / `o=ö`;
  fi `a=ä,å` / `o=ö`; da `a=å,æ` / `o=ø`; nb `a=å,æ` / `o=ø`.
- **Turkish** `i=ı o=ö u=ü c=ç g=ğ s=ş` — all correct corners. **Azerbaijani**: dedicated layout
  with ə ı ğ ş ç ö ü as primaries (§519) — letters stay; only its digit row adopts the reform.
- **hu/pt lead variants — DECIDED (assistant's call)**: keep `o=ó,ö,ő` / `u=ú,ü,ű` and `a=á,…` /
  `o=ó,õ,ô` exactly as §519 fixed them (frequency-based lead; pt device look still pending there).
  A flip is a one-character data change later.
- **Bottom-row-first-key collisions (§2.8)**: cs `y=ý`/`z=ž`, pl `z=ż,ź`, hr/bs/lt/sk `z=ž`,
  ro/da/nb/fi/sv/et/hu/it/ms/sw/tl `z=¶`-style symbols, tr/nl `z=±`, fr `w=è` (a §519 scatter hint),
  id/sw/tl `y=¶`-style. All resolve the same way: language letter/symbol → right corner (release
  preserved), `+` left. fr `w=è` may instead be retired in the fr delta (cleanup side effect).
- **s-owners**: cs/hr/bs/sk/lt/lv `š` (+pl `ś`) keep s's right corner; German `ß`; others empty.
- **n-owners**: pl `ń`, cs `ň`, lv `ņ` win the corner; ñ drops to their first popup extra.
- **Collisions the base+delta union merge resolves elsewhere**: `c` (cs/hr/bs `č,ć`, fr/tr `ç`,
  es `¿`, da/fi/nb/sv `§` — base ÷ is gone from c anyway), `e` (fr `é,è,ê,ë` — fr has NO € today;
  merged order `é è ê ë €`), `a` (cs `á`, pt `á,ã,â,à`, ro `ă,â`), `l` (Nordic `l=~` entries
  explicitly dropped in those deltas — ~ moved to 1-left; es `l=ñ` dies; the delta format needs a
  removal marker), plus currency shorthands (`da/nb/sv/fi t=kr`, `hu t=Ft`, `pl t=zł`, `tr t=₺`, …)
  which are pure deltas and stay untouched.
- **English**: stays as-is on the letters page minus the reform deltas (d `$`, n `(`+`ñ`, m `)`,
  s `*`-left, z `+`-left, new `'` key); it gains NO umlauts.

## 6. Target data/UI model

- `Key` gains a corner pair (`leftHint`/`rightHint`; the old `hint` remains as the derived
  right-corner during migration). Drawing: left corner at `left + 6dp`, right at `right - 6dp`
  (same baseline); triangle rule unchanged but corner-aware; shifted uppercasing per corner.
- Popups are explicit data: `[leftCorner, extras…, rightCorner]` on dual-corner keys (straddle,
  pre-selected = right), otherwise pre-selected centred + extras right (§2.11.2); geometry per the
  drift-gated clamp (§2.11.3/4) and D-54 for single cells. Every existing letters-page popup order
  and default is reproduced unchanged except digits and d/n/m/s/y-z-w (§3b).
- **Merge semantics**: UNION, not replace — compiled-in universal base + per-language deltas; a
  delta's letter(s) become that language's right corner + first popup entries, base symbols append
  (left corner + extras). This is "alle lateinischen Layouts übernehmen die deutsche Alt-Belegung"
  operationally, and matches §5. Deltas can also REMOVE (empty/removal marker — e.g. Nordic `l=~`
  retirements). The pack format extension (D-497 follow-up) needs to express: corner pair, extras,
  removal.
- Corner defaults: right corner if present; if only a left corner exists, it is the default.
- Hints data stays active-language driven (`AdaptKeyService.kt:1124`) — with EN keeping no umlauts
  and every other language carrying its own letters, visibility is purely the data.

## 7. Implementation steps (ausführungsbereit; one round each, own commit per round, version bump per feature completion per rules/version-bump.md)

1. **D-509 — Corner model + drawing (A):** `Key` corner pair, both corners drawn, right-corner =
   default rule (left-corner fallback), triangle rule, `cornerHintDisplayTextFor` extension, no
   behaviour change yet (every existing key still right-corner-only).
2. **D-510 — Popup geometry (A):** implement §2.11: corner-aligned straddle for dual-corner keys,
   drift-gated clamp REPLACING D-282's unconditional reversal (reversal only when the pre-selected
   cell would leave the pressed key), D-54 kept; acceptance criteria: o `[ö over key, Ø right]`,
   p unchanged (π over p, extras left), u/s nudges unchanged, a extras right; unit-tested geometry
   (`HorizontalLongPressPopup` gains the drift-gate decision as a pure function).
3. **D-511 — Number row (B):** dual-corner `NUMBER_HINTS` per §2.3, ÷ middle on 7, superscripts and
   `2`/`7` specials kept; shared by ALL layouts (§2.12 — Greek/Jcuken/Serbian/Azerbaijani
   digit-row tests updated to the new shared row).
4. **D-512 — Letters page (C):** base-map changes (d °/$, n ( + universal ñ-right, m ), s *-left,
   y/z/w +-left, v/b/c freed), `'` key on QWERTY/QWERTZ/AZERTY, bottom-row operator removal, popup
   couplings → explicit data.
5. **D-513 — Hints data (D):** base+delta union merge, all 31 packs rewritten as deltas (+ en
   asset), corner-pair/removal pack format (D-497 follow-up), version bumps,
   `LanguageLetterHintsDataTest` extended with §5's table; pl/cs/lv n-entries (ń ň ņ corner, ñ
   extra); fr `w=è` retirement; Nordic `l=~` removals.
6. **D-514 — Fallback page + docs (E):** page 2 explicitly untouched, full suite,
   `:app:assembleRelease`, spec addenda (L-05/L-06 + §24/D-336 addenda, D-44/D-282 drift-gate note,
   D-497 format note), Progress stubs + History entries.

## 8. Remaining open

None. O1 (`+` slot) closed round 4; O2 closed round 5 — (a) root-caused and turned into the
drift-gated clamp design (§2.11), (b) baseline device-confirmed (§3b). The deliberate POST-reform
device pass (§3b's "after" side and the popup acceptance criteria of §2.11) stays part of the
phases as §9 — it cannot exist before the reform does.

## 9. Device checklist (per phase, plus the reform's acceptance items)

Phase items ride their own rounds (house style: NOT device-confirmed noted per round). The reform's
own acceptance items:
- Both corners drawn and legible in portrait + landscape, light + dark; shifted uppercasing intact
  (ä→Ä, ñ→Ñ universally, ń/ň/ņ on pl/cs/lv).
- Popup acceptance: o popup shows `[ö over the key, Ø RIGHT of it]` (the round-3/5 fix); p popup
  UNCHANGED (π right-aligned over p, Greek letters left); u/s single-cell nudges unchanged; a extras
  right unchanged; digit popups corner-aligned with >3-cell adaptation sensible; `0`/`'` edge
  clamping behaves.
- Release defaults: digits commit the US row (`2`→`@` … `0`→`)`), d→`$`, n→`ñ` (→ ń/ň/ņ on
  pl/cs/lv), m→`)`, `'`→`'`, s(en)→`*`, y/z/w(en:z)→`+`; slide-left reaches `" § = + - / °`;
  language letters' releases unchanged where they own the key.
- DE shows ä ö ü ß corners; EN shows none of them; sv/fi/da/nb keep their own letters as corners;
  tr/az letters intact; es `l` carries no ñ any more.
- C-09 hidden number row: `*` via s, `+` via the bottom-row slot, `÷` via page 2 (accepted);
  comma/period popups unchanged; page 2 itself unchanged.
- URL/email modes unchanged; AZERTY `'` right of m; all packs parse, all-packs walk green.

## 10. Out of scope

Greek/Cyrillic/Azerbaijani LETTER geometry and hint deltas (their digit row does move); the
calculator page; C-01 proportions (the 10th/11th middle-row key is absorbed by the existing
letter-weight formula); the T-03 personal offset model (the new `'` key starts fresh); any
user-facing hint editor (D-281/D-301 removed it); page 2's content (stays).

## 11. Starting the execution (recommended: fresh chat)

The plan file is self-contained; a fresh session avoids carrying this conversation's full history
through a six-round implementation. Kick-off prompt to paste:

> Bitte befolge zuerst .kilo/command/start-prompt.md. Dann lies AdaptKey-Plan-Alt-Belegungen-Reform.md
> (Status: READY) und setze die Phasen der Reihe nach um, beginnend mit D-509 (Corner-Modell +
> Zeichnung, kein Verhaltenswechsel). Eine Phase = eine eigene Runde mit eigenem Commit und den im
> Plan §7 notierten Tests; Device-Checkliste §9 pro Phase beachten; nicht pushen.

Continuing here also works (the plan carries everything), but a fresh chat keeps each round's
context lean — same pattern as the D-500/D-508 rounds before.
