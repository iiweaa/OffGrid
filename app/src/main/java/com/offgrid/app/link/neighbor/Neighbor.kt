package com.offgrid.app.link.neighbor

import com.offgrid.app.link.node.NodeId
import java.net.InetAddress

/**
 * A known peer in the mesh.
 */
data class Neighbor(
    val nodeId: NodeId,
    val address: InetAddress,
    val lastSeenMs: Long
) {
    /**
     * Short, human-readable identifier (last 8 hex chars of NodeId).
     */
    fun displayName(): String = nodeId.toHex().takeLast(8).uppercase()
}
