package com.emm.justchill.hh.account

import kotlin.test.Test
import kotlin.test.assertEquals

class AccountsCopyTest {

    @Test
    fun `an account with no movement this month says so instead of naming its type`() {
        assertEquals("Sin movimientos este mes", accountSubtitle(typeLabel = "Banco", movementCount = 0))
    }

    @Test
    fun `one movement is singular`() {
        assertEquals("Banco · 1 movimiento", accountSubtitle(typeLabel = "Banco", movementCount = 1))
    }

    @Test
    fun `more than one movement is plural, behind the type label`() {
        assertEquals("Efectivo · 5 movimientos", accountSubtitle(typeLabel = "Efectivo", movementCount = 5))
    }

    @Test
    fun `nobody owing reads as nobody owing, not as zero people`() {
        assertEquals("Nadie te debe", loansSubtitle(emptyList()))
    }

    @Test
    fun `a single debtor is named`() {
        assertEquals("1 persona · Carlos", loansSubtitle(listOf("Carlos")))
    }

    @Test
    fun `several debtors are counted and then named in order`() {
        assertEquals("3 personas · Carlos, Ana, Luis", loansSubtitle(listOf("Carlos", "Ana", "Luis")))
    }
}
