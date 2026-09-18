package com.emm.justchill.core.ui.atoms

import androidx.compose.ui.window.DialogProperties
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InFlightDialogPropertiesTest {

    @Test
    fun `in flight blocks back press and outside tap dismissal`() {
        val properties: DialogProperties = inFlightDialogProperties(isInFlight = true)
        assertFalse(properties.dismissOnBackPress)
        assertFalse(properties.dismissOnClickOutside)
    }

    @Test
    fun `idle allows back press and outside tap dismissal`() {
        val properties: DialogProperties = inFlightDialogProperties(isInFlight = false)
        assertTrue(properties.dismissOnBackPress)
        assertTrue(properties.dismissOnClickOutside)
    }
}
