package com.tattoo.studio.data.repository

import com.tattoo.studio.data.remote.api.UsersApi
import com.tattoo.studio.data.remote.dto.UserDto
import com.tattoo.studio.data.local.db.dao.TattooDao
import com.tattoo.studio.data.local.db.entity.toEntity
import com.tattoo.studio.data.local.db.entity.toDto
import com.tattoo.studio.data.remote.safeApiCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: UsersApi,
    private val dao: TattooDao
) {
    suspend fun getMe(): Result<UserDto> {
        val remoteResult = safeApiCall { api.getMe() }
        
        if (remoteResult.isSuccess) {
            val user = remoteResult.getOrThrow()
            dao.insertUserProfile(user.toEntity())
            return remoteResult
        }
        
        // If remote failed, try cache
        val cachedUser = dao.getUserProfile()
        return if (cachedUser != null) {
            Result.success(cachedUser.toDto())
        } else {
            remoteResult
        }
    }

    suspend fun updateProfile(
        fullName: String? = null,
        email: String? = null,
        description: String? = null,
        avatarUrl: String? = null
    ): Result<UserDto> {
        val payload = mutableMapOf<String, String?>()
        if (fullName != null) payload["full_name"] = fullName
        if (email != null) payload["email"] = email
        if (description != null) payload["description"] = description
        if (avatarUrl != null) payload["avatar_url"] = avatarUrl
        return safeApiCall { api.updateMe(payload) }
    }

    suspend fun uploadAvatar(imageFile: File, mimeType: String): Result<UserDto> {
        return safeApiCall {
            val reqFile = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", imageFile.name, reqFile)
            api.uploadAvatar(imagePart)
        }
    }

    suspend fun verifyEmailChange(email: String, code: String): Result<Unit> {
        return safeApiCall {
            api.verifyEmailChange(com.tattoo.studio.data.remote.dto.VerifyCodeRequestDto(email, code, "email_change"))
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return safeApiCall { api.deleteMe() }
    }
}
