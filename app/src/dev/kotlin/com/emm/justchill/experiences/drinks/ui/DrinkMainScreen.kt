package com.emm.justchill.experiences.drinks.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

@Composable
fun DrinkMainScreen() {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = DrinkList) {
        composable<DrinkList> { DrinksScreen(navController = navController) }
        composable<DrinkDetail> {
            val route: DrinkDetail = it.toRoute<DrinkDetail>()
            DrinkDetailScreen(route)
        }
    }
}