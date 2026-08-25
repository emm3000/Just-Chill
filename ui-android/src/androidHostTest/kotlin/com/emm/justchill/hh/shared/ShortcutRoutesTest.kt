package com.emm.justchill.hh.shared

import com.emm.domain.transaction.TransactionType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShortcutRoutesTest {

    @Test
    fun `the loans shortcut action maps to LoansRoute`() {
        assertEquals(LoansRoute, routeForShortcutAction(ShortcutIntent(action = ACTION_OPEN_LOANS)))
    }

    @Test
    fun `a null action maps to no route`() {
        assertNull(routeForShortcutAction(ShortcutIntent(action = null)))
    }

    @Test
    fun `an unknown action maps to no route`() {
        assertNull(routeForShortcutAction(ShortcutIntent(action = "android.intent.action.VIEW")))
    }

    @Test
    fun `loans extras on the loans action are ignored`() {
        val shortcut = ShortcutIntent(
            action = ACTION_OPEN_LOANS,
            accountId = "account-1",
            categoryId = "category-1",
            type = "Income",
        )
        assertEquals(LoansRoute, routeForShortcutAction(shortcut))
    }

    @Test
    fun `the add-transaction action with the full combo preselects everything`() {
        val shortcut = ShortcutIntent(
            action = ACTION_ADD_TRANSACTION,
            accountId = "account-1",
            categoryId = "category-1",
            type = "Income",
        )
        assertEquals(
            AddTransactionRoute(
                preselectedAccountId = "account-1",
                preselectedCategoryId = "category-1",
                preselectedType = TransactionType.Income,
            ),
            routeForShortcutAction(shortcut),
        )
    }

    @Test
    fun `the add-transaction action with a missing account id leaves the account unselected`() {
        val shortcut = ShortcutIntent(action = ACTION_ADD_TRANSACTION, categoryId = "category-1", type = "Income")
        assertEquals(
            AddTransactionRoute(preselectedCategoryId = "category-1", preselectedType = TransactionType.Income),
            routeForShortcutAction(shortcut),
        )
    }

    @Test
    fun `the add-transaction action with a missing category id leaves the category unselected`() {
        val shortcut = ShortcutIntent(action = ACTION_ADD_TRANSACTION, accountId = "account-1", type = "Income")
        assertEquals(
            AddTransactionRoute(preselectedAccountId = "account-1", preselectedType = TransactionType.Income),
            routeForShortcutAction(shortcut),
        )
    }

    @Test
    fun `the add-transaction action with a missing type leaves the type unselected`() {
        val shortcut =
            ShortcutIntent(action = ACTION_ADD_TRANSACTION, accountId = "account-1", categoryId = "category-1")
        assertEquals(
            AddTransactionRoute(preselectedAccountId = "account-1", preselectedCategoryId = "category-1"),
            routeForShortcutAction(shortcut),
        )
    }

    @Test
    fun `the add-transaction action with a garbage type leaves the type unselected`() {
        val shortcut = ShortcutIntent(action = ACTION_ADD_TRANSACTION, type = "not-a-real-type")
        assertEquals(AddTransactionRoute(), routeForShortcutAction(shortcut))
    }

    @Test
    fun `the add-transaction action with an empty account id leaves the account unselected`() {
        val shortcut = ShortcutIntent(action = ACTION_ADD_TRANSACTION, accountId = "", categoryId = "category-1")
        assertEquals(
            AddTransactionRoute(preselectedCategoryId = "category-1"),
            routeForShortcutAction(shortcut),
        )
    }

    @Test
    fun `a known action with onboarding seen and a different top pushes LoansRoute`() {
        val shortcut = ShortcutIntent(action = ACTION_OPEN_LOANS)
        assertEquals(LoansRoute, shortcutRouteToPush(shortcut, firstLaunchSeen = true, currentTop = ProfileRoute))
    }

    @Test
    fun `a known action pushes nothing while onboarding has not been seen`() {
        val shortcut = ShortcutIntent(action = ACTION_OPEN_LOANS)
        assertNull(shortcutRouteToPush(shortcut, firstLaunchSeen = false, currentTop = ProfileRoute))
    }

    @Test
    fun `a known action pushes nothing when LoansRoute is already on top`() {
        val shortcut = ShortcutIntent(action = ACTION_OPEN_LOANS)
        assertNull(shortcutRouteToPush(shortcut, firstLaunchSeen = true, currentTop = LoansRoute))
    }

    @Test
    fun `a null action pushes nothing`() {
        assertNull(shortcutRouteToPush(ShortcutIntent(), firstLaunchSeen = true, currentTop = ProfileRoute))
    }

    @Test
    fun `an unknown action pushes nothing`() {
        val shortcut = ShortcutIntent(action = "android.intent.action.VIEW")
        assertNull(shortcutRouteToPush(shortcut, firstLaunchSeen = true, currentTop = ProfileRoute))
    }

    @Test
    fun `the add-transaction combo pushes when the same combo is not already on top`() {
        val shortcut = ShortcutIntent(action = ACTION_ADD_TRANSACTION, accountId = "account-1")
        assertEquals(
            AddTransactionRoute(preselectedAccountId = "account-1"),
            shortcutRouteToPush(shortcut, firstLaunchSeen = true, currentTop = ProfileRoute),
        )
    }

    @Test
    fun `the add-transaction combo pushes nothing when the identical combo is already on top`() {
        val shortcut = ShortcutIntent(action = ACTION_ADD_TRANSACTION, accountId = "account-1")
        val alreadyOnTop = AddTransactionRoute(preselectedAccountId = "account-1")
        assertNull(shortcutRouteToPush(shortcut, firstLaunchSeen = true, currentTop = alreadyOnTop))
    }
}
