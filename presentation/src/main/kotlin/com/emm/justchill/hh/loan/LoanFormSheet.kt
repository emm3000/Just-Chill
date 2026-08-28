package com.emm.justchill.hh.loan

/**
 * Every sheet on [AddEditLoanUiState] is a `ModalBottomSheet`, so two can never be open at once —
 * an enum makes that unrepresentable instead of merely true by accident (ADR 012 Decision 2).
 */
enum class LoanFormSheet { Amount, Date }
