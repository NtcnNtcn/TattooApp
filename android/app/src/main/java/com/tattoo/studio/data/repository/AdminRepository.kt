package com.tattoo.studio.data.repository

import com.tattoo.studio.data.remote.api.AdminApi
import com.tattoo.studio.data.remote.dto.AdminStatsDto
import com.tattoo.studio.data.remote.dto.ConsultationApplicationDto
import com.tattoo.studio.data.remote.dto.MasterApplicationDto
import com.tattoo.studio.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val api: AdminApi
) {
    suspend fun getStats(): Result<AdminStatsDto> =
        safeApiCall { api.getStats() }

    suspend fun getMasterApplications(): Result<List<MasterApplicationDto>> =
        safeApiCall { api.listMasterApplications() }

    suspend fun approveMasterApplication(appId: Int): Result<Unit> =
        safeApiCall { api.approveMasterApplication(appId) }

    suspend fun rejectMasterApplication(appId: Int): Result<Unit> =
        safeApiCall { api.rejectMasterApplication(appId) }

    suspend fun getConsultationApplications(): Result<List<ConsultationApplicationDto>> =
        safeApiCall { api.listConsultationApplications() }

    suspend fun closeConsultationApplication(appId: Int): Result<Unit> =
        safeApiCall { api.closeConsultationApplication(appId) }
}
