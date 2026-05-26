package com.tattoo.studio.data.repository

import com.tattoo.studio.data.remote.api.ModerationApi
import com.tattoo.studio.data.remote.dto.ReviewActionDto
import com.tattoo.studio.data.remote.dto.TattooWorkOutDto
import com.tattoo.studio.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModerationRepository @Inject constructor(
    private val api: ModerationApi
) {
    suspend fun getPendingWorks(): Result<List<TattooWorkOutDto>> {
        return safeApiCall {
            api.listPending().items
        }
    }

    suspend fun approveWork(workId: Int, comment: String? = null): Result<Unit> {
        return safeApiCall {
            api.approveWork(workId, ReviewActionDto(comment))
        }
    }

    suspend fun rejectWork(workId: Int, comment: String): Result<Unit> {
        return safeApiCall {
            api.rejectWork(workId, ReviewActionDto(comment))
        }
    }
}
