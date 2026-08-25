package com.emm.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.account.AccountLocalDataSource
import com.emm.data.account.DefaultAccountRepository
import com.emm.data.category.CategoryLocalDataSource
import com.emm.data.category.DefaultCategoryRepository
import com.emm.data.recurring.DefaultRecurringMovementRepository
import com.emm.data.recurring.RecurringMovementLocalDataSource
import com.emm.data.transaction.DefaultTransactionRepository
import com.emm.data.transaction.TransactionLocalDataSource
import com.emm.domain.account.AccountType
import com.emm.domain.account.AccountUpsert
import com.emm.domain.account.DeleteAccountUseCase
import com.emm.domain.category.CategoryType
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.category.DeleteCategoryUseCase
import com.emm.domain.recurring.DeleteRecurringMovementUseCase
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class DeleteUseCasesE2ETest {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: EmmDatabaseData

    private lateinit var deleteTransaction: DeleteTransactionUseCase
    private lateinit var deleteCategory: DeleteCategoryUseCase
    private lateinit var deleteAccount: DeleteAccountUseCase
    private lateinit var deleteRecurring: DeleteRecurringMovementUseCase

    private lateinit var accountRepo: DefaultAccountRepository
    private lateinit var categoryRepo: DefaultCategoryRepository
    private lateinit var transactionRepo: DefaultTransactionRepository
    private lateinit var recurringRepo: DefaultRecurringMovementRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        driver = AndroidSqliteDriver(
            schema = EmmDatabaseData.Schema,
            context = context,
            name = null, // in-memory
            callback = object : AndroidSqliteDriver.Callback(schema = EmmDatabaseData.Schema) {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            },
        )
        database = EmmDatabaseData(driver)

        val accountDs = AccountLocalDataSource(database, Clock.System)
        val categoryDs = CategoryLocalDataSource(database, Clock.System)
        val transactionDs = TransactionLocalDataSource(database.transactionsQueries, Clock.System)
        val recurringDs = RecurringMovementLocalDataSource(database, Clock.System)

        accountRepo = DefaultAccountRepository(accountDs)
        categoryRepo = DefaultCategoryRepository(categoryDs)
        transactionRepo = DefaultTransactionRepository(transactionDs)
        recurringRepo = DefaultRecurringMovementRepository(recurringDs, Clock.System)

        deleteTransaction = DeleteTransactionUseCase(transactionRepo)
        deleteCategory = DeleteCategoryUseCase(categoryRepo)
        deleteAccount = DeleteAccountUseCase(accountRepo, transactionRepo, recurringRepo)
        deleteRecurring = DeleteRecurringMovementUseCase(recurringRepo)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private suspend fun insertAccount(id: String): AccountId {
        val accountId = AccountId(id)
        accountRepo.create(AccountUpsert(accountId = accountId, name = "Acc $id", type = AccountType.Bank))
        return accountId
    }

    private suspend fun insertCategory(id: String): CategoryId {
        val categoryId = CategoryId(id)
        categoryRepo.create(
            CategoryUpsert(
                categoryId = categoryId,
                name = "Cat $id",
                icon = "wallet",
                color = "blue",
                categoryType = CategoryType.Spend,
            ),
        )
        return categoryId
    }

    private suspend fun insertTransaction(id: String, accountId: AccountId, categoryId: CategoryId?): TransactionId {
        val txId = TransactionId(id)
        transactionRepo.create(
            TransactionInsert(
                id = txId,
                type = TransactionType.Spend,
                amount = Money(1_000L),
                description = "",
                categoryId = categoryId,
                occurredAt = LocalDateTime(2026, 8, 10, 12, 0),
                accountId = accountId,
            ),
        )
        return txId
    }

    private suspend fun insertRecurring(
        id: String,
        accountId: AccountId,
        categoryId: CategoryId?,
    ): RecurringMovementId {
        // Insert via direct SQL to supply our own id (the DataSource generates a UUID on create).
        database.recurring_movementsQueries.insert(
            id = id,
            name = "Rec $id",
            type = "Spend",
            amount = 5_000L,
            description = "",
            categoryId = categoryId?.value,
            accountId = accountId.value,
            frequency = "Monthly",
            dayOfMonth = 1L,
            isActive = 1L,
            lastConfirmedPeriod = null,
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
        return RecurringMovementId(id)
    }

    private fun rawTransactionCount(txId: String): Long = driver.executeQuery(
        null,
        "SELECT COUNT(*) FROM transactions WHERE transactionId = '$txId'",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null) },
        0,
    ).value ?: 0L

    private fun rawTransactionDeletedAt(txId: String): Long? = driver.executeQuery(
        null,
        "SELECT deletedAt FROM transactions WHERE transactionId = '$txId'",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null) },
        0,
    ).value

    private fun rawTransactionSyncState(txId: String): String? = driver.executeQuery(
        null,
        "SELECT syncState FROM transactions WHERE transactionId = '$txId'",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) },
        0,
    ).value

    private fun rawTransactionCategoryId(txId: String): String? = driver.executeQuery(
        null,
        "SELECT categoryId FROM transactions WHERE transactionId = '$txId'",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) },
        0,
    ).value

    private fun rawAccountDeletedAt(accountId: String): Long? = driver.executeQuery(
        null,
        "SELECT deletedAt FROM accounts WHERE accountId = '$accountId'",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null) },
        0,
    ).value

    private fun rawCategoryDeletedAt(categoryId: String): Long? = driver.executeQuery(
        null,
        "SELECT deletedAt FROM categories WHERE categoryId = '$categoryId'",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null) },
        0,
    ).value

    private fun rawRecurringCategoryId(id: String): String? = driver.executeQuery(
        null,
        "SELECT categoryId FROM recurring_movements WHERE id = '$id'",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) },
        0,
    ).value

    private fun rawRecurringDeletedAt(id: String): Long? = driver.executeQuery(
        null,
        "SELECT deletedAt FROM recurring_movements WHERE id = '$id'",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null) },
        0,
    ).value

    @Test
    fun deleteTransaction_tombstonesRow_and_filters_it_from_reads() = runTest {
        val accountId = insertAccount("A1")
        val categoryId = insertCategory("C1")
        val txId = insertTransaction("TX1", accountId, categoryId)

        deleteTransaction(txId)

        assertEquals(1L, rawTransactionCount("TX1"), "physical row must survive soft-delete")
        val deletedAt = rawTransactionDeletedAt("TX1")
        assertTrue(deletedAt != null && deletedAt > 0L, "deletedAt must be set after soft-delete")
        assertEquals("Pending", rawTransactionSyncState("TX1"), "syncState must be Pending after soft-delete")
        assertNull(transactionRepo.find(txId), "tombstoned transaction must not be returned by find()")
    }

    @Test
    fun deleteCategory_tombstonesCategory_andLeavesTransactionsLinked() = runTest {
        val accountId = insertAccount("A2")
        val categoryId = insertCategory("C2")
        insertTransaction("TX2", accountId, categoryId)

        deleteCategory(categoryId)

        val catDeletedAt = rawCategoryDeletedAt("C2")
        assertTrue(catDeletedAt != null && catDeletedAt > 0L, "category deletedAt must be set")
        assertEquals("C2", rawTransactionCategoryId("TX2"), "the link to the deleted category must survive")
        assertNull(rawTransactionDeletedAt("TX2"), "live transaction must not be tombstoned")
        assertTrue(
            categoryRepo.all().first().none { it.categoryId == categoryId },
            "tombstoned category must not appear in all()",
        )
    }

    @Test
    fun deleteCategory_leavesTombstonedTransactionsUntouched() = runTest {
        val accountId = insertAccount("A3")
        val categoryId = insertCategory("C3")
        val txId = insertTransaction("TX3", accountId, categoryId)

        deleteTransaction(txId)

        deleteCategory(categoryId)

        assertEquals("C3", rawTransactionCategoryId("TX3"), "tombstoned transaction's categoryId must not be touched")
    }

    @Test
    fun deleteCategory_leavesRecurringMovementsLinked() = runTest {
        val accountId = insertAccount("A4")
        val categoryId = insertCategory("C4")
        insertRecurring("REC4", accountId, categoryId)

        deleteCategory(categoryId)

        assertEquals("C4", rawRecurringCategoryId("REC4"), "the link to the deleted category must survive")
    }

    @Test
    fun deleteAccount_withLiveTransaction_throwsValidationError() = runTest {
        val accountId = insertAccount("A5")
        val categoryId = insertCategory("C5")
        insertTransaction("TX5", accountId, categoryId)

        assertFailsWith<DomainException.ValidationError> {
            deleteAccount(accountId)
        }

        assertNull(rawAccountDeletedAt("A5"), "account must not be tombstoned when blocked")
    }

    @Test
    fun deleteAccount_withLiveRecurringMovement_throwsValidationError() = runTest {
        val accountId = insertAccount("A6")
        val categoryId = insertCategory("C6")
        insertRecurring("REC6", accountId, categoryId)

        assertFailsWith<DomainException.ValidationError> {
            deleteAccount(accountId)
        }

        assertNull(rawAccountDeletedAt("A6"), "account must not be tombstoned when blocked")
    }

    @Test
    fun deleteAccount_withOnlyTombstonedTransactions_succeeds() = runTest {
        val accountId = insertAccount("A7")
        val categoryId = insertCategory("C7")
        val txId = insertTransaction("TX7", accountId, categoryId)

        deleteTransaction(txId)

        deleteAccount(accountId)

        val deletedAt = rawAccountDeletedAt("A7")
        assertTrue(deletedAt != null && deletedAt > 0L, "account must be tombstoned after successful delete")
        assertNull(accountRepo.find(accountId), "tombstoned account must not be returned by find()")
    }

    @Test
    fun deleteRecurringMovement_unknownId_throwsNotFound() = runTest {
        assertFailsWith<DomainException.NotFound> {
            deleteRecurring(RecurringMovementId("does-not-exist"))
        }
    }

    @Test
    fun deleteRecurringMovement_tombstonesRow_and_filters_it_from_reads() = runTest {
        val accountId = insertAccount("A9")
        val categoryId = insertCategory("C9")
        val recId = insertRecurring("REC9", accountId, categoryId)

        deleteRecurring(recId)

        val deletedAt = rawRecurringDeletedAt("REC9")
        assertTrue(deletedAt != null && deletedAt > 0L, "recurring movement deletedAt must be set")
        assertNull(recurringRepo.find(recId), "tombstoned recurring movement must not be returned by find()")
    }
}
