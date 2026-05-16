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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
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
    vm: AddAccountViewModel = koinViewModel(),
) {
    AddAccountScreen(
        state = vm.state,
        onAction = vm::onAction,
        onBack = onBack,
    )
}

@Composable
private fun AddAccountScreen(
    state: AddAccountUiState,
    onAction: (AddAccountAction) -> Unit,
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
                onValueChange = { onAction(AddAccountAction.OnNameChange(it)) },
                label = "NOMBRE",
                placeholder = "ejm. Gasto diario",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        EmmButton(
            text = "Crear cuenta",
            onClick = dropUnlessResumed {
                onAction(AddAccountAction.OnSave)
                onBack()
            },
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

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun AddAccountScreenPreview() {
    EmmTheme {
        AddAccountScreen(
            state = AddAccountUiState(name = "Gasto diario", isEnabled = true),
            onAction = {},
        )
    }
}
