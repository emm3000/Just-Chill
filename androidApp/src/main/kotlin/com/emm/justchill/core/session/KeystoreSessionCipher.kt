package com.emm.justchill.core.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
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

// Applied on decrypt only: an AndroidKeyStore key defaults setRandomizedEncryptionRequired(true),
// which rejects a caller-supplied IV on encrypt — the provider must draw it, and GCMParameterSpec
// has no constructor that states a tag length without also stating an IV — so encrypt trusts
// AndroidKeyStore's own 128-bit GCM tag default instead.
private const val TAG_SIZE_BITS = 128

internal class KeystoreSessionCipher : SessionCipher {

    private val keyLock = Any()

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return SessionPayloadCodec.wrap(cipher.iv, ciphertext)
    }

    // runCatching catches Throwable, which would swallow a CancellationException — decipher neither
    // suspends nor polls for cancellation, so there is none here to swallow.
    override fun decrypt(payload: String): Result<String> = runCatching { decipher(payload) }

    private fun decipher(payload: String): String {
        val (iv, ciphertext) = SessionPayloadCodec.unwrap(payload)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, sessionKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    // Serialised because two first-use calls racing on the auth dispatcher would each generate a
    // key, and the second replaces the alias — orphaning whatever the first already encrypted.
    private fun sessionKey(): SecretKey = synchronized(keyLock) {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = runCatching { keyStore.getKey(KEY_ALIAS, null) }.getOrNull()
        existing as? SecretKey ?: generateSessionKey(keyStore)
    }

    private fun generateSessionKey(keyStore: KeyStore): SecretKey {
        // An alias present but unreadable is the documented keystore-corruption state. Left in
        // place it dead-ends the app: encrypt throws forever, so no sign-in can ever persist
        // again. Dropping it costs one re-login.
        runCatching { keyStore.deleteEntry(KEY_ALIAS) }

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
}
