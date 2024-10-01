package com.emm.justchill.loans.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoanItem(
    loan: LoanUi,
    navigateToPayments: (String) -> Unit,
    deleteLoan: (String) -> Unit,
) {

    var showOptions: Boolean by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .combinedClickable(
                onClick = {
                    navigateToPayments(loan.loanId)
                },
                onLongClick = {
                    showOptions = true
                },
            )
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = loan.status,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    fontFamily = LatoFontFamily
                )
                Text(
                    modifier = Modifier,
                    text = "${loan.readableDate}, ${loan.readableTime}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = TextUnit(1f, TextUnitType.Em),
                    color = TextColor.copy(alpha = .8f),
                    fontFamily = LatoFontFamily
                )
            }
            Column(
                modifier = Modifier.align(Alignment.CenterVertically),
                horizontalAlignment = AbsoluteAlignment.Right
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 10.dp),
                    text = "Prestamo: ${loan.amount}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    fontFamily = LatoFontFamily
                )
                Text(
                    modifier = Modifier
                        .padding(start = 10.dp),
                    text = "Total a pagar: ${loan.amountWithInterest}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    fontFamily = LatoFontFamily
                )
            }
        }

        AnimatedVisibility(showOptions) {
            Row {
                TextButton(onClick = {
                    deleteLoan(loan.loanId)
                }) {
                    Text(
                        text = "Eliminar prestamo",
                        style = TextStyle(
                            fontFamily = LatoFontFamily,
                            color = DeleteButtonColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
                TextButton(onClick = {
                    showOptions = false
                }) {
                    Text(
                        text = "Cancelar",
                        style = TextStyle(
                            fontFamily = LatoFontFamily,
                            color = TextColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
    }
}

@Preview(showBackground = true)
@Composable
fun LoanItemPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        LoanItem(
            loan = LoanUi(
                loanId = "",
                amount = "22.22",
                amountWithInterest = "23.24",
                interest = 0,
                startDate = 0,
                duration = 0,
                status = "pending",
                driverId = 0,
                readableDate = "",
                readableTime = ""
            ), navigateToPayments = {}
        ) { }
    }
}