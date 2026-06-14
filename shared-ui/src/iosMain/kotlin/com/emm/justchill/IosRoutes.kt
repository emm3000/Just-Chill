package com.emm.justchill

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.emm.domain.category.CategoryType
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

// iOS-only route keys for the iOS nav host (IosApp.kt). They mirror the LOCAL-FIRST subset of the
// Android HhRoutes/ObjectsRoutes, but live in shared-ui/iosMain because they use the JetBrains
// Compose Multiplatform navigation3 port (org.jetbrains.androidx.navigation3 / androidx.navigation3
// multiplatform runtime). The Android nav host keeps its own androidx.navigation3 NavKeys
// (Option A — do not migrate Android's nav, do not put nav in commonMain).
//
// OMITTED vs Android (phase 6): Auth, the Manifesto-as-launch-gate (iOS launches straight to Home).

/** Marker for routes that show the bottom navigation bar. */
internal sealed interface IosBottomBarRoute : NavKey

@Serializable
internal data object IosHomeRoute : IosBottomBarRoute

@Serializable
internal data object IosSeeTransactionsRoute : IosBottomBarRoute

@Serializable
internal data object IosAccountsRoute : IosBottomBarRoute

@Serializable
internal data object IosProfileRoute : IosBottomBarRoute

@Serializable
internal data object IosAddTransactionRoute : NavKey

@Serializable
internal data class IosEditTransactionRoute(val transactionId: String) : NavKey

@Serializable
internal data object IosAddAccountRoute : NavKey

@Serializable
internal data object IosCategoriesListRoute : NavKey

@Serializable
internal data class IosAddCategoryRoute(
    val initialType: CategoryType = CategoryType.Spend,
    val initialName: String = "",
    val propagateToTransaction: Boolean = false,
) : NavKey

@Serializable
internal data object IosRecurringRoute : NavKey

@Serializable
internal data class IosAddEditRecurringRoute(val id: String? = null) : NavKey

@Serializable
internal data object IosReportRoute : NavKey

// Kotlin/Native has no reflection-based serializer discovery, so the NavKey back stack cannot resolve
// route serializers the way the JVM (Android) does. Every @Serializable route above MUST be registered
// here for open NavKey polymorphism, or rememberNavBackStack throws at runtime ("You must pass a
// SavedStateConfiguration.serializersModule configured to handle NavKey open polymorphism"). When you
// add a route, register it here too.
// Source: https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html (non-JVM state serialization).
internal val iosNavSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(IosHomeRoute::class, IosHomeRoute.serializer())
            subclass(IosSeeTransactionsRoute::class, IosSeeTransactionsRoute.serializer())
            subclass(IosAccountsRoute::class, IosAccountsRoute.serializer())
            subclass(IosProfileRoute::class, IosProfileRoute.serializer())
            subclass(IosAddTransactionRoute::class, IosAddTransactionRoute.serializer())
            subclass(IosEditTransactionRoute::class, IosEditTransactionRoute.serializer())
            subclass(IosAddAccountRoute::class, IosAddAccountRoute.serializer())
            subclass(IosCategoriesListRoute::class, IosCategoriesListRoute.serializer())
            subclass(IosAddCategoryRoute::class, IosAddCategoryRoute.serializer())
            subclass(IosRecurringRoute::class, IosRecurringRoute.serializer())
            subclass(IosAddEditRecurringRoute::class, IosAddEditRecurringRoute.serializer())
            subclass(IosReportRoute::class, IosReportRoute.serializer())
        }
    }
}
