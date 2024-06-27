package com.emm.justchill

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emm.justchill.core.RootRoutes
import com.emm.justchill.core.rootData
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.experiences.hh.presentation.Hh

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContent {
            EmmTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Hh()
                }
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