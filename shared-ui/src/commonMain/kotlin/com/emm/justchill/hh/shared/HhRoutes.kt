package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavKey
import com.emm.domain.category.CategoryType
import kotlinx.serialization.Serializable

// Unified nav route keys shared by BOTH nav hosts (Android Hh.kt + iOS IosApp.kt). They live in
// commonMain because the keys are identical-shape on both platforms; only the NavDisplay/entryProvider
// host bodies stay split (Android uses androidx.navigation3:navigation3-ui, iOS uses the JetBrains
// Compose Multiplatform port — a deliberate dependency split). The androidx.navigation3:navigation3-runtime
// artifact (NavKey/NavBackStack) is multiplatform and is wired into commonMain.dependencies.
//
// Kotlin/Native has no reflection-based serializer discovery, so every @Serializable route the iOS host
// uses MUST also be registered in iosNavSavedStateConfiguration (IosRoutes.kt). Android resolves
// serializers via JVM reflection and needs no such registration.

/**
 * Marker for routes that should display the bottom navigation bar.
 * Routes that don't implement this trigger an AnimatedVisibility slide-down.
 */
sealed interface BottomBarRoute : NavKey

@Serializable
data class ManifestoRoute(val isRevisit: Boolean = false) : NavKey

@Serializable
data object PrivacyPolicyRoute : NavKey

@Serializable
data object HomeRoute : BottomBarRoute

@Serializable
data object SeeTransactionRoute : BottomBarRoute

@Serializable
data object AccountsRoute : BottomBarRoute

@Serializable
data object ProfileRoute : BottomBarRoute

@Serializable
data object AuthRoute : NavKey

@Serializable
data object AddTransactionRoute : NavKey

@Serializable
data class EditTransactionRoute(val transactionId: String) : NavKey

@Serializable
data object AddAccountRoute : NavKey

@Serializable
data object CategoriesListRoute : NavKey

@Serializable
data class CategoryRoute(
    val initialType: CategoryType = CategoryType.Spend,
    val initialName: String = "",
    val propagateToTransaction: Boolean = false,
) : NavKey

@Serializable
data object RecurringMovementsRoute : NavKey

@Serializable
data class AddEditRecurringMovementRoute(val id: String? = null) : NavKey

@Serializable
data object ReportRoute : NavKey
