package com.strling.notifyagarden

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

object ServiceState
{
    val isServiceRunning = mutableStateOf(false)

    fun setServiceRunning(running: Boolean)
    {
        isServiceRunning.value = running
    }
}

object ServiceData
{
    val growAGarden = GrowAGarden()
    val timer = Timer()
}
