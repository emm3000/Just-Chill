package com.emm.justchill.shell

import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.AddTransactionRoute
import com.emm.justchill.hh.shared.AppRoute
import com.emm.justchill.hh.shared.LoansRoute

const val ACTION_OPEN_LOANS: String = "com.emm.justchill.action.OPEN_LOANS"
const val ACTION_ADD_TRANSACTION: String = "com.emm.justchill.action.ADD_TRANSACTION"
const val EXTRA_ACCOUNT_ID: String = "com.emm.justchill.extra.ACCOUNT_ID"
const val EXTRA_CATEGORY_ID: String = "com.emm.justchill.extra.CATEGORY_ID"
const val EXTRA_TYPE: String = "com.emm.justchill.extra.TYPE"

data class ShortcutIntent(
    val action: String? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val type: String? = null,
)

internal fun routeForShortcutAction(shortcut: ShortcutIntent): AppRoute? = when (shortcut.action) {
    ACTION_OPEN_LOANS -> LoansRoute
    ACTION_ADD_TRANSACTION -> addTransactionRouteFor(shortcut)
    else -> null
}

private fun addTransactionRouteFor(shortcut: ShortcutIntent): AppRoute = AddTransactionRoute(
    preselectedAccountId = shortcut.accountId?.takeIf(String::isNotBlank),
    preselectedCategoryId = shortcut.categoryId?.takeIf(String::isNotBlank),
    preselectedType = TransactionType.entries.find { it.name == shortcut.type },
)

internal fun shortcutRouteToPush(shortcut: ShortcutIntent, firstLaunchSeen: Boolean, currentTop: NavKey?): AppRoute? =
    routeForShortcutAction(shortcut)?.takeIf { firstLaunchSeen && it != currentTop }
