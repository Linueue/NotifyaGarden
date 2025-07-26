package com.strling.notifyagarden

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.strling.notifyagarden.proto.GameItemsOuterClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Categories {
    SEEDS, GEARS, EGGS, EVENTS;

    override fun toString(): String {
        return when(this) {
            SEEDS -> "Seeds"
            GEARS -> "Gears"
            EGGS -> "Eggs"
            EVENTS -> "Event"
        }
    }
}

data class ShopDataView (
    val name: String = "",
    val stock: Int = 0,
    val color: Color = Color.White,
    val icon: String = "",
)

data class ShopDataViews (
    val items: HashMap<Categories, MutableList<ShopDataView>> = hashMapOf(),
)

class GrowAGarden {
    private val _uiState = MutableStateFlow(GrowAGardenData())
    val uiState = _uiState.asStateFlow()

    val api = GrowAGardenAPI()
    val favorites = mutableStateOf(setOf<String>())

    suspend fun fetchStocks(): Boolean
    {
        var fetched = false
        try {
            var data = _uiState.value
            for(i in 1..5) {
                val retrieved = api.fetchStocks()

                if(!retrieved.isSuccess)
                    break
                data = retrieved.getOrNull()!!
                if(data.updatedAt != _uiState.value.updatedAt) {
                    fetched = true
                    break
                }
                delay(5000)
            }
            _uiState.update { currentState ->
                currentState.copy(data.updatedAt, data.itemShops)
            }
        } catch (e: Exception) {
            println(e.toString())
        }

        return fetched
    }

    fun saveFavorites(dataStore: NotifyDataStore)
    {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.setFavorites(favorites.value)
        }
    }

    fun notifyFavorites(fn: (Item) -> Unit, favorites: Set<String>)
    {
        for(favorite in favorites)
        {
            for(category in Categories.entries) {
                if (uiState.value.itemShops[category]!!.items.containsKey(favorite))
                    fn(uiState.value.itemShops[category]!!.items[favorite]!!)
            }
        }
    }

    fun reset()
    {
        _uiState.update { currentState ->
            currentState.copy(updatedAt = 0)
        }
    }

    fun getData(stocks: GrowAGardenData, favorites: MutableState<Set<String>>, gameItems: GameItemsOuterClass.GameItems): ShopDataViews
    {
        val views = ShopDataViews()

        for(category in Categories.entries)
            views.items.put(category, mutableListOf())

        val addShopView: (Categories, String, Long, String) -> Unit = { category, name, color, icon ->
            val shops = stocks.itemShops[category]
            var stock = 0
            if(shops != null)
                stock = shops.items.getOrDefault(name.trim(), Item()).stock
            val view = ShopDataView(name, stock, Color(color), icon)
            if(stock != 0 && favorites.value.contains(name))
                views.items[category]!!.add(0, view)
            else
                views.items[category]!!.add(view)
        }

        for(item in gameItems.seedsList)
            addShopView(Categories.SEEDS, item.name, item.color, item.icon)

        for(item in gameItems.gearsList)
            addShopView(Categories.GEARS, item.name, item.color, item.icon)

        for(item in gameItems.eggsList)
            addShopView(Categories.EGGS, item.name, item.color, item.icon)

        for(item in gameItems.eventsList)
            addShopView(Categories.EVENTS, item.name, item.color, item.icon)

        return views
    }
}