package com.emm.justchill.hh.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.PlexMonoFontFamily
import com.emm.justchill.core.ui.atoms.Eyebrow

@Composable
internal fun SignToggle(
    isSpend: Boolean,
    onIncomeClick: () -> Unit,
    onSpendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val containerShape = RoundedCornerShape(12.dp)
    val cellShape = RoundedCornerShape(9.dp)

    Row(
        modifier = modifier
            .clip(containerShape)
            .background(colors.surface1)
            .border(1.dp, colors.border, containerShape)
            .padding(3.dp)
            .height(38.dp),
    ) {
        val incomeActive = !isSpend
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(cellShape)
                .then(
                    if (incomeActive) {
                        Modifier
                            .background(colors.surface3)
                            .border(1.dp, colors.borderFocus, cellShape)
                    } else {
                        Modifier
                    },
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onIncomeClick,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "+",
                color = colors.success,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = PlexMonoFontFamily,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = "Ingreso",
                fontSize = 12.sp,
                fontWeight = if (incomeActive) FontWeight.W600 else FontWeight.W500,
                fontFamily = InterFontFamily,
                color = if (incomeActive) colors.textPrimary else colors.textTertiary,
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(cellShape)
                .then(
                    if (isSpend) {
                        Modifier
                            .background(colors.surface3)
                            .border(1.dp, colors.borderFocus, cellShape)
                    } else {
                        Modifier
                    },
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSpendClick,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "−",
                color = colors.danger,
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                fontFamily = PlexMonoFontFamily,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = "Gasto",
                fontSize = 12.sp,
                fontWeight = if (isSpend) FontWeight.W600 else FontWeight.W500,
                fontFamily = InterFontFamily,
                color = if (isSpend) colors.textPrimary else colors.textTertiary,
            )
        }
    }
}

@Composable
internal fun QuickChip(
    eyebrow: String,
    value: String,
    dotColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cta: Boolean = false,
) {
    val colors = LocalEmmColors.current
    val chipShape = RoundedCornerShape(10.dp)

    val borderColor = if (cta) colors.accent else colors.border
    val eyebrowColor: Color? = if (cta) colors.accent else null
    val valueColor = if (cta) colors.accent else colors.textPrimary
    val trailingIcon = if (cta) Icons.Outlined.Add else Icons.Outlined.KeyboardArrowDown
    val trailingTint = if (cta) colors.accent else colors.textDisabled

    Row(
        modifier = modifier
            .clip(chipShape)
            .background(colors.surface1)
            .border(1.dp, borderColor, chipShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Eyebrow(text = eyebrow, color = eyebrowColor)
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = valueColor,
                letterSpacing = (-0.06).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = trailingTint,
            modifier = Modifier.size(11.dp),
        )
    }
}

@Composable
internal fun NoteRow(note: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (note.isBlank()) {
        NoteEmptyButton(onClick = onClick, modifier = modifier)
    } else {
        NoteFilledCard(note = note, onClick = onClick, modifier = modifier)
    }
}

@Composable
private fun NoteEmptyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(11.dp),
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = "Agregar nota",
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun NoteFilledCard(note: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clip(radii.rM)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rM)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(colors.accent),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "NOTA",
                fontSize = 11.sp,
                fontWeight = FontWeight.W500,
                fontFamily = InterFontFamily,
                color = colors.textTertiary,
                letterSpacing = 1.4.sp,
            )
            Text(
                text = note,
                fontSize = 13.sp,
                fontWeight = FontWeight.W400,
                fontFamily = InterFontFamily,
                fontStyle = FontStyle.Italic,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Editar nota",
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
