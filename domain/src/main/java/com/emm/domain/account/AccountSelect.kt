package com.emm.domain.account

enum class AccountSelect(val value: Long) {
    IsSelected(1), NonSelected(0);

    companion object {

        fun valueOf(value: Long): AccountSelect {
            return when (value) {
                IsSelected.value -> IsSelected
                else -> NonSelected
            }
        }
    }
}