package com.fyp.resilientp2p.data

/**
 * Represents an emergency broadcast message received from the mesh.
 * Emergency messages are never auto-deleted and are displayed prominently in the UI.
 */
data class EmergencyMessage(
    val id: String,
    val sourceId: String,
    val message: String,
    val type: String = "EMERGENCY", // "EMERGENCY" or "SOS_BEACON"
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val battery: Int? = null,
    val device: String = "Unknown"
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null
    val isSOS: Boolean get() = type == "SOS_BEACON"
}
