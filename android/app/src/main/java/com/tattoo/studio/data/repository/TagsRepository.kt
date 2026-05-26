package com.tattoo.studio.data.repository

import com.tattoo.studio.data.remote.api.TagsApi
import com.tattoo.studio.data.remote.dto.TagDto
import com.tattoo.studio.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagsRepository @Inject constructor(
    private val api: TagsApi
) {
    suspend fun getTags(): Result<List<TagDto>> {
        return safeApiCall {
            api.listTags()
        }
    }

    suspend fun searchTags(query: String): Result<List<TagDto>> {
        return safeApiCall {
            api.searchTags(query)
        }
    }
}
