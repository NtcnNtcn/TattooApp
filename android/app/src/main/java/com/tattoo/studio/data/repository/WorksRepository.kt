package com.tattoo.studio.data.repository

import com.tattoo.studio.data.local.db.dao.TattooDao
import com.tattoo.studio.data.local.db.entity.WorkEntity
import com.tattoo.studio.data.remote.api.WorksApi
import com.tattoo.studio.data.remote.dto.TattooWorkDetailDto
import com.tattoo.studio.data.remote.dto.TattooWorkFeedDto
import com.tattoo.studio.data.local.db.entity.toEntity
import com.tattoo.studio.data.local.db.entity.toDto
import com.tattoo.studio.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorksRepository @Inject constructor(
    private val api: WorksApi,
    private val dao: TattooDao
) {
    fun getWorksOffline(): Flow<List<WorkEntity>> = dao.getAllWorks()

    suspend fun refreshWorks(page: Int = 1, tags: String? = null, sort: String = "date", masterId: Int? = null): Result<List<TattooWorkFeedDto>> {
        return safeApiCall {
            val response = api.listWorks(page, 20, tags, sort, masterId)
            if (page == 1 && tags == null && masterId == null) {
                // Cache first page of general feed
                val entities = response.items.map {
                    WorkEntity(
                        id = it.id,
                        imageUrl = it.imageUrl,
                        status = it.status,
                        likeCount = it.likeCount,
                        createdAt = it.createdAt,
                        tagNames = it.tags.joinToString(",") { tag -> tag.name },
                        isLiked = it.isLiked,
                        isFavorited = it.isFavorited,
                        masterName = it.masterName,
                        masterAvatar = it.masterAvatar,
                        masterId = it.masterId
                    )
                }
                dao.clearAllWorks()
                dao.insertWorks(entities)
            }
            response.items
        }
    }

    suspend fun getWorkDetail(id: Int): Result<TattooWorkDetailDto> {
        val remoteResult = safeApiCall { api.getWork(id) }
        
        if (remoteResult.isSuccess) {
            val detail = remoteResult.getOrThrow()
            dao.insertWorkDetail(detail.toEntity())
            return remoteResult
        }
        
        // If remote failed, try cache
        val cachedDetail = dao.getWorkDetail(id)
        return if (cachedDetail != null) {
            Result.success(cachedDetail.toDto())
        } else {
            remoteResult
        }
    }

    suspend fun uploadWork(imageFile: File, description: String?, tags: String, mimeType: String): Result<Unit> {
        return safeApiCall {
            val reqFile = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, reqFile)
            val descPart = description?.toRequestBody("text/plain".toMediaTypeOrNull())
            val tagsPart = tags.toRequestBody("text/plain".toMediaTypeOrNull())

            api.uploadWork(imagePart, descPart, tagsPart)
        }
    }
}
