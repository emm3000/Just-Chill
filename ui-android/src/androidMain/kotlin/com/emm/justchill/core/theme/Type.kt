package com.emm.justchill.core.theme

import androidx.compose.material3.Typography

/**
 * Material3 [Typography] used by [EmmTheme] → [MaterialTheme].
 *
 * Lato removed (SR-1). Inter is the system sans fallback; the real
 * [InterFontFamily] is wired up in [EmmType] and consumed via
 * [LocalEmmType] — Material component text inherits the theme default.
 */
val AppTypography: Typography = Typography()
