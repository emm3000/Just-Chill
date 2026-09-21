package com.emm.justchill.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.justchill.core.domain.account.AccountType
import com.emm.justchill.core.ui.atoms.BackBtn
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.UnderlineTextField
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import org.koin.compose.viewmodel.koinViewModel

// Every account-type tile shares this one height and no EmmSpacing step is 52dp, so it cannot become a token.
private val TypeTileHeight: Dp = 52.dp

@Composable
fun AddAccountScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    vm: AddAccountViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AddAccountEffect.AccountSaved -> currentOnBack()

                is AddAccountEffect.ShowError -> snackbarHostState.showEmmSnackbar(
                    message = effect.message,
                    tone = EmmSnackbarTone.Error,
                )
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
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        JcTopBar(
            title = "Nueva cuenta",
            left = { BackBtn(onClick = onBack) },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s5),
        ) {
            Spacer(Modifier.height(spacing.s1))

            Section(eyebrow = "ATAJOS PERUANOS") {
                ShortcutsRow(
                    selectedName = state.name,
                    onSelect = { shortcut ->
                        onIntent(AddAccountIntent.OnNameChange(shortcut.label))
                        onIntent(AddAccountIntent.OnTypeChange(shortcut.type))
                    },
                )
            }

            Section(eyebrow = "NOMBRE") {
                UnderlineTextField(
                    value = state.name,
                    onValueChange = { onIntent(AddAccountIntent.OnNameChange(it)) },
                    placeholder = "ejm. Yape",
                )
            }

            Section(eyebrow = "TIPO") {
                TypeGrid(
                    selected = state.selectedType,
                    onSelect = { onIntent(AddAccountIntent.OnTypeChange(it)) },
                )
            }

            Spacer(Modifier.height(spacing.s2))
        }

        StickyCTA(
            label = "Crear cuenta",
            interaction = if (state.isEnabled) CtaInteraction.Enabled else CtaInteraction.Disabled,
            onClick = { onIntent(AddAccountIntent.OnSave) },
        )
    }
}

@Composable
private fun Section(eyebrow: String, content: @Composable () -> Unit) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s3)) {
        Eyebrow(text = eyebrow)
        content()
    }
}

private data class Shortcut(val label: String, val type: AccountType, val dotPicker: (EmmColors) -> Color)

private val PERUVIAN_SHORTCUTS = listOf(
    Shortcut("Yape", AccountType.Wallet) { it.catMauve },
    Shortcut("Plin", AccountType.Wallet) { it.catSage },
    Shortcut("BCP", AccountType.Bank) { it.catSlate },
    Shortcut("BBVA", AccountType.Bank) { it.catTerracotta },
    Shortcut("Interbank", AccountType.Bank) { it.catOchre },
    Shortcut("Scotiabank", AccountType.Bank) { it.catMauve },
    Shortcut("Efectivo", AccountType.Cash) { it.catOchre },
)

@Composable
private fun ShortcutsRow(selectedName: String, onSelect: (Shortcut) -> Unit) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        items(PERUVIAN_SHORTCUTS.size) { idx ->
            val s = PERUVIAN_SHORTCUTS[idx]
            ShortcutChip(
                label = s.label,
                dotColor = s.dotPicker(LocalEmmColors.current),
                selected = selectedName == s.label,
                onClick = { onSelect(s) },
            )
        }
    }
}

@Composable
private fun ShortcutChip(label: String, dotColor: Color, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val type: EmmType = LocalEmmType.current
    val shape: RoundedCornerShape = radii.rFull

    val bgColor = if (selected) colors.surface3 else colors.surface1
    val borderColor: Color = if (selected) colors.borderFocus else colors.border
    val textColor = if (selected) colors.textPrimary else colors.textSecondary
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(spacing.s12)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .border(spacing.hairline, borderColor, shape)
                .indication(interactionSource, ripple())
                .padding(horizontal = spacing.s3, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.s2)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = label,
                style = type.labelL.copy(fontWeight = if (selected) FontWeight.W600 else FontWeight.W500),
                color = textColor,
            )
        }
    }
}

private data class TypeOption(val label: String, val type: AccountType, val icon: ImageVector)

private val TYPE_OPTIONS = listOf(
    TypeOption("Billetera", AccountType.Wallet, Icons.Outlined.AccountBalanceWallet),
    TypeOption("Banco", AccountType.Bank, Icons.Outlined.AccountBalance),
    TypeOption("Tarjeta", AccountType.CreditCard, Icons.Outlined.CreditCard),
    TypeOption("Efectivo", AccountType.Cash, Icons.Outlined.Payments),
)

@Composable
private fun TypeGrid(selected: AccountType, onSelect: (AccountType) -> Unit) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        TYPE_OPTIONS.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                pair.forEach { option ->
                    TypeCell(
                        label = option.label,
                        icon = option.icon,
                        selected = selected == option.type,
                        onClick = { onSelect(option.type) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeCell(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val type: EmmType = LocalEmmType.current
    val shape: RoundedCornerShape = radii.rM

    val borderColor: Color = if (selected) colors.borderFocus else colors.border
    val bgColor = if (selected) colors.surface3 else colors.surface1
    val tint = if (selected) colors.textPrimary else colors.textSecondary

    Row(
        modifier = modifier
            .height(TypeTileHeight)
            .clip(shape)
            .background(bgColor)
            .border(spacing.hairline, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(spacing.s5),
        )
        Text(
            text = label,
            style = type.titleM.copy(fontWeight = if (selected) FontWeight.W600 else FontWeight.W500),
            color = tint,
        )
    }
}

@Preview
@Composable
private fun AddAccountScreenPreview() {
    EmmTheme {
        AddAccountContent(
            state = AddAccountUiState(
                name = "Yape",
                selectedType = AccountType.Wallet,
                isEnabled = true,
            ),
            onIntent = {},
        )
    }
}
