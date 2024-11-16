package com.emm.justchill.hh.fasttransaction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily

@Composable
fun EmmBoldText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onBackground,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        textAlign = TextAlign.Start,
        modifier = modifier
    )
}

@PreviewLightDark
@Composable
private fun EmmBoldTextPreview() {
    EmmTheme {
        Surface {
            EmmBoldText(
                text = "Ingrese monto",
                modifier = Modifier
            )
        }
    }
}