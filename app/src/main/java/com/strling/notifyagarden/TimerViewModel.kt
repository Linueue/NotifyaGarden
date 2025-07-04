package com.strling.notifyagarden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.floor

class TimerViewModel: ViewModel() {
    val _uiState = MutableStateFlow(TimerData(0, 0, 0))
    val uiState = _uiState.asStateFlow()

    private var _job: Job? = null

    fun updateTimer()
    {
        if(ServiceData.timer.uiState.value == 0L)
            return

        _job?.cancel()
        _job = viewModelScope.launch {
            val startTime = ServiceData.timer.uiState.value / 1000.0
            while(true)
            {
                val currentTime = System.currentTimeMillis() / 1000.0
                val diff = (60 * 5) - (currentTime - startTime)
                val minutesFloat = diff / 60
                val minutes = floor(minutesFloat).toInt()
                val seconds = floor((minutesFloat - minutes) * 60).toInt()
                if(minutes < 0 || seconds < 0)
                    break
                _uiState.update { currentState ->
                    currentState.copy(minutes, seconds)
                }

                delay(1000)
            }
        }
    }
}