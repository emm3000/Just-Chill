package com.emm.justchill.feature.auth

interface GoogleSignInLauncher {
    suspend fun signIn(serverClientId: String): GoogleSignInResult
}
