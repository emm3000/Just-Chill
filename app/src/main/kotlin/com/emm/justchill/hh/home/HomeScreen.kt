package com.emm.justchill.hh.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .statusBarsPadding(),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TotalBalance(homeData.balance)
                LastMovement(homeData.income, homeData.spend)
                LastTransactionsLabels { navigateToAll() }
            }
        }

        if (homeData.lastTransactions.isEmpty()) {
            item { NoTransactions() }
        } else {
            items(homeData.lastTransactions, TransactionUi::transactionId) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ItemTransaction(it) {
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalBalance(totalBalance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Balance Total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                fontFamily = LatoFontFamily,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "S/ ${fromCentsToSolesWith(totalBalance)}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontFamily = LatoFontFamily,
            )
        }
    }
}

@Composable
private fun LastMovement(incomeThisMonth: Double, expensesThisMonth: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MovementCard(
            modifier = Modifier.weight(1f),
            label = "Ingresos",
            amount = incomeThisMonth,
            color = MaterialTheme.colorScheme.tertiary,
            icon = Icons.Rounded.ArrowUpward
        )
        MovementCard(
            modifier = Modifier.weight(1f),
            label = "Gastos",
            amount = expensesThisMonth,
            color = MaterialTheme.colorScheme.error,
            icon = Icons.Rounded.ArrowDownward
        )
    }
}

@Composable
private fun MovementCard(
    modifier: Modifier = Modifier,
    label: String,
    amount: Double,
    color: Color,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
            
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "S/ ${fromCentsToSolesWith(amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = LatoFontFamily,
                )
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
            text = "Transacciones recientes",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Black
        )
        TextButton(onClick = onViewAllTransactionsClick) {
            Text(
                text = "Ver Todas",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ver todas las transacciones",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun NoTransactions() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                modifier = Modifier.padding(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        Text(
            text = "No hay transacciones recientes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontFamily = LatoFontFamily
        )
    }
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