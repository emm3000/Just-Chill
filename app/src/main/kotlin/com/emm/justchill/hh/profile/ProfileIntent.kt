package com.emm.justchill.hh.profile

import com.emm.justchill.core.mvi.UiIntent
import java.io.OutputStream

sealed interface ProfileIntent : UiIntent {
    data class ExportToStream(val output: OutputStream) : ProfileIntent
    data class ImportJson(val json: String) : ProfileIntent
}
