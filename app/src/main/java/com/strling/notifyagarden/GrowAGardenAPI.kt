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
    val eventStock: List<ItemResponse>,
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
    val itemShops: HashMap<Categories, ItemShop> = hashMapOf(),
)

class GrowAGardenAPI {
    // val BASE_API_URL = "https://growagardenstock.com"
    val BASE_API_URL = "https://growagarden.gg"
    val client = OkHttpClient()

    suspend fun requestStocks(): Result<StocksResponse>
    {
        val STOCKS_API_URL = BASE_API_URL + "/api/stock"
        val request = Request.Builder().url(STOCKS_API_URL).build()

        return withContext(Dispatchers.IO)
        {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        return@withContext Result.failure(IOException("Could not fetch server"))

                    val content = response.body!!.string()
                    val json = Json { ignoreUnknownKeys = true }
                    val stocksResponse = json.decodeFromString<StocksResponse>(content)
                    Result.success(stocksResponse)
                }
            } catch(e: IOException)
            {
                Result.failure(IOException("Could not fetch URL"))
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

    suspend fun fetchStocks(): Result<GrowAGardenData>
    {
        val data = GrowAGardenData()
        val responseAPI: Result<StocksResponse> = requestStocks()

        for(category in Categories.entries)
            data.itemShops.put(category, ItemShop())

        if(!responseAPI.isSuccess)
            return Result.failure(RuntimeException("Could not fetch URL"))

        val response = responseAPI.getOrNull()!!
        data.updatedAt = response.lastApiFetch

        for(seed in response.seedsStock)
        {
            val item: Item = parse(seed)
            data.itemShops[Categories.SEEDS]!!.items[item.name] = item
        }

        for(gear in response.gearStock)
        {
            val item: Item = parse(gear)
            data.itemShops[Categories.GEARS]!!.items[item.name] = item
        }

        for(egg in response.eggStock)
        {
            val item: Item = parse(egg)
            data.itemShops[Categories.EGGS]!!.items[item.name] = item
        }

        for(event in response.eventStock)
        {
            val item: Item = parse(event)
            data.itemShops[Categories.EVENTS]!!.items[item.name] = item
        }

        return Result.success(data)
    }
}