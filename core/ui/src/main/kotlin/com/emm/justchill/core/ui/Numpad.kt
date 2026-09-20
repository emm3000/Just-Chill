package com.emm.justchill.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.color
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.PlexMonoFontFamily

data class NumpadSign(val tone: AmountTone, val contentDescription: String, val onClick: () -> Unit)

@Composable
fun Numpad(
    onDigit: (Char) -> Unit,
    onDoubleZero: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    sign: NumpadSign? = null,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    val bottomRow: List<NumKey> = buildList {
        if (sign != null) add(NumKey.Sign(sign))
        add(NumKey.DoubleZero)
        add(NumKey.Digit('0'))
        add(NumKey.Backspace)
    }

    val rows: List<List<NumKey>> = listOf(
        listOf(NumKey.Digit('1'), NumKey.Digit('2'), NumKey.Digit('3')),
        listOf(NumKey.Digit('4'), NumKey.Digit('5'), NumKey.Digit('6')),
        listOf(NumKey.Digit('7'), NumKey.Digit('8'), NumKey.Digit('9')),
        bottomRow,
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    // A digit is the default action and reads as the ground; the keys that edit
                    // what is already typed step forward off it.
                    val isEditingKey = key !is NumKey.Digit
                    val bgColor = if (isEditingKey) colors.surface1 else Color.Transparent

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(radii.rM)
                            .background(bgColor)
                            .border(BorderStroke(1.dp, colors.border), radii.rM)
                            .clickable {
                                when (key) {
                                    is NumKey.Digit -> onDigit(key.ch)
                                    NumKey.DoubleZero -> onDoubleZero()
                                    NumKey.Backspace -> onBackspace()
                                    is NumKey.Sign -> key.sign.onClick()
                                }
                            },
                    ) {
                        when (key) {
                            is NumKey.Digit -> KeyGlyph(text = key.ch.toString(), color = colors.textPrimary)

                            NumKey.DoubleZero -> KeyGlyph(text = "00", color = colors.textPrimary)

                            NumKey.Backspace -> {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = "Borrar",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            is NumKey.Sign -> {
                                KeyGlyph(
                                    text = SIGN_KEY_GLYPH,
                                    color = key.sign.tone.color(colors),
                                    modifier = Modifier.clearAndSetSemantics {
                                        contentDescription = key.sign.contentDescription
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyGlyph(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 22.sp,
        fontWeight = FontWeight.W500,
        fontFamily = PlexMonoFontFamily,
        color = color,
        letterSpacing = (-0.44).sp,
        modifier = modifier,
    )
}

private const val SIGN_KEY_GLYPH = "±"

private sealed interface NumKey {
    data class Digit(val ch: Char) : NumKey
    data object DoubleZero : NumKey
    data object Backspace : NumKey
    data class Sign(val sign: NumpadSign) : NumKey
}
