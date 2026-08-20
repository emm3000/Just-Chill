# JustChill — Design System

> **`ui-android/src/androidMain/kotlin/com/emm/justchill/core/theme/` is the source of truth.**
> `EmmColors.kt`, `EmmType.kt`, `EmmSpacing.kt` and `EmmRadii.kt` hold every value. This document
> explains *why* a token exists and *when* to reach for it — it never repeats a value the code
> already holds. Need a hex, an sp or a dp? Open the file. A doc that copies a value is a cache
> with no invalidation.

---

## 1 · Principles

Five rules that govern everything below. When in doubt, return here.

1. **Numbers are the hero.** The amount is what the user came for; everything else recedes. The
   amount roles are the largest type in the app and the only ones in the mono family (§3.1).

2. **Negative space is a component.** Whitespace is a deliberate element with a name, a size and a
   reason. Crowding is a design failure, not a layout problem.

3. **Hierarchy through type and tone — never through hue.** Emphasis is a step down the text ladder
   (`textPrimary` → `textSecondary` → `textTertiary`) or a change of size and weight. It is never a
   new colour. Hue is reserved for meaning: one accent, four status tokens, six category tints.

4. **Positive is tinted; negative stays monochrome.** An income amount takes `success`; an expense
   amount stays `textPrimary` — an expense is never red. `danger` means destructive or broken, not
   "money leaving". This holds from the transaction row to the report hero; break it on one screen
   and red becomes ambiguous on all of them.

5. **Hairline over shadow.** Dark UIs read shadows as smudges. Separate surfaces with space, a 1dp
   `border` hairline, or a surface step. There is not one elevation shadow in `:ui-android` — keep
   it that way.

---

## 2 · Colour tokens

Read `EmmColors.kt` for values; reach for them through `LocalEmmColors.current`.

**Surface** — `bg`, `surface1`, `surface2`, `surface3`, `border`, `borderFocus`
The canvas is near-black, not black, and the ladder climbs in small steps so a card reads as raised
without a shadow. `border` is always drawn 1dp. **No more than three surface levels on one screen**
— if you need a fourth, the layout is over-nested.

**Text** — `textPrimary`, `textSecondary`, `textTertiary`, `textDisabled`, `textOnAccent`
Neither end of the range is pure: pure white on pure black vibrates. `textOnAccent` is the only
colour that goes on top of `accent`.

**Accent** — `accent`, `accentMuted`, `accentFocus`
One hue — a warm terracotta — and it is the brand. `accent` marks the single primary action of a
screen; `accentFocus` marks focus (input underline, caret). `accentMuted` is that same hue at low
alpha: a fill behind something, never a text colour.

**Status** — `success`, `warning`, `danger`, `info`, `posMuted`, `negMuted`
Status, not transaction type (§1.4). `posMuted`/`negMuted` are low-alpha washes of `success` and
`danger`, used as the ground behind an icon or inside a pill; the readable mark on top is the
full-strength token.

**Category** — `catSlate`, `catSage`, `catTerracotta`, `catMauve`, `catOchre`, `catGraphite`
Six muted tones chosen to coexist on `bg` without competing, `catGraphite` as the neutral fallback.
A category tints an icon or a dot, never a whole surface. The domain stores a colour *name*
(`"green"`, `"blue"`…) and `hh/report/ReportFormat.kt` maps it to a token — so the palette can be
retuned without a data migration.

> `core/theme/Color.kt` is the pre-token palette, still imported by two legacy screens. It is not
> part of this system and nothing new reads from it.

---

## 3 · Typography

### 3.1 Two families, split by job

Inter for language, IBM Plex Mono for money. Every `amount*` role is mono with `tnum`; every text
role is Inter. The split is the point: a column of amounts has to align digit-for-digit down the
screen, and the mono face makes an amount look like a *quantity* before it is read. Both families
ship as bundled font resources — nothing is fetched and nothing falls back to a system face.

### 3.2 Roles

`EmmType.kt` defines them; read the sizes there. Pick by intent:

- `amountHero`, `amountL` — the one number a screen exists to show.
- `amountM`, `amountS` — an amount inside a row or a summary line.
- `display`, `headlineL`, `headlineM` — screen and section titles.
- `titleL`, `titleM` — card titles, list section headers.
- `bodyL` (the default), `bodyM` — running text.
- `labelL`, `labelM` — buttons, tabs, chips.
- `caption` — timestamps and metadata.
- `eyebrow` — the all-caps section label (§3.4).

Never build a `TextStyle` inline to fill a gap between two roles. Add the role.

### 3.3 Number formatting

The rules only — `presentation/.../hh/shared/NumberFormatEs.kt` and `CurrencyFormat.kt` own the
code and `SpanishFormatGoldenTest` pins the output.

- Grouping is **comma-thousands, dot-decimals** (`1,234.56`). That is es-PE, and it is the reverse
  of the Spanish convention most references show. It is hardcoded rather than locale-derived, so it
  cannot drift with the device.
- The `S/` symbol goes before the number, one space. The sign goes before the symbol:
  `+S/ 1,234.56`.
- Use the Unicode minus `−`, never the hyphen `-` — it matches the width of `+`, so a column of
  signed amounts stays aligned.
- Two decimals, always. No "smart" hiding.
- In a hero amount the currency prefix and the decimals are deemphasised — smaller, and either
  `textTertiary` or a lowered alpha — so the integer part carries the glance.

### 3.4 Forbidden

- All caps outside the `Eyebrow` atom. Eyebrow labels a section the way a column header labels a
  table: a role, not decoration. Keep it short; a `·` may join two facts.
- Sizing text in `dp`. Every size is `sp` so the user's font scale works.
- More than two weights on one screen.
- Mixed text alignment inside one vertical column.

---

## 4 · Spacing and radii

Base unit **4dp** — every dimension on screen is a multiple of it. The scale is `EmmSpacing.kt`
(`s0`…`s12`); corner shapes are `EmmRadii.kt` (`r0` through `rXXL`, plus `rLTop` for sheet tops and
`rFull` for circles).

- Screen horizontal padding: `s4`. Never less.
- Card internal padding: `s4` on all sides.
- Between sections of distinct purpose: `s6`.
- Touch target minimum **48×48dp** — Material's accessibility floor, non-negotiable. An icon that
  looks 20dp still sits in a 48dp box; `core/ui/atoms/IconBtn.kt` is the pattern.
- Default to the smallest radius that reads right. Sharp corners carry the architectural feel;
  over-rounding makes it a toy.

---

## 5 · Elevation

**There are no shadows.** Separation comes from, in order of preference: space, a 1dp `border`
hairline, then a surface step. A modal that must read as "above" gets `surface1`, a hairline and
rounded top corners — and still no shadow.

---

## 6 · Iconography

- `Icons.Outlined.*`. Filled only when the icon represents a *state*.
- 24dp standard, 20dp inline with body text, 32dp rare (empty states).
- `textPrimary` by default, `textTertiary` when decorative, a `cat*` tint when it stands for a
  category or an account.
- Never a brand logo or a bank's registered colours — legal risk. An account is identified by the
  icon for its *type* plus a category tint; `hh/account/AccountPalette.kt` owns both maps.

---

## 7 · Component invariants

Per-component dimensions live in the component file. These are the rules that span components — the
ones no single file can tell you:

- **A card is `surface1` + a 1dp `border` + a rounded corner, and nothing else.** No shadow, no
  coloured card. If something needs to stand out, it usually does not need a card.
- **Rows and lists sit directly on `bg`.** A group earns a card only when a header or footer shares
  it.
- **Text inputs are underline-only**, never a filled box. The underline carries the state: `border`
  at rest, `accentFocus` on focus, `danger` on error with the helper text matching.
- **One `accent` element per screen.** If two things are terracotta, neither one is primary.
- **A destructive action is never accent-styled.** It reads as `danger` text and a `danger` hairline
  on a transparent ground.
- **Bars and charts have no track.** The empty part of a bar stays empty; a "remaining" track
  invents a total the data does not have.
- **The bar decorates, the row carries the meaning.** TalkBack reads the label-and-amount row
  (`contentDescription` on the row); the graphic itself is hidden from it.

---

## 8 · Accessibility

Non-negotiable floor:

- Contrast: text ≥ **4.5:1**, large text (≥18sp, or 14sp bold) ≥ **3:1**. Verify any new colour
  before adding it — no token is grandfathered in.
- Touch targets ≥ 48×48dp (§4).
- Every interactive element carries a `contentDescription` or `Modifier.semantics`; a decorative
  graphic is explicitly hidden, not left unlabelled.
- Never let a single character carry meaning. `+` and `−` are always paired with words TalkBack can
  read ("Ingreso", "Gasto").
- Respect the user's font scale and `ANIMATOR_DURATION_SCALE`.
