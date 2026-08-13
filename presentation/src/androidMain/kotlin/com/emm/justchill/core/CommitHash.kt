package com.emm.justchill.core

import kotlin.jvm.JvmInline

/**
 * The full 40-char sha the running build was compiled from, as a type rather than a named `String`.
 *
 * Produced by `androidPlatformModule` (`:androidApp`) from the generated `BuildInfo`, consumed by
 * `AppNavHost` (`:ui-android`), which hands it to the profile footer. The value may also be
 * `GenerateBuildInfoTask.UNKNOWN_COMMIT` — the word the generator writes when git cannot answer —
 * so this carries whatever the build produced, unvalidated; `commitHashUi()` in `:ui-android` is
 * what classifies it.
 *
 * ### Why a type and not `named("commitHash")`
 *
 * The producer and the consumer live in different Gradle modules, so a qualifier made the contract
 * a string that both sides had to spell the same way. A shared constant survived a rename but not a
 * retype: a fresh literal at either site compiled, passed the whole gate, and threw
 * `NoDefinitionFoundException` at launch. Koin resolves this by `KClass`, so there is no longer a
 * string either side can misspell. That is the whole of it — see `AppNavHost`'s injection site for
 * what this does NOT stop.
 *
 * ### Why it lives here, and in androidMain
 *
 * `:presentation` is where this project's DI contracts live — [SupabaseConfig] is the one next
 * door. `:ui-android` sees it through `api(project(":presentation"))`, and `:androidApp` sees it
 * through `:ui-android`.
 *
 * `androidMain` rather than `commonMain` because both sides of the contract are Android-only:
 * `androidPlatformModule` (`:androidApp`) produces it, `AppNavHost` (`:ui-android`) consumes it,
 * and no `iosMain` code — `KoinIos.kt` included — mentions it. Keeping it out of `commonMain`
 * keeps it out of the Kotlin/Native compilation and out of the `JustChillKit` export. If iOS ever
 * needs a commit footer, moving this back to `commonMain` is the first step, and it would then
 * have to stop being a `@JvmInline value class`: Kotlin/Native does not export those to Obj-C.
 */
@JvmInline
value class CommitHash(val value: String)
