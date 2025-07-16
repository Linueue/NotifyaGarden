package com.strling.notifyagarden

import androidx.datastore.core.DataStore
import com.strling.notifyagarden.proto.GameItemsOuterClass
import com.strling.notifyagarden.proto.copy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class ItemValue(
    val name: String = "",
    val icon: String = "",
    val color: Long = 0xFF000000,
)

@Serializable
data class ItemsList(
    val version: String,
    val seeds: List<ItemValue>,
    val gears: List<ItemValue>,
    val eggs : List<ItemValue>,
)

object GameItemsAPI
{
    val URL = "https://raw.githubusercontent.com/Linueue/NotifyaGarden/refs/heads/master/remote-config/update.json"
    val client = OkHttpClient()

    suspend fun fetch(): Result<ItemsList>
    {
        val request = Request.Builder().url(URL).build()
        return withContext(Dispatchers.IO)
        {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(RuntimeException("Could not fetch URL"))
                } else {
                    val content = response.body!!.string()
                    val json = Json { ignoreUnknownKeys = true }
                    val itemsList = json.decodeFromString<ItemsList>(content)
                    Result.success(itemsList)
                }
            }
        }
    }

    suspend fun update(version: String, dataStore: DataStore<GameItemsOuterClass.GameItems>)
    {
        val itemsList = fetch()

        if(!itemsList.isSuccess)
            return

        val gamesItemsList = itemsList.getOrNull()!!

        if(gamesItemsList.version == version)
            return

        dataStore.updateData {
            val builder = GameItemsOuterClass.GameItems.newBuilder()

            builder.setVersion(gamesItemsList.version)
            for(seed in gamesItemsList.seeds)
            {
                builder.addSeeds(GameItemsOuterClass.GameItem.newBuilder()
                    .setName(seed.name)
                    .setIcon(seed.icon)
                    .setColor(seed.color))
            }
            for(gear in gamesItemsList.gears)
            {
                builder.addGears(GameItemsOuterClass.GameItem.newBuilder()
                    .setName(gear.name)
                    .setIcon(gear.icon)
                    .setColor(gear.color))
            }
            for(egg in gamesItemsList.eggs)
            {
                builder.addEggs(GameItemsOuterClass.GameItem.newBuilder()
                    .setName(egg.name)
                    .setIcon(egg.icon)
                    .setColor(egg.color))
            }

            builder.build()
        }
    }
}