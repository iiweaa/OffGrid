package com.offgrid.app.link.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LocationPayloadTest {

    @Test
    fun `serialize and deserialize location`() {
        val location = Location(
            latitude = 39.9042,
            longitude = 116.4074,
            altitude = 45.0,
            accuracy = 3.5f,
            timestampMs = 1_700_000_000_000L
        )

        val bytes = LocationPayload.serialize(location)
        assertEquals(LocationPayload.SIZE_BYTES, bytes.size)

        val decoded = LocationPayload.deserialize(bytes)
        assertNotNull(decoded)
        assertEquals(location, decoded)
    }

    @Test
    fun `deserialize rejects wrong size`() {
        assertNull(LocationPayload.deserialize(ByteArray(0)))
        assertNull(LocationPayload.deserialize(ByteArray(LocationPayload.SIZE_BYTES - 1)))
        assertNull(LocationPayload.deserialize(ByteArray(LocationPayload.SIZE_BYTES + 1)))
    }

    @Test
    fun `deserialize preserves negative coordinates`() {
        val location = Location(
            latitude = -33.8688,
            longitude = -151.2093,
            altitude = -10.0,
            accuracy = 12.0f,
            timestampMs = 1L
        )

        val decoded = LocationPayload.deserialize(LocationPayload.serialize(location))
        assertEquals(location, decoded)
    }
}
