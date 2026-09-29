package com.emm.justchill.core

import com.emm.justchill.feature.auth.GoogleSignInLauncher
import com.emm.justchill.feature.auth.GoogleSignInResult

class NoCredentialsGoogleSignInLauncher : GoogleSignInLauncher {

    override suspend fun signIn(serverClientId: String): GoogleSignInResult = GoogleSignInResult.NoCredentials
}
