package com.emm.justchill.hh.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.seetransactions.ItemTransaction
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.transaction.CategoryUi
import com.emm.justchill.hh.transaction.TransactionUi
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = koinViewModel(),
    navigateToAll: () -> Unit = {}
) {

    val homeUiState: HomeUiState by homeViewModel.state.collectAsStateWithLifecycle()

    HomeScreen(homeData = homeUiState, navigateToAll = navigateToAll)
}

@Composable
fun HomeScreen(homeData: HomeUiState, navigateToAll: () -> Unit = {}) {

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        item {
            TotalBalance(homeData.balance)
        }

        item {
            LastMovement(homeData.income, homeData.spend)
        }

        item {
            LastTransactionsLabels { navigateToAll() }
        }

        if (homeData.lastTransactions.isEmpty()) {
            item { NoTransactions() }
        } else {
            items(homeData.lastTransactions, TransactionUi::transactionId) {
                ItemTransaction(it) {
                }
            }
        }
    }
}

@Composable
private fun TotalBalance(totalBalance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Balance Total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFontFamily,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "S/ ${fromCentsToSolesWith(totalBalance)}",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontFamily = LatoFontFamily,
            )
        }
    }
}

@Composable
private fun LastMovement(incomeThisMonth: Double, expensesThisMonth: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Movimientos del Mes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFontFamily,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ingresos",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = LatoFontFamily,
                    )
                    Text(
                        text = "S/ ${fromCentsToSolesWith(incomeThisMonth)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontFamily = LatoFontFamily,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Gastos",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = LatoFontFamily,
                    )
                    Text(
                        text = "S/ ${fromCentsToSolesWith(expensesThisMonth)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = LatoFontFamily,
                    )
                }
            }
        }
    }
}

@Composable
private fun LastTransactionsLabels(onViewAllTransactionsClick: () -> Unit) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Últimas Transacciones",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onViewAllTransactionsClick) {
            Text(
                text = "Ver Todas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ver todas las transacciones",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun NoTransactions() {
    Text(
        text = "No hay transacciones recientes.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    EmmTheme {
        HomeScreen(
            HomeUiState(
                lastTransactions = listOf(
                    TransactionUi(
                        transactionId = "metus",
                        type = TransactionType.Income,
                        amount = "fabellas",
                        description = "nulla",
                        date = 9914,
                        readableDate = "commune",
                        readableTime = "adolescens",
                        category = CategoryUi(
                            categoryIcon = Icons.Rounded.Category,
                            categoryColor = findById("gray")
                        )
                    ),
                ),
                income = 6.7,
                spend = 8.9,
                balance = 10.11
            )
        )
    }
}