package com.offgrid.app.link

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CapabilityStateHolder {
    private val _capability = MutableStateFlow(
        WifiDirectCapability(ConcurrencyStatus.UNKNOWN, "default")
    )
    val capability: StateFlow<WifiDirectCapability> = _capability.asStateFlow()

    fun update(capability: WifiDirectCapability) {
        _capability.value = capability
    }
}
