package com.fyp.resilientp2p.data

data class ChatMessage(
    val id: String,
    val peerId: String, // The OTHER party (conversation partner). "BROADCAST" for broadcasts.
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSent: Boolean // true = we sent it, false = we received it
) {
    val formattedTime: String
        get() = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
}
