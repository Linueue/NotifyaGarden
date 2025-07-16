package com.strling.notifyagarden

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
    val seeds: List<ItemValue>,
    val gears: List<ItemValue>,
    val eggs : List<ItemValue>,
)

object GameItemsAPI
{
    val URL = ""
    val client = OkHttpClient()

    suspend fun fetch(): Result<ItemsList>
    {
        val request = Request.Builder().url(URL).build()
        client.newCall(request).execute().use { response ->
            if(!response.isSuccessful)
                return Result.failure(RuntimeException("Could not fetch URL"))

            val content = response.body!!.string()
            val json = Json { ignoreUnknownKeys = true }
            val itemsList = json.decodeFromString<ItemsList>(content)
            return Result.success(itemsList)
        }
    }
}