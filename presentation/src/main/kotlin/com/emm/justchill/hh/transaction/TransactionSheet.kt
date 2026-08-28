package com.emm.justchill.hh.transaction

/**
 * Every sheet on [AddTransactionUiState] and [EditTransactionUiState] is a `ModalBottomSheet`, so
 * two can never be open at once — an enum makes that unrepresentable instead of merely true by
 * accident (ADR 012 Decision 2).
 */
enum class TransactionSheet { Account, Category, Date, Note }
