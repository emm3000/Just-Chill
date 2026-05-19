package com.emm.justchill.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.emm.justchill.R

/**
 * Design system typography — SR-1.
 *
 * Inter (bundled) for all UI text, IBM Plex Mono (bundled) for amounts.
 * Use [LocalEmmType.current] inside composables wrapped by [EmmTheme].
 */
val InterFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val PlexMonoFontFamily: FontFamily = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

private const val TABULAR = "tnum"

@Immutable
data class EmmType(
    // Amounts (mono + tnum)
    val amountHero: TextStyle,
    val amountL: TextStyle,
    val amountM: TextStyle,
    val amountS: TextStyle,

    // Display & headlines
    val display: TextStyle,
    val headlineL: TextStyle,
    val headlineM: TextStyle,

    // Titles
    val titleL: TextStyle,
    val titleM: TextStyle,

    // Body
    val bodyL: TextStyle,
    val bodyM: TextStyle,

    // Labels
    val labelL: TextStyle,
    val labelM: TextStyle,

    // Caption
    val caption: TextStyle,

    // Eyebrow (new in SR-1)
    val eyebrow: TextStyle,
)

internal val emmType: EmmType = EmmType(
    amountHero = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontSize = 48.sp,
        lineHeight = 48.sp,     // lineHeight = 1.0 × fontSize
        fontWeight = FontWeight.W500,
        letterSpacing = (-1.9).sp,  // ≈ -0.04em × 48sp
        fontFeatureSettings = TABULAR,
    ),
    amountL = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontSize = 52.sp,
        lineHeight = 52.sp,     // lineHeight = 1.0 × fontSize
        fontWeight = FontWeight.W500,
        letterSpacing = (-2.08).sp, // ≈ -0.04em × 52sp
        fontFeatureSettings = TABULAR,
    ),
    amountM = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = (-0.3).sp,  // ≈ -0.02em × 15sp
        fontFeatureSettings = TABULAR,
    ),
    amountS = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.sp,
        fontFeatureSettings = TABULAR,
    ),

    display = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = (-0.5).sp,
    ),
    headlineL = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = (-0.25).sp,
    ),
    headlineM = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
    ),

    titleL = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
    ),
    titleM = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.1.sp,
    ),

    bodyL = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.15.sp,
    ),
    bodyM = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.2.sp,
    ),

    labelL = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.1.sp,
    ),
    labelM = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.4.sp,
    ),

    caption = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.5.sp,
    ),

    eyebrow = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 1.6.sp,  // ≈ 0.16em × 10sp
    ),
)

val LocalEmmType = staticCompositionLocalOf<EmmType> {
    error("EmmType not provided. Wrap your composable in EmmTheme.")
}
