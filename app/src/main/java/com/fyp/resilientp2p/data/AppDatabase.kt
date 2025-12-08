package com.fyp.resilientp2p.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LogEntry::class, PacketEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun packetDao(): PacketDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                AppDatabase::class.java,
                                                "p2p_testbed_db"
                                        )
                                        // .fallbackToDestructiveMigration() // REMOVED: Data loss
                                        // risk
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }
}
