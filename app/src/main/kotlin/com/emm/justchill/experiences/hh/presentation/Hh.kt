package com.emm.justchill.experiences.hh.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object Spent

@Serializable
object Income

@Composable
fun Hh() {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Home) {
        composable<Home> {
            Home(navController)
        }
        composable<Spent> {
            Spent()
        }
        composable<Income> {
            Income(navController)
        }
    }
}