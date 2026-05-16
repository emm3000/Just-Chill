package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class EditTransactionRoute(val transactionId: String) : NavKey

@Serializable
data object CategoryRoute : NavKey

@Serializable
data object AddAccountRoute : NavKey

@Serializable
data object SelectCategoryRoute : NavKey

@Serializable
data object SelectIconRoute : NavKey

@Serializable
data object DashboardRoute : NavKey

@Serializable
data object AddTransactionRoute : NavKey


