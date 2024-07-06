package com.emm.justchill.experiences.supabase

import android.util.Log
import com.emm.justchill.hh.domain.TransactionModel
import com.emm.justchill.hh.domain.TransactionModelFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class SupabasePlayground(
    private val supabaseClient: SupabaseClient,
    private val transactionModelFactory: TransactionModelFactory,
) {

    suspend fun add() = withContext(Dispatchers.IO) {
        try {
            val gg = UUID.randomUUID().toString()
            val x = TransactionModel(
                transactionId = gg,
                type = "",
                amount = 0,
                description = "",
                date = 0
            )
            val gggg = TransactionModel(
                transactionId = UUID.randomUUID().toString(),
                type = "",
                amount = 0,
                description = "PERRA",
                date = 0
            )
            val gaa = transactionModelFactory.create(x)
            val xa = transactionModelFactory.create(gggg)
            supabaseClient.from("transaction").upsert(listOf(gaa, gaa, xa), ignoreDuplicates = true)
            val ggfc: List<TransactionModel> =
                supabaseClient.from("transaction").select().decodeList<TransactionModel>()
            Log.e("aea", ggfc.toString())
        } catch (e: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Log.e("aea", e.toString())
        }
    }
}