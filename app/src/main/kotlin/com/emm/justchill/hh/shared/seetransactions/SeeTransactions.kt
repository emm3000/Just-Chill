package com.emm.justchill.hh.shared.seetransactions

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.shared.EditTransaction
import com.emm.justchill.hh.shared.shared.EmmDropDown
import com.emm.justchill.hh.transaction.presentation.TransactionType
import com.emm.justchill.hh.transaction.presentation.TransactionUi
import org.koin.androidx.compose.koinViewModel
import java.util.*

@Composable
fun SeeTransactionsVersionTwo(
    navController: NavController,
    vm: SeeTransactionsViewModel = koinViewModel(),
) {

    val collectAsState: List<TransactionUi> by vm.transactions.collectAsState()
    val accounts: List<Account> by vm.accounts.collectAsState()

    SeeTransactionsVersionTwo(
        transactions = collectAsState,
        accountSelected = vm.accountSelected,
        navigateToEdit = {
            navController.navigate(EditTransaction(it))
        },
        accounts = accounts,
        onAccountChange = vm::updateAccountSelected,
    )
}

@Composable
fun SeeTransactionsVersionTwo(
    transactions: List<TransactionUi> = emptyList(),
    accountSelected: Account? = null,
    navigateToEdit: (String) -> Unit = {},
    accounts: List<Account> = emptyList(),
    onAccountChange: (Account) -> Unit = {},
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
                    text = "Transacciones",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = LatoFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            EmmDropDown(
                textLabel = "Cuentas",
                textPlaceholder = "Seleccionar cuenta",
                items = accounts,
                itemSelected = accountSelected,
                onItemSelected = onAccountChange,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp)
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

@PreviewLightDark
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

@PreviewLightDark
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