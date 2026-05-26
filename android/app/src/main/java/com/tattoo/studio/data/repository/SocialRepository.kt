package com.tattoo.studio.data.repository

import com.tattoo.studio.data.remote.api.SocialApi
import com.tattoo.studio.data.remote.dto.SubscriptionDto
import com.tattoo.studio.data.remote.dto.TattooWorkFeedDto
import com.tattoo.studio.data.local.db.dao.TattooDao
import com.tattoo.studio.data.local.db.entity.toFavoriteEntity
import com.tattoo.studio.data.local.db.entity.toDto
import com.tattoo.studio.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val api: SocialApi,
    private val dao: TattooDao
) {
    suspend fun toggleLike(workId: Int): Result<Boolean> {
        return safeApiCall {
            val response = api.toggleLike(workId)
            response.active
        }
    }

    suspend fun toggleFavorite(workId: Int): Result<Boolean> {
        return safeApiCall {
            val response = api.toggleFavorite(workId)
            response.active
        }
    }

    suspend fun toggleSubscription(masterId: Int): Result<Boolean> {
        return safeApiCall {
            val response = api.toggleSubscription(masterId)
            response.active
        }
    }

    suspend fun getLikes(): Result<List<TattooWorkFeedDto>> {
        return safeApiCall {
            api.listLikes()
        }
    }

    suspend fun getFavorites(): Result<List<TattooWorkFeedDto>> {
        val remoteResult = safeApiCall { api.listFavorites() }
        
        if (remoteResult.isSuccess) {
            val favorites = remoteResult.getOrThrow()
            dao.clearFavorites()
            dao.insertFavorites(favorites.map { it.toFavoriteEntity() })
            return remoteResult
        }
        
        // If remote failed, try cache
        val cachedFavorites = dao.getFavorites()
        return if (cachedFavorites.isNotEmpty()) {
            Result.success(cachedFavorites.map { it.toDto() })
        } else {
            remoteResult
        }
    }

    suspend fun getSubscriptions(): Result<List<SubscriptionDto>> {
        return safeApiCall {
            api.listSubscriptions()
        }
    }
}
