package com.example.worktimetracker.di

import android.content.Context
import androidx.room.Room
import com.example.worktimetracker.data.local.AppDatabase
import com.example.worktimetracker.data.local.dao.GeofenceDao
import com.example.worktimetracker.data.local.dao.PauseDao
import com.example.worktimetracker.data.local.dao.TrackingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "worktime_tracker_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTrackingDao(database: AppDatabase): TrackingDao {
        return database.trackingDao()
    }

    @Provides
    @Singleton
    fun providePauseDao(database: AppDatabase): PauseDao {
        return database.pauseDao()
    }

    @Provides
    @Singleton
    fun provideGeofenceDao(database: AppDatabase): GeofenceDao {
        return database.geofenceDao()
    }
}
