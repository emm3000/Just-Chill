package com.emm.data.auth

import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertIs

/**
 * Verifies each supabase / ktor exception family maps to the correct [DomainException] subtype.
 *
 * Calls the production [toAuthDomainException] directly (internal visibility, same package).
 */
class AuthExceptionMapperTest {

    @Test
    fun `AuthRestException maps to Unauthorized`() {
        val response = mockk<HttpResponse>(relaxed = true)
        val ex = AuthRestException(
            errorCode = "user_not_found",
            errorDescription = "User not found",
            response = response,
        )
        assertIs<DomainException.Unauthorized>(ex.toAuthDomainException())
    }

    @Test
    fun `UnauthorizedRestException maps to Unauthorized`() {
        val response = mockk<HttpResponse>(relaxed = true)
        val ex = UnauthorizedRestException(error = "unauthorized", response = response)
        assertIs<DomainException.Unauthorized>(ex.toAuthDomainException())
    }

    @Test
    fun `HttpRequestException maps to NetworkUnavailable`() {
        val request = HttpRequestBuilder()
        val ex = HttpRequestException(message = "connection reset", request = request)
        assertIs<DomainException.NetworkUnavailable>(ex.toAuthDomainException())
    }

    @Test
    fun `HttpRequestTimeoutException maps to NetworkUnavailable`() {
        val ex = HttpRequestTimeoutException(url = "https://example.com", timeoutMillis = 5000L)
        assertIs<DomainException.NetworkUnavailable>(ex.toAuthDomainException())
    }

    @Test
    fun `generic RestException maps to Unknown`() {
        val response = mockk<HttpResponse>(relaxed = true)
        every { response.status } returns mockk(relaxed = true)
        val ex = object : RestException(error = "some_error", description = "desc", response = response) {}
        assertIs<DomainException.Unknown>(ex.toAuthDomainException())
    }

    @Test
    fun `arbitrary exception maps to Unknown`() {
        val ex = IllegalStateException("unexpected")
        assertIs<DomainException.Unknown>(ex.toAuthDomainException())
    }
}
