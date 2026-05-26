package com.tattoo.studio.data.repository

import com.tattoo.studio.data.local.prefs.TokenManager
import com.tattoo.studio.data.local.prefs.UserPreferences
import com.tattoo.studio.data.remote.api.AuthApi
import com.tattoo.studio.data.remote.dto.UserDto
import com.tattoo.studio.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenManager: TokenManager,
    private val userPrefs: UserPreferences
) {
    suspend fun register(email: String, password: String, fullName: String, role: String): Result<UserDto> {
        return safeApiCall {
            api.register(
                mapOf(
                    "email" to email,
                    "password" to password,
                    "full_name" to fullName,
                    "role" to role
                )
            )
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return safeApiCall {
            val tokens = api.login(email, password)
            tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
        }
    }

    suspend fun logout() {
        tokenManager.clearTokens()
        userPrefs.clearUser()
    }
    
    suspend fun updateFcmToken(token: String) {
        try {
            api.updateFcmToken(com.tattoo.studio.data.remote.dto.FCMTokenDto(token))
        } catch (e: Exception) {
            // Best effort, ignore if fails
        }
    }

    suspend fun verifyCode(email: String, code: String, type: String): Result<Boolean> {
        return safeApiCall {
            val response = api.verifyCode(com.tattoo.studio.data.remote.dto.VerifyCodeRequestDto(email, code, type))
            val tokens = response.body()
            if (tokens != null) {
                tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                true
            } else {
                false
            }
        }
    }

    suspend fun resendCode(email: String, type: String): Result<Unit> {
        return safeApiCall {
            api.resendCode(com.tattoo.studio.data.remote.dto.ResendCodeRequestDto(email, type))
        }
    }

    suspend fun forgotPassword(email: String): Result<Unit> {
        return safeApiCall {
            api.forgotPassword(com.tattoo.studio.data.remote.dto.ForgotPasswordRequestDto(email))
        }
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> {
        return safeApiCall {
            api.resetPassword(com.tattoo.studio.data.remote.dto.ResetPasswordRequestDto(email, code, newPassword))
        }
    }
}
