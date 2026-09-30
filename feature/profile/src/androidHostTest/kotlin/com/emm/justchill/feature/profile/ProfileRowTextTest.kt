package com.emm.justchill.feature.profile

import org.junit.Test
import kotlin.test.assertEquals

class ProfileRowTextTest {

    @Test
    fun `the state labels the last export for a screen that cannot map it`() {
        assertEquals("Nunca", ProfileUiState(lastExport = LastExportUi.Never).lastExportLabel)
        assertEquals("Último: hoy", ProfileUiState(lastExport = LastExportUi.DaysAgo(0)).lastExportLabel)
        assertEquals("Último: ayer", ProfileUiState(lastExport = LastExportUi.DaysAgo(1)).lastExportLabel)
        assertEquals("Último: hace 3 días", ProfileUiState(lastExport = LastExportUi.DaysAgo(3)).lastExportLabel)
    }

    @Test
    fun `the state splits the categories row by type for a screen that cannot phrase it`() {
        assertEquals(
            "24 en total · 7 de ingreso",
            ProfileUiState(categoryCount = 24, incomeCategoryCount = 7).categoriesLabel,
        )
    }

    @Test
    fun `the import warning of a signed-in session says the other devices lose it too`() {
        assertEquals(
            "Tus movimientos, categorías y cuentas quedan tal cual el archivo. " +
                "Lo que no esté ahí se borra, y como tienes sesión iniciada también se " +
                "borra en tus otros dispositivos. No se puede deshacer.",
            signedInProfile.importWarning,
        )
    }

    @Test
    fun `the import warning of a signed-out session stays on this device`() {
        assertEquals(
            "Tus movimientos, categorías y cuentas quedan tal cual el archivo. " +
                "Lo que no esté ahí se borra. No se puede deshacer.",
            ProfileUiState(session = SessionUiState.SignedOut).importWarning,
        )
    }
}
