package com.emm.justchill.hh.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.account.AccountType
import com.emm.domain.account.Currency
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddAccountScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    vm: AddAccountViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AddAccountEffect.AccountSaved -> onBack()
                is AddAccountEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AddAccountContent(
        state = state,
        onIntent = vm::onIntent,
        onBack = onBack,
    )
}

@Composable
private fun AddAccountContent(
    state: AddAccountUiState,
    onIntent: (AddAccountIntent) -> Unit,
    onBack: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding(),
    ) {

        TopBar(title = "Nueva cuenta", onClose = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = spacing.s4),
        ) {

            Spacer(Modifier.height(spacing.s6))

            Text(
                text = "Crea una nueva cuenta para organizar tus transacciones.",
                style = type.bodyM,
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(spacing.s8))

            EmmTextInput(
                value = state.name,
                onValueChange = { onIntent(AddAccountIntent.OnNameChange(it)) },
                label = "NOMBRE",
                placeholder = "ejm. Gasto diario",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(spacing.s4))

            AccountTypeDropdown(
                selected = state.selectedType,
                onSelect = { onIntent(AddAccountIntent.OnTypeChange(it)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(spacing.s4))

            CurrencyDropdown(
                selected = state.selectedCurrency,
                onSelect = { onIntent(AddAccountIntent.OnCurrencyChange(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        EmmButton(
            text = "Crear cuenta",
            onClick = { onIntent(AddAccountIntent.OnSave) },
            enabled = state.isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun TopBar(title: String, onClose: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClose,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Cerrar",
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = title,
            style = type.titleL,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTypeDropdown(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selected) {
        AccountType.Bank -> "Banco"
        AccountType.Cash -> "Efectivo"
        AccountType.CreditCard -> "Tarjeta de crédito"
        AccountType.Investment -> "Inversión"
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("TIPO DE CUENTA") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AccountType.entries.forEach { type ->
                val name = when (type) {
                    AccountType.Bank -> "Banco"
                    AccountType.Cash -> "Efectivo"
                    AccountType.CreditCard -> "Tarjeta de crédito"
                    AccountType.Investment -> "Inversión"
                }
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(type); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selected: Currency,
    onSelect: (Currency) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = "${selected.name} (${selected.symbol})",
            onValueChange = {},
            readOnly = true,
            label = { Text("MONEDA") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Currency.entries.forEach { currency ->
                DropdownMenuItem(
                    text = { Text("${currency.name} (${currency.symbol})") },
                    onClick = { onSelect(currency); expanded = false },
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun AddAccountScreenPreview() {
    EmmTheme {
        AddAccountContent(
            state = AddAccountUiState(name = "Gasto diario", isEnabled = true),
            onIntent = {},
        )
    }
}
