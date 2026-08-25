package com.emm.justchill

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.emm.justchill.hh.shared.AppNavHost

class MainActivity : ComponentActivity() {

    private var shortcutAction by mutableStateOf<String?>(null)
    private var shortcutRequestId by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status + nav bars transparent so the Notion-dark bg (#191919) shows through edge-to-edge.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Only a fresh process consumes the launch intent — a rotation or a process-death restore
        // re-delivers the same intent and would otherwise yank the user back to the shortcut route.
        if (savedInstanceState == null) {
            shortcutAction = intent?.action
            shortcutRequestId++
        }
        // EmmTheme is applied inside AppNavHost (the unified commonMain nav host).
        setContent {
            AppNavHost(
                shortcutAction = shortcutAction,
                shortcutRequestId = shortcutRequestId,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutAction = intent.action
        shortcutRequestId++
    }
}
