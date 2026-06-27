package com.offgrid.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offgrid.app.link.wifidirect.NetworkConfig
import com.offgrid.app.link.wifidirect.NetworkConnectionState
import com.offgrid.app.link.wifidirect.WifiDirectConnector
import com.offgrid.app.service.VoiceService
import com.offgrid.app.service.VoiceStateHolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CallViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val connector = WifiDirectConnector(application, viewModelScope)
    private var pendingVoiceStart = false

    val voiceState = VoiceStateHolder.state
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            VoiceStateHolder.state.value
        )

    val networkState = connector.state
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            NetworkConnectionState.Idle
        )

    init {
        connector.start()
        viewModelScope.launch {
            networkState.collect { state ->
                when (state) {
                    is NetworkConnectionState.Connected -> {
                        if (pendingVoiceStart) {
                            pendingVoiceStart = false
                            VoiceService.start(getApplication())
                        }
                    }
                    is NetworkConnectionState.Failed -> {
                        pendingVoiceStart = false
                        VoiceStateHolder.update { it.copy(lastError = state.error) }
                    }
                    else -> { /* no-op */ }
                }
            }
        }
    }

    fun startCall() {
        if (voiceState.value.isRunning) return
        pendingVoiceStart = true
        VoiceStateHolder.update { it.copy(lastError = null) }
        connector.establish(NetworkConfig.getRole(getApplication()))
    }

    fun stopCall() {
        pendingVoiceStart = false
        VoiceService.stop(getApplication())
        connector.disconnect()
    }

    fun toggleMute() {
        VoiceService.toggleMute(getApplication())
    }

    override fun onCleared() {
        super.onCleared()
        connector.stop()
    }
}
