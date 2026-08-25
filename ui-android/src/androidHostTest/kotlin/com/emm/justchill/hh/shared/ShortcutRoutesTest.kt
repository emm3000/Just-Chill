package com.emm.justchill.hh.shared

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShortcutRoutesTest {

    @Test
    fun `the loans shortcut action maps to LoansRoute`() {
        assertEquals(LoansRoute, routeForShortcutAction(ACTION_OPEN_LOANS))
    }

    @Test
    fun `a null action maps to no route`() {
        assertNull(routeForShortcutAction(null))
    }

    @Test
    fun `an unknown action maps to no route`() {
        assertNull(routeForShortcutAction("android.intent.action.VIEW"))
    }
}
