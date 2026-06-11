package com.emm.justchill.hh.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.account.AccountType
import com.emm.justchill.core.theme.EmmColors
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import org.koin.androidx.compose.koinViewModel

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        JcTopBar(
            title = "Nueva cuenta",
            left = {
                IconBtn(
                    icon = Icons.Outlined.Close,
                    onClick = onBack,
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

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
                NameInput(
                    value = state.name,
                    onValueChange = { onIntent(AddAccountIntent.OnNameChange(it)) },
                )
            }

            Section(eyebrow = "TIPO") {
                TypeGrid(
                    selected = state.selectedType,
                    onSelect = { onIntent(AddAccountIntent.OnTypeChange(it)) },
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        StickyCTA(
            label = "Crear cuenta",
            tone = CtaTone.Accent,
            interaction = if (state.isEnabled) CtaInteraction.Enabled else CtaInteraction.Disabled,
            onClick = { onIntent(AddAccountIntent.OnSave) },
        )
    }
}

@Composable
private fun Section(eyebrow: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    val shape = RoundedCornerShape(999.dp)

    val bgColor = if (selected) colors.surface3 else colors.surface1
    val borderColor = if (selected) colors.textPrimary else colors.border
    val textColor = if (selected) colors.textPrimary else colors.textSecondary

    Row(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.W600 else FontWeight.W500,
            fontFamily = InterFontFamily,
            color = textColor,
            letterSpacing = (-0.06).sp,
        )
    }
}

@Composable
private fun NameInput(value: String, onValueChange: (String) -> Unit) {
    val colors = LocalEmmColors.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            letterSpacing = (-0.18).sp,
        ),
        cursorBrush = SolidColor(colors.accent),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeY = size.height
                drawLine(
                    color = colors.border,
                    start = Offset(0f, strokeY),
                    end = Offset(size.width, strokeY),
                    strokeWidth = 1f,
                )
            }
            .padding(vertical = 8.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = "ejm. Yape",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W400,
                        fontFamily = InterFontFamily,
                        color = colors.textTertiary,
                    )
                }
                inner()
            }
        },
    )
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TYPE_OPTIONS.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val shape = RoundedCornerShape(12.dp)

    val borderColor = if (selected) colors.textPrimary else colors.border
    val bgColor = if (selected) colors.surface3 else colors.surface1
    val tint = if (selected) colors.textPrimary else colors.textSecondary

    Row(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.W600 else FontWeight.W500,
            fontFamily = InterFontFamily,
            color = tint,
            letterSpacing = (-0.15).sp,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF191919, heightDp = 800)
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
