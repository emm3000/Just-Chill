package com.emm.justchill.feature.recurring

import com.emm.justchill.core.ui.navigation.CaptureRoute
import kotlin.test.Test
import kotlin.test.assertTrue

class RecurringRoutesTest {

    @Test
    fun `the add edit form is a capture route, so the new-category detour returns to it`() {
        assertTrue(AddEditRecurringMovementRoute() is CaptureRoute)
    }
}
