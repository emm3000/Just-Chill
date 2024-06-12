package com.emm.justchill.experiences.drinks.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.emm.justchill.experiences.drinks.data.DrinkApiModel
import kotlinx.serialization.Serializable

@Serializable
object Drink

@Serializable
object DrinkList

@Serializable
data class DrinkDetail(
    val title: String,
    val description: String,
    val image: String
)

fun DrinkApiModel.toRoute() = DrinkDetail(
    title = name,
    description = description,
    image = image
)

fun NavGraphBuilder.drinksDestination(navController: NavController) {

    navigation<Drink>(startDestination = DrinkList) {
        composable<DrinkList> { DrinksScreen(navController = navController) }
        composable<DrinkDetail> {
            val route: DrinkDetail = it.toRoute<DrinkDetail>()
            DrinkDetailScreen(route)
        }
    }
}