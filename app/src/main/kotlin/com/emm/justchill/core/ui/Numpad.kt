package com.emm.justchill.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.PlexMonoFontFamily

/**
 * Reusable 3×4 custom numeric keypad for the Notion-dark design.
 *
 * Layout (left-to-right, top-to-bottom):
 *   1  2  3
 *   4  5  6
 *   7  8  9
 *   00 0  ⌫
 *
 * The "00" key appends two zeros — a standard pattern for a cents-only numpad.
 * The caller owns all state; this composable is a pure input device.
 *
 * @param onDigit       Called with a single digit character ('0'..'9').
 * @param onDoubleZero  Called when "00" is pressed.
 * @param onBackspace   Called when ⌫ is pressed.
 */
@Composable
fun Numpad(
    onDigit: (Char) -> Unit,
    onDoubleZero: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current

    // Hairline border for digits, slightly brighter border for accented keys
    val hairline = colors.border
    val hairline2 = colors.borderFocus
    val surface2 = colors.surface2

    val rows = listOf(
        listOf(NumKey.Digit('1'), NumKey.Digit('2'), NumKey.Digit('3')),
        listOf(NumKey.Digit('4'), NumKey.Digit('5'), NumKey.Digit('6')),
        listOf(NumKey.Digit('7'), NumKey.Digit('8'), NumKey.Digit('9')),
        listOf(NumKey.DoubleZero, NumKey.Digit('0'), NumKey.Backspace),
    )

    val shapeRadius = 12.dp

    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    val isAccent = key is NumKey.DoubleZero || key is NumKey.Backspace
                    val bgColor = if (isAccent) surface2 else Color.Transparent
                    val borderColor = if (isAccent) hairline2 else hairline
                    val shape = androidx.compose.foundation.shape.RoundedCornerShape(shapeRadius)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(shape)
                            .background(bgColor)
                            .border(BorderStroke(1.dp, borderColor), shape)
                            .clickable {
                                when (key) {
                                    is NumKey.Digit -> onDigit(key.ch)
                                    NumKey.DoubleZero -> onDoubleZero()
                                    NumKey.Backspace -> onBackspace()
                                }
                            },
                    ) {
                        when (key) {
                            is NumKey.Digit -> {
                                Text(
                                    text = key.ch.toString(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.W500,
                                    fontFamily = PlexMonoFontFamily,
                                    color = colors.textPrimary,
                                    letterSpacing = (-0.44).sp,
                                )
                            }
                            NumKey.DoubleZero -> {
                                Text(
                                    text = "00",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.W500,
                                    fontFamily = PlexMonoFontFamily,
                                    color = colors.textPrimary,
                                    letterSpacing = (-0.44).sp,
                                )
                            }
                            NumKey.Backspace -> {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = "Borrar",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface NumKey {
    data class Digit(val ch: Char) : NumKey
    data object DoubleZero : NumKey
    data object Backspace : NumKey
}
