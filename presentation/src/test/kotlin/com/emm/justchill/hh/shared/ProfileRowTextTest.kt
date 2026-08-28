package com.emm.justchill.hh.shared

import com.emm.domain.shared.Money
import com.emm.justchill.hh.profile.LastExportUi
import org.junit.Test
import kotlin.test.assertEquals

class ProfileRowTextTest {

    @Test
    fun `the categories row splits the total by type`() {
        assertEquals("24 en total · 7 de ingreso", categoriesMetaText(total = 24, incomeCount = 7))
    }

    @Test
    fun `the recurring row pairs the count with the money that leaves every month`() {
        assertEquals("3 al mes · salen S/ 90.00", recurringMetaText(activeCount = 3, monthlyOutflow = Money(9_000L)))
    }

    @Test
    fun `no active template says so instead of reciting two zeroes`() {
        assertEquals("Ninguno todavía", recurringMetaText(activeCount = 0, monthlyOutflow = Money.Zero))
    }

    @Test
    fun `a template with no fixed amount still shows the count`() {
        assertEquals("1 al mes · salen S/ 0.00", recurringMetaText(activeCount = 1, monthlyOutflow = Money.Zero))
    }

    @Test
    fun `never exported says Nunca`() {
        assertEquals("Nunca", LastExportUi.Never.toMetaText())
    }

    @Test
    fun `an export is dated in days, not with a timestamp`() {
        assertEquals("Último: hoy", LastExportUi.DaysAgo(0).toMetaText())
        assertEquals("Último: ayer", LastExportUi.DaysAgo(1).toMetaText())
        assertEquals("Último: hace 3 días", LastExportUi.DaysAgo(3).toMetaText())
    }
}
