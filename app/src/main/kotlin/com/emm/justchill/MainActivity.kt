package com.emm.justchill

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emm.justchill.core.RootRoutes
import com.emm.justchill.core.rootData
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.experiences.hh.presentation.Hh

class MainActivity : AppCompatActivity() {

    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
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

    private fun checkPermissions() {
        val checkStoragePermissions = checkStoragePermissions()
        launcher.launch(checkStoragePermissions)
    }

    private fun checkStoragePermissions(): Array<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return permissions.toTypedArray()
        }

        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(WRITE_EXTERNAL_STORAGE)
        }

        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(READ_EXTERNAL_STORAGE)
        }

        return permissions.toTypedArray()
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