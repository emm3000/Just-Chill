package com.emm.justchill.feature.onboarding

import com.emm.justchill.core.ui.navigation.AppRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class ManifestoRoute(val isRevisit: Boolean = false) : AppRoute

val onboardingRoutes: List<KClass<out AppRoute>> = listOf(
    ManifestoRoute::class,
)
