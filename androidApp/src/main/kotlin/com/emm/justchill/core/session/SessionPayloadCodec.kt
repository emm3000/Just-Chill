package com.emm.justchill.core.session

import java.util.Base64

// Base64 never emits '.', so splitting on the first one cannot cut either half in two.
private const val PAYLOAD_SEPARATOR = '.'

// AndroidKeyStore's AES/GCM Cipher.init accepts only a 12-byte IV; any other length throws
// InvalidAlgorithmParameterException, a type willNeverReadBack() does not match.
private const val IV_BYTE_COUNT = 12

// This file carries no android.* import, on purpose: it is the half of KeystoreSessionCipher that
// never touches AndroidKeyStore, so androidApp/src/test can reach it on the JVM host runner.
internal object SessionPayloadCodec {

    // No length check here: wrap's only caller passes the iv AndroidKeyStore itself generated, so
    // mirroring unwrap's require would guard against a length that can never actually arrive.
    fun wrap(iv: ByteArray, ciphertext: ByteArray): String = "${encode(iv)}$PAYLOAD_SEPARATOR${encode(ciphertext)}"

    fun unwrap(payload: String): Pair<ByteArray, ByteArray> {
        val separator = payload.indexOf(PAYLOAD_SEPARATOR)
        require(separator >= 0) { "Payload carries no separator" }
        val iv = decode(payload.substring(0, separator))
        require(iv.size == IV_BYTE_COUNT) { "IV is ${iv.size} bytes, expected $IV_BYTE_COUNT" }
        return iv to decode(payload.substring(separator + 1))
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)
}
