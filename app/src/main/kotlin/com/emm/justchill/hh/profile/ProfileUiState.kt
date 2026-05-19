package com.emm.justchill.hh.profile

import com.emm.justchill.core.mvi.UiState

data class ProfileUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
) : UiState
