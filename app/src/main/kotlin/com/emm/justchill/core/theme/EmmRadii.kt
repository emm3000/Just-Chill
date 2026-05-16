package com.emm.justchill.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Radii scale. Mirrors `docs/DESIGN_SYSTEM.md §5`.
 *
 * Default to `r0` (sharp). Soft only where the element needs to feel tappable.
 */
@Immutable
data class EmmRadii(
    val r0: RoundedCornerShape = RoundedCornerShape(0.dp),
    val rS: RoundedCornerShape = RoundedCornerShape(6.dp),
    val rM: RoundedCornerShape = RoundedCornerShape(12.dp),
    val rL: RoundedCornerShape = RoundedCornerShape(20.dp),
    val rLTop: RoundedCornerShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    val rFull: RoundedCornerShape = RoundedCornerShape(999.dp),
)

internal val emmRadii: EmmRadii = EmmRadii()

val LocalEmmRadii = staticCompositionLocalOf<EmmRadii> {
    error("EmmRadii not provided. Wrap your composable in EmmTheme.")
}
