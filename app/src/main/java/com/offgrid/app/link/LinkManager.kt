package com.offgrid.app.link

import android.util.Log
import com.offgrid.app.link.location.LocationPayload
import com.offgrid.app.link.neighbor.Neighbor
import com.offgrid.app.link.node.NodeId
import com.offgrid.app.link.packet.Packet
import com.offgrid.app.link.packet.PacketSerializer
import com.offgrid.app.link.packet.PacketType
import com.offgrid.app.link.signal.SignalingEngine
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class LinkManager(private val localNodeId: NodeId) {

    companion object {
        private const val TAG = "LinkManager"
        const val PORT = 4242

        // Legacy plain-text hello, kept only for documentation/backwards reference.
        const val HELLO = "OFFGRID_HELLO"
    }

    private val signalingEngine = SignalingEngine(localNodeId)

    private val receiveExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "OffGridNetRecv").apply { isDaemon = true }
    }
    private val sendExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "OffGridNetSend").apply { isDaemon = true }
    }

    private val running = AtomicBoolean(false)
    private var socket: DatagramSocket? = null
    private val localAddresses = mutableSetOf<InetAddress>()
    private val sequence = AtomicInteger(0)

    private var onNeighborsChanged: ((List<Neighbor>) -> Unit)? = null
    private var onPacket: ((ByteArray, Int, InetAddress) -> Unit)? = null
    private var onLocationReceived: ((com.offgrid.app.link.node.NodeId, com.offgrid.app.link.location.Location) -> Unit)? = null

    fun setCallbacks(
        onNeighborsChanged: ((List<Neighbor>) -> Unit)? = null,
        onPacket: ((ByteArray, Int, InetAddress) -> Unit)? = null,
        onLocationReceived: ((com.offgrid.app.link.node.NodeId, com.offgrid.app.link.location.Location) -> Unit)? = null
    ) {
        this.onNeighborsChanged = onNeighborsChanged
        this.onPacket = onPacket
        this.onLocationReceived = onLocationReceived
        signalingEngine.setCallbacks(
            onSend = { bytes -> sendRaw(bytes) },
            onNeighborsChanged = { onNeighborsChanged?.invoke(it) }
        )
    }

    fun start() {
        if (running.getAndSet(true)) return
        receiveExecutor.execute {
            try {
                val sock = DatagramSocket(PORT).apply {
                    broadcast = true
                    reuseAddress = true
                }
                socket = sock
                refreshLocalAddresses()
                signalingEngine.start()
                startReceiver(sock)
            } catch (e: Exception) {
                Log.e(TAG, "Link start failed", e)
                running.set(false)
            }
        }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        signalingEngine.stop()
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        receiveExecutor.shutdownNow()
        sendExecutor.shutdownNow()
    }

    /**
     * Sends a raw voice frame. It is wrapped in a [Packet] before transmission.
     */
    fun send(data: ByteArray, length: Int) {
        send(PacketType.VOICE, data, length)
    }

    /**
     * Sends a typed mesh payload. Used for voice, location, etc.
     */
    fun send(type: PacketType, data: ByteArray, length: Int) {
        val payload = data.copyOf(length)
        val packet = buildPacket(type, ttl = 1, payload = payload)
        sendPacket(packet)
    }

    private fun sendPacket(packet: Packet) {
        val bytes = PacketSerializer.serialize(packet)
        sendRaw(bytes)
    }

    private fun sendRaw(bytes: ByteArray) {
        val sock = socket ?: return
        val dest = signalingEngine.snapshot().firstOrNull()?.address
            ?: getBroadcastAddress()
            ?: return

        sendExecutor.execute {
            try {
                val datagram = DatagramPacket(bytes, 0, bytes.size, dest, PORT)
                sock.send(datagram)
                Log.d(TAG, "Sent ${bytes.size} bytes to $dest")
            } catch (e: Exception) {
                if (running.get()) {
                    Log.w(TAG, "Send failed", e)
                }
            }
        }
    }

    private fun buildPacket(type: PacketType, ttl: Int, payload: ByteArray): Packet {
        return Packet(
            version = Packet.CURRENT_VERSION,
            type = type,
            ttl = ttl,
            source = localNodeId,
            destination = MeshConstants.BROADCAST_NODE_ID,
            sequence = sequence.incrementAndGet(),
            payload = payload
        )
    }

    private fun startReceiver(sock: DatagramSocket) {
        val buffer = ByteArray(1500)
        while (running.get()) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                sock.receive(packet)
                val data = packet.data.copyOfRange(packet.offset, packet.offset + packet.length)
                handlePacket(data, packet.length, packet.address)
            } catch (e: SocketException) {
                if (running.get()) {
                    Log.w(TAG, "Receive socket closed", e)
                }
                break
            } catch (e: Exception) {
                if (running.get()) {
                    Log.w(TAG, "Receive error", e)
                }
            }
        }
    }

    private fun handlePacket(data: ByteArray, length: Int, sender: InetAddress) {
        if (sender.isLoopbackAddress || localAddresses.contains(sender)) {
            return
        }
        if (length <= 0) return

        val meshPacket = PacketSerializer.deserialize(data) ?: run {
            // Drop unparseable packets (e.g. legacy plain-text HELLO from older builds).
            Log.d(TAG, "Dropped unparseable packet from $sender")
            return
        }

        when (meshPacket.type) {
            PacketType.HELLO -> {
                signalingEngine.handleHello(meshPacket.source, sender)
            }
            PacketType.VOICE -> {
                Log.d(TAG, "Received voice packet ${meshPacket.payload.size} bytes from $sender")
                onPacket?.invoke(meshPacket.payload, meshPacket.payload.size, sender)
            }
            PacketType.LOCATION -> {
                val location = LocationPayload.deserialize(meshPacket.payload)
                if (location != null && location.isValid()) {
                    Log.d(TAG, "Received location from ${meshPacket.source.toHex()}: " +
                        "${location.latitude}, ${location.longitude}")
                    onLocationReceived?.invoke(meshPacket.source, location)
                } else {
                    Log.w(TAG, "Received invalid location payload")
                }
            }
            else -> {
                Log.d(TAG, "Received ${meshPacket.type} packet, not handled yet")
            }
        }

        // Multi-hop forwarding placeholder: reserved for future extension when
        // devices support Wi-Fi Direct AP-STA concurrency.
        if (shouldForward(meshPacket)) {
            Log.d(TAG, "Forwarding reserved for multi-hop extension; TTL=${meshPacket.ttl}")
        }
    }

    private fun shouldForward(packet: Packet): Boolean {
        return packet.ttl > 1 &&
            packet.destination != localNodeId &&
            packet.destination != MeshConstants.BROADCAST_NODE_ID
    }

    private fun refreshLocalAddresses() {
        localAddresses.clear()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.interfaceAddresses) {
                    val ip = addr.address ?: continue
                    if (ip.hostAddress?.startsWith("192.168.49.") == true) {
                        localAddresses.add(ip)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh local addresses", e)
        }
    }

    private fun getBroadcastAddress(): InetAddress? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val addrs = iface.interfaceAddresses
                for (addr in addrs) {
                    val broadcast = addr.broadcast ?: continue
                    if (broadcast.hostAddress?.startsWith("192.168.49.") == true) {
                        return broadcast
                    }
                }
            }
            // Fallback for the default Wi-Fi Direct GO subnet.
            InetAddress.getByName("192.168.49.255")
        } catch (e: Exception) {
            Log.w(TAG, "No broadcast address found", e)
            null
        }
    }
}
