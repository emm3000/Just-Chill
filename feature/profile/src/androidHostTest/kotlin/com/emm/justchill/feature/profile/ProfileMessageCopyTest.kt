package com.emm.justchill.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileMessageCopyTest {

    @Test
    fun `a saved export confirms the data is kept`() {
        assertEquals("Listo, tu data está guardada.", ProfileMessage.ExportDone.toText())
    }

    @Test
    fun `a failed export names the data and asks to retry`() {
        assertEquals("No pude exportar tu data. Inténtalo de nuevo.", ProfileMessage.ExportFailed.toText())
    }

    @Test
    fun `a failed csv export names the movements and asks to retry`() {
        assertEquals("No pude exportar tus movimientos. Inténtalo de nuevo.", ProfileMessage.CsvExportFailed.toText())
    }

    @Test
    fun `a failed import suspects a damaged file`() {
        assertEquals("No pude importar el archivo — capaz está dañado.", ProfileMessage.ImportFailed.toText())
    }

    @Test
    fun `a refused operation asks to wait for the running one`() {
        assertEquals("Espera a que termine la operación en curso.", ProfileMessage.OperationInProgress.toText())
    }

    @Test
    fun `each session notice says the data stays on the phone`() {
        assertEquals("Sesión cerrada. Tus datos siguen en este teléfono.", ProfileMessage.SessionClosed.toText())
        assertEquals(
            "Sesión cerrada acá; no llegué al servidor, así que tu acceso remoto sigue activo hasta " +
                "que expire. Cierra sesión con internet para cortarlo. Tus datos siguen en este teléfono.",
            ProfileMessage.SessionClosedLocallyOnly.toText(),
        )
        assertEquals("Cuenta eliminada. Tus datos siguen en este teléfono.", ProfileMessage.AccountDeleted.toText())
    }
}
