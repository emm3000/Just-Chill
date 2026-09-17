package com.emm.justchill.core.database.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Auth.init() flips Initializing to NotAuthenticated from its own scope on the client's default
// dispatcher, and the check is not atomic: an importSession() landing between that read and its
// write is overwritten, and every test here then runs on a session that is gone.
internal suspend fun SupabaseClient.settled(): SupabaseClient = also { it.auth.awaitInitialization() }

// INFINITE is the one value for which ktor launches no timeout coroutine at all. Any finite
// one competes with the cancellation under test and wins under load.
internal val DISABLED_REQUEST_TIMEOUT: Duration = Duration.INFINITE

// A real clock covering the whole test body, so load still beats it: it buys an unambiguous
// failure — a leaked coroutine, never a session-status assertion blaming a defect that never happened.
internal val HANG_BOUND: Duration = 10.seconds
