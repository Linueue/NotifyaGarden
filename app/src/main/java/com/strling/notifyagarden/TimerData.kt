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

    suspend fun start(startTime: Long)
    {
        val time = startTime / 1000
        val currentTime = System.currentTimeMillis() / 1000
        val next = (5 * 60) - (currentTime - time)
        _uiState.update {
            startTime
        }
        delay(next * 1000)
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

    suspend fun startLegacy(startTime: Long)
    {
        if(startTime == 0L)
            return
        val time = startTime / 1000
        val currentTime = System.currentTimeMillis() / 1000
        println("currentTime: ${currentTime}, time: ${time}")

        _uiState.update { currentState ->
            val diff = currentTime - time
            val next = (5 * 60) - diff
            val minuteFloat = (next / 60.0f)
            val minute = minuteFloat.toInt()
            val seconds = ((minuteFloat - minute) * 60).toInt()
            println("Diff: $diff, next: $next, minute: $minute, seconds: $seconds")
            currentState
        }

        var isFinished = false
        while(!isFinished)
        {
            delay(1000)
            _uiState.update { currentState ->
                currentState
            }
        }
    }
}