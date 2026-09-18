package com.emm.justchill.core.auth

import com.emm.justchill.core.platform.CurrentActivityHolder
import com.emm.justchill.feature.auth.GoogleSignInLauncher
import com.emm.justchill.feature.auth.GoogleSignInResult

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
