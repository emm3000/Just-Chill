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
 * `NoDefinitionFoundException` at launch. Koin resolves this by `KClass`, so the two sides now
 * agree through the compiler's import resolution and there is no string left to spell.
 *
 * ### Why it lives here
 *
 * `:presentation` is where this project's DI contracts live (see [SupabaseConfig] next door), and
 * it is the one module both `:androidApp` and `:ui-android` can see —`ui-android/build.gradle.kts`
 * exposes it with `api(project(":presentation"))`. commonMain is compose-free and exported to iOS
 * as `JustChillKit`; a data holder over a `String` carries no Compose, `java.*` or `android.*`, so
 * the export stays clean. iOS binds nothing here: no Swift screen shows the commit today.
 */
@JvmInline
value class CommitHash(val value: String)
