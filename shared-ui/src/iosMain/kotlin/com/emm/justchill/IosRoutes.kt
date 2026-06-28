package com.emm.justchill

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.emm.justchill.hh.shared.AccountsRoute
import com.emm.justchill.hh.shared.AddAccountRoute
import com.emm.justchill.hh.shared.AddEditRecurringMovementRoute
import com.emm.justchill.hh.shared.AddTransactionRoute
import com.emm.justchill.hh.shared.AuthRoute
import com.emm.justchill.hh.shared.CategoriesListRoute
import com.emm.justchill.hh.shared.CategoryRoute
import com.emm.justchill.hh.shared.EditTransactionRoute
import com.emm.justchill.hh.shared.HomeRoute
import com.emm.justchill.hh.shared.ProfileRoute
import com.emm.justchill.hh.shared.RecurringMovementsRoute
import com.emm.justchill.hh.shared.ReportRoute
import com.emm.justchill.hh.shared.SeeTransactionRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

// The nav route keys now live in commonMain (com.emm.justchill.hh.shared.HhRoutes.kt), shared by both
// nav hosts. Android resolves their serializers via JVM reflection; Kotlin/Native (iOS) does NOT, so
// the NavKey back stack cannot resolve route serializers the way the JVM does. Every @Serializable
// route the iOS host uses MUST be registered here for open NavKey polymorphism, or rememberNavBackStack
// throws at runtime ("You must pass a SavedStateConfiguration.serializersModule configured to handle
// NavKey open polymorphism"). When the iOS host starts using a new route, register it here too.
//
// This registers EXACTLY the routes the iOS host (IosApp.kt) navigates to. ManifestoRoute /
// PrivacyPolicyRoute are Android-only launch/info screens not yet wired on iOS, so they are omitted
// on purpose — adding them would be harmless but is not required until iOS uses them.
// Source: https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html (non-JVM state serialization).
internal val iosNavSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
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
