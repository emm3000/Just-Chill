package com.emm.domain.auth

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import kotlin.test.assertEquals

class ObserveSessionUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = ObserveSessionUseCase(repository)

    @Test
    fun `invoke returns sessionStatus flow from repository`() {
        val expected = flowOf(SessionStatus.NotAuthenticated)
        every { repository.sessionStatus } returns expected

        val result = useCase()

        assertEquals(expected, result)
        verify(exactly = 1) { repository.sessionStatus }
    }

    @Test
    fun `invoke exposes Authenticated state from repository`() {
        val user = AuthUser(userId = "u1", email = "a@b.com")
        val flow = flowOf(SessionStatus.Authenticated(user))
        every { repository.sessionStatus } returns flow

        val result = useCase()

        assertEquals(flow, result)
    }
}
