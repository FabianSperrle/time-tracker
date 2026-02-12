package com.example.worktimetracker.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.altbeacon.beacon.BeaconManager
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for BeaconScanner coroutine scope.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BeaconScannerScope

/**
 * Hilt module providing beacon scanning dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object BeaconModule {

    @Provides
    @Singleton
    fun provideBeaconManager(
        @ApplicationContext context: Context
    ): BeaconManager {
        return BeaconManager.getInstanceForApplication(context)
    }

    @Provides
    @Singleton
    @BeaconScannerScope
    fun provideBeaconScannerScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }
}
