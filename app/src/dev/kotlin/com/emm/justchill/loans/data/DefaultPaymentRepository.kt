package com.emm.justchill.loans.data

import com.emm.justchill.hh.data.TableNames
import com.emm.justchill.loans.domain.Payment
import com.emm.justchill.loans.domain.PaymentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class DefaultPaymentRepository(supabaseClient: SupabaseClient) : PaymentRepository {

    private val client: PostgrestQueryBuilder = supabaseClient.from(TableNames.PAYMENTS_TABLE)

    override suspend fun add(payment: Payment) {
        client.upsert(payment)
    }

    override suspend fun addAll(payments: List<Payment>) {
        client.upsert(payments)
    }

    override suspend fun fetch(): List<Payment> {
        return client.select().decodeList()
    }
}