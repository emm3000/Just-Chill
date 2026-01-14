package com.emm.justchill.hh.transaction.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily

@Composable
fun NewButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    title: String,
) {

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(50.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
private fun NewButtonPreview() {
    EmmTheme {
        NewButton(
            title = "random button"
        )
    }
}