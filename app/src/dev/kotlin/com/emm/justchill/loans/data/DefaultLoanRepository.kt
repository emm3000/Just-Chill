package com.emm.justchill.loans.data

import com.emm.justchill.hh.data.shared.TableNames
import com.emm.justchill.loans.domain.Loan
import com.emm.justchill.loans.domain.LoanRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class DefaultLoanRepository(supabaseClient: SupabaseClient): LoanRepository {

    private val client: PostgrestQueryBuilder = supabaseClient.from(TableNames.LOANS_TABLE)

    override suspend fun add(loan: Loan) {
        client.insert(loan)
    }
}