package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
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
import kotlin.test.assertTrue

private const val REASON = "Snapshot backup failed: the payload could not be uploaded to user/backup.json."

class BackupFailuresTest {

    @Test
    fun `a rejected request carries the status the server answered with`() {
        val rejected = restException(status = 413, error = "Payload too large").asBackupFailure(REASON)

        assertIs<DomainException.RemoteRejected>(rejected)
        assertEquals(413, rejected.statusCode)
    }

    @Test
    fun `a rejection the app never anticipated is still typed, not folded into Unknown`() {
        val rejected = restException(status = 503, error = "Service unavailable").asBackupFailure(REASON)

        assertIs<DomainException.RemoteRejected>(rejected)
        assertEquals(503, rejected.statusCode)
    }

    @Test
    fun `the message keeps the reason, the status and the body the server sent`() {
        val refusal = restException(status = 415, error = "mime type text/plain is not supported")

        val message: String = refusal.asBackupFailure(REASON).message.orEmpty()
        assertTrue(message.startsWith(REASON), message)
        assertTrue(message.contains("HTTP 415"), message)
        assertTrue(message.contains("mime type text/plain is not supported"), message)
    }

    @Test
    fun `an expired session stays Unauthorized and never becomes a remote rejection`() {
        val response: HttpResponse = responseWith(status = 401)
        val refused = UnauthorizedRestException(error = "invalid JWT", response = response)

        assertIs<DomainException.Unauthorized>(refused.asBackupFailure(REASON))
    }

    @Test
    fun `an absent session stays Unauthorized and never becomes a remote rejection`() {
        val absent = SessionRequiredException(url = "https://example.supabase.co/storage/v1/object/backups")

        assertIs<DomainException.Unauthorized>(absent.asBackupFailure(REASON))
    }

    @Test
    fun `an unreachable server maps to NetworkUnavailable, not to a rejection`() {
        val refused = HttpRequestException(message = "connection reset", request = HttpRequestBuilder())
        val timedOut = HttpRequestTimeoutException(url = "https://example.supabase.co", timeoutMillis = 120_000L)

        assertIs<DomainException.NetworkUnavailable>(refused.asBackupFailure(REASON))
        assertIs<DomainException.NetworkUnavailable>(timedOut.asBackupFailure(REASON))
    }

    @Test
    fun `a throwable that never reached the server stays Unknown`() {
        assertIs<DomainException.Unknown>(IllegalStateException("nobody saw this coming").asBackupFailure(REASON))
    }

    @Test
    fun `a DomainException raised further down is passed through untouched`() {
        val original = DomainException.Busy("another operation holds the lock")

        assertEquals(original, original.asBackupFailure(REASON))
    }
}

private fun restException(status: Int, error: String): RestException =
    object : RestException(error = error, description = null, response = responseWith(status)) {}

private fun responseWith(status: Int): HttpResponse {
    val response = mockk<HttpResponse>(relaxed = true)
    every { response.status } returns HttpStatusCode(status, "test")
    return response
}
