---
paths:
  - "ui-android/src/main/kotlin/**"
  - "core/ui/src/main/kotlin/**"
---

# Shared UI rules

The design system lives in `:core:ui`: the atoms under `core/ui/atoms/` (`com.emm.justchill.core.ui.atoms`) and the tokens under `core/ui/theme/` (`com.emm.justchill.core.ui.theme`). `core/ui/components/` holds legacy `Emm*` widgets that are **not** the design system. In `:ui-android`, `hh/shared/` is the cross-feature package (nav host, bottom bar, sheets), not a feature package.

## The iron rule

Feature screens call **only** the repo's atoms. **Never** a raw Material3 control — no `Button`, `TextField`, `OutlinedTextField`, `Card`, `IconButton`, `Snackbar`, `TopAppBar`. The atoms for those are `FilledCta` / `OutlinedCta` / `StickyCTA`, `UnderlineTextField`, `IconBtn`, `showEmmSnackbar`, `JcTopBar`.

`Switch` has no atom: it is allowed only with `colors = emmSwitchColors(...)` from `:core:ui`'s `core/ui/atoms/EmmSwitch.kt`, never Material's default colours. `Text` and `Icon` have no atom; they are allowed only with a `LocalEmmType` role and a `LocalEmmColors` token, never an inline `TextStyle` or a literal color. `ModalBottomSheet` is the one Material3 container in use, always with `SheetDragHandle`.

A custom component written inside a screen never replaces an atom that exists for that purpose. If the atom does not fit, extend or modify it first.

## Before creating a component

1. **Check `core/ui/atoms/` first.** If it exists, use it. No exceptions.
2. **Decide the scope.** Used by a single screen, it belongs to the feature package in `:ui-android` (`hh/<feature>/`, as a sibling file or under `components/`). Used app-wide, it belongs in `:core:ui`'s `core/ui/atoms/`.
3. **If it must be created**, template on `IconBtn.kt` (the 48dp target around a smaller glyph) or `FilledCta.kt`, name it by its role with a matching file name, and include a `@Preview` wrapped in `EmmTheme`.

## Theme tokens are the style guide

`EmmColors.kt`, `EmmType.kt`, `EmmSpacing.kt` and `EmmRadii.kt` hold every value, reached through `LocalEmmColors`, `LocalEmmType`, `LocalEmmSpacing` and `LocalEmmRadii`. There is no document holding hex codes or dp, and none should be created: a second copy drifts. This section says which token to reach for and why; the value is in the file.

Nothing enforces these rules mechanically: the gate sees Kotlin, not dp, and goes green on a 14dp tap target. Conformance is verified on the `medium_phone` emulator. A deliberate exception is stated at the site in one line naming the constraint; a screen that needs a new value adds a token, never a literal.

### Principles

1. **Numbers are the hero.** The amount is what the user came for. The `amount*` roles are the largest type in the app and the only ones in the mono family. A screen's summary has **one** hero amount; the others step down to `textSecondary` on one line.
2. **Negative space is a component.** Whitespace has a name (`EmmSpacing`), a size and a reason. Crowding is a design failure.
3. **Hierarchy through type and tone, never through hue.** Emphasis is a step down the text ladder (`textPrimary` → `textSecondary` → `textTertiary`) or a change of size and weight. Hue is reserved for meaning: one accent, four status tokens, six category tints.
4. **Positive is tinted; negative stays monochrome.** An income amount takes `success`; an expense stays `textPrimary`, never red. `danger` means destructive or broken, not "money leaving". A signed net or balance aggregate (a month net, a total owed) follows the same rule: positive takes `+` and `success`, zero or negative stays monochrome. A magnitude under its own label ("Por cobrar", "Entran") is not a net and keeps the unsigned income/expense semantics. A new net or balance routes its sign and tint through `Money.positiveMoneyFormatted()` (`:presentation`) and `AmountTone.color()` (`:core:ui`'s `core/ui/atoms/AmountTone.kt`); an inline `if` there reopens the monochrome-positive bug.
5. **Hairline over shadow.** Surfaces separate with space, a 1dp `border` `Hairline`, or a surface step. There is not one elevation shadow in the design system; keep it that way.

### Colour

- **Surface** (`bg`, `surface1`…`surface3`, `border`, `borderFocus`): the ladder climbs in small steps so a card reads as raised without a shadow. No more than three surface levels on one screen; a fourth means the layout is over-nested.
- **Text** (`textPrimary`, `textSecondary`, `textTertiary`, `textDisabled`, `textOnAccent`): `textOnAccent` is the only colour that goes on top of `accent`.
- **Accent** (`accent`, `accentMuted`, `accentFocus`): one hue, and it is the brand. `accent` marks the single primary action of a screen; `accentFocus` marks focus; `accentMuted` is a fill behind something, never a text colour.
- **Status** (`success`, `warning`, `danger`, `info`, `posMuted`, `negMuted`): system state, never transaction type. The `*Muted` washes are the ground behind an icon or inside a `Pill`; the readable mark on top is the full-strength token.
- **Category** (`cat*` tints, `catGraphite` the fallback): tint an icon or a dot, never a whole surface. The domain stores a colour *name*; `hh/report/ReportFormat.kt` maps it to a token, so the palette can be retuned without a migration.

### Typography

- Two families split by job: Inter for language, IBM Plex Mono with `tnum` for every `amount*` role, both bundled. Pick a role by intent: `amountHero` / `amountL` for the one number a screen exists to show, `amountCard` inside a card, `amountLead` for the amount a row leads with, `amountM` / `amountS` where the label leads; `display` / `headlineL` / `headlineM` for titles, `titleL` / `titleM` for cards and section headers, `bodyL` (default) / `bodyM` for running text, `labelL` / `labelM` for buttons and chips, `caption` for metadata, `eyebrow` only through the `Eyebrow` atom.
- Number formatting is owned by `:core:ui`'s `NumberFormatEs.kt` and `CurrencyFormat.kt` and pinned by their golden tests: comma thousands, dot decimals (es-PE, hardcoded), `S/` before the number with one space, the sign before the symbol (`+S/ 1,234.56`), the Unicode minus `−` never the hyphen, two decimals always. In a hero amount the prefix and decimals are deemphasised so the integer part carries the glance.
- Italics are reserved for a secondary meta label (the `Variable` marker, a note). Never an amount.
- Forbidden: all caps outside `Eyebrow`; text sized in `dp`; more than two weights on one screen; mixed alignment inside one vertical column.

### Spacing, radii, elevation, icons

- Base unit 4dp: `EmmSpacing` `s0`…`s12`. Screen horizontal padding `s4`, never less; card padding `s4`; `s6` between sections of distinct purpose.
- Touch targets are 48×48dp, non-negotiable. A child of a fixed-height row is not 48dp by inheritance: `Alignment.CenterVertically` measures at intrinsic height, so a clickable inside a 48dp band carries `fillMaxHeight()` itself. A 48dp header target keeps its glyph on the rows' column by giving the padding back at the edge, never by shrinking the target.
- `EmmRadii` `r0`…`rXXL`, `rLTop` for sheet tops, `rFull` for circles. Default to the smallest radius that reads right.
- No shadows. A modal that must read as "above" gets `surface1`, a hairline and rounded top corners.
- `Icons.Outlined.*`; filled only when the icon represents a state. 24dp standard, 20dp inline with body text, 32dp rare. Never a brand logo or a bank's registered colours: an account is its type's icon plus a category tint, both maps in `hh/account/AccountPalette.kt`.

### Component invariants

- A card is `surface1` + a 1dp `border` + a rounded corner, and nothing else. Rows and lists sit directly on `bg`; a group earns a card only when a header or footer shares it.
- Text inputs are underline-only (`UnderlineTextField`): `border` at rest, `accentFocus` on focus, `danger` on error with the helper text matching.
- One `accent` element per screen. A destructive action is never accent-styled: `danger` text and a `danger` hairline on a transparent ground.
- Bars and charts have no track; the bar decorates and the row carries the meaning (`contentDescription` on the row, the graphic hidden from TalkBack).
- A transaction row is titled by what the user wrote, then by the category, never by a placeholder; `TransactionUi.title` / `subtitle` derive it in `:presentation`.
- No screen shows an account "balance": accounts have no opening balance, so every per-account figure is a month-scoped net and says so.
- A screen never applies window insets itself: `AppNavHost`'s `Scaffold` owns them and hands them down as `PaddingValues`. `ModalBottomSheet` is its own window and the one exception.

### Accessibility floor

Contrast ≥ 4.5:1 for text, ≥ 3:1 for large text, no token grandfathered. Every interactive element carries a `contentDescription` or `Modifier.semantics`; a decorative graphic is explicitly hidden. `+` and `−` are always paired with words TalkBack can read. Font scale and `ANIMATOR_DURATION_SCALE` are respected.

## Never

- A raw Material3 control in a feature screen.
- A literal color, `.sp` size or `.dp` padding outside `:core:ui`'s `core/ui/theme/`.
- Semantic colors (`success` / `warning` / `danger`) used to mean anything other than a system state.
- An illustrated mascot in an empty state.
- Emoji as an icon. Icons are vector assets or drawn paths.
