package com.emm.justchill.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.modalScreenInsets(): Modifier = this
    .statusBarsPadding()
    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
