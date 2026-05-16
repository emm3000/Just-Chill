package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.transaction.CategoryUi
import com.emm.justchill.hh.transaction.TransactionUi
import com.emm.justchill.hh.transaction.components.EmmCenteredToolbar
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.util.UUID

@Composable
fun SeeTransactionsScreen(
    onEditTransaction: (String) -> Unit,
    vm: SeeTransactionsViewModel = koinViewModel(),
) {

    val collectAsState: List<DayGroup> by vm.transactions.collectAsStateWithLifecycle()

    SeeTransactionsScreen(
        transactions = collectAsState,
        navigateToEdit = onEditTransaction,
    )
}

@Composable
fun SeeTransactionsScreen(
    transactions: List<DayGroup> = emptyList(),
    navigateToEdit: (String) -> Unit = {},
) {

    val listState = rememberLazyListState()

    LaunchedEffect(transactions.size) {
        listState.scrollToItem(0)
    }

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
            state = listState,
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            if (transactions.isNotEmpty()) {
                transactions.forEach { dayGroup ->
                    item {
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(start = 10.dp),
                            text = dayGroup.readableDate,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LatoFontFamily,
                        )
                    }
                    items(dayGroup.transactions, key = TransactionUi::transactionId) {
                        TransactionItem(transactionUi = it)
                    }
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
                category = CategoryUi(
                    categoryIcon = Icons.Rounded.Category,
                    categoryColor = findById("gray")
                )
            )
        ) {}
    }
}

@PreviewLightDark
@Composable
fun SeeTransactionsVersionTwoPreview() {
    EmmTheme {
        val xx: List<TransactionUi> = remember {
            (0..15).map {
                TransactionUi(
                    transactionId = UUID.randomUUID().toString(),
                    type = TransactionType.Income,
                    amount = "2000",
                    description = "gaa asocinas coinas ocinasoc nasco nas coias coñ",
                    date = 0,
                    readableDate = "20 de abril",
                    readableTime = "00:00 am",
                    category = CategoryUi(
                        categoryIcon = Icons.Rounded.Category,
                        categoryColor = findById("gray")
                    )
                )
            }
        }
        val w = remember {
            listOf(
                DayGroup(
                    date = LocalDate.now(),
                    transactions = xx
                )
            )

        }
        SeeTransactionsScreen(
            transactions = w
        )
    }
}