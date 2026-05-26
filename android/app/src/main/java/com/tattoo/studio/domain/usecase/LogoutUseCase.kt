package com.tattoo.studio.domain.usecase

import com.tattoo.studio.data.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user logout.
 * Clears tokens and user data.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.logout()
    }
}
