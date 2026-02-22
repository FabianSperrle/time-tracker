package com.example.worktimetracker.service

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.database.sqlite.SQLiteDatabase
import android.os.ParcelFileDescriptor

class AppBackupAgent : BackupAgent() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        val dbFile = getDatabasePath("worktime_tracker_db")
        if (dbFile.exists()) {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            ).use { db ->
                db.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
            }
        }
        super.onFullBackup(data)
    }

    // Key/value backup not used — only full backup via Auto Backup
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) {}
    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) {}
}
