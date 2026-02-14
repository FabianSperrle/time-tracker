package com.example.worktimetracker.di

import com.example.worktimetracker.domain.GeocodingService
import com.example.worktimetracker.domain.GeocodingServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GeocodingModule {

    @Binds
    @Singleton
    abstract fun bindGeocodingService(
        impl: GeocodingServiceImpl
    ): GeocodingService
}
