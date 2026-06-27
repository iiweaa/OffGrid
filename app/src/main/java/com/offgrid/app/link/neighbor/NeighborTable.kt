package com.offgrid.app.link.neighbor

import com.offgrid.app.link.node.NodeId
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe table of mesh neighbors with aging.
 */
class NeighborTable(private val timeoutMs: Long = 8000L) {

    private val table = ConcurrentHashMap<NodeId, Neighbor>()
    private var onChanged: ((List<Neighbor>) -> Unit)? = null

    fun setOnChanged(listener: (List<Neighbor>) -> Unit) {
        onChanged = listener
    }

    /**
     * Adds or refreshes a neighbor.
     */
    fun update(nodeId: NodeId, address: InetAddress) {
        val neighbor = Neighbor(nodeId, address, System.currentTimeMillis())
        val previous = table.put(nodeId, neighbor)
        if (previous == null || previous.address != address) {
            onChanged?.invoke(snapshot())
        }
    }

    /**
     * Removes neighbors that have not been seen within [timeoutMs].
     *
     * @return list of removed [NodeId]s.
     */
    fun removeStale(): List<NodeId> {
        val now = System.currentTimeMillis()
        val removed = mutableListOf<NodeId>()
        val iterator = table.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.lastSeenMs > timeoutMs) {
                removed.add(entry.key)
                iterator.remove()
            }
        }
        if (removed.isNotEmpty()) {
            onChanged?.invoke(snapshot())
        }
        return removed
    }

    fun get(nodeId: NodeId): Neighbor? = table[nodeId]

    fun snapshot(): List<Neighbor> = table.values.toList()

    fun clear() {
        if (table.isNotEmpty()) {
            table.clear()
            onChanged?.invoke(emptyList())
        }
    }
}
