package com.example.worktimetracker.service

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationChannelManagerTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var channelManager: NotificationChannelManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        channelManager = NotificationChannelManager(context)
    }

    @Test
    fun `createChannels creates all three channels`() {
        // When
        channelManager.createChannels()

        // Then - verify 3 channels were created
        val channels = notificationManager.notificationChannels
        assertEquals(3, channels.size)
    }

    @Test
    fun `createChannels creates tracking_active channel with LOW importance`() {
        // When
        channelManager.createChannels()

        // Then
        val trackingChannel = notificationManager.getNotificationChannel("tracking_active")
        assertNotNull(trackingChannel)
        assertEquals("tracking_active", trackingChannel.id)
        assertEquals(NotificationManager.IMPORTANCE_LOW, trackingChannel.importance)
    }

    @Test
    fun `createChannels creates tracking_events channel with HIGH importance`() {
        // When
        channelManager.createChannels()

        // Then
        val eventsChannel = notificationManager.getNotificationChannel("tracking_events")
        assertNotNull(eventsChannel)
        assertEquals("tracking_events", eventsChannel.id)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, eventsChannel.importance)
    }

    @Test
    fun `createChannels creates tracking_reminders channel with DEFAULT importance`() {
        // When
        channelManager.createChannels()

        // Then
        val remindersChannel = notificationManager.getNotificationChannel("tracking_reminders")
        assertNotNull(remindersChannel)
        assertEquals("tracking_reminders", remindersChannel.id)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, remindersChannel.importance)
    }

    @Test
    fun `CHANNEL_TRACKING_ACTIVE constant is correct`() {
        assertEquals("tracking_active", NotificationChannelManager.CHANNEL_TRACKING_ACTIVE)
    }

    @Test
    fun `CHANNEL_TRACKING_EVENTS constant is correct`() {
        assertEquals("tracking_events", NotificationChannelManager.CHANNEL_TRACKING_EVENTS)
    }

    @Test
    fun `CHANNEL_TRACKING_REMINDERS constant is correct`() {
        assertEquals("tracking_reminders", NotificationChannelManager.CHANNEL_TRACKING_REMINDERS)
    }
}
