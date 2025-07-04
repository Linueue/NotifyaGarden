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

class NotificationService: Service() {
    private val job = SupervisorJob()
    private val coroutineJob = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action)
        {
            Actions.START.toString() -> start()
            Actions.STOP.toString() -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceState.setServiceRunning(false)
        coroutineJob.cancel()
    }

    private fun buildNotification(title: String, info: String): Notification
    {
        val notification = NotificationCompat.Builder(this, "notify_a_garden")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(info)
            .build()
        return notification
    }
    
    private fun start()
    {
        val startNotification = buildNotification("Notify a Garden started", "The app will now notify you of stocks.")
        startForeground(1, startNotification)
        ServiceState.setServiceRunning(true)
        val context = this

        coroutineJob.launch {
            val favoriteAvailableFn = { favorite: Item ->
                val notification = buildNotification(
                    "${favorite.name} is available!",
                    "${favorite.stock}x stock"
                )
                with(NotificationManagerCompat.from(context))
                {
                    if(ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        notify((0..Int.MAX_VALUE).random(), notification)
                    }
                }
            }

            while(true)
            {
                ServiceData.growAGarden.fetchStocks()
                ServiceData.growAGarden.notifyFavorites(favoriteAvailableFn)
                ServiceData.timer.start(ServiceData.growAGarden.uiState.value.updatedAt)
            }
        }
    }

    enum class Actions
    {
        START,
        STOP,
    }
}