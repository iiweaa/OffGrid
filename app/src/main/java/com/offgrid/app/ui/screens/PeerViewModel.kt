package com.offgrid.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offgrid.app.service.VoiceStateHolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class PeerViewModel : ViewModel() {

    val voiceState = VoiceStateHolder.state
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            VoiceStateHolder.state.value
        )
}
