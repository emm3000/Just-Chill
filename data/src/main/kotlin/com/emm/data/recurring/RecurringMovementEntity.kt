package com.emm.data.recurring

data class RecurringMovementEntity(
    val id: String,
    val name: String,
    val type: String,
    val amount: Long?,
    val description: String,
    val categoryId: String?,
    val accountId: String,
    val frequency: String,
    val dayOfMonth: Long,
    val isActive: Long,
    val lastConfirmedPeriod: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
