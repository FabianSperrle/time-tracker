package com.example.worktimetracker.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ServiceDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CacheDirectory

@Module
@InstallIn(SingletonComponent::class)
object TrackingModule {

    @Provides
    @Singleton
    fun provideTrackingSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("tracking_state", Context.MODE_PRIVATE)
    }

    @Provides
    @ServiceDispatcher
    fun provideServiceDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @CacheDirectory
    @Singleton
    fun provideCacheDirectory(
        @ApplicationContext context: Context
    ): File {
        return context.cacheDir
    }
}
