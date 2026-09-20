package com.emm.justchill.feature.report

import com.emm.justchill.core.ui.navigation.AppRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object ReportRoute : AppRoute

val reportRoutes: List<KClass<out AppRoute>> = listOf(ReportRoute::class)
