package com.emm.justchill.hh.auth

import com.emm.justchill.core.platform.CurrentActivityHolder

class ActivityGoogleSignInLauncher(
    private val activityHolder: CurrentActivityHolder,
    private val client: GoogleCredentialClient,
) : GoogleSignInLauncher {

    override suspend fun signIn(serverClientId: String): GoogleSignInResult {
        val activity = activityHolder.current
            ?: return GoogleSignInResult.Failure(
                IllegalStateException("No foreground activity to launch credential sheet"),
            )
        return client.signIn(activity, serverClientId)
    }
}
