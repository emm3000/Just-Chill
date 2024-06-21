package com.emm.justchill.experiences.hh.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emm.justchill.experiences.hh.presentation.category.Category
import com.emm.justchill.experiences.hh.presentation.home.Home
import com.emm.justchill.experiences.hh.presentation.income.Income
import com.emm.justchill.experiences.hh.presentation.spent.Spent
import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object Spent

@Serializable
object Income

@Serializable
object Category

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
        composable<Category> {
            Category(navController)
        }
    }
}