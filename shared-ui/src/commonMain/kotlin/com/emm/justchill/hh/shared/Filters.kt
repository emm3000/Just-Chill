package com.emm.justchill.hh.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun Filters(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor),
    ) {
    }
}

@Preview
@Composable
private fun FiltersPreview() {
    EmmTheme {
        Filters()
    }
}
