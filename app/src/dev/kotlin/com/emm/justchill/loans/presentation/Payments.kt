package com.emm.justchill.loans.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.components.ContainerWithLoading
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor

data class Payments(val name: String)

@Composable
fun Payments(
    modifier: Modifier = Modifier,
    payments: List<Payments>,
) {

    ContainerWithLoading(isLoading = false) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor),
            contentPadding = PaddingValues(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        modifier = Modifier,
                        text = "Pagos",
                        color = TextColor,
                        fontFamily = LatoFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (payments.isNotEmpty()) {
                items(payments, key = Payments::name) {
                    ItemPayment()
                }
            } else {
                item {
                    Text(
                        text = "No tienes pagos en este prestamo",
                        modifier = Modifier.padding(top = 20.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.LightGray.copy(alpha = 0.8f)
                    )
                }
            }

        }
    }
}

@Composable
fun ItemPayment() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .clickable {
            }
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "random description",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    fontFamily = LatoFontFamily
                )
                Text(
                    modifier = Modifier,
                    text = "amount ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = TextUnit(1f, TextUnitType.Em),
                    color = TextColor.copy(alpha = .8f),
                    fontFamily = LatoFontFamily
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 13.dp),
                text = "amount",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = LatoFontFamily
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
    }

}

@Preview
@Composable
fun ItemPaymentPreview() {
    EmmTheme {
        ItemPayment()
    }
}

@Preview(showBackground = true)
@Composable
fun PaymentsPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Payments(
            payments = listOf(
                Payments("gaa"),
                Payments("f"),
            )
        )
    }
}