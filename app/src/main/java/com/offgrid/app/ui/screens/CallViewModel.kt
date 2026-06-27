package com.offgrid.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offgrid.app.service.VoiceService
import com.offgrid.app.service.VoiceStateHolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class CallViewModel(
    application: Application
) : AndroidViewModel(application) {

    val voiceState = VoiceStateHolder.state
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            VoiceStateHolder.state.value
        )

    fun startCall() {
        VoiceService.start(getApplication())
    }

    fun stopCall() {
        VoiceService.stop(getApplication())
    }

    fun toggleMute() {
        VoiceService.toggleMute(getApplication())
    }
}
