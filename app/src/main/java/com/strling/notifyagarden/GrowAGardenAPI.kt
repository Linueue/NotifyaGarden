package com.strling.notifyagarden

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

@Serializable
data class ItemResponse (
    val name: String,
    val value: Int,
)

@Serializable
data class StocksResponse (
    val gearStock: List<ItemResponse>,
    val eggStock: List<ItemResponse>,
    val seedsStock: List<ItemResponse>,
    val lastApiFetch: Long,
)

data class Item (
    val name: String = "",
    val stock: Int = 0,
)

data class ItemShop (
    val items: HashMap<String, Item> = HashMap<String, Item>(),
)

data class GrowAGardenData (
    var updatedAt: Long = 0,
    val seedShop: ItemShop = ItemShop(),
    val gearShop: ItemShop = ItemShop(),
    val eggShop: ItemShop = ItemShop(),
)

class GrowAGardenAPI {
    // val BASE_API_URL = "https://growagardenstock.com"
    val BASE_API_URL = "https://growagarden.gg"
    val client = OkHttpClient()

    suspend fun requestStocks(): StocksResponse
    {
        val STOCKS_API_URL = BASE_API_URL + "/api/stock"
        val request = Request.Builder().url(STOCKS_API_URL).build()

        return withContext(Dispatchers.IO)
        {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful)
                    throw IOException("Could not fetch server")

                val content = response.body!!.string()
                val json = Json { ignoreUnknownKeys = true }
                val stocksResponse = json.decodeFromString<StocksResponse>(content)
                stocksResponse
            }
        }
    }

    private fun parse(item: ItemResponse): Item
    {
        val item = Item(
            name = item.name,
            stock = item.value,
        )

        return item
    }

    suspend fun fetchStocks(): GrowAGardenData
    {
        val data = GrowAGardenData()
        val response: StocksResponse = requestStocks()

        data.updatedAt = response.lastApiFetch

        for(seed in response.seedsStock)
        {
            val item: Item = parse(seed)
            data.seedShop.items[item.name] = item
        }

        for(gear in response.gearStock)
        {
            val item: Item = parse(gear)
            data.gearShop.items[item.name] = item
        }

        for(egg in response.eggStock)
        {
            val item: Item = parse(egg)
            data.eggShop.items[item.name] = item
        }

        return data
    }
}