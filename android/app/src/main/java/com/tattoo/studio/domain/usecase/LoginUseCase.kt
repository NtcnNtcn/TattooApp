package com.tattoo.studio.domain.usecase

import com.tattoo.studio.data.repository.AuthRepository
import com.tattoo.studio.data.remote.AppError
import javax.inject.Inject

/**
 * Use case for user login.
 * Encapsulates the business logic of authenticating a user.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        // Validate input
        if (email.isBlank()) {
            return Result.failure(AppError.Validation(userMessage = "Email не может быть пустым"))
        }
        if (password.isBlank()) {
            return Result.failure(AppError.Validation(userMessage = "Пароль не может быть пустым"))
        }
        
        // Perform login
        return authRepository.login(email, password)
    }
}
