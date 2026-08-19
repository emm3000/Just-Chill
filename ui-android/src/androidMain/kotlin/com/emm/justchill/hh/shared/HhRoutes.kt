package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavKey
import com.emm.domain.category.CategoryType
import kotlinx.serialization.Serializable

/**
 * Every subtype must be `@Serializable`, fields included: the back stack re-resolves each entry by
 * class name on process-death restore, so a missing annotation crashes there and nowhere else.
 * Declaring a route outside this hierarchy opts it out of `RouteSerializationTest`, the only guard.
 */
sealed interface AppRoute : NavKey

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
