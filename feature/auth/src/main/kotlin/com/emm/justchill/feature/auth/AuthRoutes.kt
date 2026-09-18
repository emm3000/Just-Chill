package com.emm.justchill.feature.auth

import com.emm.justchill.core.ui.navigation.AppRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object AuthRoute : AppRoute

val authRoutes: List<KClass<out AppRoute>> = listOf(
    AuthRoute::class,
)
