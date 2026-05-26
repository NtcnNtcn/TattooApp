package com.tattoo.studio.domain.usecase

import com.tattoo.studio.data.repository.AuthRepository
import com.tattoo.studio.data.remote.AppError
import javax.inject.Inject

/**
 * Use case for user registration.
 * Encapsulates the business logic of registering a new user.
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        fullName: String,
        role: String
    ): Result<Unit> {
        // Validate input
        if (email.isBlank()) {
            return Result.failure(AppError.Validation(userMessage = "Email не может быть пустым"))
        }
        if (password.length < 8) {
            return Result.failure(AppError.Validation(userMessage = "Пароль должен быть не менее 8 символов"))
        }
        if (fullName.isBlank()) {
            return Result.failure(AppError.Validation(userMessage = "Имя не может быть пустым"))
        }
        
        // Perform registration
        return authRepository.register(email, password, fullName, role).map { }
    }
}
