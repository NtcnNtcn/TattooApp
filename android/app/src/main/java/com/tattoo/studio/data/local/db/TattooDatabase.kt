package com.tattoo.studio.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tattoo.studio.data.local.db.dao.TattooDao
import com.tattoo.studio.data.local.db.entity.WorkEntity
import com.tattoo.studio.data.local.db.entity.UserEntity
import com.tattoo.studio.data.local.db.entity.FavoriteEntity
import com.tattoo.studio.data.local.db.entity.WorkDetailEntity

@Database(
    entities = [WorkEntity::class, UserEntity::class, FavoriteEntity::class, WorkDetailEntity::class],
    version = 7,
    exportSchema = false
)
abstract class TattooDatabase : RoomDatabase() {
    abstract val tattooDao: TattooDao
}
