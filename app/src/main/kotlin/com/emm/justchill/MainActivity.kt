package com.emm.justchill

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.domain.BackupManager
import com.emm.justchill.hh.presentation.Hh
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class MainActivity : AppCompatActivity() {

    private val defaultBackupManager: BackupManager by inject(named("supabase"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        populate()
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

    private fun populate() = lifecycleScope.launch {
        defaultBackupManager.seed()
    }
}

