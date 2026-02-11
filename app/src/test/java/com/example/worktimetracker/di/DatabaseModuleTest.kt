package com.example.worktimetracker.di

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.worktimetracker.data.local.AppDatabase
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test for DatabaseModule to verify Room database initialization.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class DatabaseModuleTest {

    @Test
    fun `provideAppDatabase returns non-null database instance`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = DatabaseModule.provideAppDatabase(context)

        assertNotNull("Database should not be null", database)
    }

    @Test
    fun `database can be opened and closed without errors`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = DatabaseModule.provideAppDatabase(context)

        // Verify database is valid
        assertNotNull(database)
        database.openHelper.writableDatabase.isOpen

        database.close()
    }
}
