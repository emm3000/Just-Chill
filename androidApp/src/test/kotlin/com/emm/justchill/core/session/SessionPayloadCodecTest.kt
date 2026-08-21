package com.emm.justchill.core.session

import org.junit.Test
import java.util.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SessionPayloadCodecTest {

    @Test
    fun `wrap then unwrap returns the exact same iv and ciphertext bytes`() {
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val ciphertext = byteArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1)

        val (unwrappedIv, unwrappedCiphertext) = SessionPayloadCodec.unwrap(SessionPayloadCodec.wrap(iv, ciphertext))

        assertContentEquals(iv, unwrappedIv)
        assertContentEquals(ciphertext, unwrappedCiphertext)
    }

    @Test
    fun `a ciphertext half that is empty still round-trips`() {
        val iv = byteArrayOf(1, 2, 3)

        val (unwrappedIv, unwrappedCiphertext) = SessionPayloadCodec.unwrap(SessionPayloadCodec.wrap(iv, byteArrayOf()))

        assertContentEquals(iv, unwrappedIv)
        assertContentEquals(byteArrayOf(), unwrappedCiphertext)
    }

    @Test
    fun `a payload with no separator throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { SessionPayloadCodec.unwrap("no-separator-here") }
    }

    @Test
    fun `garbage base64 in the iv half throws IllegalArgumentException`() {
        val payload = "not!valid.${base64Of(byteArrayOf(1, 2, 3))}"

        assertFailsWith<IllegalArgumentException> { SessionPayloadCodec.unwrap(payload) }
    }

    @Test
    fun `garbage base64 in the ciphertext half throws IllegalArgumentException`() {
        val payload = "${base64Of(byteArrayOf(1, 2, 3))}.not!valid"

        assertFailsWith<IllegalArgumentException> { SessionPayloadCodec.unwrap(payload) }
    }

    @Test
    fun `every exception this codec throws for malformed input is one willNeverReadBack treats as unreadable`() {
        val validHalf = base64Of(byteArrayOf(1, 2, 3))
        val malformedPayloads = listOf(
            "no-separator-here",
            "not!valid.$validHalf",
            "$validHalf.not!valid",
        )

        malformedPayloads.forEach { payload ->
            val thrown = assertFailsWith<Throwable> { SessionPayloadCodec.unwrap(payload) }
            assertTrue(thrown.willNeverReadBack(), "expected $thrown to be treated as unreadable")
        }
    }

    private fun base64Of(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}
