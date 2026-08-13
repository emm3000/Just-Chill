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
 * ### Why it lives here
 *
 * `:presentation` is where this project's DI contracts live (see [SupabaseConfig] next door), and
 * it is the one module both `:androidApp` and `:ui-android` can see — `ui-android/build.gradle.kts`
 * exposes it with `api(project(":presentation"))`. commonMain is compose-free, and a holder over a
 * `String` carries no Compose, `java.*` or `android.*`.
 *
 * ### It is NOT on the Swift surface, and that is the point of the value class
 *
 * Unlike [SupabaseConfig], which appears in the generated `JustChillKit.h` as `JCKSupabaseConfig`,
 * this type appears nowhere in it: Kotlin/Native does not export `@JvmInline value class` to
 * Obj-C, and no exported declaration references it. Checked by grepping the header after
 * `linkDebugFrameworkIosSimulatorArm64` — zero occurrences of `CommitHash`.
 *
 * Deliberate. No Swift screen shows the commit, so a `data class` here would only add a symbol to
 * `JustChillKit` that nothing on that side calls. If iOS ever needs this — a binding in
 * `KoinIos.kt`, a footer in SwiftUI — Swift will not be able to see the type, and that is the
 * moment to make it a `data class`, not a bug to debug.
 */
@JvmInline
value class CommitHash(val value: String)
