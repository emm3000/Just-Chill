package com.emm.justchill.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale. Base unit 4dp. Mirrors `docs/DESIGN_SYSTEM.md §4`.
 *
 * Naming reflects the multiplier: `s4` = 4 * base = 16dp.
 */
@Immutable
data class EmmSpacing(
    val s0: Dp = 0.dp,
    val s1: Dp = 4.dp,
    val s2: Dp = 8.dp,
    val s3: Dp = 12.dp,
    val s4: Dp = 16.dp,
    val s5: Dp = 20.dp,
    val s6: Dp = 24.dp,
    val s8: Dp = 32.dp,
    val s10: Dp = 40.dp,
    val s12: Dp = 48.dp,
)

internal val emmSpacing: EmmSpacing = EmmSpacing()

val LocalEmmSpacing = staticCompositionLocalOf<EmmSpacing> {
    error("EmmSpacing not provided. Wrap your composable in EmmTheme.")
}
