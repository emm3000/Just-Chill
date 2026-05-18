package com.emm.justchill.hh.report

import com.emm.justchill.core.mvi.UiEffect

sealed interface ReportEffect : UiEffect {
    data class ShowError(val message: String) : ReportEffect
}
