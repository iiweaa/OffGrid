package com.offgrid.app.link.location

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary serializer for [Location].
 *
 * Layout (big-endian, 36 bytes):
 * ```
 * Offset | Size | Field
 * -------|------|------------------
 *      0 |    8 | Latitude
 *      8 |    8 | Longitude
 *     16 |    8 | Altitude
 *     24 |    4 | Accuracy
 *     28 |    8 | Timestamp (ms)
 * ```
 */
object LocationPayload {

    const val SIZE_BYTES = 8 + 8 + 8 + 4 + 8

    fun serialize(location: Location): ByteArray {
        return ByteBuffer.allocate(SIZE_BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putDouble(location.latitude)
            .putDouble(location.longitude)
            .putDouble(location.altitude)
            .putFloat(location.accuracy)
            .putLong(location.timestampMs)
            .array()
    }

    fun deserialize(bytes: ByteArray): Location? {
        if (bytes.size != SIZE_BYTES) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        return Location(
            latitude = buffer.double,
            longitude = buffer.double,
            altitude = buffer.double,
            accuracy = buffer.float,
            timestampMs = buffer.long
        )
    }
}
