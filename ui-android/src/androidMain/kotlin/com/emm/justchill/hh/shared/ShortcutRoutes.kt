package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavKey

const val ACTION_OPEN_LOANS = "com.emm.justchill.action.OPEN_LOANS"

internal fun routeForShortcutAction(action: String?): NavKey? = when (action) {
    ACTION_OPEN_LOANS -> LoansRoute
    else -> null
}

internal fun shortcutRouteToPush(action: String?, firstLaunchSeen: Boolean, currentTop: NavKey?): NavKey? =
    routeForShortcutAction(action)?.takeIf { firstLaunchSeen && it != currentTop }
