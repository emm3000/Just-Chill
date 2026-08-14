package com.emm.data.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * [sha256Hex] against the PUBLISHED digests, not against itself.
 *
 * A suite that only asserted "the same input hashes the same twice" would prove the function is
 * deterministic and nothing else — every wrong digest is deterministic too. The vectors below are
 * FIPS 180-4's own (`""`, `"abc"`, and the 56-byte two-block message), so the test fails if okio is
 * swapped for something that is not SHA-256, or if `hex()` ever stops being lowercase.
 */
class Sha256HexTest {

    @Test
    fun matches_the_published_digest_of_the_empty_input() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256Hex(ByteArray(0)),
        )
    }

    @Test
    fun matches_the_published_digest_of_abc() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex("abc".encodeToByteArray()),
        )
    }

    @Test
    fun matches_the_published_two_block_digest() {
        // The 448-bit vector — long enough to cross SHA-256's 512-bit block boundary, so it exercises
        // the padding path that a single short message never reaches.
        val message = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"

        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            sha256Hex(message.encodeToByteArray()),
        )
    }

    @Test
    fun hashes_the_utf8_bytes_of_an_accented_string() {
        // "ñ" is one char and two UTF-8 bytes (C3 B1). The app's data is Spanish, so an account named
        // "Ahorro año" is the ordinary case, not the exotic one — and it is exactly where a String
        // overload that encoded internally could quietly pick a different encoding than the upload.
        assertEquals("c3b1", "ñ".encodeToByteArray().joinToString("") { byte -> byte.toHexPair() })

        assertEquals(
            "024bb90888ca89a15a19e9bdd8c712bfb070465fce1ef25e43c170ea44fc5e5f",
            sha256Hex("ñ".encodeToByteArray()),
        )
        assertEquals(
            "a50d706a380c6492b50feed6b75838895111eabf12787454a45faf070e0bda1c",
            sha256Hex("categoría".encodeToByteArray()),
        )
    }

    @Test
    fun a_different_encoding_of_the_same_text_is_a_different_digest() {
        // The reason the signature takes bytes. UTF-16LE of "ñ" is F1 00 — same character, different
        // bytes, different hash. Whatever is stored has to be what was hashed.
        val utf16le = byteArrayOf(0xF1.toByte(), 0x00)

        assertNotEquals(sha256Hex(utf16le), sha256Hex("ñ".encodeToByteArray()))
    }

    @Test
    fun is_always_64_lowercase_hex_characters() {
        val digest = sha256Hex("cualquier cosa".encodeToByteArray())

        assertEquals(64, digest.length)
        assertEquals(digest.lowercase(), digest)
        assertEquals(true, digest.all { char -> char in "0123456789abcdef" })
    }
}

private fun Byte.toHexPair(): String = toInt().and(0xFF).toString(16).padStart(2, '0')
