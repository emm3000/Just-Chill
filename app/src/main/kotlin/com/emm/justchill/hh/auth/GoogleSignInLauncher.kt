package com.emm.justchill.hh.auth

import com.emm.justchill.core.platform.CurrentActivityHolder

interface GoogleSignInLauncher {
    suspend fun signIn(serverClientId: String): GoogleCredentialClient.Result
}

class ActivityGoogleSignInLauncher(
    private val activityHolder: CurrentActivityHolder,
    private val client: GoogleCredentialClient,
) : GoogleSignInLauncher {

    override suspend fun signIn(serverClientId: String): GoogleCredentialClient.Result {
        val activity = activityHolder.current
            ?: return GoogleCredentialClient.Result.Failure(
                IllegalStateException("No foreground activity to launch credential sheet"),
            )
        return client.signIn(activity, serverClientId)
    }
}
