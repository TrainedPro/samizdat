
package com.fyp.resilientp2p.data

import androidx.room.TypeConverter

/**
 * Room [TypeConverter] collection for all enum types stored in the database.
 *
 * Converts enums to/from their [Enum.name] string representation. Unknown values
 * fall back to a safe default rather than crashing, which protects against DB
 * corruption after enum refactors or destructive migrations.
 *
 * @see AppDatabase
 */
class Converters {
    @TypeConverter
    fun fromLogLevel(value: LogLevel): String = value.name

    @TypeConverter
    fun toLogLevel(value: String): LogLevel = try {
        LogLevel.valueOf(value)
    } catch (e: Exception) {
        LogLevel.INFO
    }

    @TypeConverter
    fun fromLogType(value: LogType): String = value.name

    @TypeConverter
    fun toLogType(value: String): LogType = try {
        LogType.valueOf(value)
    } catch (e: Exception) {
        LogType.SYSTEM
    }

    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = try {
        MessageType.valueOf(value)
    } catch (e: Exception) {
        MessageType.TEXT
    }

    @TypeConverter
    fun fromTelemetryEventType(value: TelemetryEventType): String = value.name

    @TypeConverter
    fun toTelemetryEventType(value: String): TelemetryEventType = try {
        TelemetryEventType.valueOf(value)
    } catch (e: Exception) {
        TelemetryEventType.STATS_SNAPSHOT
    }
}
