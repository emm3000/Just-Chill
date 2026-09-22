package com.emm.justchill.core.ui.pending

import com.emm.justchill.core.domain.transaction.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals

class SkipPeriodCopyTest {

    @Test fun `spend reads no lo pague`() {
        assertEquals("No lo pagué en agosto", skipPeriodCopy(TransactionType.Spend, "agosto"))
    }

    @Test fun `income reads no lo recibi`() {
        assertEquals("No lo recibí en agosto", skipPeriodCopy(TransactionType.Income, "agosto"))
    }
}
