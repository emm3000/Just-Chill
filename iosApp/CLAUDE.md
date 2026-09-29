# iosApp — CLAUDE.md

The SwiftUI app over `JustChillKit` (ADR 024 Decisions 7 and 8). Bundle id `com.emm.justchill.ios`, iOS 17.0, iPhone only, no signing identity. Swift, UI copy in Spanish addressing the user as tú.

## Build

- `xcodebuild -project iosApp/JustChill.xcodeproj -scheme JustChill -destination 'generic/platform=iOS Simulator' build` from the repo root. `scripts/justchill-ci` runs it after the Gradle gate; no workflow does, since ubuntu has no Xcode and skips the framework link.
- The `Compile Kotlin Framework` phase runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode` before Swift compiles; the framework lands in `shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`. It is static, so the target links `-lsqlite3` for SQLDelight's native driver.
- `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64`: `:shared` builds only `iosSimulatorArm64`, and a generic simulator destination asks for `x86_64` too.
- `ENABLE_USER_SCRIPT_SANDBOXING = NO`, or the sandbox blocks Gradle from the repo.
- `INFOPLIST_FILE = Info.plist` points at `iosApp/Info.plist`, outside the synchronized folder, and Xcode merges it with the generated `INFOPLIST_KEY_*` keys; inside `JustChill/` it is copied as a resource and the build fails on "Multiple commands produce Info.plist" (#527).
- Swift 6 language mode. `@preconcurrency import JustChillKit` in every file that imports the kit: Kotlin classes are not `Sendable`.
- SKIE runs at link time, so a green `compileKotlinIos*` proves nothing about the Swift API; grep `shared/build/bin/iosSimulatorArm64/debugFramework/JustChillKit.framework/Headers/JustChillKit.h` for a type before using it. `initKoin` reaches Swift with a `do` prefix, since ObjC reserves `init`.

## Launch

- `JustChillApp.init` starts Koin once with a `KitConfig` whose Supabase fields are blank, `isSnapshotBackupEnabled` false and `appVersion` from `CFBundleShortVersionString` (`MARKETING_VERSION`), so the app runs offline (PRD §2).

## Simulator

- Pool of two, `justchill-ios27` and `justchill-ios27-b` (iPhone 17, the one iOS 27 runtime), one device per peer (ADR 024 `## Amendments (2026-09-29)`, `docs/agents/multi-session.md` `## Isolation: emulators`). Never a second runtime, never `simctl clone`.
- Every `KotlinNativeSimulatorTest` targets `justchill-ios27` with `standalone = false` (`build-logic`'s `KmpLibraryConventionPlugin`) and never boots, shuts down or creates one; it never installs the app, so it shares the device with a peer safely. Never shut down, erase or rename `justchill-ios27` while a peer or a test uses it.
- The dispatch boots each peer's device and names it: `xcrun simctl boot <device>`. Every later call names that device, never `booted`, which is ambiguous with two booted.
- Smoke on `<device>`: `xcrun simctl install <device> <DerivedData>/Build/Products/Debug-iphonesimulator/JustChill.app`, then `xcrun simctl launch --console-pty <device> com.emm.justchill.ios`, then `xcrun simctl io <device> screenshot <file>.png`. `xcodebuild ... -showBuildSettings | rg BUILT_PRODUCTS_DIR` prints the directory. Run install and launch in sequence, never backgrounded together, or the shot lands on the home screen. A shot right after a cold boot can come out black; relaunch and shoot again.

## Layout

`JustChill/` is a synchronized folder (`PBXFileSystemSynchronizedRootGroup`): adding a Swift file never touches `project.pbxproj`.

- `JustChillApp.swift` — the entry and Koin start. `AppShell.swift` — the `TabView`: Movimientos, Reporte, the add action, Cuentas, Más (ADR 022, `AppBottomBar.kt`); the add action opens `CaptureScreen` full screen instead of selecting a tab.
- One folder per screen family, one root type each: `Movements/`, `Report/`, `Accounts/`, `More/`, `Capture/`. A screen ticket works inside its own folder and never shares a file with another.
- Every new screen family gets its own top-level folder under `JustChill/` (`Categories/`, `Loans/`, `Auth/`, `Onboarding/`); code more than one screen uses goes in `Bridge/` or `Theme/` (#527), never inside a screen folder.
- A tab item's spoken name goes on its `Label`'s `Text` (`Text("Anotar").accessibilityLabel("Anotar movimiento")`); a modifier on the tab's content view never reaches the tab bar.

## The store

- `Bridge/MviStore.swift` is the one way a screen drives a ViewModel: `MviStore(resolveAccountsHandle())`, over the handle `:shared` hands out (`shared/CLAUDE.md`, the ViewModel handle). It is `@MainActor @Observable`: `state` is observed, `send(_:)` forwards to `onIntent`, and `onEffect(_:)` sets the one closure every effect goes to, exactly once each. Effects wait in the ViewModel's channel until the first `onEffect` call; a later call swaps the closure and keeps the one collector.
- The store clears its handle in `deinit`, which cancels `viewModelScope`; SwiftUI never calls `onCleared()`. Whoever holds the store holds the ViewModel.
- One construction pattern: the screen holds `@State private var store: MviStore<...>?` and builds it in `.task` only while it is `nil`. Never build it in `init` or a `@State` initializer: SwiftUI evaluates those again on every parent redraw, and each throwaway store builds and clears a ViewModel.
- The first store resolved opens and migrates the database synchronously (#548). Never resolve one inside `body`; until the database is warmed off the main thread, the first screen pays that cost on the main thread.
- A screen never collects a Kotlin flow itself: `rg -n 'for await' iosApp/JustChill --glob '!**/Bridge/**'` prints nothing.
