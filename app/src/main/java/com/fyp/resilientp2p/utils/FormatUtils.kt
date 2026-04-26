package com.fyp.resilientp2p.utils

/**
 * Utility functions for formatting durations and byte sizes.
 */
object FormatUtils {
    /**
     * Format milliseconds into a human-readable duration string.
     * Examples: "2h15m30s", "45m12s", "30s"
     */
    fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}h${minutes % 60}m${seconds % 60}s"
            minutes > 0 -> "${minutes}m${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Format bytes into a human-readable size string.
     * Examples: "1.5MB", "512.0KB", "128B"
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_048_576 -> String.format(java.util.Locale.US, "%.1fMB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format(java.util.Locale.US, "%.1fKB", bytes / 1024.0)
            else -> "${bytes}B"
        }
    }
}
