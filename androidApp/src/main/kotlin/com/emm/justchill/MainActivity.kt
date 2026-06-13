package com.emm.justchill

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.shared.Hh

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status + nav bars transparent so the Notion-dark bg (#191919) shows through edge-to-edge.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            EmmTheme {
                Hh()
            }
        }
    }
}
