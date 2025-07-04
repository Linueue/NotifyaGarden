package com.strling.notifyagarden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GrowAGardenTimerViewModel: ViewModel() {

    private var _job: Job? = null

    fun startFetch()
    {
        _job?.cancel()

        _job = viewModelScope.launch {
        }
    }
}