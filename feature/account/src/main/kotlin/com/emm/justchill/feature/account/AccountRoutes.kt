package com.emm.justchill.feature.account

import com.emm.justchill.core.ui.navigation.AppRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object AccountsRoute : AppRoute

@Serializable
data object AddAccountRoute : AppRoute

val accountRoutes: List<KClass<out AppRoute>> = listOf(
    AccountsRoute::class,
    AddAccountRoute::class,
)
