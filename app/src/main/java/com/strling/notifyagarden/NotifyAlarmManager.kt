package com.strling.notifyagarden

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class NotifyAlarmManager(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    val notifyRequestCode = 1

    fun isRunning(): Boolean
    {
        val intent = Intent(context, NotifyBroadcast::class.java).apply {
            action = "com.strling.notifyagarden.NOTIFY_ACTION"
        }
        val broadcast = PendingIntent.getBroadcast(context, notifyRequestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)

        return broadcast != null
    }

    fun schedule(time: Long, favorites: Set<String>)
    {
        ServiceState.setServiceRunning(true)
        val intent = Intent(context, NotifyBroadcast::class.java).apply {
            putStringArrayListExtra("favorites", favorites.toCollection(ArrayList()))
            action = "com.strling.notifyagarden.NOTIFY_ACTION"
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            PendingIntent.getBroadcast(
                context,
                notifyRequestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE,
            )
        )
    }

    fun cancel()
    {
        ServiceState.setServiceRunning(false)
        val intent = Intent(context, NotifyBroadcast::class.java).apply {
            action = "com.strling.notifyagarden.NOTIFY_ACTION"
        }
        val broadcast = PendingIntent.getBroadcast(
            context,
            notifyRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE,
        )
        broadcast.cancel()
        alarmManager.cancel(broadcast)
    }
}