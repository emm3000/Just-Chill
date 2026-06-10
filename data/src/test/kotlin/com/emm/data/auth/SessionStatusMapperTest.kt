package com.emm.data.auth

import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.SessionStatus
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import io.github.jan.supabase.auth.status.SessionStatus as SupabaseSessionStatus

class SessionStatusMapperTest {

    @Test
    fun `Initializing maps to domain Initializing`() {
        val result = SupabaseSessionStatus.Initializing.toDomain()
        assertIs<SessionStatus.Initializing>(result)
    }

    @Test
    fun `NotAuthenticated maps to domain NotAuthenticated`() {
        val result = SupabaseSessionStatus.NotAuthenticated(isSignOut = false).toDomain()
        assertIs<SessionStatus.NotAuthenticated>(result)
    }

    @Test
    fun `NotAuthenticated with isSignOut true maps to domain NotAuthenticated`() {
        val result = SupabaseSessionStatus.NotAuthenticated(isSignOut = true).toDomain()
        assertIs<SessionStatus.NotAuthenticated>(result)
    }

    @Test
    fun `Authenticated with UserInfo maps to domain Authenticated with correct AuthUser`() {
        val userInfo = buildUserInfo(id = "uid-123", email = "test@example.com")
        val session = buildSession(userInfo)
        val result = SupabaseSessionStatus.Authenticated(session = session).toDomain()

        assertIs<SessionStatus.Authenticated>(result)
        assertEquals(AuthUser(userId = "uid-123", email = "test@example.com"), result.user)
    }

    @Test
    fun `Authenticated with null user maps to NotAuthenticated`() {
        val session = buildSession(user = null)
        val result = SupabaseSessionStatus.Authenticated(session = session).toDomain()
        assertIs<SessionStatus.NotAuthenticated>(result)
    }

    @Test
    fun `RefreshFailure maps to domain NotAuthenticated`() {
        val cause = RefreshFailureCause.NetworkError(RuntimeException("timeout"))

        // RefreshFailure(cause) constructor verified against supabase-kt 3.6.0; revisit on BOM bumps.
        @Suppress("DEPRECATION")
        val result = SupabaseSessionStatus.RefreshFailure(cause = cause).toDomain()
        assertIs<SessionStatus.NotAuthenticated>(result)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun buildUserInfo(id: String, email: String?): UserInfo {
        val info = mockk<UserInfo>()
        every { info.id } returns id
        every { info.email } returns email
        return info
    }

    private fun buildSession(user: UserInfo?): UserSession {
        val session = mockk<UserSession>()
        every { session.user } returns user
        return session
    }
}
