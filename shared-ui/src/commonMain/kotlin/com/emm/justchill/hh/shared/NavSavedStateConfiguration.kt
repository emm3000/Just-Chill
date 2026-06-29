package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

// SavedStateConfiguration for the unified nav host (AppNavHost). rememberNavBackStack persists the
// back stack through SavedStateConfiguration's serializersModule, which must register EVERY NavKey
// subtype the host can push. Kotlin/Native (iOS) has no reflection-based serializer discovery, so the
// DEFAULT (empty module) crashes at runtime on restore with "You must pass a
// SavedStateConfiguration.serializersModule configured to handle NavKey open polymorphism". Android
// resolves serializers via JVM reflection and historically used the 1-arg rememberNavBackStack, but
// the unified host passes this configuration on BOTH platforms (one host, one back-stack call).
//
// ALL routes are registered here — including PrivacyPolicyRoute. iOS never pushes PrivacyPolicyRoute
// (the privacy click is inert there), so registering it is harmless; it keeps the configuration a
// single source of truth for the shared host. When the host starts using a new route, register it here.
// Source: https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html (non-JVM state serialization).
internal val navSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(ManifestoRoute::class, ManifestoRoute.serializer())
            subclass(PrivacyPolicyRoute::class, PrivacyPolicyRoute.serializer())
            subclass(HomeRoute::class, HomeRoute.serializer())
            subclass(SeeTransactionRoute::class, SeeTransactionRoute.serializer())
            subclass(AccountsRoute::class, AccountsRoute.serializer())
            subclass(ProfileRoute::class, ProfileRoute.serializer())
            subclass(AddTransactionRoute::class, AddTransactionRoute.serializer())
            subclass(EditTransactionRoute::class, EditTransactionRoute.serializer())
            subclass(AddAccountRoute::class, AddAccountRoute.serializer())
            subclass(CategoriesListRoute::class, CategoriesListRoute.serializer())
            subclass(CategoryRoute::class, CategoryRoute.serializer())
            subclass(RecurringMovementsRoute::class, RecurringMovementsRoute.serializer())
            subclass(AddEditRecurringMovementRoute::class, AddEditRecurringMovementRoute.serializer())
            subclass(ReportRoute::class, ReportRoute.serializer())
            subclass(AuthRoute::class, AuthRoute.serializer())
        }
    }
}
