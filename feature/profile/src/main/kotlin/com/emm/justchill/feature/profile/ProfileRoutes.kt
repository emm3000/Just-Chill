package com.emm.justchill.feature.profile

import com.emm.justchill.core.ui.navigation.AppRoute
import com.emm.justchill.core.ui.navigation.BottomBarRoute
import com.emm.justchill.feature.profile.privacy.PrivacyPolicyRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object ProfileRoute : BottomBarRoute

val profileRoutes: List<KClass<out AppRoute>> = listOf(
    ProfileRoute::class,
    PrivacyPolicyRoute::class,
)
