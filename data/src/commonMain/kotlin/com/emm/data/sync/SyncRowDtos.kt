package com.emm.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Common fields required by the shared push/pull algorithm in [BaseTableSync].
 * Every DTO adds this interface so the algorithm can access these fields without
 * knowing which table it operates on.
 *
 * DTO conventions (applies to all concrete implementors below):
 * - Fields named with @SerialName("snake_case") to match server column names.
 * - [serverUpdatedAt] is nullable with a default of null: the server sets this column via trigger;
 *   we never send it on push. ignoreUnknownKeys=true on the client makes round-trips safe even
 *   if the field is absent from a push response.
 * - All client timestamps stay epoch-millis (Long), matching the Supabase `bigint` schema.
 * - [deletedAt] is nullable — null means the row is live.
 */
interface SyncRowDto {
    val userId: String
    val updatedAt: Long
    val serverUpdatedAt: String?
}

@Serializable
data class AccountRowDto(
    @SerialName("account_id") val accountId: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @SerialName("currency") val currency: String,
    @SerialName("updated_at") override val updatedAt: Long,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("user_id") override val userId: String,
    @SerialName("deleted_at") val deletedAt: Long? = null,
    @SerialName("server_updated_at") override val serverUpdatedAt: String? = null,
) : SyncRowDto

@Serializable
data class CategoryRowDto(
    @SerialName("category_id") val categoryId: String,
    @SerialName("name") val name: String,
    @SerialName("icon") val icon: String,
    @SerialName("color") val color: String,
    @SerialName("category_type") val categoryType: String,
    @SerialName("is_default") val isDefault: Boolean,
    @SerialName("updated_at") override val updatedAt: Long,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("user_id") override val userId: String,
    @SerialName("deleted_at") val deletedAt: Long? = null,
    @SerialName("server_updated_at") override val serverUpdatedAt: String? = null,
) : SyncRowDto

@Serializable
data class TransactionRowDto(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("type") val type: String,
    @SerialName("amount") val amount: Long,
    @SerialName("description") val description: String,
    /**
     * The wire still carries the occurrence as epoch millis, because the server column is
     * `date bigint not null`. It is NOT what the app stores — see `FixedPeruOffset.kt`, which owns
     * both directions of the conversion until phase two changes the column.
     */
    @SerialName("date") val date: Long,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("account_id") val accountId: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") override val updatedAt: Long,
    @SerialName("user_id") override val userId: String,
    @SerialName("deleted_at") val deletedAt: Long? = null,
    @SerialName("server_updated_at") override val serverUpdatedAt: String? = null,
) : SyncRowDto

@Serializable
data class RecurringMovementRowDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @SerialName("amount") val amount: Long? = null,
    @SerialName("description") val description: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("account_id") val accountId: String,
    @SerialName("frequency") val frequency: String,
    @SerialName("day_of_month") val dayOfMonth: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("last_confirmed_period") val lastConfirmedPeriod: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") override val updatedAt: Long,
    @SerialName("user_id") override val userId: String,
    @SerialName("deleted_at") val deletedAt: Long? = null,
    @SerialName("server_updated_at") override val serverUpdatedAt: String? = null,
) : SyncRowDto
