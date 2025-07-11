package com.strling.notifyagarden

import androidx.compose.runtime.mutableStateOf

object NotifyState
{
    val isNotifyRunning = mutableStateOf(false)

    fun setNotifyRunning(running: Boolean)
    {
        isNotifyRunning.value = running
    }
}

object NotifyData
{
    val game = GrowAGarden()
    val timer = Timer()
}
