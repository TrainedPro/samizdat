package com.fyp.resilientp2p.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
        entities = [LogEntry::class, PacketEntity::class, ChatMessage::class],
        version = 5,
        exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun packetDao(): PacketDao
    abstract fun chatDao(): ChatDao

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
                                        .fallbackToDestructiveMigration() // Enabled to handle
                                        // schema changes
                                        // (LogEntry fields)
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }
}
