package com.emm.justchill.hh.me.driver.presentation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PlaceholderOrLabel
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.core.theme.TextDisableColor
import com.emm.justchill.hh.me.driver.domain.Driver
import com.emm.justchill.hh.auth.presentation.LabelTextField
import com.emm.justchill.hh.me.loan.presentation.LoanItem
import com.emm.justchill.hh.me.loan.presentation.LoanUi
import com.emm.justchill.hh.me.daily.presentation.DailyItem
import com.emm.justchill.hh.me.daily.presentation.DailyUi

@Composable
fun DriverItem(
    driver: Driver,
    loans: List<LoanUi>,
    dailies: List<DailyUi>,
    navigateToSeeDailies: (Long) -> Unit,
    navigateToSeePayments: (String, String) -> Unit,
    navigateToAddLoans: (Long) -> Unit,
    addDaily: (Long, String) -> Unit,
    deleteLoan: (String) -> Unit,
    deleteDaily: (String) -> Unit,
) {

    var showAddDaily: Boolean by remember {
        mutableStateOf(false)
    }

    val sizeInside by animateFloatAsState(
        targetValue = if (showAddDaily) 180f else 0f,
        animationSpec = tween(easing = LinearOutSlowInEasing),
        label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            text = driver.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextColor,
            fontFamily = LatoFontFamily
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {
                    showAddDaily = !showAddDaily
                },
                label = {
                    Text("VER FERIAS", color = TextColor)
                },
                trailingIcon = {
                    Icon(
                        modifier = Modifier
                            .rotate(sizeInside),
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextColor,
                    )
                },
            )
        }

        AnimatedVisibility(showAddDaily) {
            var amountValue: String by remember {
                mutableStateOf("")
            }

            val keyboardController = LocalSoftwareKeyboardController.current
            val context = LocalContext.current

            Column {
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        modifier = Modifier.weight(1f),
                        value = amountValue,
                        onValueChange = { value ->
                            val filter: String = value.filter { it.isDigit() || it == '.' }
                            amountValue = filter
                        },
                        placeholder = {
                            LabelTextField("Ingrese el monto")
                        },
                        label = {
                            LabelTextField("Monto:")
                        },
                        prefix = {
                            Text(
                                text = "S/ ",
                                fontWeight = FontWeight.Normal,
                                fontFamily = LatoFontFamily,
                                color = PlaceholderOrLabel,
                                fontSize = 17.sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextColor
                        ),
                        textStyle = TextStyle(
                            color = TextColor,
                            fontFamily = LatoFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 17.sp
                        )
                    )
                    TextButton(
                        modifier = Modifier
                            .align(Alignment.Bottom),
                        onClick = {
                            addDaily(driver.driverId, amountValue)
                            keyboardController?.hide()
                            amountValue = ""
                            Toast.makeText(
                                context,
                                "Feria agregada para ${driver.name} 😀",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (amountValue.isNotEmpty()) TextColor else TextDisableColor
                        ),
                        enabled = amountValue.isNotEmpty(),
                    ) {
                        Text(
                            text = "Agregar monto",
                            fontSize = 15.sp,
                            color = if (amountValue.isNotEmpty()) TextColor else TextDisableColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LatoFontFamily
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        modifier = Modifier,
                        text = "Últimas ferias agregadas",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor,
                        fontFamily = LatoFontFamily
                    )
                    TextButton(
                        onClick = {
                            navigateToSeeDailies(driver.driverId)
                        },
                        contentPadding = PaddingValues(horizontal = 1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.padding(end = 4.dp),
                                text = "Ver todo",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextColor,
                                fontFamily = LatoFontFamily
                            )
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = TextColor
                            )
                        }
                    }
                }

                dailies.forEach {
                    key(it.dailyId) {
                        DailyItem(it, deleteDaily)
                    }
                }

            }
        }

        var showPayments: Boolean by remember {
            mutableStateOf(true)
        }

        val sizeInsidePayments by animateFloatAsState(
            targetValue = if (showPayments) 180f else 0f,
            animationSpec = tween(easing = LinearOutSlowInEasing),
            label = ""
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {
                    showPayments = !showPayments
                },
                label = {
                    Text("VER PRESTAMOS", color = TextColor)
                },
                trailingIcon = {
                    Icon(
                        modifier = Modifier
                            .rotate(sizeInsidePayments),
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextColor,
                    )
                },
            )
            AssistChip(
                onClick = {
                    navigateToAddLoans(driver.driverId)
                },
                label = {
                    Text("AGREGAR PRESTAMOS", color = TextColor)
                }
            )
        }

        AnimatedVisibility(showPayments) {
            
            Column {
                loans.forEach { loanUi ->
                    key(loanUi.loanId) {
                        LoanItem(
                            loan = loanUi,
                            navigateToPayments = {
                                navigateToSeePayments(it, driver.name)
                            },
                            deleteLoan = {
                                deleteLoan(it)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
    }
}

@Preview
@Composable
fun ItemPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        DriverItem(
            driver = Driver(driverId = 0, name = "Random name"),
            navigateToSeeDailies = {},
            navigateToAddLoans = {},
            addDaily = { _: Long, _: String -> },
            loans = listOf(
                LoanUi(
                    loanId = "1",
                    amount = "S/ 480.00",
                    amountWithInterest = "S/ 480.00",
                    interest = 0,
                    startDate = 0,
                    duration = 0,
                    status = "PENDING",
                    driverId = 0,
                    readableDate = "20 de junio",
                    readableTime = "23:10 am"
                ),
                LoanUi(
                    loanId = "2",
                    amount = "S/ 480.00",
                    amountWithInterest = "S/ 480.00",
                    interest = 0,
                    startDate = 0,
                    duration = 0,
                    status = "PENDING",
                    driverId = 0,
                    readableDate = "20 de junio",
                    readableTime = "23:10 am"
                )
            ),
            navigateToSeePayments = { _, _ -> },
            deleteLoan = {},
            dailies = listOf(
                DailyUi(
                    dailyId = "",
                    amount = "S/ 23.00",
                    dailyDate = 22,
                    driverId = 0,
                    readableTime = "22.22",
                    day = "Miercoles",
                    dayNumber = "22"
                ),
                DailyUi(
                    dailyId = "",
                    amount = "S/ 23.00",
                    dailyDate = 22,
                    driverId = 0,
                    readableTime = "22.22",
                    day = "Miercoles",
                    dayNumber = "22"
                )
            ),
            deleteDaily = {},
        )
    }
}