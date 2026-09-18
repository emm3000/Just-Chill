package com.emm.justchill.core.ui.preview

import androidx.compose.ui.tooling.preview.Preview

// For components, which do not own the screen: the Redmi 15C's width constraint and nothing else,
// so height stays at content and no device frame is left over to render as blank.
@Preview(name = "Redmi 15C width", widthDp = 360)
annotation class PreviewRedmi15CWidth
