package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavKey
import com.emm.domain.category.CategoryType
import kotlinx.serialization.Serializable

@Serializable
data class EditTransactionRoute(val transactionId: String) : NavKey

@Serializable
data class CategoryRoute(val initialType: CategoryType = CategoryType.Spend) : NavKey

@Serializable
data object AddAccountRoute : NavKey

@Serializable
data object SelectCategoryRoute : NavKey

@Serializable
data object DashboardRoute : NavKey

@Serializable
data object AddTransactionRoute : NavKey

@Serializable
data object ReportRoute : NavKey


