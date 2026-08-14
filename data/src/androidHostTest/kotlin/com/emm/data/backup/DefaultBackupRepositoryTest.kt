package com.emm.data.backup

import com.emm.data.EmmDatabaseData
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountType
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.recurring.Frequency
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultBackupRepositoryTest {

    private val transactionRepo = mockk<TransactionRepository>()
    private val categoryRepo = mockk<CategoryRepository>()
    private val accountRepo = mockk<AccountRepository>()
    private val recurringRepo = mockk<RecurringMovementRepository> {
        every { allLive() } returns flowOf(emptyList())
    }
    private val db = mockk<EmmDatabaseData>(relaxed = true)

    // Export takes its `exportedAt` from the caller, so nothing in this suite reads the clock —
    // but it is stated rather than left to the machine all the same. What an import stamps is
    // asserted in DefaultBackupRepositoryImportTest, which has a real database to read it back from.
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-11T15:04:05Z")
    }

    private val repository =
        DefaultBackupRepository(transactionRepo, categoryRepo, accountRepo, recurringRepo, db, clock)

    private val account = Account(
        accountId = AccountId("acc-1"),
        name = "Yape",
        type = AccountType.Cash,
    )

    private val categoryIncome = Category(
        categoryId = CategoryId("cat-1"),
        name = "Sueldo",
        icon = "work",
        color = "#00FF00",
        categoryType = CategoryType.Income,
    )

    private val categorySpend = Category(
        categoryId = CategoryId("cat-2"),
        name = "Comida",
        icon = "food",
        color = "#FF0000",
        categoryType = CategoryType.Spend,
    )

    private val transactionWithCategory = Transaction(
        transactionId = TransactionId("tx-1"),
        type = TransactionType.Income,
        amount = Money(4500_00L),
        description = "Sueldo mayo",
        occurredAt = LocalDateTime(2026, 5, 23, 9, 33, 20),
        accountId = AccountId("acc-1"),
        categoryId = CategoryId("cat-1"),
    )

    private val transactionNullCategory = Transaction(
        transactionId = TransactionId("tx-2"),
        type = TransactionType.Spend,
        amount = Money(150_00L),
        description = "Almuerzo",
        occurredAt = LocalDateTime(2026, 5, 24, 13, 20, 0),
        accountId = AccountId("acc-1"),
        categoryId = null,
    )

    /** A template created in June 2026 and never settled. */
    private val neverConfirmedTemplate = RecurringMovement(
        id = RecurringMovementId("rec-1"),
        name = "Alquiler",
        type = TransactionType.Spend,
        amount = Money(1200_00L),
        description = "Depa",
        categoryId = CategoryId("cat-2"),
        accountId = AccountId("acc-1"),
        frequency = Frequency.Monthly,
        dayOfMonth = 5,
        isActive = true,
        lastConfirmedPeriod = null,
        createdAt = TEMPLATE_CREATED,
    )

    @Test
    fun `produces JSON containing all accounts, categories and transactions`() = runTest {
        every { accountRepo.all() } returns flowOf(listOf(account))
        every { categoryRepo.all() } returns flowOf(listOf(categoryIncome, categorySpend))
        every { transactionRepo.all() } returns flowOf(listOf(transactionWithCategory, transactionNullCategory))

        val json = repository.exportToJson(exportedAt = 1_748_000_000_000L, appVersion = "1.0.0")

        val payload = Json.decodeFromString<ExportPayloadDto>(json)
        // The literal, deliberately, and not BACKUP_SCHEMA_VERSION: asserting the constant against
        // itself only says `encodeDefaults` works. This number is the one written into files other
        // versions of the app have to read, so bumping it must cost a deliberate edit here.
        assertEquals(3, payload.schemaVersion)
        assertEquals(1_748_000_000_000L, payload.exportedAt)
        assertEquals("1.0.0", payload.appVersion)
        assertEquals(1, payload.accounts.size)
        assertEquals("acc-1", payload.accounts[0].accountId)
        assertEquals("Yape", payload.accounts[0].name)
        assertEquals(2, payload.categories.size)
        assertEquals(2, payload.transactions.size)
        assertEquals(4500_00L, payload.transactions[0].amountCents)
        assertEquals(150_00L, payload.transactions[1].amountCents)
    }

    @Test
    fun `a categoryId whose category is gone is exported as null`() = runTest {
        // Deleting a category no longer nulls the column on its movements, so a live transaction
        // can point at a tombstoned category. Tombstoned categories are not exported, so leaving
        // the id in the file would produce a backup that fails its own import on the FK.
        val orphan = transactionWithCategory.copy(
            transactionId = TransactionId("tx-3"),
            categoryId = CategoryId("cat-deleted"),
        )
        every { accountRepo.all() } returns flowOf(listOf(account))
        every { categoryRepo.all() } returns flowOf(listOf(categoryIncome))
        every { transactionRepo.all() } returns flowOf(listOf(transactionWithCategory, orphan))

        val json = repository.exportToJson(exportedAt = 0L, appVersion = "1.0.0")

        val payload = Json.decodeFromString<ExportPayloadDto>(json)
        val exportedIds = payload.categories.map { it.categoryId }
        assertEquals(listOf("cat-1"), exportedIds)
        assertEquals("cat-1", payload.transactions.single { it.transactionId == "tx-1" }.categoryId)
        assertNull(payload.transactions.single { it.transactionId == "tx-3" }.categoryId)
    }

    @Test
    fun `empty state - produces valid JSON with the current schemaVersion and empty arrays`() = runTest {
        every { accountRepo.all() } returns flowOf(emptyList())
        every { categoryRepo.all() } returns flowOf(emptyList())
        every { transactionRepo.all() } returns flowOf(emptyList())

        val json = repository.exportToJson(exportedAt = 0L, appVersion = "1.0.0")

        val payload = Json.decodeFromString<ExportPayloadDto>(json)
        assertEquals(3, payload.schemaVersion)
        assertTrue(payload.accounts.isEmpty())
        assertTrue(payload.categories.isEmpty())
        assertTrue(payload.transactions.isEmpty())
        assertTrue(payload.recurringMovements.isEmpty())
    }

    @Test
    fun `JSON output is pretty-printed`() = runTest {
        every { accountRepo.all() } returns flowOf(emptyList())
        every { categoryRepo.all() } returns flowOf(emptyList())
        every { transactionRepo.all() } returns flowOf(emptyList())

        val json = repository.exportToJson(exportedAt = 0L, appVersion = "1.0.0")

        assertTrue(json.contains('\n'))
    }

    // ── recurring movements: what version 3 added ─────────────────────────────
    //
    // This suite is the EXPORT half and only that: `db` is a mock here, so nothing below can observe
    // a restore. The two data-loss guards that used to sit in this section — `lastConfirmedPeriod`
    // and `createdAt`, the two fields that lose data in opposite directions — moved to
    // `DefaultBackupRepositoryImportTest` when the restore mapper landed. They run the full round
    // trip there, against a real database, which is the only place the claim their names make can
    // actually be held.

    /**
     * A paused template is data the user still owns, so the export reads every LIVE row — not every
     * ACTIVE one.
     *
     * `RecurringMovementRepository` already had two all-row reads when this landed and neither fit:
     * `allActive()` filters `isActive = 1`, which would drop this test's second template on the
     * floor, and `allWithDetails()` is a joined model carrying three columns the format does not
     * want. The read this asserts through is the third one, added for the export.
     */
    @Test
    fun `the export carries every live template, the paused ones included`() = runTest {
        val paused = neverConfirmedTemplate.copy(
            id = RecurringMovementId("rec-2"),
            name = "Gimnasio",
            isActive = false,
        )
        every { accountRepo.all() } returns flowOf(listOf(account))
        every { categoryRepo.all() } returns flowOf(listOf(categoryIncome, categorySpend))
        every { transactionRepo.all() } returns flowOf(emptyList())
        every { recurringRepo.allLive() } returns flowOf(listOf(neverConfirmedTemplate, paused))

        val payload = exportedPayload()

        assertEquals(listOf("rec-1", "rec-2"), payload.recurringMovements.map { it.recurringMovementId })
        assertEquals(listOf(true, false), payload.recurringMovements.map { it.isActive })
        // The rest of the shape, once: a template is twelve columns and a file that carries the id
        // and loses the amount is not a backup of anything.
        val exported = payload.recurringMovements.first()
        assertEquals("Alquiler", exported.name)
        assertEquals("Spend", exported.type)
        assertEquals(1200_00L, exported.amountCents)
        assertEquals("Depa", exported.description)
        assertEquals("cat-2", exported.categoryId)
        assertEquals("acc-1", exported.accountId)
        assertEquals("Monthly", exported.frequency)
        assertEquals(5, exported.dayOfMonth)
    }

    @Test
    fun `a template with no fixed amount exports a null amount rather than a zero`() = runTest {
        // The column is nullable and the two readings are different money: "no amount agreed yet"
        // is not "an alquiler of S/ 0.00", and a restore that turns one into the other writes a
        // number the user never entered.
        stubEmptyLedger()
        every { recurringRepo.allLive() } returns flowOf(listOf(neverConfirmedTemplate.copy(amount = null)))

        assertNull(exportedPayload().recurringMovements.single().amountCents)
    }

    /**
     * The two fields that are behaviour rather than storage reach the file at all.
     *
     * Shape only, and deliberately so: what each of them costs when a restore loses it — a re-minted
     * month, or every owed month swallowed — is held by the round-trip guards in
     * `DefaultBackupRepositoryImportTest`, which can run the restored values back through the real
     * `pendingPeriods` rule. A `createdAt` this suite could assert against the export alone is a
     * `createdAt` the import is still free to overwrite.
     */
    @Test
    fun `the export carries a template's own settled mark and createdAt`() = runTest {
        val settledThroughJuly = neverConfirmedTemplate.copy(lastConfirmedPeriod = "2026-07")
        stubEmptyLedger()
        every { recurringRepo.allLive() } returns flowOf(listOf(settledThroughJuly))

        val exported = exportedPayload(exportedAt = EXPORTED_AT).recurringMovements.single()

        assertEquals("2026-07", exported.lastConfirmedPeriod)
        // The export's own instant is August; the template's is June, and it is the template's that
        // has to travel.
        assertEquals(TEMPLATE_CREATED, exported.createdAt)
    }

    @Test
    fun `a template whose category is gone is exported with no category`() = runTest {
        // Same hazard as the transaction above, through the same composite key: `recurring_movements`
        // carries (categoryId, type) -> categories(categoryId, categoryType). Exporting an id whose
        // category is tombstoned — and therefore not in the file — writes a backup that fails its
        // own import on the FK.
        every { accountRepo.all() } returns flowOf(listOf(account))
        every { categoryRepo.all() } returns flowOf(listOf(categoryIncome))
        every { transactionRepo.all() } returns flowOf(emptyList())
        every { recurringRepo.allLive() } returns flowOf(
            listOf(
                neverConfirmedTemplate.copy(categoryId = CategoryId("cat-deleted")),
                neverConfirmedTemplate.copy(id = RecurringMovementId("rec-2"), categoryId = CategoryId("cat-1")),
            ),
        )

        val exported = exportedPayload().recurringMovements

        assertNull(exported.single { it.recurringMovementId == "rec-1" }.categoryId)
        // ...and the live one keeps its label: a scrub that stripped every id would also pass above.
        assertEquals("cat-1", exported.single { it.recurringMovementId == "rec-2" }.categoryId)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun stubEmptyLedger() {
        every { accountRepo.all() } returns flowOf(emptyList())
        every { categoryRepo.all() } returns flowOf(emptyList())
        every { transactionRepo.all() } returns flowOf(emptyList())
    }

    private suspend fun exportedPayload(exportedAt: Long = 0L): ExportPayloadDto =
        Json.decodeFromString(repository.exportToJson(exportedAt = exportedAt, appVersion = "1.0.0"))

    private companion object {
        /** 2026-06-10 — the template's own instant, and two months before the export's. */
        val TEMPLATE_CREATED: Long = Instant.parse("2026-06-10T12:00:00Z").toEpochMilliseconds()

        /**
         * August, deliberately — it is the value a `createdAt = now` restore would stamp, so a file
         * that lost the field would still look plausible while owing nothing before this month.
         */
        val EXPORTED_AT: Long = Instant.parse("2026-08-11T15:04:05Z").toEpochMilliseconds()
    }
}
