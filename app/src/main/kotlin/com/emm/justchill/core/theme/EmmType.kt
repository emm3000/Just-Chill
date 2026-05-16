package com.emm.justchill.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Design system typography. Mirrors `docs/DESIGN_SYSTEM.md §3`.
 *
 * TODO: replace [InterFontFamily] with the real Inter family once the `.ttf`
 * files land in `res/font/` (bundled) OR Google Fonts downloadable is wired up.
 * For now we fall back to the system sans (Roboto on AOSP) — visually close to
 * Inter and supports `tnum` for tabular figures.
 */
val InterFontFamily: FontFamily = FontFamily.SansSerif

private const val TABULAR = "tnum"

@Immutable
data class EmmType(
    // Amounts (the hero)
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
)

internal val emmType: EmmType = EmmType(
    amountHero = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TABULAR,
    ),
    amountL = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = (-0.25).sp,
        fontFeatureSettings = TABULAR,
    ),
    amountM = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.sp,
        fontFeatureSettings = TABULAR,
    ),
    amountS = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
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
)

val LocalEmmType = staticCompositionLocalOf<EmmType> {
    error("EmmType not provided. Wrap your composable in EmmTheme.")
}
