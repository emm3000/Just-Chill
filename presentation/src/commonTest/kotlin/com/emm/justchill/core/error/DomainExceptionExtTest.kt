package com.emm.justchill.core.error

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the boundary that let 24 English validation strings reach the Spanish snackbar:
 * `toUserMessage()` must translate the [ValidationCode], never echo the diagnostic `message`.
 */
class DomainExceptionExtTest {

    private val diagnosticMessage = "Name cannot be empty"

    @Test
    fun `every validation code maps to a non-blank message`() {
        ValidationCode.entries.forEach { code ->
            val message = DomainException.ValidationError(diagnosticMessage, code).toUserMessage()
            assertTrue(message.isNotBlank(), "ValidationCode.$code has no user-facing message")
        }
    }

    @Test
    fun `no validation code echoes the diagnostic message`() {
        ValidationCode.entries.forEach { code ->
            val message = DomainException.ValidationError(diagnosticMessage, code).toUserMessage()
            assertFalse(
                message == diagnosticMessage,
                "ValidationCode.$code leaks the English diagnostic message to the user",
            )
        }
    }

    @Test
    fun `every code except Unspecified has its own message`() {
        val specified = ValidationCode.entries - ValidationCode.Unspecified
        val messages = specified.map { DomainException.ValidationError(diagnosticMessage, it).toUserMessage() }
        assertEquals(specified.size, messages.toSet().size, "two validation codes share the same message: $messages")
    }

    @Test
    fun `an untagged validation error falls back to the generic message`() {
        val untagged = DomainException.ValidationError(diagnosticMessage)
        val explicit = DomainException.ValidationError(diagnosticMessage, ValidationCode.Unspecified)
        assertEquals(explicit.toUserMessage(), untagged.toUserMessage())
    }

    @Test
    fun `password length message states the actual minimum`() {
        val message = DomainException.ValidationError(
            diagnosticMessage,
            ValidationCode.PasswordTooShort,
        ).toUserMessage()
        assertTrue(message.contains("8"), "expected the minimum length in the message, got: $message")
    }
}
