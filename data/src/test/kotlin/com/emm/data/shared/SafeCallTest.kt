package com.emm.data.shared

import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class SafeCallTest {

    @Test
    fun `safeDbCall rethrows a cancellation untouched instead of typing it as Unknown`() = runTest {
        val cancelled = CancellationException("the caller cancelled the loader")

        val thrown = assertFailsWith<CancellationException> {
            safeDbCall<Unit> { throw cancelled }
        }

        assertSame(cancelled, thrown)
    }

    @Test
    fun `safeDbCall still types an ordinary failure as Unknown`() = runTest {
        assertFailsWith<DomainException.Unknown> {
            safeDbCall<Unit> { throw IllegalStateException("the query blew up") }
        }
    }

    @Test
    fun `catchAsDomainException rethrows a cancellation untouched instead of typing it as Unknown`() = runTest {
        val cancelled = CancellationException("the upstream cancelled the emission")

        val thrown = assertFailsWith<CancellationException> {
            flow<Unit> { throw cancelled }.catchAsDomainException().toList()
        }

        assertSame(cancelled, thrown)
    }

    @Test
    fun `catchAsDomainException still types an ordinary failure as Unknown`() = runTest {
        assertFailsWith<DomainException.Unknown> {
            flow<Unit> { throw IllegalStateException("the query blew up") }.catchAsDomainException().toList()
        }
    }
}
