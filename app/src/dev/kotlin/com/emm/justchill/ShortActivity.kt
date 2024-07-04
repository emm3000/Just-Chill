package com.emm.justchill

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.experiences.Root

class ShortActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EmmTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Root()
                }
            }
        }
    }
}

