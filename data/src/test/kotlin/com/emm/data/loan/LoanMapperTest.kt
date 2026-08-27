package com.emm.data.loan

import com.emm.domain.shared.Money
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LoanMapperTest {

    @Test
    fun `asExternalModelOrNull - valid lentAt round-trips every field`() {
        val entity = loanEntity(lentAt = "2026-08-10T12:00:00")

        val result = entity.asExternalModelOrNull()

        assertNotNull(result)
        assertEquals("loan-1", result.id.value)
        assertEquals("Ana", result.personName)
        assertEquals("ana", result.personKey)
        assertEquals(Money(100_000L), result.principal)
        assertEquals(250, result.interestBps)
        assertEquals(Money(102_500L), result.totalDue)
    }

    @Test
    fun `asExternalModelOrNull - unparseable lentAt returns null`() {
        assertNull(loanEntity(lentAt = "not-a-date").asExternalModelOrNull())
    }

    @Test
    fun `list asExternalModel - unparseable row is skipped, valid rows survive`() {
        val entities = listOf(
            loanEntity(loanId = "loan-good-1", lentAt = "2026-08-10T12:00:00"),
            loanEntity(loanId = "loan-bad", lentAt = "not-a-date"),
            loanEntity(loanId = "loan-good-2", lentAt = "2026-08-11T12:00:00"),
        )

        val result = entities.asExternalModel()

        assertEquals(2, result.size)
        assertEquals("loan-good-1", result[0].id.value)
        assertEquals("loan-good-2", result[1].id.value)
    }

    private fun loanEntity(loanId: String = "loan-1", lentAt: String) = LoanEntity(
        loanId = loanId,
        personName = "Ana",
        personKey = "ana",
        principal = 100_000L,
        interestBps = 250L,
        totalDue = 102_500L,
        note = "",
        lentAt = lentAt,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
