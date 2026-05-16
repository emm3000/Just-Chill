package com.emm.justchill.hh.category

import com.emm.justchill.core.mvi.UiIntent

sealed interface SelectCategoryIntent : UiIntent {

    data class UpdateQuery(val value: String) : SelectCategoryIntent
}
