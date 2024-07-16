package com.emm.justchill.hh.presentation.seetransactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.emm.justchill.core.Result
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.domain.TransactionType
import com.emm.justchill.hh.presentation.transaction.TransactionUi
import org.koin.androidx.compose.koinViewModel
import java.util.*

@Composable
fun SeeTransactions(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    vm: SeeTransactionsViewModel = koinViewModel(),
) {

    val collectAsState = vm.transactions.collectAsState()

    SeeTransactions(
        transactions = collectAsState.value,
        firstDataHolder = vm.holderForStartDate,
        secondDataHolder = vm.holderForEndDate,
        updateFirst = vm::updateDataHolder,
        updateSecond = vm::updateDataHolder2
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeTransactions(
    transactions: Result<List<TransactionUi>> = Result.Success(emptyList()),
    firstDataHolder: DateDataHolder = DateDataHolder(),
    secondDataHolder: DateDataHolder = DateDataHolder(),
    updateFirst: (Long?) -> Unit = {},
    updateSecond: (Long?) -> Unit = {},
) {

    val datePickerState: DatePickerState = rememberDatePickerState()
    val datePickerState2: DatePickerState = rememberDatePickerState()

    LaunchedEffect(datePickerState.selectedDateMillis) {
        updateFirst(datePickerState.selectedDateMillis)
    }

    LaunchedEffect(datePickerState2.selectedDateMillis) {
        updateSecond(datePickerState2.selectedDateMillis)
    }

    val (showSelectDate, setShowSelectDate) = remember {
        mutableStateOf(false)
    }

    val (showSelectDate2, setShowSelectDate2) = remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Listas", modifier = Modifier.padding(vertical = 20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(onClick = { setShowSelectDate(true) }) {
                    Text(text = "Desde")
                }
                if (firstDataHolder.readableDate.isNotEmpty()) {
                    Text(text = firstDataHolder.readableDate)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedButton(onClick = { setShowSelectDate2(true) }) {
                    Text(text = "Hasta")
                }
                if (secondDataHolder.readableDate.isNotEmpty()) {
                    Text(text = secondDataHolder.readableDate)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(10.dp),
        ) {
            if (transactions is Result.Success && transactions.data.isNotEmpty()) {
                items(transactions.data) {
                    ItemTransaction(it)
                }
            } else {
                item {
                    Text(
                        text = "No tienes transacciones",
                        modifier = Modifier.padding(top = 20.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.LightGray.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
    Calendar(showSelectDate, setShowSelectDate, datePickerState)
    Calendar(showSelectDate2, setShowSelectDate2, datePickerState2)
}

@Composable
fun ItemTransaction(transactionUi: TransactionUi) {

    val borderColor = when (transactionUi.type) {
        TransactionType.INCOME -> Color.Unspecified
        TransactionType.SPENT -> Color.Red
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {  }
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = transactionUi.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier,
                    text = transactionUi.readableDate,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Light,
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                text = transactionUi.amount,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = borderColor
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
    }

}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Calendar(
    showSelectDate: Boolean,
    setShowSelectDate: (Boolean) -> Unit,
    datePickerState: DatePickerState
) {
    if (showSelectDate) {
        DatePickerDialog(
            onDismissRequest = {
                setShowSelectDate(false)
            },
            confirmButton = {
                OutlinedButton(onClick = {
                    setShowSelectDate(false)
                }) {
                    Text(text = "Ok")
                }
            },
            dismissButton = {
                Button(onClick = { setShowSelectDate(false) }) {
                    Text(text = "Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ItemPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        ItemTransaction(
            TransactionUi(
                transactionId = UUID.randomUUID().toString(),
                type = TransactionType.INCOME,
                amount = "2000",
                description = "gaa",
                date = 0,
                readableDate = "20 de abril"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SeeTransactionsPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        SeeTransactions()
    }
}