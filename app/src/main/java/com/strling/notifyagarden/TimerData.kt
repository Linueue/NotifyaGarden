package com.strling.notifyagarden

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TimerData(
    var minute: Int,
    var seconds: Int,
    var startTime: Long,
)

class Timer {
    val _uiState: MutableStateFlow<Long> = MutableStateFlow(0)
    val uiState = _uiState.asStateFlow()

    fun formatTimer(timer: TimerData): String
    {
        val minute = timer.minute
        val seconds = if (timer.seconds < 10) { "0${timer.seconds}" } else { timer.seconds.toString() }
        return "${minute}:${seconds}"
    }

    fun resetTime()
    {
        _uiState.update {
            0L
        }
    }

    fun getTime(startTime: Long): Long
    {
        val time = startTime / 1000
        val currentTime = System.currentTimeMillis() / 1000
        val next = (5 * 60) - (currentTime - time)
        _uiState.update {
            startTime
        }
        return ((currentTime + next) * 1000)
    }
}