package com.emm.data.backup

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.data.recurring.asEntity
import com.emm.data.recurring.asExternalModelOrNull
import com.emm.data.transaction.asEntity
import com.emm.data.transaction.asExternalModel
import com.emm.domain.account.AccountType
import com.emm.domain.recurring.Frequency
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.pendingPeriods
import com.emm.domain.recurring.periodKey
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.backup.ImportStats
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class DefaultBackupRepositoryImportTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: EmmDatabaseData

    private lateinit var repository: DefaultBackupRepository

    private class TickingClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant.also { instant += 1.milliseconds }
    }

    private val clock = TickingClock(FIRST_IMPORT)

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        exec("PRAGMA foreign_keys=ON")
        clock.instant = FIRST_IMPORT
        repository = DefaultBackupRepository(db = db, clock = clock)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `valid payload returns correct ImportStats`() = runTest {
        val json = buildPayloadJson(accounts = 1, categories = 2, transactions = 3)

        val stats: ImportStats = repository.importFromJson(json)

        assertEquals(
            ImportStats(accounts = 1, categories = 2, transactions = 3, recurring = 0, loans = 0, loanPayments = 0),
            stats,
        )
    }

    @Test
    fun `valid payload inserts all rows into the DB`() = runTest {
        val json = buildPayloadJson(accounts = 2, categories = 3, transactions = 4)

        repository.importFromJson(json)

        val accountCount = db.accountsQueries.all().executeAsList().size
        val categoryCount = db.categoriesQueries.all().executeAsList().size
        val txCount = db.transactionsQueries.all().executeAsList().size
        assertEquals(2, accountCount)
        assertEquals(3, categoryCount)
        assertEquals(4, txCount)
    }

    @Test
    fun `importing twice leaves only the rows of the second backup live`() = runTest {
        val firstJson = buildPayloadJson(accounts = 2, categories = 3, transactions = 4)
        val secondJson = buildPayloadJson(accounts = 1, categories = 1, transactions = 1)

        repository.importFromJson(firstJson)
        repository.importFromJson(secondJson)

        val accountCount = db.accountsQueries.all().executeAsList().size
        val categoryCount = db.categoriesQueries.all().executeAsList().size
        val txCount = db.transactionsQueries.all().executeAsList().size
        assertEquals(1, accountCount)
        assertEquals(1, categoryCount)
        assertEquals(1, txCount)
    }

    @Test
    fun `empty payload succeeds with ImportStats zeros and wipes the DB`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        val stats = repository.importFromJson(EMPTY_PAYLOAD_JSON)

        assertEquals(
            ImportStats(accounts = 0, categories = 0, transactions = 0, recurring = 0, loans = 0, loanPayments = 0),
            stats,
        )
        assertTrue(db.accountsQueries.all().executeAsList().isEmpty())
        assertTrue(db.categoriesQueries.all().executeAsList().isEmpty())
        assertTrue(db.transactionsQueries.all().executeAsList().isEmpty())
    }

    @Test
    fun `corrupt JSON throws ValidationError and DB is untouched`() = runTest {
        val beforeAccounts = db.accountsQueries.all().executeAsList().size

        assertFailsWith<DomainException.ValidationError> {
            repository.importFromJson("{ not valid json at all")
        }

        assertEquals(beforeAccounts, db.accountsQueries.all().executeAsList().size)
    }

    @Test
    fun `wrong schemaVersion throws ValidationError and DB is untouched`() = runTest {
        val json = """{"schemaVersion":99,"exportedAt":0,"appVersion":"1.0.0",""" +
            """"accounts":[],"categories":[],"transactions":[]}"""
        val beforeAccounts = db.accountsQueries.all().executeAsList().size

        assertFailsWith<DomainException.ValidationError> {
            repository.importFromJson(json)
        }

        assertEquals(beforeAccounts, db.accountsQueries.all().executeAsList().size)
    }

    @Test
    fun `a movement the app cannot read is not restored, and the balance still matches the ledger`() = runTest {
        val json = payloadWithTransactions(
            """{"transactionId":"tx-ok","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":null}""",
            """{"transactionId":"tx-broken","type":"Income","amountCents":777700,"description":"Roto",""" +
                """"occurredAt":"2026-8-1 12:00","accountId":"acc-1","categoryId":null}""",
        )

        repository.importFromJson(json)

        assertEquals(0, rawCount("SELECT COUNT(*) FROM transactions WHERE transactionId = 'tx-broken'"))
        val balance = db.transactionsQueries.liveTotals().executeAsOne().balance
        val visible = db.transactionsQueries.all().executeAsList().asEntity().asExternalModel()
        assertEquals(visible.sumOf { it.amount.cents }, balance)
        assertEquals(10_000L, balance)
    }

    @Test
    fun `the reported count is what landed, not what the file held`() = runTest {
        val json = payloadWithTransactions(
            """{"transactionId":"tx-ok","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":null}""",
            """{"transactionId":"tx-broken","type":"Income","amountCents":7777,"description":"Roto",""" +
                """"occurredAt":"not a date at all","accountId":"acc-1","categoryId":null}""",
        )

        val stats = repository.importFromJson(json)

        assertEquals(1, stats.transactions)
    }

    @Test
    fun `a movement with a type the app does not know is dropped rather than failing the import`() = runTest {
        val json = payloadWithTransactions(
            """{"transactionId":"tx-ok","type":"Spend","amountCents":5000,"description":"Mercado",""" +
                """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":null}""",
            """{"transactionId":"tx-weird","type":"Transfer","amountCents":5000,"description":"?",""" +
                """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":null}""",
        )

        val stats = repository.importFromJson(json)

        assertEquals(1, stats.transactions)
        assertEquals(1, db.transactionsQueries.all().executeAsList().size)
    }

    @Test
    fun `a movement written without seconds restores in the canonical shape`() = runTest {
        val json = payloadWithTransactions(
            """{"transactionId":"tx-short","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                """"occurredAt":"2026-05-23T09:33","accountId":"acc-1","categoryId":null}""",
        )

        repository.importFromJson(json)

        assertEquals("2026-05-23T09:33:00", db.transactionsQueries.find("tx-short").executeAsOne().occurredAt)
    }

    @Test
    fun `a movement filed under a category of the other type restores uncategorized`() = runTest {
        val json = payloadWith(
            categoriesJson = listOf(
                """{"categoryId":"cat-spend","name":"Café","icon":"i","color":"c","categoryType":"Spend"}""",
            ),
            transactionsJson = listOf(
                """{"transactionId":"tx-mismatch","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                    """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":"cat-spend"}""",
            ),
        )

        val stats = repository.importFromJson(json)

        assertEquals(1, stats.transactions)
        val tx = db.transactionsQueries.find("tx-mismatch").executeAsOne()
        assertNull(tx.categoryId)
        assertEquals("Income", tx.type)
        assertEquals(10_000L, tx.amount)
    }

    @Test
    fun `one mismatched movement does not cost the rest of the file`() = runTest {
        val json = payloadWith(
            categoriesJson = listOf(
                """{"categoryId":"cat-income","name":"Sueldo","icon":"i","color":"c","categoryType":"Income"}""",
            ),
            transactionsJson = listOf(
                """{"transactionId":"tx-mismatch","type":"Spend","amountCents":500,"description":"Café",""" +
                    """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":"cat-income"}""",
                """{"transactionId":"tx-ok","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                    """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":"cat-income"}""",
            ),
        )

        val stats = repository.importFromJson(json)

        assertEquals(2, stats.transactions)
        assertNull(db.transactionsQueries.find("tx-mismatch").executeAsOne().categoryId)
        assertEquals("cat-income", db.transactionsQueries.find("tx-ok").executeAsOne().categoryId)
    }

    @Test
    fun `a movement pointing at a category the file never carried restores uncategorized`() = runTest {
        val json = payloadWith(
            categoriesJson = emptyList(),
            transactionsJson = listOf(
                """{"transactionId":"tx-orphan","type":"Spend","amountCents":500,"description":"Café",""" +
                    """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":"cat-gone"}""",
            ),
        )

        val stats = repository.importFromJson(json)

        assertEquals(1, stats.transactions)
        assertNull(db.transactionsQueries.find("tx-orphan").executeAsOne().categoryId)
    }

    @Test
    fun `a backup that redefines a category's type does not abort on the movements filed under it`() = runTest {
        repository.importFromJson(
            payloadWith(
                categoriesJson = listOf(
                    """{"categoryId":"cat-1","name":"Bar","icon":"i","color":"c","categoryType":"Spend"}""",
                ),
                transactionsJson = emptyList(),
            ),
        )
        exec(
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, " +
                "accountId, dayOfMonth, createdAt, updatedAt) " +
                "VALUES ('rec-1', 'Alquiler', 'Spend', 5000, '', 'cat-1', 'acc-1', 5, 1, 1)",
        )

        val stats = repository.importFromJson(
            payloadWith(
                categoriesJson = listOf(
                    """{"categoryId":"cat-1","name":"Sueldo","icon":"i","color":"c","categoryType":"Income"}""",
                ),
                transactionsJson = listOf(
                    """{"transactionId":"tx-1","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                        """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":"cat-1"}""",
                ),
            ),
        )

        assertEquals(1, stats.transactions, "the import must complete, not roll back")
        assertEquals("Income", db.categoriesQueries.find("cat-1").executeAsOne().categoryType)
        assertEquals("cat-1", db.transactionsQueries.find("tx-1").executeAsOne().categoryId)
        assertEquals(1, rawCount("SELECT COUNT(*) FROM recurring_movements WHERE id = 'rec-1'"))
        assertEquals(
            1,
            rawCount("SELECT COUNT(*) FROM recurring_movements WHERE id = 'rec-1' AND categoryId IS NULL"),
        )
    }

    @Test
    fun `a file with no schemaVersion reads as version 1 rather than as an unreadable version`() = runTest {
        val json = """{"exportedAt":0,"appVersion":"2.4.0","accounts":[""" +
            """{"accountId":"acc-1","name":"BCP","type":"Bank","currency":"PEN"}],"categories":[],""" +
            """"transactions":[{"transactionId":"tx-1","type":"Income","amountCents":450000,""" +
            """"description":"Sueldo","date":1748000000000,"accountId":"acc-1","categoryId":null}]}"""

        val stats = repository.importFromJson(json)

        assertEquals(1, stats.transactions)
        assertEquals("2025-05-23T06:33:20", db.transactionsQueries.find("tx-1").executeAsOne().occurredAt)
    }

    @Test
    fun `a file whose root is not an object is reported as corrupt, not as a raw crash`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            repository.importFromJson("""[{"schemaVersion":2}]""")
        }

        assertEquals(ValidationCode.BackupFileInvalid, ex.code)
    }

    @Test
    fun `a shape mismatch on decode is reported as an invalid file, not as SerializationError`() = runTest {
        val json = """{"schemaVersion":3,"exportedAt":0,"appVersion":"1.0.0","accounts":"not-a-list",""" +
            """"categories":[],"transactions":[],"recurringMovements":[]}"""

        val ex = assertFailsWith<DomainException.ValidationError> {
            repository.importFromJson(json)
        }

        assertEquals(ValidationCode.BackupFileInvalid, ex.code)
    }

    @Test
    fun `a schemaVersion that is not a number is reported as corrupt, not as an unsupported version`() = runTest {
        listOf("""{"schemaVersion":{}}""", """{"schemaVersion":[]}""", """{"schemaVersion":"dos"}""").forEach { root ->
            val ex = assertFailsWith<DomainException.ValidationError>("root was $root") {
                repository.importFromJson(root)
            }
            assertEquals(ValidationCode.BackupFileInvalid, ex.code, "root was $root")
        }
    }

    @Test
    fun `an import reads the clock once and stamps every restored row with it`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        val expected = FIRST_IMPORT.toEpochMilliseconds()
        assertEquals(expected, rawStamp("SELECT createdAt FROM accounts WHERE accountId = 'acc-1'"))
        assertEquals(expected, rawStamp("SELECT updatedAt FROM accounts WHERE accountId = 'acc-1'"))
        assertEquals(expected, rawStamp("SELECT createdAt FROM categories WHERE categoryId = 'cat-1'"))
        assertEquals(expected, rawStamp("SELECT updatedAt FROM categories WHERE categoryId = 'cat-1'"))
        assertEquals(expected, rawStamp("SELECT createdAt FROM transactions WHERE transactionId = 'tx-1'"))
        assertEquals(expected, rawStamp("SELECT updatedAt FROM transactions WHERE transactionId = 'tx-1'"))
    }

    @Test
    fun `a second import stamps its own instant, and the tombstone it leaves shares it`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 2, categories = 1, transactions = 1))
        clock.instant = SECOND_IMPORT

        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        val second = SECOND_IMPORT.toEpochMilliseconds()
        assertEquals(second, rawStamp("SELECT deletedAt FROM accounts WHERE accountId = 'acc-2'"))
        assertEquals(second, rawStamp("SELECT updatedAt FROM accounts WHERE accountId = 'acc-2'"))
        assertEquals(second, rawStamp("SELECT updatedAt FROM accounts WHERE accountId = 'acc-1'"))
        assertNull(rawStamp("SELECT deletedAt FROM accounts WHERE accountId = 'acc-1'"))
        assertEquals(
            FIRST_IMPORT.toEpochMilliseconds(),
            rawStamp("SELECT createdAt FROM accounts WHERE accountId = 'acc-1'"),
        )
    }

    @Test
    fun `rows missing from the backup are tombstoned so the deletion can sync`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 2, categories = 2, transactions = 2))

        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        assertEquals(2, rawCount("SELECT COUNT(*) FROM accounts"))
        assertEquals(
            1,
            rawCount("SELECT COUNT(*) FROM accounts WHERE accountId = 'acc-2' AND deletedAt IS NOT NULL"),
        )
        assertEquals(
            1,
            rawCount("SELECT COUNT(*) FROM transactions WHERE transactionId = 'tx-2' AND syncState = 'Pending'"),
        )
    }

    @Test
    fun `a row that comes back in a later backup is resurrected`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))
        repository.importFromJson(EMPTY_PAYLOAD_JSON)
        assertEquals(1, rawCount("SELECT COUNT(*) FROM accounts WHERE deletedAt IS NOT NULL"))

        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        assertEquals(0, rawCount("SELECT COUNT(*) FROM accounts WHERE deletedAt IS NOT NULL"))
        assertEquals(1, db.accountsQueries.all().executeAsList().size)
        assertEquals(1, db.transactionsQueries.all().executeAsList().size)
    }

    @Test
    fun `import does not break when a recurring movement references an account`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))
        exec(
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, " +
                "accountId, dayOfMonth, createdAt, updatedAt) " +
                "VALUES ('rec-1', 'Alquiler', 'Spend', 5000, '', 'cat-1', 'acc-1', 5, 1, 1)",
        )

        repository.importFromJson(EMPTY_PAYLOAD_JSON)

        assertEquals(1, rawCount("SELECT COUNT(*) FROM recurring_movements WHERE id = 'rec-1'"))
    }

    @Test
    fun `an already claimed row keeps its userId through an import`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))
        exec("UPDATE accounts SET userId = 'user-1', syncState = 'Synced'")

        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        assertEquals(1, rawCount("SELECT COUNT(*) FROM accounts WHERE userId = 'user-1'"))
        assertEquals(1, rawCount("SELECT COUNT(*) FROM accounts WHERE syncState = 'Pending'"))
    }

    @Test
    fun `a restored template is re-queued for push and keeps the userId it was claimed under`() = runTest {
        val json = payloadWith(
            categoriesJson = emptyList(),
            transactionsJson = emptyList(),
            recurringJson = listOf(template(id = "rec-1")),
        )
        repository.importFromJson(json)
        exec("UPDATE recurring_movements SET userId = 'user-1', syncState = 'Synced'")

        repository.importFromJson(json)

        assertEquals(
            1,
            rawCount("SELECT COUNT(*) FROM recurring_movements WHERE id = 'rec-1' AND userId = 'user-1'"),
        )
        assertEquals(
            1,
            rawCount("SELECT COUNT(*) FROM recurring_movements WHERE id = 'rec-1' AND syncState = 'Pending'"),
        )
    }

    @Test
    fun `a template with a type the app does not know is dropped rather than restored invisibly`() = runTest {
        val json = payloadWith(
            categoriesJson = emptyList(),
            transactionsJson = emptyList(),
            recurringJson = listOf(
                template(id = "rec-ok", type = "Spend"),
                template(id = "rec-weird", type = "Transfer"),
            ),
        )

        val stats = repository.importFromJson(json)

        assertEquals(0, rawCount("SELECT COUNT(*) FROM recurring_movements WHERE id = 'rec-weird'"))
        assertEquals(1, rawCount("SELECT COUNT(*) FROM recurring_movements WHERE id = 'rec-ok'"))
        assertEquals(1L, db.recurring_movementsQueries.countLiveByAccount("acc-1").executeAsOne())
        assertEquals(1, stats.recurring)
    }

    @Test
    fun `a template whose settled mark is malformed restores intact with the mark cleared`() = runTest {
        val json = payloadWith(
            categoriesJson = listOf(
                """{"categoryId":"cat-1","name":"Casa","icon":"i","color":"c","categoryType":"Spend"}""",
            ),
            transactionsJson = emptyList(),
            recurringJson = listOf(
                template(
                    id = "rec-1",
                    categoryId = "cat-1",
                    lastConfirmedPeriod = "julio",
                    dayOfMonth = 17,
                    isActive = false,
                    amountCents = 999_900,
                    createdAt = 1_780_555_000_000,
                ),
            ),
        )

        repository.importFromJson(json)

        val restored = db.recurring_movementsQueries.find("rec-1").executeAsOne()
        assertNull(restored.lastConfirmedPeriod)
        assertEquals("Alquiler", restored.name)
        assertEquals("Spend", restored.type)
        assertEquals(999_900L, restored.amount)
        assertEquals("Depa", restored.description)
        assertEquals("cat-1", restored.categoryId)
        assertEquals("acc-1", restored.accountId)
        assertEquals("Monthly", restored.frequency)
        assertEquals(17L, restored.dayOfMonth)
        assertEquals(0L, restored.isActive)
        assertEquals(1_780_555_000_000L, restored.createdAt)
    }

    @Test
    fun `a well-formed but out-of-range settled mark restores the template intact with the mark cleared`() = runTest {
        val json = payloadWith(
            categoriesJson = listOf(
                """{"categoryId":"cat-1","name":"Casa","icon":"i","color":"c","categoryType":"Spend"}""",
            ),
            transactionsJson = emptyList(),
            recurringJson = listOf(
                template(
                    id = "rec-1",
                    categoryId = "cat-1",
                    lastConfirmedPeriod = "12345-07",
                    dayOfMonth = 22,
                    isActive = false,
                    amountCents = 555_500,
                    createdAt = 1_780_666_000_000,
                ),
            ),
        )

        repository.importFromJson(json)

        val restored = db.recurring_movementsQueries.find("rec-1").executeAsOne()
        assertNull(restored.lastConfirmedPeriod)
        assertEquals("Alquiler", restored.name)
        assertEquals("Spend", restored.type)
        assertEquals(555_500L, restored.amount)
        assertEquals("Depa", restored.description)
        assertEquals("cat-1", restored.categoryId)
        assertEquals("acc-1", restored.accountId)
        assertEquals("Monthly", restored.frequency)
        assertEquals(22L, restored.dayOfMonth)
        assertEquals(0L, restored.isActive)
        assertEquals(1_780_666_000_000L, restored.createdAt)
    }

    @Test
    fun `a settled mark whose year was padded to four characters restores with no mark`() = runTest {
        val json = payloadWith(
            categoriesJson = emptyList(),
            transactionsJson = emptyList(),
            recurringJson = listOf(template(id = "rec-1", lastConfirmedPeriod = "0999-07")),
        )

        repository.importFromJson(json)

        val restored = db.recurring_movementsQueries.find("rec-1").executeAsOne()
        assertNull(restored.lastConfirmedPeriod)
        assertEquals("Alquiler", restored.name)
    }

    @Test
    fun `a settled mark the parser accepts is re-encoded into the shape the comparison needs`() = runTest {
        val json = payloadWith(
            categoriesJson = emptyList(),
            transactionsJson = emptyList(),
            recurringJson = listOf(template(id = "rec-1", lastConfirmedPeriod = "2026-7")),
        )

        repository.importFromJson(json)

        assertEquals("2026-07", db.recurring_movementsQueries.find("rec-1").executeAsOne().lastConfirmedPeriod)
    }

    @Test
    fun `a template keeps the category the FILE gives it, not the one the device still held`() = runTest {
        repository.importFromJson(
            payloadWith(
                categoriesJson = listOf(
                    """{"categoryId":"cat-1","name":"Bar","icon":"i","color":"c","categoryType":"Spend"}""",
                ),
                transactionsJson = emptyList(),
            ),
        )

        repository.importFromJson(
            payloadWith(
                categoriesJson = listOf(
                    """{"categoryId":"cat-1","name":"Sueldo","icon":"i","color":"c","categoryType":"Income"}""",
                ),
                transactionsJson = emptyList(),
                recurringJson = listOf(template(id = "rec-1", type = "Income", categoryId = "cat-1")),
            ),
        )

        assertEquals("cat-1", db.recurring_movementsQueries.find("rec-1").executeAsOne().categoryId)
    }

    @Test
    fun `a template filed under a category of the other type restores uncategorized`() = runTest {
        val json = payloadWith(
            categoriesJson = listOf(
                """{"categoryId":"cat-spend","name":"Café","icon":"i","color":"c","categoryType":"Spend"}""",
            ),
            transactionsJson = emptyList(),
            recurringJson = listOf(template(id = "rec-1", type = "Income", categoryId = "cat-spend")),
        )

        repository.importFromJson(json)

        val restored = db.recurring_movementsQueries.find("rec-1").executeAsOne()
        assertNull(restored.categoryId)
        assertEquals("Income", restored.type)
    }

    @Test
    fun `a restored template keeps the mark it was settled through, so the app does not re-mint it`() = runTest {
        val settledThroughJuly = NEVER_CONFIRMED.copy(lastConfirmedPeriod = "2026-07")

        val restored = assertNotNull(roundTrip(settledThroughJuly))

        assertEquals("2026-07", restored.lastConfirmedPeriod)
        assertEquals(listOf("2026-08"), owedPeriodsOf(restored))
    }

    @Test
    fun `a restored template keeps its own createdAt, so the months it still owes survive`() = runTest {
        val restored = assertNotNull(roundTrip(NEVER_CONFIRMED))

        assertEquals(TEMPLATE_CREATED, restored.createdAt)
        assertEquals(listOf("2026-06", "2026-07", "2026-08"), owedPeriodsOf(restored))
    }

    private suspend fun roundTrip(template: RecurringMovement): RecurringMovement? {
        db.accountsQueries.insert(
            accountId = template.accountId.value,
            name = "Cuenta",
            type = AccountType.Cash.name,
            currency = "PEN",
            updatedAt = 0L,
            createdAt = 0L,
        )
        db.recurring_movementsQueries.insert(
            id = template.id.value,
            name = template.name,
            type = template.type.name,
            amount = template.amount?.cents,
            description = template.description,
            categoryId = template.categoryId?.value,
            accountId = template.accountId.value,
            frequency = template.frequency.name,
            dayOfMonth = template.dayOfMonth.toLong(),
            isActive = if (template.isActive) 1L else 0L,
            lastConfirmedPeriod = template.lastConfirmedPeriod,
            createdAt = template.createdAt,
            updatedAt = 0L,
        )

        repository.importFromJson(repository.exportToJson(exportedAt = 0L, appVersion = "1.0.0"))

        return db.recurring_movementsQueries.find(template.id.value)
            .executeAsOneOrNull()
            ?.asEntity()
            ?.asExternalModelOrNull()
    }

    private fun owedPeriodsOf(template: RecurringMovement): List<String> =
        pendingPeriods(template, TODAY, TimeZone.UTC).map(::periodKey)

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun rawCount(sql: String): Long = rawStamp(sql) ?: 0L

    private fun rawStamp(sql: String): Long? = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0))
        },
        parameters = 0,
    ).value

    private fun buildPayloadJson(accounts: Int, categories: Int, transactions: Int): String {
        val accountsJson = (1..accounts).joinToString(",") { i ->
            """{"accountId":"acc-$i","name":"Cuenta $i","type":"Cash","currency":"PEN"}"""
        }
        val categoriesJson = (1..categories).joinToString(",") { i ->
            """{"categoryId":"cat-$i","name":"Cat $i","icon":"icon","color":"#000","categoryType":"Spend"}"""
        }
        val transactionsJson = (1..transactions).joinToString(",") { i ->
            """{"transactionId":"tx-$i","type":"Spend","amountCents":1000,""" +
                """"description":"Tx $i","occurredAt":"2026-05-23T09:33:20",""" +
                """"accountId":"acc-1","categoryId":null}"""
        }
        return """
            {
                "schemaVersion": 3,
                "exportedAt": 0,
                "appVersion": "1.0.0",
                "accounts": [$accountsJson],
                "categories": [$categoriesJson],
                "transactions": [$transactionsJson],
                "recurringMovements": []
            }
        """.trimIndent()
    }

    private fun payloadWithTransactions(vararg transactionsJson: String): String =
        payloadWith(categoriesJson = emptyList(), transactionsJson = transactionsJson.toList())

    private fun payloadWith(
        categoriesJson: List<String>,
        transactionsJson: List<String>,
        recurringJson: List<String> = emptyList(),
    ): String = """
        {
            "schemaVersion": 3,
            "exportedAt": 0,
            "appVersion": "1.0.0",
            "accounts": [{"accountId":"acc-1","name":"Cuenta","type":"Cash","currency":"PEN"}],
            "categories": [${categoriesJson.joinToString(",")}],
            "transactions": [${transactionsJson.joinToString(",")}],
            "recurringMovements": [${recurringJson.joinToString(",")}]
        }
    """.trimIndent()

    private fun template(
        id: String,
        type: String = "Spend",
        categoryId: String? = null,
        lastConfirmedPeriod: String? = null,
        dayOfMonth: Int = 5,
        isActive: Boolean = true,
        amountCents: Long = 120000,
        createdAt: Long = 1780000000000,
    ): String = """
        {"recurringMovementId":"$id","name":"Alquiler","type":"$type","amountCents":$amountCents,
         "description":"Depa","categoryId":${categoryId?.let { "\"$it\"" }},"accountId":"acc-1",
         "frequency":"Monthly","dayOfMonth":$dayOfMonth,"isActive":$isActive,
         "lastConfirmedPeriod":${lastConfirmedPeriod?.let { "\"$it\"" }},"createdAt":$createdAt}
    """.trimIndent()

    private companion object {
        const val EMPTY_PAYLOAD_JSON = """{"schemaVersion":3,"exportedAt":0,"appVersion":"1.0.0",""" +
            """"accounts":[],"categories":[],"transactions":[],"recurringMovements":[]}"""

        val FIRST_IMPORT: Instant = Instant.parse("2026-08-11T15:04:05Z")
        val SECOND_IMPORT: Instant = Instant.parse("2026-08-11T16:04:05Z")

        val TEMPLATE_CREATED: Long = Instant.parse("2026-06-10T12:00:00Z").toEpochMilliseconds()

        val TODAY = LocalDate(2026, 8, 13)

        val NEVER_CONFIRMED = RecurringMovement(
            id = RecurringMovementId("rec-1"),
            name = "Alquiler",
            type = TransactionType.Spend,
            amount = Money(1200_00L),
            description = "Depa",
            categoryId = null,
            accountId = AccountId("acc-1"),
            frequency = Frequency.Monthly,
            dayOfMonth = 5,
            isActive = true,
            lastConfirmedPeriod = null,
            createdAt = TEMPLATE_CREATED,
        )
    }
}
