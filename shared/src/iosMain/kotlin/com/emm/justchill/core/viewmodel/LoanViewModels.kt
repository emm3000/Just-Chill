package com.emm.justchill.core.viewmodel

import com.emm.justchill.feature.loan.AddEditLoanEffect
import com.emm.justchill.feature.loan.AddEditLoanIntent
import com.emm.justchill.feature.loan.AddEditLoanUiState
import com.emm.justchill.feature.loan.AddEditLoanViewModel
import com.emm.justchill.feature.loan.LoanDetailEffect
import com.emm.justchill.feature.loan.LoanDetailIntent
import com.emm.justchill.feature.loan.LoanDetailUiState
import com.emm.justchill.feature.loan.LoanDetailViewModel
import com.emm.justchill.feature.loan.LoansEffect
import com.emm.justchill.feature.loan.LoansIntent
import com.emm.justchill.feature.loan.LoansUiState
import com.emm.justchill.feature.loan.LoansViewModel
import com.emm.justchill.feature.loan.PersonLoansEffect
import com.emm.justchill.feature.loan.PersonLoansIntent
import com.emm.justchill.feature.loan.PersonLoansUiState
import com.emm.justchill.feature.loan.PersonLoansViewModel
import org.koin.core.parameter.parametersOf

fun loansViewModel(): MviHandle<LoansUiState, LoansIntent, LoansEffect> = handleOf(LoansViewModel::class)

fun personLoansViewModel(personKey: String): MviHandle<PersonLoansUiState, PersonLoansIntent, PersonLoansEffect> =
    handleOf(PersonLoansViewModel::class) { parametersOf(personKey) }

fun loanDetailViewModel(loanId: String): MviHandle<LoanDetailUiState, LoanDetailIntent, LoanDetailEffect> =
    handleOf(LoanDetailViewModel::class) { parametersOf(loanId) }

fun addEditLoanViewModel(loanId: String?): MviHandle<AddEditLoanUiState, AddEditLoanIntent, AddEditLoanEffect> =
    handleOf(AddEditLoanViewModel::class) { parametersOf(loanId) }
