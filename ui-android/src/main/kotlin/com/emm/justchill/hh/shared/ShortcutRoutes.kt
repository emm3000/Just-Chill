package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavKey
import com.emm.domain.transaction.TransactionType

const val ACTION_OPEN_LOANS = "com.emm.justchill.action.OPEN_LOANS"
const val ACTION_ADD_TRANSACTION = "com.emm.justchill.action.ADD_TRANSACTION"
const val EXTRA_ACCOUNT_ID = "com.emm.justchill.extra.ACCOUNT_ID"
const val EXTRA_CATEGORY_ID = "com.emm.justchill.extra.CATEGORY_ID"
const val EXTRA_TYPE = "com.emm.justchill.extra.TYPE"

/**
 * The only shape a launcher intent may cross into `:androidApp` as: nav3 and `:domain` types cannot
 * follow, since `ui-android/build.gradle.kts` declares nav3 `implementation`, not `api` (E06-11).
 */
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
