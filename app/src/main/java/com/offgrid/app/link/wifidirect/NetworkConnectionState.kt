package com.offgrid.app.link.wifidirect

data class GroupInfo(
    val networkName: String,
    val passphrase: String?,
    val isGroupOwner: Boolean,
    val clientCount: Int = 0
)

sealed class NetworkConnectionState {
    object Idle : NetworkConnectionState()
    data class Connecting(val message: String) : NetworkConnectionState()
    data class GroupCreated(val info: GroupInfo) : NetworkConnectionState()
    data class Connected(val info: GroupInfo) : NetworkConnectionState()
    data class Failed(val error: String, val hint: String) : NetworkConnectionState()
}
