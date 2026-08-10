package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavKey
import com.emm.domain.category.CategoryType
import kotlinx.serialization.Serializable

// Nav route keys for the unified host (AppNavHost). The whole nav stack is now commonMain: the
// androidx.navigation3:navigation3-runtime artifact (NavKey/NavBackStack) is multiplatform, and the
// JetBrains Compose Multiplatform navigation3-UI port (NavDisplay) drives BOTH platforms.
//
// Kotlin/Native has no reflection-based serializer discovery, so every @Serializable route the host can
// push MUST also be registered in navSavedStateConfiguration (NavSavedStateConfiguration.kt). Android
// resolves serializers via JVM reflection and tolerates the explicit registration.
//
// The route set is CLOSED: every route here descends from the sealed AppRoute, and
// NavSavedStateConfigurationTest (androidHostTest) enumerates those subclasses by reflection and fails
// when one of them is missing from navSavedStateConfiguration. Declaring a new route as anything other
// than an AppRoute subtype opts it out of that guard — don't.

/**
 * Closed set of every navigation key [AppNavHost] can push.
 *
 * Sealed on purpose: `NavSavedStateConfigurationTest` enumerates the subclasses by reflection and
 * fails when a route is not registered in [navSavedStateConfiguration]. Kotlin/Native has no
 * reflective serializer discovery, so an unregistered route crashes `rememberNavBackStack` on
 * process-death restore — a failure no compiler and no build gate can see.
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
