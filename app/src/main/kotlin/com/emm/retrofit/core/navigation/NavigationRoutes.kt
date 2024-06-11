package com.emm.retrofit.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.emm.retrofit.experiences.drinks.ui.DrinkDetail
import com.emm.retrofit.experiences.drinks.ui.Drinks
import kotlinx.serialization.Serializable

@Serializable
object Experiences

@Serializable
object Drink

@Serializable
object DrinkList

@Serializable
object DrinkDetail

fun NavGraphBuilder.contactsDestination(navController: NavController) {
    navigation<Drink>(startDestination = DrinkList) {
        composable<DrinkList> { Drinks(navController = navController) }
        composable<DrinkDetail> { DrinkDetail() }
    }
}