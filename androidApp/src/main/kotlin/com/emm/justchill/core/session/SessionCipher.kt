package com.emm.justchill.core.session

internal interface SessionCipher {

    fun encrypt(plaintext: String): String

    /**
     * Null whenever the payload cannot be read back — a wiped or invalidated key must land the user
     * on the login screen, never in a crash loop at cold start.
     */
    fun decrypt(payload: String): String?
}
