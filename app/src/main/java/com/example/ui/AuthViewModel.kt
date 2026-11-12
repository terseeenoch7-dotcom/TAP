package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthViewModel"

sealed interface AuthUiState {
    data object Unauthenticated : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val user: FirebaseUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    private val _uiState = MutableStateFlow<AuthUiState>(
        if (auth.currentUser != null) AuthUiState.Authenticated(auth.currentUser!!) else AuthUiState.Unauthenticated
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            _uiState.value = AuthUiState.Authenticated(user)
        } else {
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    fun attemptAutoSignIn(context: Context) {
        if (auth.currentUser != null) {
            _uiState.value = AuthUiState.Authenticated(auth.currentUser!!)
            return
        }

        val clientId = getWebClientId(context)

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCred = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(authCred).await()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Silent auto sign-in unfulfilled: ${e.message}")
            }
        }
    }

    private fun getWebClientId(context: Context): String {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val str = context.getString(resId)
                if (str.isNotBlank()) return str
            }
        } catch (_: Exception) {}

        try {
            return context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {}

        return "1004173276169-lau2sk38rj95mmpuing126ao1sva1r6m.apps.googleusercontent.com"
    }

    fun signInWithGoogle(context: Context) {
        val clientId = getWebClientId(context)
        _uiState.value = AuthUiState.Loading

        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        viewModelScope.launch {
            try {
                val activity = context as? Activity
                    ?: throw IllegalStateException("Context must be an Activity for Google Sign-In")
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCred = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(authCred).await()
                } else {
                    _uiState.value = AuthUiState.Error("Unsupported credential received")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In dismissed: ${e.message}")
                _uiState.value = AuthUiState.Unauthenticated
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Sign-in failed")
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(trimmedEmail, password).await()
                val user = result.user
                if (user != null) {
                    _uiState.value = AuthUiState.Authenticated(user)
                } else {
                    _uiState.value = AuthUiState.Error("Sign-in succeeded but user session could not be established.")
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _uiState.value = AuthUiState.Error("Invalid email or password. Please check your credentials.")
            } catch (e: Exception) {
                Log.e(TAG, "Email sign-in failed", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Sign-in failed. Please try again.")
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, username: String) {
        val trimmedEmail = email.trim()
        val trimmedUsername = username.trim()

        if (trimmedUsername.length < 2) {
            _uiState.value = AuthUiState.Error("Username must be at least 2 characters.")
            return
        }
        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
                val user = result.user
                if (user != null) {
                    // Update user profile display name
                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedUsername)
                            .build()
                        user.updateProfile(profileUpdates).await()
                    } catch (pe: Exception) {
                        Log.w(TAG, "Failed to set display name on FirebaseUser", pe)
                    }
                    _uiState.value = AuthUiState.Authenticated(user)
                } else {
                    _uiState.value = AuthUiState.Error("Account creation succeeded but user session is empty.")
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                _uiState.value = AuthUiState.Error("An account with this email already exists. Please sign in.")
            } catch (e: Exception) {
                Log.e(TAG, "Email sign-up failed", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Account creation failed. Please try again.")
            }
        }
    }

    fun signOut(context: Context) {
        auth.signOut()
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state", e)
            } finally {
                _uiState.value = AuthUiState.Unauthenticated
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }
}
