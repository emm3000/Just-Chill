package com.emm.justchill.hh.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme

@Composable
fun EmmHeadlineMedium(
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    modifier: Modifier = Modifier,
) {

    Text(
        text = text,
        modifier = modifier,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
}

@Composable
fun EmmHeadlineMediumLight(
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    modifier: Modifier = Modifier,
) {

    Text(
        text = text,
        modifier = modifier,
        fontSize = 28.sp,
        fontWeight = FontWeight.Light,
        color = textColor
    )
}

@Composable
fun EmmHeading(
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    modifier: Modifier = Modifier
) {

    Text(
        text = text,
        modifier = modifier,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
}

@PreviewLightDark
@Composable
private fun EmmTextStylesPreview() {

    EmmTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmmHeadlineMedium(
                    text = "Random EmmHeadlineMedium"
                )
                EmmHeading(
                    text = "Random EmmHeading"
                )
                EmmHeadlineMediumLight(
                    text = "Random EmmHeadlineMediumLight"
                )
            }
        }
    }
}