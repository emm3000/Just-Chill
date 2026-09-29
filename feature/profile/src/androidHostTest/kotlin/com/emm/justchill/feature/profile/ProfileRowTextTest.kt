package com.emm.justchill.feature.profile

import org.junit.Test
import kotlin.test.assertEquals

class ProfileRowTextTest {

    @Test
    fun `the categories row splits the total by type`() {
        assertEquals("24 en total · 7 de ingreso", categoriesMetaText(total = 24, incomeCount = 7))
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

    @Test
    fun `the state labels the last export for a screen that cannot map it`() {
        assertEquals("Nunca", ProfileUiState(lastExport = LastExportUi.Never).lastExportLabel)
        assertEquals("Último: hoy", ProfileUiState(lastExport = LastExportUi.DaysAgo(0)).lastExportLabel)
        assertEquals("Último: ayer", ProfileUiState(lastExport = LastExportUi.DaysAgo(1)).lastExportLabel)
        assertEquals("Último: hace 3 días", ProfileUiState(lastExport = LastExportUi.DaysAgo(3)).lastExportLabel)
    }
}
