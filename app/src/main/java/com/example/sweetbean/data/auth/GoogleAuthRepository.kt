package com.example.sweetbean.data.auth

interface GoogleAuthRepository {

    suspend fun signInWithGoogle(
        idToken: String
    ): GoogleSignInResult
}
