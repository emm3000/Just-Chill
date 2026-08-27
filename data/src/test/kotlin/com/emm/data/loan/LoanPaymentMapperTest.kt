package com.emm.data.loan

import com.emm.domain.loan.PaymentMethod
import com.emm.domain.shared.Money
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LoanPaymentMapperTest {

    @Test
    fun `asExternalModelOrNull - Cash method round-trips every field`() {
        val entity = paymentEntity(method = "Cash")

        val result = entity.asExternalModelOrNull()

        assertNotNull(result)
        assertEquals("pay-1", result.id.value)
        assertEquals("loan-1", result.loanId.value)
        assertEquals(Money(300L), result.amount)
        assertEquals(PaymentMethod.Cash, result.method)
    }

    @Test
    fun `asExternalModelOrNull - Transfer method round-trips`() {
        val result = paymentEntity(method = "Transfer").asExternalModelOrNull()

        assertNotNull(result)
        assertEquals(PaymentMethod.Transfer, result.method)
    }

    @Test
    fun `asExternalModelOrNull - unparseable method returns null`() {
        assertNull(paymentEntity(method = "Card").asExternalModelOrNull())
    }

    @Test
    fun `list asExternalModel - a row with an unparseable method is dropped, valid rows survive`() {
        val entities = listOf(
            paymentEntity(paymentId = "pay-good-1", method = "Cash"),
            paymentEntity(paymentId = "pay-bad", method = "Card"),
            paymentEntity(paymentId = "pay-good-2", method = "Transfer"),
        )

        val result = entities.asExternalModel()

        assertEquals(2, result.size)
        assertEquals("pay-good-1", result[0].id.value)
        assertEquals("pay-good-2", result[1].id.value)
    }

    private fun paymentEntity(paymentId: String = "pay-1", method: String) = LoanPaymentEntity(
        paymentId = paymentId,
        loanId = "loan-1",
        amount = 300L,
        method = method,
        paidAt = "2026-08-11T12:00:00",
        note = "",
        createdAt = 0L,
        updatedAt = 0L,
    )
}
