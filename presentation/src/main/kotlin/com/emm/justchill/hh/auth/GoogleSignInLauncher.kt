package com.emm.justchill.hh.auth

interface GoogleSignInLauncher {
    suspend fun signIn(serverClientId: String): GoogleSignInResult
}
