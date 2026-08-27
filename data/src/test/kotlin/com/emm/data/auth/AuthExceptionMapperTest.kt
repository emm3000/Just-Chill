package com.emm.data.auth

import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthExceptionMapperTest {

    @Test
    fun `AuthRestException with non-validation code maps to Unauthorized`() {
        val response = mockk<HttpResponse>(relaxed = true)
        val ex = AuthRestException(
            errorCode = "user_not_found",
            errorDescription = "User not found",
            response = response,
        )
        assertIs<DomainException.Unauthorized>(ex.toAuthDomainException())
    }

    @Test
    fun `AuthRestException with weak_password code maps to ValidationError`() {
        val response = mockk<HttpResponse>(relaxed = true)
        val ex = AuthRestException(
            errorCode = "weak_password",
            errorDescription = "Password is too weak",
            response = response,
        )
        assertIs<DomainException.ValidationError>(ex.toAuthDomainException())
    }

    @Test
    fun `AuthRestException with email_exists code maps to ValidationError`() {
        val response = mockk<HttpResponse>(relaxed = true)
        val ex = AuthRestException(
            errorCode = "email_exists",
            errorDescription = "Email already registered",
            response = response,
        )
        assertIs<DomainException.ValidationError>(ex.toAuthDomainException())
    }

    @Test
    fun `AuthWeakPasswordException subtype maps to ValidationError`() {
        val response = mockk<HttpResponse>(relaxed = true)
        val ex = AuthWeakPasswordException(
            description = "Password is too weak",
            response = response,
            reasons = listOf("length"),
        )
        assertIs<DomainException.ValidationError>(ex.toAuthDomainException())
    }

    @Test
    fun `UnauthorizedRestException maps to Unauthorized`() {
        val response = mockk<HttpResponse>(relaxed = true)
        val ex = UnauthorizedRestException(error = "unauthorized", response = response)
        assertIs<DomainException.Unauthorized>(ex.toAuthDomainException())
    }

    @Test
    fun `SessionRequiredException maps to Unauthorized`() {
        val ex = SessionRequiredException(url = "https://example.supabase.co/rest/v1/rpc/delete_account")
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
    fun `generic RestException is typed as RemoteRejected, mirroring the backup path`() {
        val response = mockk<HttpResponse>(relaxed = true)
        every { response.status } returns HttpStatusCode(503, "test")
        val ex = object : RestException(error = "some_error", description = "desc", response = response) {}

        val rejected = ex.toAuthDomainException()

        assertIs<DomainException.RemoteRejected>(rejected)
        assertEquals(503, rejected.statusCode)
    }

    @Test
    fun `arbitrary exception maps to Unknown`() {
        val ex = IllegalStateException("unexpected")
        assertIs<DomainException.Unknown>(ex.toAuthDomainException())
    }
}
