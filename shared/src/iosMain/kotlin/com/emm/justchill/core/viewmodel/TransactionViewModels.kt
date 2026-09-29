package com.emm.justchill.core.viewmodel

import com.emm.justchill.feature.transaction.capture.AddTransactionEffect
import com.emm.justchill.feature.transaction.capture.AddTransactionIntent
import com.emm.justchill.feature.transaction.capture.AddTransactionUiState
import com.emm.justchill.feature.transaction.capture.AddTransactionViewModel
import com.emm.justchill.feature.transaction.capture.EditTransactionEffect
import com.emm.justchill.feature.transaction.capture.EditTransactionIntent
import com.emm.justchill.feature.transaction.capture.EditTransactionUiState
import com.emm.justchill.feature.transaction.capture.EditTransactionViewModel
import com.emm.justchill.feature.transaction.list.SeeTransactionsEffect
import com.emm.justchill.feature.transaction.list.SeeTransactionsIntent
import com.emm.justchill.feature.transaction.list.SeeTransactionsUiState
import com.emm.justchill.feature.transaction.list.SeeTransactionsViewModel
import org.koin.core.parameter.parametersOf

fun resolveAddTransactionHandle(): MviHandle<AddTransactionUiState, AddTransactionIntent, AddTransactionEffect> =
    handleOf(AddTransactionViewModel::class)

fun resolveEditTransactionHandle(
    transactionId: String,
): MviHandle<EditTransactionUiState, EditTransactionIntent, EditTransactionEffect> =
    handleOf(EditTransactionViewModel::class) { parametersOf(transactionId) }

fun resolveSeeTransactionsHandle(): MviHandle<SeeTransactionsUiState, SeeTransactionsIntent, SeeTransactionsEffect> =
    handleOf(SeeTransactionsViewModel::class)
