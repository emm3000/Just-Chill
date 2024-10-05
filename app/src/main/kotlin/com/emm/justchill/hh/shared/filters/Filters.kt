package com.emm.justchill.hh.shared.filters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme

@Composable
fun Filters() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

    }

}

@Preview(showBackground = true)
@Composable
fun FiltersPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Filters()
    }
}