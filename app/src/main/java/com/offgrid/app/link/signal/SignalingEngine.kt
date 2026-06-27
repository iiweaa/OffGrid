package com.offgrid.app.link.signal

import com.offgrid.app.link.MeshConstants
import com.offgrid.app.link.neighbor.Neighbor
import com.offgrid.app.link.neighbor.NeighborTable
import com.offgrid.app.link.node.NodeId
import com.offgrid.app.link.packet.Packet
import com.offgrid.app.link.packet.PacketSerializer
import com.offgrid.app.link.packet.PacketType
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages mesh control-plane signaling: HELLO heartbeats and neighbor aging.
 *
 * The engine is transport-agnostic. It produces raw HELLO bytes via the
 * [onSend] callback and expects the transport layer to deliver them. Incoming
 * HELLO packets are reported via [handleHello].
 */
class SignalingEngine(
    private val localNodeId: NodeId,
    private val helloIntervalMs: Long = DEFAULT_HELLO_INTERVAL_MS,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS
) {

    companion object {
        const val DEFAULT_HELLO_INTERVAL_MS = 2000L
        const val DEFAULT_TIMEOUT_MS = 8000L
    }

    private val neighborTable = NeighborTable(timeoutMs)
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "OffGridSignal").apply { isDaemon = true }
    }
    private val running = AtomicBoolean(false)
    private val sequence = AtomicInteger(0)

    private var onSend: ((ByteArray) -> Unit)? = null
    private var onNeighborConnected: ((Neighbor) -> Unit)? = null
    private var onNeighborDisconnected: ((NodeId) -> Unit)? = null
    private var onNeighborsChanged: ((List<Neighbor>) -> Unit)? = null

    fun setCallbacks(
        onSend: ((ByteArray) -> Unit)? = null,
        onNeighborConnected: ((Neighbor) -> Unit)? = null,
        onNeighborDisconnected: ((NodeId) -> Unit)? = null,
        onNeighborsChanged: ((List<Neighbor>) -> Unit)? = null
    ) {
        this.onSend = onSend
        this.onNeighborConnected = onNeighborConnected
        this.onNeighborDisconnected = onNeighborDisconnected
        this.onNeighborsChanged = onNeighborsChanged
        neighborTable.setOnChanged { onNeighborsChanged?.invoke(it) }
    }

    fun start() {
        if (running.getAndSet(true)) return
        scheduler.scheduleWithFixedDelay(
            ::sendHello,
            0,
            helloIntervalMs,
            TimeUnit.MILLISECONDS
        )
        scheduler.scheduleWithFixedDelay(
            ::checkAging,
            helloIntervalMs,
            helloIntervalMs,
            TimeUnit.MILLISECONDS
        )
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        scheduler.shutdownNow()
        neighborTable.clear()
    }

    /**
     * Reports a HELLO packet received from a remote peer.
     */
    fun handleHello(nodeId: NodeId, address: InetAddress) {
        val previous = neighborTable.get(nodeId)
        neighborTable.update(nodeId, address)
        if (previous == null) {
            neighborTable.get(nodeId)?.let { onNeighborConnected?.invoke(it) }
        }
    }

    fun snapshot(): List<Neighbor> = neighborTable.snapshot()

    private fun sendHello() {
        if (!running.get()) return
        val packet = Packet(
            version = Packet.CURRENT_VERSION,
            type = PacketType.HELLO,
            ttl = 1,
            source = localNodeId,
            destination = MeshConstants.BROADCAST_NODE_ID,
            sequence = sequence.incrementAndGet(),
            payload = ByteArray(0)
        )
        val bytes = PacketSerializer.serialize(packet)
        onSend?.invoke(bytes)
    }

    private fun checkAging() {
        val removed = neighborTable.removeStale()
        removed.forEach { nodeId ->
            onNeighborDisconnected?.invoke(nodeId)
        }
    }
}
