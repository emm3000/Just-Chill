package com.emm.justchill.loans.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor

@Composable
fun PaymentsScreen(
    payments: List<PaymentUi>,
) {

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
            items(payments, key = PaymentUi::paymentId) {
                PaymentItem(it)
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

@Preview(showBackground = true)
@Composable
fun PaymentsPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        PaymentsScreen(
            payments = listOf(
            )
        )
    }
}