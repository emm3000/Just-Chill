package com.emm.justchill.quota

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.emm.justchill.hh.presentation.auth.LabelTextField
import com.emm.justchill.loans.presentation.LoanItem
import com.emm.justchill.loans.presentation.LoanUi
import com.emm.justchill.quota.domain.Driver

@Composable
fun DriverItem(
    driver: Driver,
    loans: List<LoanUi>,
    navigateToSeeQuotas: (Long) -> Unit,
    navigateToSeePayments: (String, String) -> Unit,
    navigateToAddLoans: (Long) -> Unit,
    addQuota: (Long, String) -> Unit,
) {

    var showAddQuota: Boolean by remember {
        mutableStateOf(false)
    }

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
                    showAddQuota = true
                },
                label = {
                    Text("AGREGAR FERIA 👍", color = TextColor)
                }
            )
            AssistChip(
                onClick = {
                    navigateToSeeQuotas(driver.driverId)
                },
                label = {
                    Text("VER FERIAS 😇", color = TextColor)
                }
            )
        }

        if (showAddQuota) {

            var amountValue: String by remember {
                mutableStateOf("")
            }

            val keyboardController = LocalSoftwareKeyboardController.current
            val context = LocalContext.current

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = amountValue,
                onValueChange = { value ->
                    val filter: String = value.filter { it.isDigit() || it == '.' }
                    amountValue = filter
                },
                placeholder = {
                    LabelTextField("Ingrese la cantidad 😎")
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
                trailingIcon = {
                    Row {
                        IconButton({
                            showAddQuota = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = TextColor
                            )
                        }
                        IconButton(
                            onClick = {
                                addQuota(driver.driverId, amountValue)
                                keyboardController?.hide()
                                showAddQuota = false
                                Toast.makeText(
                                    context,
                                    "Feria agregada para ${driver.name} 😀",
                                    Toast.LENGTH_LONG
                                ).show()

                            },
                            enabled = amountValue.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = TextColor
                            )
                        }
                    }
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
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {
                    navigateToAddLoans(driver.driverId)
                },
                label = {
                    Text("AGREGAR PRESTAMOS 💵💲\uD83D\uDCB2\uD83D\uDCB2", color = TextColor)
                }
            )
        }

        loans.forEach { loanUi ->
            LoanItem(loanUi) {
                navigateToSeePayments(it, driver.name)
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
            navigateToSeeQuotas = {},
            navigateToAddLoans = {},
            addQuota = { _: Long, _: String -> },
            loans = listOf(),
            navigateToSeePayments = { a, b -> },
        )
    }
}