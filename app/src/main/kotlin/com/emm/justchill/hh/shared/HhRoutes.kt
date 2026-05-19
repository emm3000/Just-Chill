package com.emm.justchill.hh.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistAddCheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Marker for routes that should display the bottom navigation bar.
 * Routes that don't implement this trigger an AnimatedVisibility slide-down.
 */
sealed interface BottomBarRoute : NavKey

@Serializable
data class ManifestoRoute(val isRevisit: Boolean = false) : NavKey

@Serializable
data object HomeRoute : BottomBarRoute

@Serializable
data object SeeTransactionRoute : BottomBarRoute

@Serializable
data object AccountsRoute : BottomBarRoute

@Serializable
data object ProfileRoute : BottomBarRoute

data class HhNavBarItem(
    val name: String,
    val icon: ImageVector,
)

val TOP_LEVEL_ROUTES: Map<BottomBarRoute, HhNavBarItem> = mapOf(
    HomeRoute to HhNavBarItem(name = "Inicio", icon = Icons.Filled.Home),
    SeeTransactionRoute to HhNavBarItem(name = "Ver", icon = Icons.Filled.AttachMoney),
    AccountsRoute to HhNavBarItem(name = "Cuentas", icon = Icons.Filled.PlaylistAddCheckCircle),
)
