package com.example.worktimetracker.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.worktimetracker.service.GeofenceBroadcastReceiver
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeofenceModule {

    @Provides
    @Singleton
    fun provideGeofencingClient(
        @ApplicationContext context: Context
    ): GeofencingClient {
        return LocationServices.getGeofencingClient(context)
    }

    @Provides
    @Singleton
    fun provideGeofencePendingIntent(
        @ApplicationContext context: Context
    ): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}
