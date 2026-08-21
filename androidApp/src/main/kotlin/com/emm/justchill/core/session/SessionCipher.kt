package com.emm.justchill.core.session

internal interface SessionCipher {

    fun encrypt(plaintext: String): String

    /**
     * A failed read is returned, never thrown: a wiped or invalidated key must land the user on the
     * login screen, not in a crash loop at cold start. The cause travels with the failure because it
     * is the only thing separating a key that is gone from a provider that failed once — the caller
     * discards the payload in the first case and keeps it in the second.
     */
    fun decrypt(payload: String): Result<String>
}
