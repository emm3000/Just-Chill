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
import com.emm.justchill.hh.shared.EXTRA_ACCOUNT_ID
import com.emm.justchill.hh.shared.EXTRA_CATEGORY_ID
import com.emm.justchill.hh.shared.EXTRA_TYPE
import com.emm.justchill.hh.shared.ShortcutIntent

class MainActivity : ComponentActivity() {

    private var shortcut by mutableStateOf(ShortcutIntent())
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
            shortcut = intent.toShortcutIntent()
            shortcutRequestId++
        }
        // EmmTheme is applied inside AppNavHost, not here.
        setContent {
            AppNavHost(
                shortcut = shortcut,
                shortcutRequestId = shortcutRequestId,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcut = intent.toShortcutIntent()
        shortcutRequestId++
    }

    // MainActivity is exported (it is the launcher activity), so any app on the device can start it
    // with arbitrary extras — read only plain strings here, never a nav-runtime or :domain type.
    private fun Intent?.toShortcutIntent(): ShortcutIntent = ShortcutIntent(
        action = this?.action,
        accountId = this?.getStringExtra(EXTRA_ACCOUNT_ID),
        categoryId = this?.getStringExtra(EXTRA_CATEGORY_ID),
        type = this?.getStringExtra(EXTRA_TYPE),
    )
}
