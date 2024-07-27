package com.emm.justchill.loans.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme

@Composable
fun Payments(modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(horizontal = 20.dp, vertical = 30.dp)
    ) {

    }
}

@Preview(showBackground = true)
@Composable
fun PaymentsPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Payments()
    }
}