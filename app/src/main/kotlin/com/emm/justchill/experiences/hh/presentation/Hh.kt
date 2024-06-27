package com.emm.justchill.experiences.hh.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emm.justchill.experiences.hh.presentation.category.Category
import com.emm.justchill.experiences.hh.presentation.home.Home
import com.emm.justchill.experiences.hh.presentation.seetransactions.SeeTransactions
import com.emm.justchill.experiences.hh.presentation.transaction.Transaction
import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object Income

@Serializable
object Category

@Serializable
object SeeTransactions

@Composable
fun Hh() {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Home) {
        composable<Home> {
            Home(navController)
        }
        composable<Income> {
            Transaction(navController)
        }
        composable<Category> {
            Category(navController)
        }
        composable<SeeTransactions> {
            SeeTransactions(navController)
        }
    }
}