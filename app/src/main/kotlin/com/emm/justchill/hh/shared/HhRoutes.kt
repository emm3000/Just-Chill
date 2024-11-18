package com.emm.justchill.hh.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.AirlineSeatLegroomExtra
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistAddCheckCircle
import androidx.compose.ui.graphics.vector.ImageVector

sealed class HhRoutes(
    val name: String,
    val route: String,
    val icon: ImageVector,
) {

    data object HhHome: HhRoutes("Inicio", "home", Icons.Filled.Home)
    data object HnNewHome: HhRoutes("Cuentas", "cuentas", Icons.Filled.AccountBalance)
    data object AddCategory: HhRoutes("Categoría", "addCategory", Icons.Filled.Create)
    data object AddTransaction: HhRoutes("Transacción", "addTransaction", Icons.Filled.AddChart)
    data object SeeTransaction: HhRoutes("Ver", "seeTransactions", Icons.Filled.PlaylistAddCheckCircle)
    data object Me: HhRoutes("Me", "me", Icons.Filled.AirlineSeatLegroomExtra)
}

val hhRoutes = listOf(
    HhRoutes.HnNewHome,
    HhRoutes.HhHome,
    HhRoutes.AddTransaction,
    HhRoutes.SeeTransaction,
)