package com.offgrid.app.link.location

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun `distance between Beijing and Shanghai`() {
        val beijing = Location(39.9042, 116.4074, 0.0, 0f, 0)
        val shanghai = Location(31.2304, 121.4737, 0.0, 0f, 0)

        val distance = GeoUtils.distanceMeters(beijing, shanghai)
        // Known distance ~1067 km.
        assertEquals(1_067_000.0, distance, 10_000.0)
    }

    @Test
    fun `bearing from north to east is 90 degrees`() {
        val center = Location(0.0, 0.0, 0.0, 0f, 0)
        val east = Location(0.0, 1.0, 0.0, 0f, 0)

        assertEquals(90.0, GeoUtils.bearingDegrees(center, east), 0.1)
    }

    @Test
    fun `bearing from north to south is 180 degrees`() {
        val center = Location(10.0, 10.0, 0.0, 0f, 0)
        val south = Location(-10.0, 10.0, 0.0, 0f, 0)

        assertEquals(180.0, GeoUtils.bearingDegrees(center, south), 0.1)
    }

    @Test
    fun `bearing from north to west is 270 degrees`() {
        val center = Location(0.0, 0.0, 0.0, 0f, 0)
        val west = Location(0.0, -1.0, 0.0, 0f, 0)

        assertEquals(270.0, GeoUtils.bearingDegrees(center, west), 0.1)
    }

    @Test
    fun `distance of same point is zero`() {
        val point = Location(12.34, 56.78, 0.0, 1f, 0)
        assertEquals(0.0, GeoUtils.distanceMeters(point, point), 0.01)
    }
}
