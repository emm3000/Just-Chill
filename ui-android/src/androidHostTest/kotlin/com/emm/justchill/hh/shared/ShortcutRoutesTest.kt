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

    @Test
    fun `a known action with onboarding seen and a different top pushes LoansRoute`() {
        assertEquals(
            LoansRoute,
            shortcutRouteToPush(ACTION_OPEN_LOANS, firstLaunchSeen = true, currentTop = ProfileRoute),
        )
    }

    @Test
    fun `a known action pushes nothing while onboarding has not been seen`() {
        assertNull(shortcutRouteToPush(ACTION_OPEN_LOANS, firstLaunchSeen = false, currentTop = ProfileRoute))
    }

    @Test
    fun `a known action pushes nothing when LoansRoute is already on top`() {
        assertNull(shortcutRouteToPush(ACTION_OPEN_LOANS, firstLaunchSeen = true, currentTop = LoansRoute))
    }

    @Test
    fun `a null action pushes nothing`() {
        assertNull(shortcutRouteToPush(null, firstLaunchSeen = true, currentTop = ProfileRoute))
    }

    @Test
    fun `an unknown action pushes nothing`() {
        assertNull(
            shortcutRouteToPush("android.intent.action.VIEW", firstLaunchSeen = true, currentTop = ProfileRoute),
        )
    }
}
