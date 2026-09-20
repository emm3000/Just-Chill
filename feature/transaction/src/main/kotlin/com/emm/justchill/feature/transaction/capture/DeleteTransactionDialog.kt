package com.emm.justchill.feature.transaction.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.atoms.CategoryDot
import com.emm.justchill.core.ui.atoms.EmmDialog
import com.emm.justchill.core.ui.atoms.IconBtnTone
import com.emm.justchill.core.ui.format.formatCentsForDisplay
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Suppress("LongParameterList")
@Composable
internal fun DeleteTransactionDialog(
    type: TransactionType,
    amountCents: String,
    accountName: String?,
    categoryName: String?,
    categoryColor: Color?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors: EmmColors = LocalEmmColors.current
    val typography: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    val isSpend: Boolean = type == TransactionType.Spend
    val title: String = if (isSpend) "¿Eliminar este gasto?" else "¿Eliminar este ingreso?"

    EmmDialog(
        title = title,
        confirmLabel = "Eliminar",
        onConfirm = onConfirm,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        confirmTone = IconBtnTone.Danger,
    ) {
        Text(
            text = "Esta acción no se puede deshacer.",
            style = typography.bodyM,
            color = colors.textSecondary,
        )
        TransactionSummaryRow(
            isSpend = isSpend,
            amountCents = amountCents,
            accountName = accountName,
            categoryName = categoryName,
            categoryColor = categoryColor,
            modifier = Modifier.padding(top = spacing.s3),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun TransactionSummaryRow(
    isSpend: Boolean,
    amountCents: String,
    accountName: String?,
    categoryName: String?,
    categoryColor: Color?,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current
    val typography: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current

    val amountColor: Color = if (isSpend) colors.textPrimary else colors.success
    val amountDisplay: String = "S/ " + formatCentsForDisplay(amountCents)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(radii.rL)
            .background(colors.surface2)
            .border(width = 1.dp, color = colors.border, shape = radii.rL)
            .padding(horizontal = spacing.s3, vertical = spacing.s3),
    ) {
        if (categoryColor != null) {
            CategoryDot(color = categoryColor)
            Spacer(Modifier.width(spacing.s2))
        }
        Text(
            text = summaryLabel(categoryName, accountName, colors),
            style = typography.labelL,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = amountDisplay,
            style = typography.amountM,
            color = amountColor,
            maxLines = 1,
            modifier = Modifier.padding(start = spacing.s2),
        )
    }
}

private fun summaryLabel(categoryName: String?, accountName: String?, colors: EmmColors): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = colors.textPrimary)) {
            append(categoryName ?: "—")
        }
        if (accountName != null) {
            withStyle(SpanStyle(color = colors.textTertiary)) {
                append(" / ")
            }
            withStyle(SpanStyle(color = colors.textSecondary)) {
                append(accountName)
            }
        }
    }

@Preview
@Composable
private fun DeleteTransactionDialogPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        DeleteTransactionDialog(
            type = TransactionType.Spend,
            amountCents = "8540",
            accountName = "Yape",
            categoryName = "Comida",
            categoryColor = colors.catTerracotta,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun DeleteTransactionDialogOverflowPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        DeleteTransactionDialog(
            type = TransactionType.Spend,
            amountCents = "99999999",
            accountName = "Tarjeta de crédito BCP",
            categoryName = "Cuidado personal y salud",
            categoryColor = colors.catTerracotta,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
