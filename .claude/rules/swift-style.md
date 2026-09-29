---
paths:
  - "iosApp/**"
---

# Swift style rules

The Kotlin rules' philosophy in Swift idiom, never transliterated. How each Kotlin rule reaches `iosApp/`:

| Kotlin rule | In Swift |
|---|---|
| `kotlin-style.md` | Adapted below: comments carry over, types and complexity adapt |
| `naming.md` | Adapted below: the Swift API Design Guidelines replace official Kotlin |
| `principles.md` | Carries over unchanged; it loads on `iosApp/` too |
| `architecture.md` | Only the MVI contract, adapted in `swift-screens.md`; Koin, routes, modules and SQLDelight dropped |
| `ui-components.md` | Adapted in `swift-screens.md` |
| `sqldelight.md`, `github-workflows.md` | Dropped |

## Comments

`kotlin-style.md`'s `## Comments` carries over whole: write none, with its three exceptions (why a non-obvious constraint exists, a warning of consequences, an external reference), each one to three lines at the declaration it constrains. The Guidelines' "Write a documentation comment for every declaration" is not adopted: the app has no public API, and `principles.md` rejects comprehensive KDoc for the same reason. The code explains itself or it gets renamed.

## Types

- A stored property and every non-`private` declaration carry their type: `private(set) var state: State`, `static let s4: CGFloat = 16`, a function's return type.
- A local `let` / `var` infers. The Swift Programming Language, The Basics: "It's rare that you need to write type annotations in practice." Annotate one only when inference would pick the wrong type or hide it from the reader.
- `let` over `var`; `var` only when reassignment is the point.

## Complexity

No tool measures it in Swift; review does, the way `kotlin-style.md`'s `### Compose sizing` does for Compose. A View is measured by decomposition, not length: one file carrying the store, the layout and N sub-views fails review. Split sub-views into sibling files in the screen's folder. Deep nesting or an `if` / `else if` chain in a `body` becomes a named computed property, a sub-view or a `switch`.

## Naming

The [Swift API Design Guidelines](https://www.swift.org/documentation/api-design-guidelines/) replace official Kotlin; Uncle Bob's table in `naming.md` still applies.

- Clarity at the point of use first; omit needless words, keep every word that removes ambiguity.
- Name a variable or parameter by its role, never its type: `onClose`, `selectedTab`, not `closure`, `tabEnum`.
- A function with side effects reads as an imperative verb (`send(_:)`, `logAvailability()`); one without reads as a noun phrase (`resolvedColor(_:)`).
- A Boolean reads as an assertion about its receiver: `isCapturePresented`, `isTabular`.
- Types and protocols `UpperCamelCase`, everything else `lowerCamelCase`, an acronym uniformly cased (`init(argb:)`, `sRGB`).
- A name the kit exports keeps its Kotlin spelling (`supabaseUrl`, `doInitKoin`); rename in Swift only what Swift declares.
- A screen folder's root type is `<Feature>Screen` (`AccountsScreen`); `naming.md`'s layer table is dropped.
- `naming.md`'s `## Language` carries over: English identifiers, Spanish only in user-facing values, addressing the reader as tú.

## swift-format

`iosApp/.swift-format` is the lint `scripts/justchill-ci` runs: `xcrun swift-format lint --strict -r iosApp`. Fix a finding with `xcrun swift-format format -i -r iosApp/JustChill`, or change the rule in that file; there is no baseline. It departs from swift-format's defaults in four keys:

- `indentation.spaces` 4 and `lineLength` 120: Xcode's indent and the Kotlin line width.
- `GroupNumericLiterals` off: an ARGB literal reads by byte pair (`0xFF6FA876`), as its Kotlin twin does.
- `NoAccessLevelOnExtensionDeclaration` off: `private extension` is the file-local helper idiom the theme and the app entry use.

It checks layout and `/* */` (`NoBlockComments`), never a `//` comment, a type annotation, complexity or a token; those stay review-enforced.
