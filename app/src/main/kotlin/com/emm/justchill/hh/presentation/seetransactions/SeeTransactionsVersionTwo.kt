package com.emm.justchill.hh.presentation.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.domain.TransactionType
import com.emm.justchill.hh.presentation.EditTransaction
import com.emm.justchill.hh.presentation.transaction.TransactionUi
import org.koin.androidx.compose.koinViewModel
import java.util.*

@Composable
fun SeeTransactionsVersionTwo(
    navController: NavController,
    vm: SeeTransactionsVersionTwoViewModel = koinViewModel(),
) {

    val collectAsState: List<TransactionUi> by vm.transactions.collectAsState()

    SeeTransactionsVersionTwo(
        transactions = collectAsState,
        navigateToEdit = {
            navController.navigate(EditTransaction(it))
        }
    )
}

@Composable
fun SeeTransactionsVersionTwo(
    transactions: List<TransactionUi> = emptyList(),
    navigateToEdit: (String) -> Unit = {},
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentPadding = PaddingValues(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        item {
            Text(
                text = "Listas",
                color = TextColor,
                fontFamily = LatoFontFamily,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )
        }

        if (transactions.isNotEmpty()) {
            items(transactions, key = TransactionUi::transactionId) {
                ItemTransaction(it, navigateToEdit)
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

@Preview(showBackground = true)
@Composable
fun ItemPreviewVersionTwo(modifier: Modifier = Modifier) {
    EmmTheme {
        ItemTransaction(
            TransactionUi(
                transactionId = UUID.randomUUID().toString(),
                type = TransactionType.INCOME,
                amount = "2000",
                description = "gaa asocinas coinas ocinasoc nasco nas coias coñ",
                date = 0,
                readableDate = "20 de abril",
                readableTime = "00:00 am",
            )
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
fun SeeTransactionsVersionTwoPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        val xx = remember {
            (0..15).map {
                TransactionUi(
                    transactionId = UUID.randomUUID().toString(),
                    type = TransactionType.INCOME,
                    amount = "2000",
                    description = "gaa asocinas coinas ocinasoc nasco nas coias coñ",
                    date = 0,
                    readableDate = "20 de abril",
                    readableTime = "00:00 am",
                )
            }
        }
        SeeTransactionsVersionTwo(
            transactions = xx
        )
    }
}