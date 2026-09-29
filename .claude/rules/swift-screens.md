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
- **A `body` maps, never decides.** It turns `store.state` into views and a user event into `store.send(...)` with an intent named for what the user did. A domain rule in Swift (a balance summed, a validity check, a date compared) is a missing field on the Kotlin `UiState`: add it there.
- **An effect is handled once, in the closure.** The `onEffect` closure navigates, dismisses or shows a transient message. An effect is never copied into `@State`, where the next redraw fires it again; what the screen keeps rendering (a sheet, a dialog) is a `UiState` field.
  - `rg -n '@State[^\n]*Effect' iosApp/JustChill`
- **Copy is resolved in the View.** The ViewModel emits a typed value (an enum case, an id, an amount); the View turns it into Spanish text. A string that arrives already written from Kotlin is `:core:presentation`'s formatters (money, dates), never a sentence a ViewModel wrote.

Koin, routes, module boundaries and SQLDelight are dropped: Swift binds nothing, `AppShell` owns navigation, and the kit is the one dependency beyond Apple's frameworks.

## Theme tokens

`Theme/` (#527) is the iOS design system: `EmmColors`, `EmmType` applied with `.emmTextStyle(_:)`, `EmmSpacing`, `EmmRadii`, and `EmmCategory` for a category's colour and symbol. A screen styles only through them, never with a raw literal: a colour, a font, a size, a spacing or a radius. A value the tokens lack is added to `Theme/` as a token, mirrored from `:core:ui`'s, never written inline.

- `rg -n 'Color\((red|white|hex|argb|\.sRGB)|Font\.custom|\.system\(size|UIFont\(' iosApp/JustChill --glob '!**/Theme/**'`
- `rg -n '\.(padding|frame|cornerRadius|lineSpacing|tracking|opacity|offset)\([^)]*\b[0-9]' iosApp/JustChill --glob '!**/Theme/**'`

A category's colour or symbol goes through `EmmCategory.resolvedColor(_:)` / `resolvedSymbol(_:)`, the one map, as `CategoryResolve.kt` is on Android.

## Visual rules

Read `ui-components.md` before building a screen, and apply its sections as written: `### Principles` (the hero number, negative space, hierarchy through type and tone, monochrome negatives, hairline over surface), `### Colour` and `### Accessibility floor`. In SwiftUI, TalkBack there means VoiceOver (`accessibilityLabel`, `accessibilityHidden(true)` for a decorative graphic), and font scale means Dynamic Type, which `EmmType` already scales through its `relativeTo` text style.

The falsifiers above are the only mechanical check; swift-format never sees a token. Everything else here is review-enforced, as it is for Kotlin.
