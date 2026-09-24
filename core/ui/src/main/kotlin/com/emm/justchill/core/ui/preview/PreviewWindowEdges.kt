package com.emm.justchill.core.ui.preview

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "360x640", device = "spec:width=360dp,height=640dp,dpi=320")
@Preview(name = "320x640", device = "spec:width=320dp,height=640dp,dpi=240")
@Preview(name = "800x360 landscape", device = "spec:width=360dp,height=800dp,dpi=320,orientation=landscape")
@Preview(name = "360x350 split", device = "spec:width=360dp,height=350dp,dpi=320")
@Preview(name = "360x800 font 200%", device = "spec:width=360dp,height=800dp,dpi=320", fontScale = 2f)
@Preview(name = "360x640 font 200%", device = "spec:width=360dp,height=640dp,dpi=320", fontScale = 2f)
@Preview(name = "360x568 bars", device = "spec:width=360dp,height=568dp,dpi=320")
@Preview(name = "360x568 bars font 200%", device = "spec:width=360dp,height=568dp,dpi=320", fontScale = 2f)
annotation class PreviewWindowEdges
