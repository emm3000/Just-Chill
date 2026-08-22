package com.emm.justchill.hh.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.IconBtnTone
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone

@Composable
fun PersonLoansScreen(
    state: PersonLoansUiState,
    onIntent: (PersonLoansIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        JcTopBar(
            title = state.personName,
            left = { IconBtn(icon = Icons.AutoMirrored.Outlined.ArrowBack, onClick = onBack) },
        )
        Hairline()

        if (state.loans.isEmpty()) {
            PersonLoansEmptyState(personName = state.personName, modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)) {
                items(state.loans, key = { it.loanId }) { loan ->
                    LoanRow(
                        loan = loan,
                        onEditClick = { onIntent(PersonLoansIntent.OnEditLoanClick(loan.loanId)) },
                        onDeleteClick = { onIntent(PersonLoansIntent.OnDeleteClick(loan.loanId)) },
                    )
                }
            }
        }
    }

    state.pendingDelete?.let { loanId ->
        val target = remember(loanId, state.loans) { state.loans.find { it.loanId == loanId } }
        DeleteLoanDialog(
            loan = target,
            onConfirm = { onIntent(PersonLoansIntent.OnDeleteConfirm) },
            onDismiss = { onIntent(PersonLoansIntent.OnDeleteDismiss) },
        )
    }
}

@Composable
private fun LoanRow(loan: LoanRowUi, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val remainingColor = if (loan.isSettled) colors.textTertiary else colors.textPrimary

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = loan.readableLentAt,
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        color = colors.textTertiary,
                    )
                    if (loan.isSettled) {
                        Pill(text = "Liquidado", tone = PillTone.Pos)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = loan.remaining,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = InterFontFamily,
                    color = remainingColor,
                    letterSpacing = (-0.3).sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Prestado ${loan.principal} · Total ${loan.totalDue} · Pagado ${loan.paidSoFar}",
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    color = colors.textSecondary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconBtn(icon = Icons.Outlined.Edit, onClick = onEditClick)
                IconBtn(icon = Icons.Outlined.Delete, tone = IconBtnTone.Danger, onClick = onDeleteClick)
            }
        }
        Hairline()
    }
}

@Composable
private fun PersonLoansEmptyState(personName: String, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Payments,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Sin préstamos con $personName",
            fontSize = 18.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun DeleteLoanDialog(loan: LoanRowUi?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEmmColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Borrar este préstamo?") },
        text = {
            Text(
                "Prestado el ${loan?.readableLentAt.orEmpty()} por ${loan?.principal.orEmpty()}. " +
                    "Se borra junto con sus abonos registrados.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Borrar", color = colors.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Preview
@Composable
private fun PersonLoansScreenPreview() {
    EmmTheme {
        PersonLoansScreen(
            state = PersonLoansUiState(
                personName = "Juan",
                loans = listOf(
                    LoanRowUi(
                        loanId = "1",
                        principal = "S/ 200.00",
                        totalDue = "S/ 210.00",
                        paidSoFar = "S/ 50.00",
                        remaining = "S/ 160.00",
                        isSettled = false,
                        readableLentAt = "3 de julio",
                    ),
                    LoanRowUi(
                        loanId = "2",
                        principal = "S/ 100.00",
                        totalDue = "S/ 100.00",
                        paidSoFar = "S/ 100.00",
                        remaining = "S/ 0.00",
                        isSettled = true,
                        readableLentAt = "12 de mayo",
                    ),
                ),
            ),
            onIntent = {},
            onBack = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun PersonLoansScreenEmptyPreview() {
    EmmTheme {
        PersonLoansScreen(
            state = PersonLoansUiState(personName = "Juan"),
            onIntent = {},
            onBack = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
