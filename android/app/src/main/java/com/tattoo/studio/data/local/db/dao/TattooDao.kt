package com.tattoo.studio.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tattoo.studio.data.local.db.entity.WorkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TattooDao {
    @Query("SELECT * FROM works ORDER BY createdAt DESC")
    fun getAllWorks(): Flow<List<WorkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorks(works: List<WorkEntity>)

    @Query("DELETE FROM works")
    suspend fun clearAllWorks()

    // Profile caching
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfile(): com.tattoo.studio.data.local.db.entity.UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(user: com.tattoo.studio.data.local.db.entity.UserEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    // Favorites caching
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    suspend fun getFavorites(): List<com.tattoo.studio.data.local.db.entity.FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<com.tattoo.studio.data.local.db.entity.FavoriteEntity>)

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    // Work Details caching
    @Query("SELECT * FROM work_details WHERE id = :id")
    suspend fun getWorkDetail(id: Int): com.tattoo.studio.data.local.db.entity.WorkDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDetail(detail: com.tattoo.studio.data.local.db.entity.WorkDetailEntity)
}
