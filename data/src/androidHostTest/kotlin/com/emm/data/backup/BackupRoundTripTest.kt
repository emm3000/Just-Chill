package com.emm.data.backup

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.Accounts
import com.emm.data.Categories
import com.emm.data.EmmDatabaseData
import com.emm.data.Recurring_movements
import com.emm.data.Transactions
import com.emm.domain.shared.backup.ImportStats
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class BackupRoundTripTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var repository: DefaultBackupRepository

    private val clock = object : Clock {
        override fun now(): Instant = IMPORTED_AT
    }

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        db = EmmDatabaseData(driver)
        repository = DefaultBackupRepository(db = db, clock = clock)
        seedFixture()
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `the file declares the current schema version and every live row`() = runTest {
        val payload = exportedPayload()

        assertEquals(BACKUP_SCHEMA_VERSION, payload.schemaVersion)
        assertEquals(EXPORTED_AT, payload.exportedAt)
        assertEquals(APP_VERSION, payload.appVersion)
        assertEquals(LIVE_ACCOUNTS, payload.accounts.size)
        assertEquals(LIVE_CATEGORIES, payload.categories.size)
        assertEquals(LIVE_TRANSACTIONS, payload.transactions.size)
        assertEquals(LIVE_RECURRING, payload.recurringMovements.size)
    }

    @Test
    fun `the restored database holds exactly the live rows the file carried`() = runTest {
        val stats = roundTrip()

        assertEquals(ImportStats(LIVE_ACCOUNTS, LIVE_CATEGORIES, LIVE_TRANSACTIONS, LIVE_RECURRING), stats)
        assertEquals(LIVE_ACCOUNTS.toLong(), totalRows("accounts"))
        assertEquals(LIVE_CATEGORIES.toLong(), totalRows("categories"))
        assertEquals(LIVE_TRANSACTIONS.toLong(), totalRows("transactions"))
        assertEquals(LIVE_RECURRING.toLong(), totalRows("recurring_movements"))
    }

    @Test
    fun `accounts survive accountId, name and type`() = runTest {
        roundTrip()

        val walletAccount = account("acc-wallet")
        assertEquals("acc-wallet", walletAccount.accountId)
        assertEquals("Yape", walletAccount.name)
        assertEquals("Wallet", walletAccount.type)
        val bankAccount = account("acc-bank")
        assertEquals("acc-bank", bankAccount.accountId)
        assertEquals("BCP", bankAccount.name)
        assertEquals("Bank", bankAccount.type)
    }

    @Test
    fun `accounts lose currency - the domain model carries none to write`() = runTest {
        roundTrip()

        assertEquals("PEN", account("acc-bank").currency)
    }

    @Test
    fun `an account type this build cannot name comes back as Bank, not dropped`() = runTest {
        roundTrip()

        assertEquals("Bank", account("acc-unknown-type").type)
    }

    @Test
    fun `categories survive categoryId, name, icon, color and categoryType`() = runTest {
        roundTrip()

        val incomeCategory = category("cat-income")
        assertEquals("cat-income", incomeCategory.categoryId)
        assertEquals("Sueldo", incomeCategory.name)
        assertEquals("work", incomeCategory.icon)
        assertEquals("#00FF00", incomeCategory.color)
        assertEquals("Income", incomeCategory.categoryType)
        val spendCategory = category("cat-spend")
        assertEquals("Comida", spendCategory.name)
        assertEquals("food", spendCategory.icon)
        assertEquals("#FF0000", spendCategory.color)
        assertEquals("Spend", spendCategory.categoryType)
    }

    @Test
    fun `categories lose isDefault - no DTO field carries it and restore writes 0`() = runTest {
        roundTrip()

        assertFalse(category("cat-income").isDefault)
    }

    @Test
    fun `transactions survive every field the file carries`() = runTest {
        roundTrip()

        val incomeTransaction = transaction("tx-income")
        assertEquals("tx-income", incomeTransaction.transactionId)
        assertEquals("Income", incomeTransaction.type)
        assertEquals(4500_00L, incomeTransaction.amount)
        assertEquals("Sueldo mayo", incomeTransaction.description)
        assertEquals("2026-05-23T09:33:20", incomeTransaction.occurredAt)
        assertEquals("cat-income", incomeTransaction.categoryId)
        assertEquals("acc-wallet", incomeTransaction.accountId)
        val spendTransaction = transaction("tx-spend")
        assertEquals("Spend", spendTransaction.type)
        assertEquals(150_00L, spendTransaction.amount)
        assertEquals("Menu del dia", spendTransaction.description)
        assertEquals("2026-05-24T13:20:00", spendTransaction.occurredAt)
        assertEquals("cat-spend", spendTransaction.categoryId)
        assertEquals("acc-bank", spendTransaction.accountId)
    }

    @Test
    fun `an uncategorized transaction comes back uncategorized, not dropped`() = runTest {
        roundTrip()

        val uncategorized = transaction("tx-uncategorized")
        assertEquals(42_50L, uncategorized.amount)
        assertEquals("", uncategorized.description)
        assertEquals("2026-05-25T08:00:00", uncategorized.occurredAt)
        assertNull(uncategorized.categoryId)
        assertEquals("acc-wallet", uncategorized.accountId)
    }

    @Test
    fun `templates survive every field the file carries`() = runTest {
        roundTrip()

        val activeTemplate = template("rec-active")
        assertEquals("rec-active", activeTemplate.id)
        assertEquals("Alquiler", activeTemplate.name)
        assertEquals("Spend", activeTemplate.type)
        assertEquals(1200_00L, activeTemplate.amount)
        assertEquals("Depa", activeTemplate.description)
        assertEquals("cat-spend", activeTemplate.categoryId)
        assertEquals("acc-bank", activeTemplate.accountId)
        assertEquals("Monthly", activeTemplate.frequency)
        assertEquals(5L, activeTemplate.dayOfMonth)
        assertEquals(1L, activeTemplate.isActive)
    }

    @Test
    fun `a paused template comes back paused, with its nulls intact`() = runTest {
        roundTrip()

        val pausedTemplate = template("rec-paused")
        assertEquals("Gimnasio", pausedTemplate.name)
        assertNull(pausedTemplate.amount)
        assertNull(pausedTemplate.categoryId)
        assertNull(pausedTemplate.lastConfirmedPeriod)
        assertEquals(20L, pausedTemplate.dayOfMonth)
        assertEquals(0L, pausedTemplate.isActive)
    }

    @Test
    fun `a template keeps its own createdAt and settled mark, unlike every other row`() = runTest {
        roundTrip()

        assertEquals(ACTIVE_TEMPLATE_CREATED, template("rec-active").createdAt)
        assertEquals("2026-07", template("rec-active").lastConfirmedPeriod)
        assertEquals(PAUSED_TEMPLATE_CREATED, template("rec-paused").createdAt)
    }

    @Test
    fun `every other row loses createdAt and updatedAt to the importing clock`() = runTest {
        roundTrip()

        assertEquals(IMPORT_STAMP, account("acc-wallet").createdAt)
        assertEquals(IMPORT_STAMP, account("acc-wallet").updatedAt)
        assertEquals(IMPORT_STAMP, category("cat-income").createdAt)
        assertEquals(IMPORT_STAMP, category("cat-income").updatedAt)
        assertEquals(IMPORT_STAMP, transaction("tx-income").createdAt)
        assertEquals(IMPORT_STAMP, transaction("tx-income").updatedAt)
        assertEquals(IMPORT_STAMP, template("rec-active").updatedAt)
    }

    @Test
    fun `a claimed row comes back unclaimed - the restore never writes userId`() = runTest {
        roundTrip()

        assertNull(account("acc-wallet").userId)
        assertNull(category("cat-income").userId)
        assertNull(transaction("tx-income").userId)
        assertNull(template("rec-active").userId)
    }

    @Test
    fun `every restored row comes back Pending - syncState is never read from the file`() = runTest {
        roundTrip()

        assertEquals("Pending", account("acc-wallet").syncState)
        assertEquals("Pending", category("cat-income").syncState)
        assertEquals("Pending", transaction("tx-income").syncState)
        assertEquals("Pending", template("rec-active").syncState)
    }

    @Test
    fun `no tombstoned row comes back, in any of the four tables`() = runTest {
        assertEquals(1L, rowsWithId("accounts", "accountId", "acc-dead"))
        assertEquals(1L, rowsWithId("categories", "categoryId", "cat-dead"))
        assertEquals(1L, rowsWithId("transactions", "transactionId", "tx-dead"))
        assertEquals(1L, rowsWithId("recurring_movements", "id", "rec-dead"))

        roundTrip()

        assertEquals(0L, rowsWithId("accounts", "accountId", "acc-dead"))
        assertEquals(0L, rowsWithId("categories", "categoryId", "cat-dead"))
        assertEquals(0L, rowsWithId("transactions", "transactionId", "tx-dead"))
        assertEquals(0L, rowsWithId("recurring_movements", "id", "rec-dead"))
    }

    private suspend fun roundTrip(): ImportStats {
        val json = repository.exportToJson(exportedAt = EXPORTED_AT, appVersion = APP_VERSION)
        wipePhysically()
        return repository.importFromJson(json)
    }

    private fun wipePhysically() {
        driver.execute(null, "DELETE FROM transactions", 0)
        db.recurring_movementsQueries.deleteAll()
        driver.execute(null, "DELETE FROM categories", 0)
        driver.execute(null, "DELETE FROM accounts", 0)
        assertEquals(0L, totalRows("transactions"))
        assertEquals(0L, totalRows("recurring_movements"))
        assertEquals(0L, totalRows("categories"))
        assertEquals(0L, totalRows("accounts"))
    }

    private suspend fun exportedPayload(): ExportPayloadDto =
        Json.decodeFromString(repository.exportToJson(exportedAt = EXPORTED_AT, appVersion = APP_VERSION))

    private fun account(accountId: String): Accounts = db.accountsQueries.find(accountId).executeAsOne()

    private fun category(categoryId: String): Categories = db.categoriesQueries.find(categoryId).executeAsOne()

    private fun transaction(id: String): Transactions = db.transactionsQueries.find(id).executeAsOne()

    private fun template(id: String): Recurring_movements = db.recurring_movementsQueries.find(id).executeAsOne()

    private fun totalRows(table: String): Long = rawCount("SELECT COUNT(*) FROM $table")

    private fun rowsWithId(table: String, column: String, id: String): Long =
        rawCount("SELECT COUNT(*) FROM $table WHERE $column = '$id'")

    private fun rawCount(sql: String): Long = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 0,
    ).value

    private fun seedFixture() {
        seedAccounts()
        seedCategories()
        seedTransactions()
        seedTemplates()
        tombstoneTheDeadRows()
        claimAndMarkEverythingSynced()
    }

    private fun seedAccounts() {
        insertAccount(accountId = "acc-wallet", name = "Yape", type = "Wallet")
        insertAccount(accountId = "acc-bank", name = "BCP", type = "Bank", currency = "USD")
        insertAccount(accountId = "acc-unknown-type", name = "Binance", type = "Crypto")
        insertAccount(accountId = "acc-dead", name = "Cuenta vieja", type = "Cash")
    }

    private fun seedCategories() {
        insertCategory("cat-income", "Sueldo", "work", "#00FF00", "Income", isDefault = true)
        insertCategory("cat-spend", "Comida", "food", "#FF0000", "Spend")
        insertCategory("cat-dead", "Bono", "gift", "#0000FF", "Income")
    }

    private fun seedTransactions() {
        insertTransaction(
            transactionId = "tx-income",
            type = "Income",
            amount = 4500_00L,
            description = "Sueldo mayo",
            occurredAt = "2026-05-23T09:33:20",
            categoryId = "cat-income",
            accountId = "acc-wallet",
        )
        insertTransaction(
            transactionId = "tx-spend",
            type = "Spend",
            amount = 150_00L,
            description = "Menu del dia",
            occurredAt = "2026-05-24T13:20:00",
            categoryId = "cat-spend",
            accountId = "acc-bank",
        )
        insertTransaction(
            transactionId = "tx-uncategorized",
            type = "Spend",
            amount = 42_50L,
            description = "",
            occurredAt = "2026-05-25T08:00:00",
            categoryId = null,
            accountId = "acc-wallet",
        )
        insertTransaction(
            transactionId = "tx-dead",
            type = "Spend",
            amount = 999_00L,
            description = "Anulado",
            occurredAt = "2026-05-26T10:00:00",
            categoryId = "cat-spend",
            accountId = "acc-wallet",
        )
    }

    private fun seedTemplates() {
        insertTemplate(
            id = "rec-active",
            name = "Alquiler",
            amount = 1200_00L,
            description = "Depa",
            categoryId = "cat-spend",
            accountId = "acc-bank",
            dayOfMonth = 5L,
            lastConfirmedPeriod = "2026-07",
            createdAt = ACTIVE_TEMPLATE_CREATED,
        )
        insertTemplate(
            id = "rec-paused",
            name = "Gimnasio",
            amount = null,
            description = "Mensualidad",
            categoryId = null,
            accountId = "acc-wallet",
            dayOfMonth = 20L,
            isActive = 0L,
            createdAt = PAUSED_TEMPLATE_CREATED,
        )
        insertTemplate(
            id = "rec-dead",
            name = "Cable",
            amount = 80_00L,
            description = "Cancelado",
            categoryId = null,
            accountId = "acc-wallet",
            dayOfMonth = 1L,
            createdAt = PAUSED_TEMPLATE_CREATED,
        )
    }

    private fun tombstoneTheDeadRows() {
        db.transactionsQueries.softDelete(deletedAt = SEEDED_AT, updatedAt = SEEDED_AT, transactionId = "tx-dead")
        db.recurring_movementsQueries.softDelete(deletedAt = SEEDED_AT, updatedAt = SEEDED_AT, id = "rec-dead")
        db.categoriesQueries.softDelete(deletedAt = SEEDED_AT, updatedAt = SEEDED_AT, categoryId = "cat-dead")
        db.accountsQueries.softDelete(deletedAt = SEEDED_AT, updatedAt = SEEDED_AT, accountId = "acc-dead")
    }

    private fun claimAndMarkEverythingSynced() {
        db.accountsQueries.claimAll(USER_ID)
        db.categoriesQueries.claimAll(USER_ID)
        db.transactionsQueries.claimAll(USER_ID)
        db.recurring_movementsQueries.claimAll(USER_ID)
        db.accountsQueries.markSynced(accountId = "acc-wallet", updatedAt = SEEDED_AT)
        db.categoriesQueries.markSynced(categoryId = "cat-income", updatedAt = SEEDED_AT)
        db.transactionsQueries.markSynced(transactionId = "tx-income", updatedAt = SEEDED_AT)
        db.recurring_movementsQueries.markSynced(id = "rec-active", updatedAt = SEEDED_AT)
    }

    private fun insertAccount(accountId: String, name: String, type: String, currency: String = "PEN") {
        db.accountsQueries.insert(
            accountId = accountId,
            name = name,
            type = type,
            currency = currency,
            updatedAt = SEEDED_AT,
            createdAt = SEEDED_AT,
        )
    }

    private fun insertCategory(
        categoryId: String,
        name: String,
        icon: String,
        color: String,
        categoryType: String,
        isDefault: Boolean = false,
    ) {
        db.categoriesQueries.insert(
            categoryId = categoryId,
            name = name,
            icon = icon,
            color = color,
            categoryType = categoryType,
            isDefault = isDefault,
            updatedAt = SEEDED_AT,
            createdAt = SEEDED_AT,
        )
    }

    private fun insertTransaction(
        transactionId: String,
        type: String,
        amount: Long,
        description: String,
        occurredAt: String,
        categoryId: String?,
        accountId: String,
    ) {
        db.transactionsQueries.insert(
            transactionId = transactionId,
            type = type,
            amount = amount,
            description = description,
            occurredAt = occurredAt,
            categoryId = categoryId,
            accountId = accountId,
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
        )
    }

    private fun insertTemplate(
        id: String,
        name: String,
        amount: Long?,
        description: String,
        categoryId: String?,
        accountId: String,
        dayOfMonth: Long,
        isActive: Long = 1L,
        lastConfirmedPeriod: String? = null,
        createdAt: Long,
    ) {
        db.recurring_movementsQueries.insert(
            id = id,
            name = name,
            type = "Spend",
            amount = amount,
            description = description,
            categoryId = categoryId,
            accountId = accountId,
            frequency = "Monthly",
            dayOfMonth = dayOfMonth,
            isActive = isActive,
            lastConfirmedPeriod = lastConfirmedPeriod,
            createdAt = createdAt,
            updatedAt = SEEDED_AT,
        )
    }

    private companion object {
        const val APP_VERSION = "2.4.0"
        const val USER_ID = "user-1"
        const val SEEDED_AT = 1_000L

        const val LIVE_ACCOUNTS = 3
        const val LIVE_CATEGORIES = 2
        const val LIVE_TRANSACTIONS = 3
        const val LIVE_RECURRING = 2

        val EXPORTED_AT: Long = Instant.parse("2026-08-16T10:00:00Z").toEpochMilliseconds()
        val IMPORTED_AT: Instant = Instant.parse("2026-08-16T11:30:00Z")
        val IMPORT_STAMP: Long = IMPORTED_AT.toEpochMilliseconds()
        val ACTIVE_TEMPLATE_CREATED: Long = Instant.parse("2026-06-10T12:00:00Z").toEpochMilliseconds()
        val PAUSED_TEMPLATE_CREATED: Long = Instant.parse("2026-07-01T08:00:00Z").toEpochMilliseconds()
    }
}
