package com.tattoo.studio.domain.usecase

import com.tattoo.studio.data.repository.AuthRepository
import com.tattoo.studio.data.remote.AppError
import javax.inject.Inject

/**
 * Use case for verifying authentication codes.
 * Used for email verification, password reset, etc.
 */
class VerifyCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, code: String, type: String): Result<Boolean> {
        // Validate input
        if (email.isBlank()) {
            return Result.failure(AppError.Validation(userMessage = "Email не может быть пустым"))
        }
        if (code.length != 6) {
            return Result.failure(AppError.Validation(userMessage = "Код должен содержать 6 цифр"))
        }
        
        // Perform verification
        return authRepository.verifyCode(email, code, type)
    }
}
