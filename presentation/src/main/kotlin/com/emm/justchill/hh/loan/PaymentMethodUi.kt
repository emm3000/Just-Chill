package com.emm.justchill.hh.loan

import com.emm.justchill.core.domain.loan.PaymentMethod

val PaymentMethod.label: String
    get() = when (this) {
        PaymentMethod.Cash -> "Efectivo"
        PaymentMethod.Transfer -> "Transferencia"
    }
