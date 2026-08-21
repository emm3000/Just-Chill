package com.emm.justchill.core.session

import java.util.Base64

// Base64 never emits '.', so splitting on the first one cannot cut either half in two.
private const val PAYLOAD_SEPARATOR = '.'

// This file carries no android.* import, on purpose: it is the half of KeystoreSessionCipher that
// never touches AndroidKeyStore, so androidApp/src/test can reach it on the JVM host runner.
internal object SessionPayloadCodec {

    fun wrap(iv: ByteArray, ciphertext: ByteArray): String = "${encode(iv)}$PAYLOAD_SEPARATOR${encode(ciphertext)}"

    fun unwrap(payload: String): Pair<ByteArray, ByteArray> {
        val separator = payload.indexOf(PAYLOAD_SEPARATOR)
        require(separator >= 0) { "Payload carries no separator" }
        return decode(payload.substring(0, separator)) to decode(payload.substring(separator + 1))
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)
}
