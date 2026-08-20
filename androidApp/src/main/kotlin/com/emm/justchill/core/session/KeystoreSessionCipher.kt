package com.emm.justchill.core.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Alias of the AES key held by the AndroidKeyStore. It is storage identity, not a name: change it
// and every session already encrypted under the old alias becomes unreadable, forcing a re-login.
private const val KEY_ALIAS = "justchill_session_v1"
private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val KEY_SIZE_BITS = 256
private const val TAG_SIZE_BITS = 128

// Base64 never emits '.', so splitting on the first one cannot cut either half in two.
private const val PAYLOAD_SEPARATOR = '.'

internal class KeystoreSessionCipher : SessionCipher {

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // GCM breaks if an IV repeats under one key, so the provider draws a fresh one per
        // operation and it is stored beside the ciphertext rather than fixed anywhere.
        return "${encode(cipher.iv)}$PAYLOAD_SEPARATOR${encode(ciphertext)}"
    }

    override fun decrypt(payload: String): String? = runCatching { decipher(payload) }.getOrNull()

    private fun decipher(payload: String): String {
        val separator = payload.indexOf(PAYLOAD_SEPARATOR)
        val iv = decode(payload.substring(0, separator))
        val ciphertext = decode(payload.substring(separator + 1))

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, sessionKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun sessionKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: generateSessionKey()
    }

    private fun generateSessionKey(): SecretKey {
        val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, purposes)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            // Supabase refreshes the token before any screen is drawn, so the key has to be usable
            // with nobody watching; a lock-screen requirement would deadlock the cold start.
            .setUserAuthenticationRequired(false)
            .build()

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(spec)
        return generator.generateKey()
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)
}
