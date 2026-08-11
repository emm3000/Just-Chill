package com.emm.data.backup

import com.emm.domain.account.Account
import com.emm.domain.account.AccountType
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionType
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BackupMappersTest {

    private val json = Json { encodeDefaults = true }

    // ── Account ──────────────────────────────────────────────────────────────

    @Test
    fun `Account roundtrip - entity to DTO to JSON to DTO to entity preserves all fields`() {
        val original = Account(
            accountId = AccountId("acc-bank-001"),
            name = "BCP Cuenta Ahorros",
            type = AccountType.Bank,
        )

        val dto = original.toDto()
        val jsonStr = json.encodeToString(dto)
        val decoded = json.decodeFromString<AccountDto>(jsonStr)
        val restored = decoded.toEntity()

        assertEquals(original, restored)
    }

    @Test
    fun `Account roundtrip - all AccountType values survive serialization`() {
        AccountType.entries.forEach { accountType ->
            val original = Account(
                accountId = AccountId("id-$accountType"),
                name = "Test",
                type = accountType,
            )
            val restored = json.decodeFromString<AccountDto>(
                json.encodeToString(original.toDto()),
            ).toEntity()
            assertEquals(accountType, restored.type)
        }
    }

    @Test
    fun `Account export always writes PEN regardless of legacy data`() {
        val account = Account(
            accountId = AccountId("acc-1"),
            name = "Yape",
            type = AccountType.Wallet,
        )
        val dto = account.toDto()
        assertEquals("PEN", dto.currency)
    }

    @Test
    fun `Account import ignores currency field - old JSON with USD does not fail`() {
        // Simulates a backup JSON created by an old app version that stored "USD"
        val oldJson = """{"accountId":"acc-old","name":"Old Account","type":"Bank","currency":"USD"}"""
        val dto = Json.decodeFromString<AccountDto>(oldJson)
        // toEntity() must not throw; currency field is ignored
        val entity = dto.toEntity()
        assertEquals(AccountId("acc-old"), entity.accountId)
        assertEquals(AccountType.Bank, entity.type)
    }

    @Test
    fun `Account import - missing currency field in JSON uses default PEN`() {
        // Simulates a minimal future backup that omits the currency field entirely
        val minimalJson = """{"accountId":"acc-2","name":"Minimal","type":"Cash"}"""
        val dto = Json { ignoreUnknownKeys = true }.decodeFromString<AccountDto>(minimalJson)
        assertEquals("PEN", dto.currency)
        val entity = dto.toEntity()
        assertEquals(AccountId("acc-2"), entity.accountId)
    }

    // ── Category ─────────────────────────────────────────────────────────────

    @Test
    fun `Category roundtrip - entity to DTO to JSON to DTO to entity preserves all fields`() {
        val original = Category(
            categoryId = CategoryId("cat-sueldo"),
            name = "Sueldo",
            icon = "work_outline",
            color = "#4CAF50",
            categoryType = CategoryType.Income,
        )

        val dto = original.toDto()
        val jsonStr = json.encodeToString(dto)
        val decoded = json.decodeFromString<CategoryDto>(jsonStr)
        val restored = decoded.toEntity()

        assertEquals(original, restored)
    }

    @Test
    fun `Category roundtrip - all CategoryType values survive serialization`() {
        CategoryType.entries.forEach { categoryType ->
            val original = Category(
                categoryId = CategoryId("id-$categoryType"),
                name = "Test",
                icon = "icon",
                color = "#000000",
                categoryType = categoryType,
            )
            val restored = json.decodeFromString<CategoryDto>(
                json.encodeToString(original.toDto()),
            ).toEntity()
            assertEquals(categoryType, restored.categoryType)
        }
    }

    // ── Transaction ──────────────────────────────────────────────────────────

    @Test
    fun `Transaction roundtrip - entity to DTO to JSON to DTO to entity preserves all fields`() {
        val original = Transaction(
            transactionId = TransactionId("tx-abc123"),
            type = TransactionType.Income,
            amount = Money(4500_00L),
            description = "Sueldo quincenal",
            occurredAt = LocalDateTime(2026, 5, 23, 9, 33, 20),
            accountId = AccountId("acc-1"),
            categoryId = CategoryId("cat-1"),
        )

        val dto = original.toDto()
        val jsonStr = json.encodeToString(dto)
        val decoded = json.decodeFromString<TransactionDto>(jsonStr)
        val restored = decoded.toEntityOrNull()

        assertEquals(original, restored)
    }

    @Test
    fun `Transaction roundtrip - null categoryId survives serialization`() {
        val original = Transaction(
            transactionId = TransactionId("tx-no-cat"),
            type = TransactionType.Spend,
            amount = Money(50_00L),
            description = "Misceláneos",
            occurredAt = LocalDateTime(2026, 5, 24, 13, 20, 0),
            accountId = AccountId("acc-1"),
            categoryId = null,
        )

        val restored = json.decodeFromString<TransactionDto>(
            json.encodeToString(original.toDto()),
        ).toEntityOrNull()

        assertNotNull(restored)
        assertNull(restored.categoryId)
        assertEquals(original, restored)
    }

    @Test
    fun `Transaction roundtrip - amount stored as Long cents without floating-point loss`() {
        val exactCents = 123456789_99L
        val original = Transaction(
            transactionId = TransactionId("tx-big"),
            type = TransactionType.Income,
            amount = Money(exactCents),
            description = "",
            occurredAt = LocalDateTime(1970, 1, 1, 0, 0),
            accountId = AccountId("acc-1"),
            categoryId = null,
        )

        val restored = json.decodeFromString<TransactionDto>(
            json.encodeToString(original.toDto()),
        ).toEntityOrNull()

        assertNotNull(restored)
        assertEquals(exactCents, restored.amount.cents)
    }

    @Test
    fun `Transaction roundtrip - all TransactionType values survive serialization`() {
        TransactionType.entries.forEach { txType ->
            val original = Transaction(
                transactionId = TransactionId("id-$txType"),
                type = txType,
                amount = Money(100L),
                description = "",
                occurredAt = LocalDateTime(1970, 1, 1, 0, 0),
                accountId = AccountId("acc-1"),
                categoryId = null,
            )
            val restored = json.decodeFromString<TransactionDto>(
                json.encodeToString(original.toDto()),
            ).toEntityOrNull()
            assertNotNull(restored)
            assertEquals(txType, restored.type)
        }
    }
}
