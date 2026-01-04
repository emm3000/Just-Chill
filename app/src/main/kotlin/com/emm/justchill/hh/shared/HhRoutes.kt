package com.emm.justchill.hh.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAddCheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object AccountsRoute : NavKey

@Serializable
data object SeeTransactionRoute : NavKey

@Serializable
data object ProfileRoute : NavKey

data class HhNavBarItem(
    val name: String,
    val route: String,
    val icon: ImageVector,
)

val TOP_LEVEL_ROUTES: Map<NavKey, HhNavBarItem> = mapOf(
    HomeRoute to HhNavBarItem(name = "Inicio", route = "class", icon = Icons.Filled.Home),
    SeeTransactionRoute to HhNavBarItem(name = "Ver", route = "class", icon = Icons.Filled.AttachMoney),
    AccountsRoute to HhNavBarItem(name = "Cuentas", route = "class", icon = Icons.Filled.PlaylistAddCheckCircle),
    ProfileRoute to HhNavBarItem(name = "Profile", route = "class", icon = Icons.Filled.Person),
)