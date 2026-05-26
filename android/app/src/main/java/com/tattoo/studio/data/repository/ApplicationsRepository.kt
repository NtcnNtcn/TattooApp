package com.tattoo.studio.data.repository

import com.tattoo.studio.data.remote.api.ApplicationsApi
import com.tattoo.studio.data.remote.dto.ApplicationCreateDto
import com.tattoo.studio.data.remote.dto.ApplicationOutDto
import com.tattoo.studio.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationsRepository @Inject constructor(
    private val api: ApplicationsApi
) {
    suspend fun createApplication(masterId: Int, contactInfo: String, message: String?): Result<ApplicationOutDto> {
        return safeApiCall {
            api.createApplication(
                ApplicationCreateDto(
                    masterId = masterId,
                    contactInfo = contactInfo,
                    message = message
                )
            )
        }
    }

    suspend fun getMyApplications(): Result<List<ApplicationOutDto>> {
        return safeApiCall {
            api.listMyApplications()
        }
    }
}
