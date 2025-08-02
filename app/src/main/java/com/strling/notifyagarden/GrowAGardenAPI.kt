package com.strling.notifyagarden

import androidx.annotation.Keep
import com.google.firebase.Firebase
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

@Keep
@Serializable
data class ItemResponse (
    @set:PropertyName("display_name")
    @get:PropertyName("display_name")
    var name: String = "",
    @set:PropertyName("quantity")
    @get:PropertyName("quantity")
    var value: Int = 0,
)

@Keep
@Serializable
data class WeatherResponse (
    @set:PropertyName("weather_name")
    @get:PropertyName("weather_name")
    var name: String = "",
)

@Keep
@Serializable
data class StocksResponse (
    @set:PropertyName("gear_stock")
    @get:PropertyName("gear_stock")
    var gearStock: List<ItemResponse> = listOf(),
    @set:PropertyName("egg_stock")
    @get:PropertyName("egg_stock")
    var eggStock: List<ItemResponse> = listOf(),
    @set:PropertyName("seed_stock")
    @get:PropertyName("seed_stock")
    var seedsStock: List<ItemResponse> = listOf(),
    @set:PropertyName("eventshop_stock")
    @get:PropertyName("eventshop_stock")
    var eventStock: List<ItemResponse> = listOf(),
    var weather: List<WeatherResponse> = listOf(),
    @set:PropertyName("updated_at")
    @get:PropertyName("updated_at")
    var updatedAt: Long = 0,
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
    var weather: List<WeatherResponse> = listOf(),
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
        val db = Firebase.firestore

        val document = db.collection("stocks")
            .orderBy("updated_at", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        if(document.isEmpty)
            return Result.failure(RuntimeException("Could not get values from database"))

        val documentResponse = document.documents.first()
        val stocks = documentResponse.toObject(StocksResponse::class.java)

        if(stocks == null)
            return Result.failure(RuntimeException("Could not get values"))

        var data = GrowAGardenData()

        data.weather = stocks!!.weather
        data.updatedAt = stocks!!.updatedAt

        for(category in Categories.entries)
            data.itemShops.put(category, ItemShop())

        for(seed in stocks!!.seedsStock)
        {
            val item: Item = parse(seed)
            data.itemShops[Categories.SEEDS]!!.items[item.name] = item
        }

        for(gear in stocks!!.gearStock)
        {
            val item: Item = parse(gear)
            data.itemShops[Categories.GEARS]!!.items[item.name] = item
        }

        for(egg in stocks!!.eggStock)
        {
            val item: Item = parse(egg)
            data.itemShops[Categories.EGGS]!!.items[item.name] = item
        }

        for(event in stocks!!.eventStock)
        {
            val item: Item = parse(event)
            data.itemShops[Categories.EVENTS]!!.items[item.name] = item
        }

        return Result.success(data)
    }
}