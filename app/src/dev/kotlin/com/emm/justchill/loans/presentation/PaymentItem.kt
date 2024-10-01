package com.emm.justchill.loans.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.loans.domain.PaymentStatus

@Composable
fun PaymentItem(
    payment: PaymentUi,
    markPay: (Boolean, String) -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = payment.day,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    fontFamily = LatoFontFamily
                )
                Text(
                    text = payment.dayNumber,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    fontFamily = LatoFontFamily
                )
            }

            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier,
                    text = payment.amount,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LatoFontFamily,
                    color = TextColor,
                    textDecoration = when (payment.status) {
                        PaymentStatus.PAID -> TextDecoration.LineThrough
                        else -> null
                    }
                )
                Checkbox(
                    checked = when (payment.status) {
                        PaymentStatus.PAID -> true
                        else -> false
                    },
                    onCheckedChange = {
                        markPay(it, payment.paymentId)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryButtonColor,
                        uncheckedColor = Color.Gray
                    )
                )
            }

        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
    }

}

@Preview(showBackground = true)
@Composable
fun Preview(modifier: Modifier = Modifier) {
    EmmTheme {
        PaymentItem(
            payment = PaymentUi(
                paymentId = "",
                loanId = "",
                dueDate = 0,
                amount = "0.0",
                status = PaymentStatus.PENDING,
                day = "2",
                dayNumber = "2"
            ),
            markPay = { a, b ->

            }
        )
    }
}