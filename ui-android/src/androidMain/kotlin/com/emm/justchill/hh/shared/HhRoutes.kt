package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavKey
import com.emm.domain.category.CategoryType
import kotlinx.serialization.Serializable

// Nav route keys for AppNavHost, on Google's androidx.navigation3 (NavKey/NavBackStack). This module
// is Android-only (ADR 005), so the host takes the 1-arg rememberNavBackStack — the overload that
// persists the back stack by JVM reflection, with no subtype registry to keep in sync.
//
// The one obligation left is @Serializable on every route: NavKeySerializer stores each entry as its
// class name plus its own serializer, and re-resolves it with Class.forName(name).kotlin.serializer()
// on restore. Miss the annotation (or give a route a field that cannot serialize) and the app dies on
// process-death restore ONLY — the compiler, assembleDevDebug and lint all see nothing wrong.
//
// The route set is CLOSED: every route here descends from the sealed AppRoute, and
// RouteSerializationTest (androidHostTest) enumerates those subclasses by reflection and round-trips
// each one through that same serializer. Declaring a new route as anything other than an AppRoute
// subtype opts it out of that guard — don't.

/**
 * Closed set of every navigation key [AppNavHost] can push.
 *
 * Sealed on purpose: `RouteSerializationTest` enumerates the subclasses by reflection, so the guard
 * covers the whole route set instead of a hand-copied list — a route that is not `@Serializable`
 * fails there rather than on a user's process-death restore, which is the only place it would
 * otherwise surface.
 */
sealed interface AppRoute : NavKey

/**
 * Marker for routes that should display the bottom navigation bar.
 * Routes that don't implement this trigger an AnimatedVisibility slide-down.
 */
sealed interface BottomBarRoute : AppRoute

@Serializable
data class ManifestoRoute(val isRevisit: Boolean = false) : AppRoute

@Serializable
data object PrivacyPolicyRoute : AppRoute

@Serializable
data object HomeRoute : BottomBarRoute

@Serializable
data object SeeTransactionRoute : BottomBarRoute

@Serializable
data object AccountsRoute : BottomBarRoute

@Serializable
data object ProfileRoute : BottomBarRoute

@Serializable
data object AuthRoute : AppRoute

@Serializable
data object AddTransactionRoute : AppRoute

@Serializable
data class EditTransactionRoute(val transactionId: String) : AppRoute

@Serializable
data object AddAccountRoute : AppRoute

@Serializable
data object CategoriesListRoute : AppRoute

@Serializable
data class CategoryRoute(
    val initialType: CategoryType = CategoryType.Spend,
    val initialName: String = "",
    val propagateToTransaction: Boolean = false,
) : AppRoute

@Serializable
data object RecurringMovementsRoute : AppRoute

@Serializable
data class AddEditRecurringMovementRoute(val id: String? = null) : AppRoute

@Serializable
data object ReportRoute : AppRoute
