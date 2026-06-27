package com.offgrid.app.link.location

/**
 * Geographic location reported by a mesh node.
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val timestampMs: Long
) {
    fun isValid(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}
