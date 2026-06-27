package com.offgrid.app.service

import com.offgrid.app.link.location.Location
import com.offgrid.app.link.neighbor.Neighbor
import com.offgrid.app.link.node.NodeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetAddress

data class VoiceState(
    val isRunning: Boolean = false,
    val isMuted: Boolean = false,
    val peer: InetAddress? = null,
    val neighbors: List<Neighbor> = emptyList(),
    val myLocation: Location? = null,
    val peerLocations: Map<NodeId, Location> = emptyMap(),
    val lastError: String? = null
)

object VoiceStateHolder {
    private val _state = MutableStateFlow(VoiceState())
    val state: StateFlow<VoiceState> = _state

    fun update(transform: (VoiceState) -> VoiceState) {
        _state.value = transform(_state.value)
    }
}
