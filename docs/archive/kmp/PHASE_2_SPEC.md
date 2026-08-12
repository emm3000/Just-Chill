# Phase 2 — `:data` → KMP — Execution Spec

> **Archived.** This contract was executed: `:data` is Kotlin Multiplatform on trunk and
> `data/src/commonMain/` exists. Every `git mv` and build-file body below has already landed,
> so nothing here is actionable. Kept for the reasoning. Live KMP doc:
> `docs/kmp/ORCHESTRATION.md`; current state: `docs/PROGRESS.md`.

> Contract for converting `:data` to Kotlin Multiplatform. Android MUST stay green;
> iOS target MUST compile. Companion to `docs/archive/kmp/MIGRATION_PLAN.md`.
> Branch: `kmp/phase-0-scaffolding` (continuation). Language: English. No Co-Authored-By.

## Surface audit (complete — verified 2026-06-12)

ALL Android/JVM coupling in `data/src/main`, nothing else:
- **D1** `Providers.kt` — `Context`, `AndroidSqliteDriver`, `SupportSQLiteDatabase`, `BuildConfig.LIBRARY_PACKAGE_NAME`.
- **D2** `SyncCursorUtils.kt`, `BaseTableSync.kt`, `DefaultSyncRepository.kt` — `java.time.Instant` / `OffsetDateTime`.
- **D3** `DefaultBackupRepository.kt:58` — `System.currentTimeMillis()`.
- **D4** `SafeCall.kt` (`SQLiteException`) + `AccountTableSync/CategoryTableSync/TransactionTableSync/RecurringMovementTableSync.kt` (`SQLiteConstraintException`).
- **D5** Ktor engine `ktor-client-okhttp` → needs `darwin` for iOS.
- **D6** Dead deps: `androidx.core.ktx`, `androidx.appcompat`, `com.google.android.material`.
- **D7** `Dispatchers.IO` — used in ~15 files (every `LocalDataSource` + `TableSync` + auth). JVM-only; absent in commonMain.

Two `expect/actual` surfaces only: **D4** (sqlite exceptions) and **D7** (io dispatcher). NOTHING ELSE gets expect/actual. Do not invent more.

## Source layout moves (git mv, preserve history)
- `data/src/main/kotlin` → `data/src/commonMain/kotlin`
- `data/src/main/sqldelight` → `data/src/commonMain/sqldelight` (includes `databases/3.db`)
- `data/src/test/kotlin` → `data/src/androidHostTest/kotlin` (JVM unit tests, MockK)
- `data/src/androidTest/kotlin` → `data/src/androidDeviceTest/kotlin` (instrumented; if the androidLibrary DSL rejects this source-set name, REPORT — do not block the Android assemble gate, instrumented tests need a device and are not in the gate)
- Empty `java/` sibling dirs: remove if empty.

After moves, the D1 and D4 android-coupled files get split/edited (below) so `commonMain` has ZERO android imports.

## data/build.gradle.kts (exact)
```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("app.cash.sqldelight") version "2.3.2"
    kotlin("plugin.serialization") version libs.versions.kotlinVersion
}

kotlin {
    androidLibrary {
        namespace = "com.emm.data"
        compileSdk = 37
        minSdk = 26
        withHostTest { }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(libs.coroutines.extensions)
            api(platform(libs.supabase.bom))
            api(libs.supabase.auth.kt)
            api(libs.supabase.postgrest.kt)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.android.driver)
            api(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            api(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.mockk)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.junit)
        }
    }
}

sqldelight {
    databases {
        create("EmmDatabaseData") {
            packageName.set("com.emm.data")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}
```
Dead deps D6 are simply absent above — that is the removal.

## D7 — io dispatcher expect/actual

`commonMain/kotlin/com/emm/data/shared/Dispatchers.kt`:
```kotlin
package com.emm.data.shared

import kotlinx.coroutines.CoroutineDispatcher

expect val ioDispatcher: CoroutineDispatcher
```
`androidMain/kotlin/com/emm/data/shared/Dispatchers.android.kt`:
```kotlin
package com.emm.data.shared

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
```
`iosMain/kotlin/com/emm/data/shared/Dispatchers.ios.kt`:
```kotlin
package com.emm.data.shared

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// iOS has no dedicated IO dispatcher. Default is the standard substitute.
// TODO(phase6): verify SQLDelight native-driver threading needs under load.
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
```
MECHANICAL SWAP: in every `data/src/commonMain` file using `Dispatchers.IO`, replace `Dispatchers.IO` → `ioDispatcher` and fix imports (remove `import kotlinx.coroutines.Dispatchers` if now unused; add `import com.emm.data.shared.ioDispatcher`). Do NOT touch `Dispatchers.Main`/`Dispatchers.Default` if any.

## D4 — sqlite exception classifier expect/actual

`commonMain/kotlin/com/emm/data/shared/SqliteExceptions.kt`:
```kotlin
package com.emm.data.shared

/** True if [this] is a SQLite constraint violation (e.g. FK/unique). */
expect fun Throwable.isSqliteConstraintViolation(): Boolean

/** True if [this] is any SQLite-layer failure. */
expect fun Throwable.isSqliteException(): Boolean
```
`androidMain/kotlin/com/emm/data/shared/SqliteExceptions.android.kt`:
```kotlin
package com.emm.data.shared

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException

actual fun Throwable.isSqliteConstraintViolation(): Boolean = this is SQLiteConstraintException

actual fun Throwable.isSqliteException(): Boolean = this is SQLiteException
```
`iosMain/kotlin/com/emm/data/shared/SqliteExceptions.ios.kt`:
```kotlin
package com.emm.data.shared

// Best-effort: the SQLDelight native driver surfaces SQLite errors with the engine's
// message text. Matched by message to avoid importing an uncertain native exception FQN.
// TODO(phase6): verify against the real co.touchlab.sqliter exception on an iOS device.
actual fun Throwable.isSqliteConstraintViolation(): Boolean =
    message?.contains("constraint", ignoreCase = true) == true

actual fun Throwable.isSqliteException(): Boolean =
    message?.contains("sqlite", ignoreCase = true) == true ||
        (this::class.simpleName?.contains("SQL", ignoreCase = true) == true)
```

`SafeCall.kt` (rewrite — remove `import android.database.sqlite.SQLiteException`):
```kotlin
package com.emm.data.shared

import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

@Suppress("TooGenericExceptionCaught")
suspend fun <T> safeDbCall(block: suspend () -> T): T = try {
    block()
} catch (e: DomainException) {
    throw e
} catch (e: Exception) {
    if (e.isSqliteException()) throw DomainException.DatabaseError(e)
    throw DomainException.Unknown(e)
}

fun <T> Flow<T>.catchAsDomainException(): Flow<T> = catch { e ->
    when {
        e is DomainException -> throw e
        e.isSqliteException() -> throw DomainException.DatabaseError(e)
        else -> throw DomainException.Unknown(e)
    }
}
```
In the 4 `*TableSync.kt`: remove `import android.database.sqlite.SQLiteConstraintException`; change the `catch (e: SQLiteConstraintException) { ... false }` to:
```kotlin
} catch (e: Exception) {
    if (e.isSqliteConstraintViolation()) false else throw e
}
```
and add `import com.emm.data.shared.isSqliteConstraintViolation`. Keep the `@Suppress` annotations.

## D1 — Providers split

DELETE `commonMain/.../Providers.kt`. Create two files:

`androidMain/kotlin/com/emm/data/DatabaseDriver.android.kt`:
```kotlin
package com.emm.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

// Filename MUST stay exactly "com.emm.data.db". It was previously derived from
// BuildConfig.LIBRARY_PACKAGE_NAME (= namespace "com.emm.data"). Changing it orphans
// every existing user's local database.
private const val DATABASE_NAME = "com.emm.data.db"

fun provideSqlDriver(context: Context): SqlDriver = AndroidSqliteDriver(
    schema = EmmDatabaseData.Schema,
    context = context,
    name = DATABASE_NAME,
    callback = csm(),
)

fun csm() = object : AndroidSqliteDriver.Callback(schema = EmmDatabaseData.Schema) {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedDefaultCategories(db)
    }
}

private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
    // KEEP the exact INSERT block from the original Providers.kt, verbatim.
    db.execSQL(
        """ <COPY THE ORIGINAL MULTI-ROW INSERT EXACTLY> """.trimIndent(),
    )
}
```
`commonMain/kotlin/com/emm/data/DatabaseProviders.kt`:
```kotlin
package com.emm.data

import app.cash.sqldelight.db.SqlDriver

fun provideDb(sqlDriver: SqlDriver): EmmDatabaseData = EmmDatabaseData(sqlDriver)

fun provideTransactionQueries(db: EmmDatabaseData): TransactionsQueries = db.transactionsQueries

fun provideRecurringMovementQueries(db: EmmDatabaseData): Recurring_movementsQueries = db.recurring_movementsQueries
```
The iOS driver factory is NOT created in Phase 2 (deferred to Phase 5 when iOS is wired). iOS compiles without it because nothing in commonMain references an expect driver.

## D2 — java.time → kotlinx-datetime

`SyncCursorUtils.kt` (rewrite):
```kotlin
package com.emm.data.sync

import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

internal fun parseServerInstant(value: String): Instant = Instant.parse(value)

internal fun overlapCursor(cursor: String?): String? =
    cursor?.let { (parseServerInstant(it) - OVERLAP_SECONDS.seconds).toString() }

private const val OVERLAP_SECONDS = 10L
```
`BaseTableSync.kt`: change `import java.time.Instant` → `import kotlinx.datetime.Instant`; replace `inst.isAfter(maxInstant)` → `maxInstant == null || inst > maxInstant` (keep existing null logic; `kotlinx.datetime.Instant` is `Comparable`). `maxInstant?.toString()` stays (kotlinx Instant.toString() emits canonical 'Z' ISO-8601).
`DefaultSyncRepository.kt`: `import java.time.Instant` → `import kotlinx.datetime.Instant`; `Instant.parse(it)` works unchanged.

## D3 — System.currentTimeMillis
`DefaultBackupRepository.kt:58`: `System.currentTimeMillis()` → `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()` (add `import kotlinx.datetime.Clock`).

## Cursor round-trip test (MANDATORY — ADR-002 format must not drift)
`commonTest/kotlin/com/emm/data/sync/SyncCursorUtilsTest.kt`:
```kotlin
package com.emm.data.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncCursorUtilsTest {
    @Test fun overlap_shifts_back_10s_and_emits_canonical_Z() {
        assertEquals("2026-06-10T01:29:50Z", overlapCursor("2026-06-10T01:30:00Z"))
    }
    @Test fun overlap_normalises_offset_input_to_Z() {
        assertEquals("2026-06-10T01:29:50Z", overlapCursor("2026-06-10T01:30:00+00:00"))
    }
    @Test fun overlap_preserves_fractional_seconds() {
        assertEquals("2026-06-10T01:29:50.123Z", overlapCursor("2026-06-10T01:30:00.123Z"))
    }
    @Test fun null_cursor_returns_null() {
        assertNull(overlapCursor(null))
    }
}
```
If kotlinx-datetime emits a different fractional/precision format than asserted, ADJUST the expected strings to kotlinx's actual canonical output AND record the exact format in the commit message — but the offset→Z normalisation and the −10s shift MUST hold.

## Gates (all must pass; iterate)
1. `./gradlew assembleDevDebug` — Android app green (CRITICAL). :app still consumes data's android variant.
2. `./gradlew :data:testAndroidHostTest` — moved JVM unit tests pass.
3. `./gradlew :data:compileKotlinIosArm64` — iOS target compiles (the payoff).
4. `./gradlew :domain:test`-equivalent already green from Phase 1 — don't regress.

## Guardrails
- Do NOT add expect/actual beyond D4 and D7.
- Do NOT edit `:app` or `:domain` to force a pass. If they break, the data config is wrong — fix data.
- Do NOT change the DB filename, the seed SQL, or the cursor −10s/Z semantics.
- If a gate fails, diagnose and fix minimally; if blocked, REPORT back — do not invent structure.
