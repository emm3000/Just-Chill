---
paths:
  - "iosApp/**"
---

# Swift screen rules

A SwiftUI screen renders a Kotlin ViewModel's state and forwards the user's events; every decision lives in Kotlin, where `architecture.md` governs it. This file adapts `architecture.md`'s `## MVI contract` and `ui-components.md` to SwiftUI. Each falsifier prints nothing on a conforming tree.

## The MVI contract

- **The store is the only door.** A screen drives its ViewModel through `Bridge/MviStore.swift` (#526) and nothing else: `state` to read, `send(_:)` for an intent, `onEffect(_:)` for the one effect closure. How a screen holds and builds its store is `iosApp/CLAUDE.md` `## The store`.
  - `rg -n 'for await' iosApp/JustChill --glob '!**/Bridge/**'`
  - `rg -n 'collectState|collectEffects|send\(intent:|\.clear\(\)' iosApp/JustChill --glob '!**/Bridge/**'`
- **A `body` maps, never decides.** It turns `store.state` into views and a user event into `store.send(...)` with an intent named for what the user did. A domain rule in Swift (a balance summed, a validity check, a date compared) is a missing field on the Kotlin `UiState`: a screen ticket never edits Kotlin, so it reports the field as a follow-up ticket and never computes it in Swift.
- **An effect is handled once, in the closure.** The `onEffect` closure navigates, dismisses or shows a transient message. An effect is never copied into `@State`, where the next redraw fires it again; what the screen keeps rendering (a sheet, a dialog) is a `UiState` field.
  - One exception: a payload handed from an effect to a system presenter (`.fileExporter`, the share sheet) and cleared when that presenter closes may sit in `@State`, because a presenter has no `UiState` field to read it from and moving it there would store an effect in state. Only `pendingExport` and `pendingCsv` in `iosApp/JustChill/More/` hold one; a new holder needs this rule amended first.
  - `rg -nP '@State\b(?![^\n]*\bpending(Export|Csv)\b)[^\n]*(Effect|Ready|BackupDocument|SharedCsv)' iosApp/JustChill`
  - `rg -n '@State[^\n]*(Effect|Ready|BackupDocument|SharedCsv)' iosApp/JustChill --glob '!**/More/**'`
- **Copy is resolved in the View.** The ViewModel emits a typed value (an enum case, an id, an amount); the View turns it into Spanish text. Text that arrives already written from Kotlin comes from `:core:presentation`: the money and date formatters, and the error copy an effect such as `ShowError(message:)` or `ShowMessage(text:)` carries, built with `toUserMessage()`. Swift renders it as-is and never re-maps it; an effect carrying a `DomainException` renders through the kit's `toUserMessage()`, never a Swift-side map.

Koin, routes, module boundaries and SQLDelight are dropped: Swift binds nothing, `AppShell` owns navigation, and the kit is the one dependency beyond Apple's frameworks.

## Theme tokens

`Theme/` (#527) is the iOS design system: `EmmColors`, `EmmType` applied with `.emmTextStyle(_:)`, `EmmSpacing`, `EmmRadii`, and `EmmCategory` for a category's colour and symbol. A screen styles only through them, never with a raw literal: a colour, a font, a size, a spacing or a radius. A value the tokens lack is added to `Theme/` as a token, mirrored from `:core:ui`'s, never written inline.

- `rg -n 'Color\((red|white|hex|argb|\.sRGB)|Font\.custom|\.system\(size|UIFont\(' iosApp/JustChill --glob '!**/Theme/**'`
- `rg -n '\.(padding|frame|cornerRadius|lineSpacing|tracking|opacity|offset)\([^)]*\b[0-9]' iosApp/JustChill --glob '!**/Theme/**'`
- `rg -n '\.font\(' iosApp/JustChill --glob '!**/Theme/**'`: a system font, `.body` as much as `.system(size:)`.
- `rg -n '\.(red|orange|yellow|green|mint|teal|cyan|blue|indigo|purple|pink|brown|white|gray|black|primary|secondary|tertiary)\b' iosApp/JustChill --glob '!**/Theme/**'`: a named colour or hierarchical style.

A category's colour or symbol goes through `EmmCategory.resolvedColor(_:)` / `resolvedSymbol(_:)`, the one map, as `CategoryResolve.kt` is on Android.

## Visual rules

Read `ui-components.md` before building a screen, and apply its sections as written: `### Principles` (the hero number, negative space, hierarchy through type and tone, monochrome negatives, hairline over surface), `### Colour` and `### Accessibility floor`. In SwiftUI, TalkBack there means VoiceOver (`accessibilityLabel`, `accessibilityHidden(true)` for a decorative graphic), font scale means Dynamic Type, which `EmmType` already scales through its `relativeTo` text style, and `ANIMATOR_DURATION_SCALE` means Reduce Motion (`@Environment(\.accessibilityReduceMotion)`).

The falsifiers above are the only mechanical check; swift-format never sees a token. Everything else here is review-enforced, as it is for Kotlin.
