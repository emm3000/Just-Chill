package com.emm.justchill.core

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

class Feature(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val resource: String,
    val route: RootRoutes,
    val screen: @Composable (NavController) -> Unit,
)