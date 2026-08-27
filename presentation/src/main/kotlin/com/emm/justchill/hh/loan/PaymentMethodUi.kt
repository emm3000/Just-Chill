package com.emm.justchill.hh.loan

import com.emm.domain.loan.PaymentMethod

val PaymentMethod.label: String
    get() = when (this) {
        PaymentMethod.Cash -> "Efectivo"
        PaymentMethod.Transfer -> "Transferencia"
    }
