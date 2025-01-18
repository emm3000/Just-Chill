package com.emm.justchill.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.HhBackgroundColor
import com.emm.justchill.core.theme.HhCardBackground

@Composable
fun HhCard(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    paddingContent: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit,
) {

    Card(
        modifier = modifier
            .background(HhBackgroundColor),
        colors = CardDefaults.cardColors(
            containerColor = HhCardBackground,
        ),
        shape = RoundedCornerShape(4)
    ) {
        Column(
            modifier = Modifier.padding(paddingContent),
            verticalArrangement = verticalArrangement,
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HhCardPreview() {
    EmmTheme {
        HhCard(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            paddingContent = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            HhPrimaryButton(
                text = "gaa askc nmaslc naslk n",
                isEnabled = true,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )
            HhPrimaryButton(
                text = "gaa askc nmaslc naslk n",
                isEnabled = true,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )
            HhPrimaryButton(
                text = "gaa askc nmaslc naslk n",
                isEnabled = true,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}