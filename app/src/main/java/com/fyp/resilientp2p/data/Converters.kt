
package com.fyp.resilientp2p.data

import androidx.room.TypeConverter

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
}
