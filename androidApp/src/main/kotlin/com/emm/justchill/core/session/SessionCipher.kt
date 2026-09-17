package com.emm.justchill.core.session

internal interface SessionCipher {

    fun encrypt(plaintext: String): String

    // A failed read is returned, never thrown: a wiped or invalidated key must land the user on the
    // login screen, not a crash loop at cold start. The cause travels with it because it is what
    // tells the caller whether to discard the payload or keep it for next launch.
    fun decrypt(payload: String): Result<String>
}
