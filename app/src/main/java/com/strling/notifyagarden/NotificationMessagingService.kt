package com.strling.notifyagarden

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MessageItem (
    @SerialName("display_name")
    val name: String,
    val quantity: Int,
)

@Serializable
data class MessageWeather (
    @SerialName("weather_name")
    val name: String,
)

@Serializable
data class MessageStock (
    val seed_stock     : List<MessageItem>?    = null,
    val gear_stock     : List<MessageItem>?    = null,
    val egg_stock      : List<MessageItem>?    = null,
    val eventshop_stock: List<MessageItem>?    = null,
    val weather        : List<MessageWeather>? = null,
)

class NotificationMessagingService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Firebase.messaging.subscribeToTopic("all")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val json = Json { ignoreUnknownKeys = true }
        val stocks = json.decodeFromString<MessageStock>(data["stocks"]!!)

        CoroutineScope(Dispatchers.IO).launch {
            val preferences = NotifyDataStore(applicationContext)
            val favorites = preferences.favorites.first()

            val favoritesFn: (List<MessageItem>?) -> Unit = { items ->
                if(items != null)
                {
                    for(item in items)
                    {
                        if(favorites.contains(item.name))
                        {
                            val itemNotify = Item(item.name, item.quantity)
                            val notification = buildNotification(
                                this@NotificationMessagingService,
                                "${itemNotify.name} is available!",
                                "${itemNotify.stock}x stock"
                            )
                            notify(notification)
                        }
                    }
                }
            }

            favoritesFn(stocks.seed_stock)
            favoritesFn(stocks.gear_stock)
            favoritesFn(stocks.egg_stock)

            if(stocks.weather != null)
            {
                for(weather in stocks.weather)
                {
                    val notification = buildNotification(
                        this@NotificationMessagingService,
                        "Weather is now ${weather.name}!",
                        ""
                    )
                    notify(notification)
                }
            }
        }
    }

    private fun notify(notification: Notification)
    {
        with(NotificationManagerCompat.from(this))
        {
            if(ActivityCompat.checkSelfPermission(this@NotificationMessagingService, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify((0..Int.MAX_VALUE).random(), notification)
            }
        }
    }

    private fun buildNotification(context: Context, title: String, info: String): Notification
    {
        val notification = NotificationCompat.Builder(context, "notify_a_garden")
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xE31E62)
            .setContentTitle(title)
            .setContentText(info)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .build()
        return notification
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }
}