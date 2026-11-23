package com.example.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object AuthHelper {
    private const val TAG = "Auth"
    const val BOOTSTRAP_ADMIN_EMAIL = "satyamchakraborty14@gmail.com"

    fun authStateFlow(auth: FirebaseAuth = Firebase.auth): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun isBootstrapAdmin(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return user.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun getAppSigningSha1(context: Context): String {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = info.signingInfo ?: return ""
                if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                info.signatures
            }
            val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return ""
            val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onUnauthenticated()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        scope.launch {
            try {
                val cred = credentialManager.getCredential(context, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: NoCredentialException) {
                Log.d(TAG, "No saved Google credentials for auto sign-in: ${e.message}")
                onUnauthenticated()
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "Auto sign-in cancelled: ${e.message}")
                onUnauthenticated()
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Auto sign-in unavailable: ${e.message}")
                onUnauthenticated()
            } catch (e: Exception) {
                Log.w(TAG, "Auto sign-in failed: ${e.message}")
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        scope.launch {
            try {
                val activity = context.findActivity()
                    ?: throw IllegalStateException("Activity context required for Credential Manager")
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google account available on device: ${e.message}")
                val sha1 = getAppSigningSha1(context)
                val shaHint = if (sha1.isNotBlank()) "\n\nIf you already have a Google account on this phone, register this APK SHA-1 in Firebase Console:\n$sha1" else ""
                onAuthError("No Google account found on this device. Please add a Google account in device Settings and try again.$shaHint")
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Google Sign-In credential error: ${e.message}")
                val sha1 = getAppSigningSha1(context)
                val baseMsg = e.errorMessage?.toString() ?: e.localizedMessage ?: "Google Sign-In failed"
                val shaHint = if (sha1.isNotBlank()) "\n\nAPK SHA-1: $sha1" else ""
                onAuthError("$baseMsg$shaHint")
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    suspend fun resendEmailVerification(): Result<Unit> = try {
        val user = Firebase.auth.currentUser ?: throw IllegalStateException("Not signed in.")
        user.sendEmailVerification().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun reloadCurrentUser(): Result<FirebaseUser?> = try {
        val user = Firebase.auth.currentUser
        user?.reload()?.await()
        Result.success(Firebase.auth.currentUser)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun startPhoneVerification(
        activity: Activity,
        phoneNumberE164: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneNumberE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun linkOrUpdatePhoneCredential(credential: PhoneAuthCredential): Result<String> = try {
        val user = Firebase.auth.currentUser ?: throw IllegalStateException("Must be signed in.")
        try {
            user.linkWithCredential(credential).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            return Result.failure(IllegalStateException("This mobile number is already linked to another account."))
        } catch (_: FirebaseAuthException) {
            user.updatePhoneNumber(credential).await()
        }
        user.reload().await()
        val verifiedNumber = Firebase.auth.currentUser?.phoneNumber ?: ""
        Result.success(verifiedNumber)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }
}
