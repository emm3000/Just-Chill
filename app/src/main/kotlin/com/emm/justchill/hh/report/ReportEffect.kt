package com.emm.justchill.hh.report

import com.emm.justchill.core.mvi.UiEffect

sealed interface ReportEffect : UiEffect {
    // No effects yet — VM is read-only in the mock-data phase.
    // When SQL/use cases land, ShowError(message) is the first candidate.
}
