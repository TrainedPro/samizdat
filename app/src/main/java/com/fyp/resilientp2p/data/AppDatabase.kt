package com.fyp.resilientp2p.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
        entities = [LogEntry::class, PacketEntity::class, ChatMessage::class, TelemetryEvent::class],
        version = 7,
        exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun packetDao(): PacketDao
    abstract fun chatDao(): ChatDao
    abstract fun telemetryDao(): TelemetryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        @Suppress("DEPRECATION")
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                AppDatabase::class.java,
                                                "p2p_testbed_db"
                                        )
                                        .fallbackToDestructiveMigration()
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }
}
