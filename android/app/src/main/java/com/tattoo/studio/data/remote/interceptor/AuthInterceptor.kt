package com.tattoo.studio.data.remote.interceptor

import com.tattoo.studio.data.local.prefs.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the JWT access token to every request.
 * If a 401 is received, attempts to refresh the token once.
 * If refresh fails, clears tokens to force re-authentication.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {

    companion object {
        // Used to signal auth failures to the app (e.g., for logout navigation)
        val authFailureOccurred = AtomicBoolean(false)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking { tokenManager.getAccessToken() }

        val request = chain.request().newBuilder().apply {
            if (accessToken != null) {
                addHeader("Authorization", "Bearer $accessToken")
            }
        }.build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            // Attempt token refresh
            val newToken = runBlocking { tryRefresh(tokenManager) }
            
            if (newToken == null) {
                // Refresh failed - clear tokens and signal auth failure
                runBlocking { tokenManager.clearTokens() }
                authFailureOccurred.set(true)
                return response
            }

            // Free connection before retry
            response.close()

            val retryRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $newToken")
                .build()
            return chain.proceed(retryRequest)
        }

        return response
    }

    private suspend fun tryRefresh(tokenManager: TokenManager): String? {
        val refreshToken = tokenManager.getRefreshToken() ?: return null
        return try {
            // Minimal OkHttp call to avoid circular Retrofit dependency
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val mediaType = "application/json".toMediaTypeOrNull()
            val body = """{"refresh_token":"$refreshToken"}""".toRequestBody(mediaType)
            val req = okhttp3.Request.Builder()
                .url("${com.tattoo.studio.BuildConfig.BASE_URL}/api/auth/refresh")
                .post(body)
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = resp.body?.string() ?: return null
                    val gson = com.google.gson.Gson()
                    val tokens = gson.fromJson(json, com.tattoo.studio.data.remote.dto.TokenPairDto::class.java)
                    tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                    tokens.accessToken
                } else {
                    // Server rejected refresh token (expired/invalid)
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
