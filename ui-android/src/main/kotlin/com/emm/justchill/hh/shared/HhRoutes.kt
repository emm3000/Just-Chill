package com.emm.justchill.hh.shared

import com.emm.justchill.core.ui.navigation.AppRoute
import com.emm.justchill.core.ui.navigation.BottomBarRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object PrivacyPolicyRoute : AppRoute

@Serializable
data object ProfileRoute : BottomBarRoute

val hhRoutes: List<KClass<out AppRoute>> = listOf(
    PrivacyPolicyRoute::class,
    ProfileRoute::class,
)
