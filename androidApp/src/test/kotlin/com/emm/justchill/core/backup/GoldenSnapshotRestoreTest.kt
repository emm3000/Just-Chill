package com.emm.justchill.core.backup

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.justchill.core.database.JustChillDatabase
import com.emm.justchill.core.database.backup.SqlDelightSnapshotStore
import com.emm.justchill.core.domain.shared.backup.ImportStats
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

// The fixture is the byte-for-byte output of DefaultBackupRepository.exportToJson at 31b37217, the
// commit before :core:backup existed. A Snapshot on the owner's device was written by that code.
private const val TRUNK_SNAPSHOT: String = "/backup/snapshot-v4-trunk.json"

class GoldenSnapshotRestoreTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: JustChillDatabase
    private lateinit var repository: DefaultBackupRepository

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-14T00:00:00Z")
    }

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JustChillDatabase.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        db = JustChillDatabase(driver)
        repository = DefaultBackupRepository(SqlDelightSnapshotStore(db = db, clock = clock))
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `a snapshot written before the split restores every table`() = runTest {
        val stats: ImportStats = repository.importFromJson(trunkSnapshot())

        assertEquals(
            ImportStats(accounts = 1, categories = 1, transactions = 1, recurring = 1, loans = 1, loanPayments = 1),
            stats,
        )
    }

    @Test
    fun `restoring and exporting it again writes the same bytes`() = runTest {
        val original: String = trunkSnapshot()

        repository.importFromJson(original)

        assertEquals(original, repository.exportToJson(exportedAt = EXPORTED_AT, appVersion = APP_VERSION))
    }

    private fun trunkSnapshot(): String = checkNotNull(javaClass.getResource(TRUNK_SNAPSHOT)).readText()

    private companion object {
        const val EXPORTED_AT: Long = 1_755_000_000_000L
        const val APP_VERSION: String = "1.2.3"
    }
}
