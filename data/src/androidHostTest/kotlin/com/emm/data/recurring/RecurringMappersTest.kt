package com.emm.data.recurring

import com.emm.data.SelectAllWithDetails
import com.emm.domain.recurring.Frequency
import com.emm.domain.transaction.TransactionType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RecurringMappersTest {

    // ── RecurringMovementEntity.asExternalModelOrNull ─────────────────────────

    @Test
    fun `asExternalModelOrNull - valid type and frequency returns RecurringMovement`() {
        val entity = recurringEntity("rm-1", "Income", "Monthly")
        val result = entity.asExternalModelOrNull()
        assertNotNull(result)
        assertEquals(TransactionType.Income, result.type)
        assertEquals(Frequency.Monthly, result.frequency)
    }

    @Test
    fun `asExternalModelOrNull - unknown type returns null`() {
        assertNull(recurringEntity("rm-1", "INCOME", "Monthly").asExternalModelOrNull())
    }

    @Test
    fun `asExternalModelOrNull - unknown frequency returns null`() {
        assertNull(recurringEntity("rm-1", "Income", "MONTHLY").asExternalModelOrNull())
    }

    @Test
    fun `asExternalModelOrNull - unknown type and unknown frequency returns null`() {
        assertNull(recurringEntity("rm-1", "Transfer", "Weekly").asExternalModelOrNull())
    }

    // ── List<RecurringMovementEntity>.asExternalModel ─────────────────────────

    @Test
    fun `list asExternalModel - unknown type row is skipped, valid rows survive`() {
        val entities = listOf(
            recurringEntity("rm-good-1", "Income", "Monthly"),
            recurringEntity("rm-bad", "INCOME", "Monthly"), // unknown type
            recurringEntity("rm-good-2", "Spend", "Monthly"),
        )
        val result = entities.asExternalModel()
        assertEquals(2, result.size)
        assertEquals("rm-good-1", result[0].id.value)
        assertEquals("rm-good-2", result[1].id.value)
    }

    @Test
    fun `list asExternalModel - unknown frequency row is skipped`() {
        val entities = listOf(
            recurringEntity("rm-1", "Income", "Monthly"),
            recurringEntity("rm-2", "Spend", "MONTHLY"), // unknown frequency
        )
        val result = entities.asExternalModel()
        assertEquals(1, result.size)
        assertEquals("rm-1", result[0].id.value)
    }

    // ── SelectAllWithDetails.asExternalModelOrNull ────────────────────────────

    @Test
    fun `SelectAllWithDetails asExternalModelOrNull - valid type returns RecurringMovementDetails`() {
        val result = detailsRow("Spend").asExternalModelOrNull()
        assertNotNull(result)
        assertEquals(TransactionType.Spend, result.type)
    }

    @Test
    fun `SelectAllWithDetails asExternalModelOrNull - unknown type returns null`() {
        assertNull(detailsRow("SPEND").asExternalModelOrNull())
    }

    @Test
    fun `SelectAllWithDetails asExternalModelOrNull - unknown type Transfer returns null`() {
        assertNull(detailsRow("Transfer").asExternalModelOrNull())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun recurringEntity(id: String, type: String, frequency: String) = RecurringMovementEntity(
        id = id,
        name = "Test movement",
        type = type,
        amount = 100_00L,
        description = "desc",
        categoryId = null,
        accountId = "acc-1",
        frequency = frequency,
        dayOfMonth = 15L,
        isActive = 1L,
        lastConfirmedPeriod = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun detailsRow(type: String) = SelectAllWithDetails(
        id = "rm-details-1",
        name = "Rent",
        type = type,
        amount = 500_00L,
        description = "desc",
        categoryId = null,
        accountId = "acc-1",
        frequency = "Monthly",
        dayOfMonth = 1L,
        isActive = 1L,
        lastConfirmedPeriod = null,
        createdAt = 0L,
        updatedAt = 0L,
        userId = null,
        deletedAt = null,
        syncState = "Pending",
        categoryName = null,
        categoryColor = null,
        accountName = "BCP",
    )
}
