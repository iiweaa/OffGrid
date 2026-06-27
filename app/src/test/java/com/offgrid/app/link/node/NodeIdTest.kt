package com.offgrid.app.link.node

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NodeIdTest {

    @Test
    fun `random returns non-zero identifier`() {
        val id = NodeId.random()
        assertNotEquals(0L, id.value)
    }

    @Test
    fun `hex representation is 16 lowercase characters`() {
        val id = NodeId(0x1234_ABCD_5678_9012)
        assertEquals("1234abcd56789012", id.toHex())
    }

    @Test
    fun `hex roundtrip preserves value`() {
        val original = NodeId.random()
        val parsed = NodeId.parse(original.toHex())
        assertNotNull(parsed)
        assertEquals(original, parsed)
    }

    @Test
    fun `parse rejects invalid strings`() {
        assertNull(NodeId.parse(""))
        assertNull(NodeId.parse("1234"))
        assertNull(NodeId.parse("notahexvalue00"))
        assertNull(NodeId.parse("1234abcd5678901g"))
    }

    @Test
    fun `negative long roundtrips through hex`() {
        val id = NodeId(-1L)
        assertEquals("ffffffffffffffff", id.toHex())
        assertEquals(id, NodeId.parse(id.toHex()))
    }
}
