package com.emm.domain.shared

import kotlin.jvm.JvmInline

@JvmInline
value class AccountId(val value: String)

@JvmInline
value class TransactionId(val value: String)

@JvmInline
value class CategoryId(val value: String)

@JvmInline
value class RecurringMovementId(val value: String)
