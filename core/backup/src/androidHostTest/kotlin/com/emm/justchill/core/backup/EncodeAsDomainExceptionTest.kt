package com.emm.justchill.core.backup

import com.emm.justchill.core.domain.shared.error.DomainException
import kotlinx.serialization.SerializationException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EncodeAsDomainExceptionTest {

    @Test
    fun `a serialization failure translates to SerializationError, not Unknown or DatabaseError`() {
        val ex = assertFailsWith<DomainException.SerializationError> {
            encodeAsDomainException<String> { throw SerializationException("boom") }
        }

        assertEquals("boom", ex.message)
    }
}
