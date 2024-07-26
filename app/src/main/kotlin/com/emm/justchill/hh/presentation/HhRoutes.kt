package com.emm.justchill.hh.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistAddCheckCircle
import androidx.compose.ui.graphics.vector.ImageVector

sealed class HhRoutes(
    val name: String,
    val route: String,
    val icon: ImageVector
) {

    data object HhHome: HhRoutes("Inicio", "home", Icons.Filled.Home)
    data object AddCategory: HhRoutes("Categoría", "addCategory", Icons.Filled.Create)
    data object AddTransaction: HhRoutes("Transacción", "addTransaction", Icons.Filled.AddChart)
    data object SeeTransaction: HhRoutes("Ver", "seeTransactions", Icons.Filled.PlaylistAddCheckCircle)
}

val hhRoutes = listOf(
    HhRoutes.HhHome,
    HhRoutes.AddTransaction,
    HhRoutes.SeeTransaction,
)