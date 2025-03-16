package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.shared.shared.EditTransactionRoute
import com.emm.justchill.hh.transaction.EmmCenteredToolbar
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.transaction.TransactionUi
import org.koin.androidx.compose.koinViewModel
import java.util.*

@Composable
fun SeeTransactionsVersionTwo(
    navController: NavController,
    vm: SeeTransactionsViewModel = koinViewModel(),
) {

    val collectAsState: List<TransactionUi> by vm.transactions.collectAsStateWithLifecycle()

    SeeTransactionsVersionTwo(
        transactions = collectAsState,
        navigateToEdit = {
            navController.navigate(EditTransactionRoute(it))
        },
    )
}

@Composable
fun SeeTransactionsVersionTwo(
    transactions: List<TransactionUi> = emptyList(),
    navigateToEdit: (String) -> Unit = {},
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        EmmCenteredToolbar(
            title = "Transacciones",
            modifier = Modifier.fillMaxWidth()
                .padding(bottom = 10.dp),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

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

}

@PreviewLightDark
@Composable
fun ItemPreviewVersionTwo() {
    EmmTheme {
        ItemTransaction(
            TransactionUi(
                transactionId = UUID.randomUUID().toString(),
                type = TransactionType.Income,
                amount = "2000",
                description = "gaa asocinas coinas ocinasoc nasco nas coias coñ",
                date = 0,
                readableDate = "20 de abril",
                readableTime = "00:00 am",
            )
        ) {}
    }
}

@PreviewLightDark
@Composable
fun SeeTransactionsVersionTwoPreview() {
    EmmTheme {
        val xx = remember {
            (0..15).map {
                TransactionUi(
                    transactionId = UUID.randomUUID().toString(),
                    type = TransactionType.Income,
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