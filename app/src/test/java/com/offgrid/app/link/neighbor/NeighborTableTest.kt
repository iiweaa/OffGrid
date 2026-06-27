package com.offgrid.app.link.neighbor

import com.offgrid.app.link.node.NodeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class NeighborTableTest {

    private val idA = NodeId(0x1111_2222_3333_4444)
    private val idB = NodeId(0x5555_6666_7777_8888)
    private val addrA = InetAddress.getByName("192.168.49.100")
    private val addrB = InetAddress.getByName("192.168.49.200")

    @Test
    fun `update adds neighbor`() {
        val table = NeighborTable()
        table.update(idA, addrA)

        assertEquals(1, table.snapshot().size)
        assertEquals(idA, table.snapshot().first().nodeId)
        assertEquals(addrA, table.snapshot().first().address)
    }

    @Test
    fun `update refreshes existing neighbor`() {
        val table = NeighborTable()
        table.update(idA, addrA)
        val firstSeen = table.snapshot().first().lastSeenMs

        Thread.sleep(10)
        table.update(idA, addrA)

        assertEquals(1, table.snapshot().size)
        assertTrue(table.snapshot().first().lastSeenMs > firstSeen)
    }

    @Test
    fun `removeStale evicts timed out neighbors`() {
        val table = NeighborTable(timeoutMs = 1)
        table.update(idA, addrA)

        Thread.sleep(10)
        val removed = table.removeStale()

        assertEquals(1, removed.size)
        assertTrue(table.snapshot().isEmpty())
    }

    @Test
    fun `removeStale keeps fresh neighbors`() {
        val table = NeighborTable(timeoutMs = 10_000)
        table.update(idA, addrA)

        val removed = table.removeStale()

        assertTrue(removed.isEmpty())
        assertEquals(1, table.snapshot().size)
    }

    @Test
    fun `clear removes all neighbors`() {
        val table = NeighborTable()
        table.update(idA, addrA)
        table.update(idB, addrB)

        table.clear()

        assertTrue(table.snapshot().isEmpty())
    }

    @Test
    fun `get returns correct neighbor`() {
        val table = NeighborTable()
        table.update(idA, addrA)
        table.update(idB, addrB)

        assertEquals(idA, table.get(idA)?.nodeId)
        assertEquals(idB, table.get(idB)?.nodeId)
        assertNull(table.get(NodeId(-1L)))
    }

    @Test
    fun `callback fires on change`() {
        val table = NeighborTable()
        var callbacks = 0
        table.setOnChanged { callbacks++ }

        table.update(idA, addrA)
        table.update(idA, addrA) // same address -> no change callback
        table.update(idB, addrB)

        assertEquals(2, callbacks)
    }
}
