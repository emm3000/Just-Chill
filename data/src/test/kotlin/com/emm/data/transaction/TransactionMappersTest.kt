package com.emm.data.transaction

import com.emm.domain.transaction.TransactionType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TransactionMappersTest {

    @Test
    fun `asExternalModelOrNull - valid Income type returns Transaction`() {
        val entity = transactionEntity("tx-1", "Income")
        val result = entity.asExternalModelOrNull()
        assertNotNull(result)
        assertEquals(TransactionType.Income, result.type)
        assertEquals("tx-1", result.transactionId.value)
    }

    @Test
    fun `asExternalModelOrNull - valid Spend type returns Transaction`() {
        val entity = transactionEntity("tx-1", "Spend")
        val result = entity.asExternalModelOrNull()
        assertNotNull(result)
        assertEquals(TransactionType.Spend, result.type)
    }

    @Test
    fun `asExternalModelOrNull - unknown type INCOME returns null`() {
        assertNull(transactionEntity("tx-1", "INCOME").asExternalModelOrNull())
    }

    @Test
    fun `asExternalModelOrNull - unknown type Transfer returns null`() {
        assertNull(transactionEntity("tx-1", "Transfer").asExternalModelOrNull())
    }

    @Test
    fun `List asExternalModel - bad-type row is skipped, valid rows survive`() {
        val entities = listOf(
            transactionEntity("tx-good-1", "Income"),
            transactionEntity("tx-bad", "INCOME"),
            transactionEntity("tx-good-2", "Spend"),
        )
        val result = entities.asExternalModel()
        assertEquals(2, result.size)
        assertEquals("tx-good-1", result[0].transactionId.value)
        assertEquals("tx-good-2", result[1].transactionId.value)
    }

    @Test
    fun `List asExternalModel - all-bad list returns empty list`() {
        val entities = listOf(
            transactionEntity("tx-1", "INCOME"),
            transactionEntity("tx-2", "SPEND"),
        )
        assertEquals(emptyList(), entities.asExternalModel())
    }

    @Test
    fun `toDomainOrNull - valid type and category returns full TransactionWithCategory`() {
        val entity = transactionWithCategoryEntity(
            type = "Spend",
            categoryType = "Spend",
        )
        val result = entity.toDomainOrNull()
        assertNotNull(result)
        assertEquals(TransactionType.Spend, result.type)
        assertNotNull(result.category)
    }

    @Test
    fun `toDomainOrNull - unknown transaction type returns null`() {
        val entity = transactionWithCategoryEntity(type = "SPEND", categoryType = "Spend")
        assertNull(entity.toDomainOrNull())
    }

    @Test
    fun `toDomainOrNull - valid type but unknown categoryType keeps transaction with null category`() {
        val entity = transactionWithCategoryEntity(
            type = "Income",
            categoryType = "INCOME",
        )
        val result = entity.toDomainOrNull()
        assertNotNull(result)
        assertEquals(TransactionType.Income, result.type)
        assertNull(result.category)
    }

    @Test
    fun `toDomainOrNull - valid type and null categoryId produces null category`() {
        val entity = transactionWithCategoryEntity(
            type = "Income",
            categoryId = null,
            categoryName = null,
            categoryIcon = null,
            categoryColor = null,
            categoryType = null,
        )
        val result = entity.toDomainOrNull()
        assertNotNull(result)
        assertNull(result.category)
    }

    @Test
    fun `toDomainOrNull - missing categoryId still produces null category`() {
        val entity = transactionWithCategoryEntity(
            type = "Income",
            categoryId = null,
            categoryName = "Comida",
            categoryIcon = "food",
            categoryColor = "#FF0000",
            categoryType = "Spend",
        )
        val result = entity.toDomainOrNull()
        assertNotNull(result)
        assertNull(result.category)
    }

    @Test
    fun `toDomainOrNull - missing categoryName still produces null category`() {
        val entity = transactionWithCategoryEntity(
            type = "Income",
            categoryId = "cat-1",
            categoryName = null,
            categoryIcon = "food",
            categoryColor = "#FF0000",
            categoryType = "Spend",
        )
        val result = entity.toDomainOrNull()
        assertNotNull(result)
        assertNull(result.category)
    }

    @Test
    fun `toDomainOrNull - missing categoryIcon still produces null category`() {
        val entity = transactionWithCategoryEntity(
            type = "Income",
            categoryId = "cat-1",
            categoryName = "Comida",
            categoryIcon = null,
            categoryColor = "#FF0000",
            categoryType = "Spend",
        )
        val result = entity.toDomainOrNull()
        assertNotNull(result)
        assertNull(result.category)
    }

    @Test
    fun `toDomainOrNull - missing categoryColor still produces null category`() {
        val entity = transactionWithCategoryEntity(
            type = "Income",
            categoryId = "cat-1",
            categoryName = "Comida",
            categoryIcon = "food",
            categoryColor = null,
            categoryType = "Spend",
        )
        val result = entity.toDomainOrNull()
        assertNotNull(result)
        assertNull(result.category)
    }

    @Test
    fun `toDomainOrNull - categoryId present but categoryType null keeps transaction with null category`() {
        val entity = transactionWithCategoryEntity(
            type = "Income",
            categoryId = "cat-1",
            categoryName = "Food",
            categoryIcon = "food",
            categoryColor = "#00FF00",
            categoryType = null,
        )
        val result = entity.toDomainOrNull()
        assertNotNull(result)
        assertEquals(TransactionType.Income, result.type)
        assertNull(result.category)
    }

    @Test
    fun `toDomain list - bad-type row is skipped, valid rows survive`() {
        val entities = listOf(
            transactionWithCategoryEntity(id = "tx-a", type = "Income", categoryType = "Income"),
            transactionWithCategoryEntity(id = "tx-b", type = "SPEND"),
            transactionWithCategoryEntity(id = "tx-c", type = "Spend", categoryType = "Spend"),
        )
        val result = entities.toDomain()
        assertEquals(2, result.size)
        assertEquals("tx-a", result[0].transactionId.value)
        assertEquals("tx-c", result[1].transactionId.value)
    }

    private fun transactionEntity(id: String, type: String) = TransactionEntity(
        transactionId = id,
        type = type,
        amount = 10000L,
        description = "test",
        occurredAt = "2026-08-10T21:47:33",
        categoryId = null,
        accountId = "acc-1",
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun transactionWithCategoryEntity(
        id: String = "tx-1",
        type: String = "Income",
        accountName: String? = "BCP",
        categoryId: String? = "cat-1",
        categoryName: String? = "Comida",
        categoryIcon: String? = "food",
        categoryColor: String? = "#FF0000",
        categoryType: String? = "Spend",
    ) = TransactionWithCategoryEntity(
        transactionId = id,
        type = type,
        amount = 5000L,
        description = "desc",
        occurredAt = "2026-08-10T21:47:33",
        accountId = "acc-1",
        accountName = accountName,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryIcon = categoryIcon,
        categoryColor = categoryColor,
        categoryType = categoryType,
    )
}
