package com.emm.justchill.hh.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import java.security.MessageDigest
import java.util.UUID

class GoogleCredentialClient {

    sealed interface Result {
        data class Success(val idToken: String, val rawNonce: String) : Result
        data object Cancelled : Result
        data object NoCredentials : Result
        data class Failure(val cause: Throwable) : Result
    }

    // Intentional broad catch: this is the adapter boundary for the Credential Manager —
    // any unexpected failure maps to Result.Failure for the ViewModel to translate.
    @Suppress("TooGenericExceptionCaught")
    suspend fun signIn(activityContext: Context, serverClientId: String): Result = try {
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = sha256Hex(rawNonce)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = CredentialManager.create(activityContext)
            .getCredential(activityContext, request)

        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            Result.Success(idToken = googleCredential.idToken, rawNonce = rawNonce)
        } else {
            Result.Failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
        }
    } catch (_: GetCredentialCancellationException) {
        Result.Cancelled
    } catch (_: NoCredentialException) {
        Result.NoCredentials
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Failure(e)
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
