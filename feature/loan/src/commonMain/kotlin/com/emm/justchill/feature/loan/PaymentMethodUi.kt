package com.emm.justchill.feature.loan

import com.emm.justchill.core.domain.loan.PaymentMethod

val PaymentMethod.label: String
    get() = when (this) {
        PaymentMethod.Cash -> "Efectivo"
        PaymentMethod.Transfer -> "Transferencia"
    }

data class PaymentMethodOptionUi(val method: PaymentMethod, val label: String, val isSelected: Boolean)
