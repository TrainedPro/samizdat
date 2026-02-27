package com.fyp.resilientp2p.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for the Samizdat mesh node.
 *
 * Hosts tables:
 * - **logs** — diagnostic and metric log entries ([LogEntry])
 * - **packet_queue** — store-and-forward packets ([PacketEntity])
 * - **chat_messages** — user chat messages and file transfers ([ChatMessage])
 * - **telemetry_events** — cloud telemetry snapshots ([TelemetryEvent])
 * - **chat_groups** — named chat channels ([ChatGroup])
 * - **group_messages** — group chat messages ([GroupMessage])
 * - **shared_files** — content-addressable shared files ([SharedFile])
 * - **encounter_log** — DTN encounter records ([EncounterLog])
 *
 * Uses [fallbackToDestructiveMigration] because this is a research/test-bed app
 * where data persistence across schema changes is not required.
 *
 * @see Converters
 */
@Database(
        entities = [
            LogEntry::class,
            PacketEntity::class,
            ChatMessage::class,
            TelemetryEvent::class,
            ChatGroup::class,
            GroupMessage::class,
            SharedFile::class,
            EncounterLog::class
        ],
        version = 8,
        exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun packetDao(): PacketDao
    abstract fun chatDao(): ChatDao
    abstract fun telemetryDao(): TelemetryDao
    abstract fun chatGroupDao(): ChatGroupDao
    abstract fun groupMessageDao(): GroupMessageDao
    abstract fun sharedFileDao(): SharedFileDao
    abstract fun encounterDao(): EncounterDao

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
                                        .fallbackToDestructiveMigration(dropAllTables = true)
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }
}
