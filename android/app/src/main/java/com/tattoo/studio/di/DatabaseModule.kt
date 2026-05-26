package com.tattoo.studio.di

import android.app.Application
import androidx.room.Room
import com.tattoo.studio.data.local.db.TattooDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTattooDatabase(app: Application): TattooDatabase {
        return Room.databaseBuilder(
            app,
            TattooDatabase::class.java,
            "tattoo_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideTattooDao(db: TattooDatabase) = db.tattooDao
}
