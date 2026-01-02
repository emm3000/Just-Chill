package com.emm.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

class DefaultUserIdProvider(private val client: SupabaseClient): UserIdProvider {

    override val userId: String
        get() = client.auth.currentUserOrNull()?.id ?: throw IllegalStateException()
}