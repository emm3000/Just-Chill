package com.emm.justchill.feature.report

import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface ReportEffect : UiEffect {
    data class ShowError(val message: String) : ReportEffect
    data class ShareReport(val text: String) : ReportEffect
}
