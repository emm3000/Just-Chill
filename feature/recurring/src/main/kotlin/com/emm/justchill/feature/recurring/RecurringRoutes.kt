package com.emm.justchill.feature.recurring

import com.emm.justchill.core.ui.navigation.AppRoute
import com.emm.justchill.core.ui.navigation.CaptureRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object RecurringMovementsRoute : AppRoute

@Serializable
data class AddEditRecurringMovementRoute(val id: String? = null) : CaptureRoute

val recurringRoutes: List<KClass<out AppRoute>> = listOf(
    RecurringMovementsRoute::class,
    AddEditRecurringMovementRoute::class,
)
