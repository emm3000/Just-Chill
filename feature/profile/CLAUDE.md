# :feature:profile — CLAUDE.md

The fifth bottom-bar tab, "Más" (ADR 022): a corner-icon-free `JcTopBar`, then four eyebrow-grouped sections — `Cuenta` (signed in only), `Registro` (Categorías, Recurrentes, Préstamos), `Datos` (Exportar mi data, Importar respaldo, then sign-in or the backup rows) and `App` (Acerca de, Privacidad) — and the commit hash row. Movimientos, Reporte del mes and Cuentas left this screen for their own tabs when ADR 022 landed. `privacy/` holds the privacy policy screen and its route. `ProfileViewModel` with its `UiState` / `Intent` / `Effect`, both routes, `profileEntries` and the copy mappers live in `com.emm.justchill.feature.profile`.

One flat package plus `privacy/`: the backup rows are fields of `ProfileUiState` and their copy mappers read its types, so a `backup/` sub-package would import the root and be imported back. The privacy screen shares nothing with them and sits on its own.

`id("justchill.android.feature")` plus `kotlinx-coroutines-core`, `kotlinx-datetime`, `androidx-lifecycle-runtime-compose` and `androidx-material-icons-extended` (the cloud, shield and file glyphs). Depends on `:core:ui` and `:core:domain` and nothing else; `checkModuleBoundaries` fails the gate on any other edge. The atoms, the tokens and `toUserMessage` are `:core:ui`'s: consume them, never copy them here.

`./gradlew :feature:profile:testDebugUnitTest`. `MainDispatcherRule` and `FakeTodayFlow` come from `:core:testing`. `ProfileViewModelBackupFailureTest` stayed in `:androidApp`'s test set under this package: it wires a real `BackupOrchestrator`, an `:androidApp` class a feature module may not see.

## Koin and the graph

`profileModule` is declared here and binds `ProfileViewModel`, nothing else; the DSL builds its constructor by hand, so every dependency is listed, `clock` included. `:androidApp`'s `wiring/ProfileWiring.kt` includes it and binds one use case of its own, `ExportTransactionsCsvUseCase`; every other port this feature injects is app-level backup, auth, category or recurring vocabulary already bound in `backupModule`, `dataModule`, `authWiring` and `recurringWiring`. `ProfileViewModel` is listed in `AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS`.

## Routes and cross-feature navigation

`ProfileRoute` is a `BottomBarRoute`, the `Más` tab (ADR 022) and the only way in; `PrivacyPolicyRoute` is another, pushed from this screen, the one door into `privacy/`. Both are in `profileRoutes`, which `RouteSerializationTest` concatenates. Categories, recurring movements, loans, the manifesto and sign-in all leave this feature, so each arrives as an `(AppNavigator) -> Unit` callback supplied by `AppEntryGraph.kt`, never as a route value. Every destination row takes `pushToTop`, never `push`, which silently does nothing on a buried route. `pushToTop` cannot strand the user here, because the list owns index 0 and this tab index 1, so the deepest a row can truncate to is `[Movimientos, Más]`. `rememberPlatformHostActions` stays in `:androidApp`'s `shell/`: the SAF launchers must be registered at the nav host root, or a picker result arriving after its entry left composition is dropped.

## Backup

- `BackupAvailability` (`:core:domain`) reaches `ProfileViewModel` injected and lands once in `ProfileUiState.isCloudBackupAvailable`: `dev` true, `prod` false. `BackupSection` reads it in one `if/else` that keeps the sign-in row and the local-only note exclusive; never split it into two reads. Turning it on for `prod` is a disclosure change first — see `androidApp/CLAUDE.md` `## Backup` and ADR 009.
- A backup failure never signs out and reaches the UI only as `ProfileEffect.Notify`, never `ShowError`, which would read as expired credentials. `verifyBackup` follows the same rule.
- `BackupRowUi` ranks `NeedsAccount` > `DisclosurePending` > `BackingUp`: the orchestrator raises `isBackingUp` for the whole cycle, including one about to refuse, so ranking `BackingUp` higher would flash "Respaldando…" over a device uploading nothing. `DisclosurePending` is `Warning`, never `Danger`. A failure annotates the snapshot (`Failed` carries a `LastSnapshot`); warn on the count, never on the reason.
- `severity()` and `toMetaText()` stay out of composables (`ProfileViewModelBackupRowTest` pins them). `:androidApp`'s `disclosureIsPending` and `resolveBackupRow` answer the same question and must agree. The badge that drew the first one died with the tab strip (ADR 017); `BackupDisclosureSignal` stays bound and tested, with no screen reading it, until a follow-up re-homes it on the Más tab or removes it.
- The acknowledgement is written regardless of the running op so it is never lost; only the follow-on cycle is gated.
- Staleness is read through `GetBackupStalenessUseCase` inside a broad catch on purpose: a frozen Perfil is the alternative, and the row falls back to an undated snapshot rather than claiming a health it could not determine.
- `ExportHistory` (`:core:domain`) is the export watermark seam; its Settings-backed `LocalExportHistory` is `:androidApp`'s `core/backup/`. A saved export records itself only when the SAF write succeeded.
- Restoring replaces everything, so `ImportConfirmationDialog` gates it and the json arrives through the host's `pendingImportJson` accessor, read as a `() -> T` because a `NavEntry.content` closure is cached until the back stack changes.

## Screens

- The privacy policy screen uses `FilledCta` and `ProfileEntries`' `ImportConfirmationDialog`, `DeleteAccountDialog` and `ImportBackupDialog` all use `EmmDialog` (`.claude/rules/ui-components.md`); the atoms pass on the first landed in #202, on the latter two in #214.
- The screen is layout only: `ProfileScreen.kt` composes `AccountSection.kt`, `DestinationsSection.kt`, `BackupSection.kt`, `AppSection.kt` and `CommitFooter.kt`, one sibling file per section.
- `commitHashUi()` is the pattern for pure UI logic: a plain function beside the screen with a test here, never logic inside a composable.
