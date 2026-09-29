package com.emm.justchill.feature.category

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.ui.navigation.AppRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object CategoriesListRoute : AppRoute

@Serializable
data class CategoryRoute(
    val initialType: CategoryType = CategoryType.Spend,
    val initialName: String = "",
    val propagateToTransaction: Boolean = false,
) : AppRoute

val categoryRoutes: List<KClass<out AppRoute>> = listOf(
    CategoriesListRoute::class,
    CategoryRoute::class,
)
