package com.emm.justchill.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class EmmRadii(
    val r0: RoundedCornerShape = RoundedCornerShape(0.dp),
    val rXS: RoundedCornerShape = RoundedCornerShape(8.dp),
    val rS: RoundedCornerShape = RoundedCornerShape(10.dp),
    val rM: RoundedCornerShape = RoundedCornerShape(12.dp),
    val rL: RoundedCornerShape = RoundedCornerShape(14.dp),
    val rXL: RoundedCornerShape = RoundedCornerShape(16.dp),
    val rXXL: RoundedCornerShape = RoundedCornerShape(20.dp),
    val rLTop: RoundedCornerShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    val rFull: RoundedCornerShape = RoundedCornerShape(999.dp),
)

internal val emmRadii: EmmRadii = EmmRadii()

val LocalEmmRadii = staticCompositionLocalOf<EmmRadii> {
    error("EmmRadii not provided. Wrap your composable in EmmTheme.")
}
