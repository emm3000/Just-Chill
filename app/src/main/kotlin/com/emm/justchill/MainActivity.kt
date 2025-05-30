package com.emm.justchill

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.backgroundDarkHighContrast
import com.emm.justchill.hh.shared.Hh

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(backgroundDarkHighContrast.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(backgroundDarkHighContrast.toArgb())
        )
        setContent {
            EmmTheme {
                Hh()
            }
        }
    }
}