package com.emm.justchill.experiences

import com.emm.justchill.ExperienceItem
import com.emm.justchill.core.Feature
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emm.justchill.core.RootRoutes
import com.emm.justchill.toBundle
import com.emm.justchill.core.rootData

@Composable
fun RootExperiences(navController: NavController) {

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {

        items(rootData, key = Feature::id) { feature ->
            ExperienceItem(data = feature.toBundle()) { bundle ->
                navController.navigate(bundle.route)
            }
        }
    }
}

@Composable
fun Root() {

    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = RootRoutes.ExperiencesRoot.route) {
        composable(RootRoutes.ExperiencesRoot.route) {
            RootExperiences(navController = navController)
        }
        rootData.forEach { features ->
            composable(features.route.route) { features.screen(navController) }
        }
    }
}