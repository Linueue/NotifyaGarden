package com.strling.notifyagarden

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotifyBroadcast: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val favorites = intent!!.getStringArrayListExtra("favorites")!!.toSet()
        val favoriteAvailableFn = { favorite: Item ->
            val notification = buildNotification(
                context!!,
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
        CoroutineScope(Dispatchers.IO).launch {
            val fetched = NotifyData.game.fetchStocks()
            var time = System.currentTimeMillis() + (5 * 60 * 1000)
            if(fetched) {
                NotifyData.game.notifyFavorites(favoriteAvailableFn, favorites)
                time = NotifyData.timer.getTime(NotifyData.game.uiState.value.updatedAt)
            }

            val scheduler = NotifyAlarmManager(context!!)
            scheduler.schedule(time, favorites)
        }

    }

    private fun buildNotification(context: Context, title: String, info: String): Notification
    {
        val notification = NotificationCompat.Builder(context, "notify_a_garden")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(info)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .build()
        return notification
    }
}