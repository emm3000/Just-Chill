package com.emm.justchill.hh.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone
import com.emm.justchill.core.ui.preview.PreviewRedmi15C

@Composable
fun LoansScreen(
    state: LoansUiState,
    onIntent: (LoansIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        JcTopBar(
            title = "Préstamos",
            left = {
                IconBtn(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Volver",
                )
            },
            right = {
                IconBtn(
                    icon = Icons.Outlined.Add,
                    onClick = { onIntent(LoansIntent.OnAddLoanClick) },
                    contentDescription = "Nuevo préstamo",
                )
            },
        )
        Hairline()

        if (state.people.isEmpty()) {
            LoansEmptyState(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(contentPadding = PaddingValues(top = spacing.s1, bottom = spacing.s3)) {
                items(state.people, key = { it.personKey }) { person ->
                    PersonRow(
                        person = person,
                        onClick = { onIntent(LoansIntent.OnPersonClick(person.personKey)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonRow(person: PersonBalanceUi, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val nameColor = if (person.isSettled) colors.textTertiary else colors.textPrimary

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Ver los préstamos de esta persona",
                    onClick = onClick,
                )
                .padding(horizontal = spacing.s4, vertical = spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            IconTile(icon = Icons.Outlined.Person, size = IconTileSize.Md)

            Text(
                text = person.personName,
                style = type.titleM,
                color = nameColor,
                modifier = Modifier.weight(1f),
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = person.remaining,
                    style = type.amountM,
                    color = nameColor,
                )
                if (person.isSettled) {
                    Spacer(Modifier.height(spacing.s1))
                    Pill(text = "Liquidado", tone = PillTone.Pos)
                }
            }
        }
        Hairline()
    }
}

@Composable
private fun LoansEmptyState(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.padding(horizontal = spacing.s6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.People,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(spacing.s4))
        Text(
            text = "Aún sin préstamos",
            style = type.titleL,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = "Lo que prestes y te devuelvan aparece aquí, por persona",
            style = type.bodyM,
            color = colors.textSecondary,
        )
    }
}

@Preview
@PreviewRedmi15C
@Composable
private fun LoansScreenPreview() {
    EmmTheme {
        LoansScreen(
            state = LoansUiState(
                people = listOf(
                    PersonBalanceUi(
                        personKey = "juan",
                        personName = "Juan",
                        remaining = "S/ 250.00",
                        isSettled = false,
                    ),
                    PersonBalanceUi(
                        personKey = "maria",
                        personName = "María",
                        remaining = "S/ 0.00",
                        isSettled = true,
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
@PreviewRedmi15C
@Composable
private fun LoansScreenEmptyPreview() {
    EmmTheme {
        LoansScreen(
            state = LoansUiState(),
            onIntent = {},
            onBack = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@PreviewRedmi15C
@Composable
private fun PersonRowOverflowPreview() {
    EmmTheme {
        PersonRow(
            person = PersonBalanceUi(
                personKey = "maria-fernanda",
                personName = "María Fernanda Rodríguez Quispe",
                remaining = "S/ 999,999.99",
                isSettled = false,
            ),
            onClick = {},
        )
    }
}
