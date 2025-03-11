package com.emm.justchill.hh.fasttransaction

import androidx.compose.ui.text.input.TextFieldValue
import com.emm.domain.transaction.TransactionType

sealed interface FastTransactionAction {

    class OnAmountChange(val amount: TextFieldValue) : FastTransactionAction

    class OnDescriptionChange(val description: String) : FastTransactionAction

    class AddTransaction(val accountId: String, val type: TransactionType) : FastTransactionAction
}