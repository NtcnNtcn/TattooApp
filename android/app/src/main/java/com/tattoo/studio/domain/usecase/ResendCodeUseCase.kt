package com.tattoo.studio.domain.usecase

import com.tattoo.studio.data.repository.AuthRepository
import javax.inject.Inject

class ResendCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, type: String): Result<Unit> {
        return authRepository.resendCode(email, type)
    }
}
