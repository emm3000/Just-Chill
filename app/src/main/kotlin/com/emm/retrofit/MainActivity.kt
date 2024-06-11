package com.emm.retrofit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emm.retrofit.core.navigation.Experiences
import com.emm.retrofit.core.navigation.contactsDestination
import com.emm.retrofit.core.theme.EmmTheme
import com.emm.retrofit.experiences.readassets.ui.Experiences

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmmTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues()),
                ) {
                    Root()
                }
            }
        }
    }
}

@Composable
fun Root() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = Experiences) {
        composable<Experiences> {
            Experiences(navController)
        }
        contactsDestination(navController)
    }
}