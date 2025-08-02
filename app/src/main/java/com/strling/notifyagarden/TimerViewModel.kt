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
        _job?.cancel()
        _job = viewModelScope.launch {
            fetch(false)

            var startTime = NotifyData.game.uiState.value.updatedAt / 1000.0
            while(true)
            {
                val currentTime = System.currentTimeMillis() / 1000.0
                val nextFiveMins = (60 * 5) - (currentTime % (60 * 5))
                val diff = (nextFiveMins)
                val minutesFloat = diff / 60
                val minutes = floor(minutesFloat).toInt()
                val seconds = floor((minutesFloat - minutes) * 60).toInt()
                if(minutes < 0 && seconds <= 0 || seconds <= 0) {
                    fetch(minutes == 0)
                    startTime = NotifyData.game.uiState.value.updatedAt / 1000.0
                    continue
                }
                _uiState.update { currentState ->
                    currentState.copy(minutes, seconds)
                }

                delay(1000)
            }
        }
    }

    fun getTime(startTime: Long): Long
    {
        val time = startTime / 1000
        val currentTime = System.currentTimeMillis() / 1000
        val next = (5 * 60) - (currentTime - time)
        return ((currentTime + next) * 1000)
    }

    suspend fun fetch(tryConnect: Boolean)
    {
        val currentUpdatedAt = NotifyData.game.uiState.value.updatedAt
        
        for(i in 1..5) {
            NotifyData.game.fetchStocks()

            if(!tryConnect)
                break
            
            if(currentUpdatedAt != NotifyData.game.uiState.value.updatedAt)
                break

            delay(5000)
        }
    }
}